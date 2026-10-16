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
            <version>v3.0.0</version>
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
  implementation 'com.github.MPGxxYT:AbstractYaml:v3.0.0'
  annotationProcessor 'com.github.MPGxxYT:AbstractYaml:v3.0.0'
}
```

Kotlin DSL:

```kotlin
dependencies {
  implementation("com.github.MPGxxYT:AbstractYaml:v3.0.0")
  annotationProcessor("com.github.MPGxxYT:AbstractYaml:v3.0.0")
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

Use the wrapper from where the build writes it; don't copy it into `src/main/java`. The processor skips classes that already exist in your sources, so a copy stops updating when the schema changes. If IntelliJ marks the wrapper's import red, reload the Maven or Gradle project so it picks up the generated sources folder.

## Generating wrappers while other code doesn't compile

The processor runs before javac checks types, so the wrapper is written even when other files have type errors (missing classes, wrong types, calls to a wrapper that doesn't exist yet). The build still reports failure. Code that only failed because the wrapper was missing compiles on the next build.

A syntax error (a missing brace, an unclosed method) stops javac while it parses, before the processor runs, so no wrapper is generated. Fix it first, or run the processor on the schema file alone (below).

### Maven

Tell the compiler plugin not to fail the build on compile errors:

```bash
mvn compile -Dmaven.compiler.failOnError=false
```

The errors are still printed, but the build ends with `BUILD SUCCESS`. With a syntax error it also reports `BUILD SUCCESS` without generating anything, so check that the wrapper exists in `target/generated-sources/annotations`.

In IntelliJ, open the Maven tool window, right-click **Lifecycle > compile**, choose **Modify Run Configuration...** and set the command line to `compile -Dmaven.compiler.failOnError=false`. The saved configuration appears under **Run Configurations** in the same window.

Avoid setting `maven.compiler.failOnError` in the pom or in IntelliJ's Maven runner properties: every build, including `package` and `install`, would then pass with broken code.

The flag only covers errors in your code. A JDK error such as `invalid target release: 21` means Maven runs on an older JDK than the pom targets; in IntelliJ, set **File > Project Structure > Project > SDK** to a matching JDK.

### Gradle

Gradle has no command-line switch for this. Add a property check to the build script:

```groovy
tasks.withType(JavaCompile).configureEach {
  options.failOnError = !project.hasProperty('ignoreCompileErrors')
}
```

Kotlin DSL:

```kotlin
tasks.withType<JavaCompile>().configureEach {
  options.isFailOnError = !project.hasProperty("ignoreCompileErrors")
}
```

Normal builds still fail on errors. To generate wrappers anyway:

```bash
gradle compileJava -PignoreCompileErrors
```

As with Maven, the build reports success even after a syntax error, so check that the wrapper exists in `build/generated/sources/annotationProcessor/java/main`.

### Processing the schema file alone

`javac -proc:only` runs annotation processors without compiling anything. Pass it only the schema files and the rest of the project is never read, so this also works when other files have syntax errors. With Maven, write the classpath to a file first:

```bash
mvn -q dependency:build-classpath -Dmdep.outputFile=cp.txt
javac -proc:only -cp "$(cat cp.txt)" -s target/generated-sources/annotations src/main/java/com/example/config/AbilitiesConfigSchema.java
```

On JDK 23 and newer, also pass `-processorpath` with the path to the AbstractYaml jar. The schema's own imports must still resolve.

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
