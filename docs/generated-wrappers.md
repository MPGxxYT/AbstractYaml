# Generated wrappers

String paths are easy to mistype, and a typo only shows up when the line runs. AbstractYaml includes an annotation processor that turns a schema into a class of static accessors at compile time, so a typo becomes a compile error and your IDE can autocomplete config values:

```java
int cooldown = config.getInt("fireball.cooldwon");    // fails at runtime
int cooldown = AbilitiesWrapper.Fireball.cooldown();   // checked by the compiler
```

## Setup

The processor ships in the AbstractYaml jar. Add the library to the compiler's annotation processor path as well as your dependencies.

### Maven

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <version>3.13.0</version>
      <configuration>
        <annotationProcessorPaths>
          <path>
            <groupId>com.github.MPGxxYT</groupId>
            <artifactId>AbstractYaml</artifactId>
            <version>TAG</version>
          </path>
        </annotationProcessorPaths>
      </configuration>
    </plugin>
  </plugins>
</build>
```

Wrappers are written to `target/generated-sources/annotations`, which Maven and IntelliJ add to the build automatically.

> [!NOTE]
> On JDK 21 and older, javac also finds the processor on the normal classpath, so builds work without `annotationProcessorPaths`. From JDK 23, javac only runs processors that are configured explicitly. If you already list other processors (Lombok, for example), add AbstractYaml to the same `annotationProcessorPaths` block.

### Gradle

```groovy
dependencies {
  implementation 'com.github.MPGxxYT:AbstractYaml:TAG'
  annotationProcessor 'com.github.MPGxxYT:AbstractYaml:TAG'
}
```

Kotlin DSL:

```kotlin
dependencies {
  implementation("com.github.MPGxxYT:AbstractYaml:TAG")
  annotationProcessor("com.github.MPGxxYT:AbstractYaml:TAG")
}
```

Wrappers are written to `build/generated/sources/annotationProcessor/java/main`.

## Marking a schema

Add `@GenerateWrapper` to any schema that should get a wrapper:

```java
@RegisterConfig(priority = 10)
@GenerateWrapper
public class AbilitiesConfigSchema extends ConfigSchema {
  public AbilitiesConfigSchema() {
    super("abilities");

    section("fireball")
        .intValue("cooldown", 10, Validator.range(1, 300), "Seconds between casts")
        .doubleValue("radius", 3.0, "Explosion radius in blocks")
        .section("damage")
        .intMapValue("by-mob", Map.of("zombie", 4), "Damage per mob type");
  }
}
```

The next build generates `com.example.config.wrappers.AbilitiesWrapper`:

```java
int cooldown = AbilitiesWrapper.Fireball.cooldown();
double radius = AbilitiesWrapper.Fireball.radius();
Map<String, Integer> damage = AbilitiesWrapper.Fireball.Damage.byMob();
```

Wrappers are regenerated on every build, so they always match the schema. Don't commit them and don't edit them.

## How schemas are read

The processor reads the schema's constructor from the source code instead of running it. It understands:

- `super("name")`, or the `value` of `@RegisterConfig` when set;
- `section(...)`, nested `.section(...)`, `.parent()` and `.header(...)`;
- every value method (`intValue`, `stringListValue`, `intMapValue`, ...);
- `value(new ConfigValue.Int("path", ...))` for top-level values;
- section builders stored in local variables (`SectionBuilder rewards = section("rewards");`).

Keys and section names must be string literals or `static final String` constants. A key built at runtime is reported as a compile error at that line. Calls spread over several lines are fine.

## Naming

| From | Becomes |
| --- | --- |
| Schema class `AbilitiesConfigSchema` or `AbilitiesSchema` | `AbilitiesWrapper` in a `wrappers` package next to the schema |
| Section `capture-flag` | nested class `CaptureFlag` |
| Key `allow-block-breaking` | method `allowBlockBreaking()` |
| Key `class`, `2x` | `class_()`, `_2x()` |
| Section with the same name as its parent | class name with `Section` appended |

Two keys in the same section that give the same method name (`max-hp` and `max_hp`) are reported as a compile error.

## Options

```java
@GenerateWrapper(
    value = "AbilitySettings",           // class name (default: <Name>Wrapper)
    packageName = "com.example.api",     // package (default: <schema package>.wrappers)
    makeFinal = false,                   // default: true
    generateReload = false)              // default: true
```

## Behaviour

- Accessors read the config loaded by the shared `ConfigManager` on every call, so they always reflect the latest reload. `ConfigManager.init(...)` and `loadAll()` must have run before the first call.
- The generated `reload()` method does nothing. It exists so code written for earlier, caching wrappers still compiles.
- If a class with the wrapper's name already exists in your sources, the processor leaves it alone and prints a note. This keeps hand-written or customised wrappers working. Delete the file to switch to the generated one.

> [!TIP]
> Wrappers use the shared manager, so tests that call them need one. Build a manager with in-memory sources and install it with `ConfigManager.init(manager)`; see [Testing](testing.md#testing-code-that-uses-configmanager).

## Moving from the standalone generator

Earlier versions used a separate `SourceBasedWrapperGenerator` tool that wrote wrappers into `src/main/java`. To switch:

1. Add the processor setup above to your build.
2. Delete the wrapper files it generated (the `wrappers` packages). Keep any you edited by hand; the processor skips classes that already exist.
3. Build. Method and class names follow the same rules as before, so calling code compiles unchanged.
