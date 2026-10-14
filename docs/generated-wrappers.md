# Generated wrappers

String paths are easy to mistype, and a typo only shows up when the line runs. A wrapper generator can turn a schema into a class of static accessors, so a typo becomes a compile error:

```java
int cooldown = config.getInt("fireball.cooldwon");  // fails at runtime
int cooldown = AbilitiesWrapper.Fireball.cooldown(); // checked by the compiler
```

> [!NOTE]
> The generator is a separate project, `config-wrapper-maven-plugin`, and is not published yet. You need a local copy to use it.

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
        .doubleValue("radius", 3.0, "Explosion radius in blocks");
  }
}
```

The generator reads the schema's source text rather than running it, so:

- keep one builder call per line, as in the example;
- write the config name as a string literal in `super("...")`;
- use `.section(...)` and `.parent()` for nesting, as described in [Schemas](schemas.md#sections).

## Running the generator

Build the generator once, then point it at your plugin's project folder:

```bash
cd config-wrapper-maven-plugin
mvn compile
java -cp target/classes me.mortaldev.maven.config.SourceBasedWrapperGenerator /path/to/MyPlugin
```

It scans `src/main/java` for classes annotated with `@GenerateWrapper` and writes one wrapper per schema:

- the class name drops `ConfigSchema` or `Schema` and adds `Wrapper` (`AbilitiesConfigSchema` becomes `AbilitiesWrapper`);
- the wrapper goes in a `wrappers` package next to the schema (`com.example.config.wrappers`);
- a wrapper newer than its schema is skipped, so run the generator again after editing a schema.

Commit the generated files with your plugin's source. They are plain Java and compile like any other class.

> [!IMPORTANT]
> The options on `@GenerateWrapper` (`value`, `packageName`, `generateReload`, `makeFinal`) are not read by the current generator. Names and packages always follow the rules above.

## What gets generated

For the schema above:

```java
public final class AbilitiesWrapper {
    private static final String CONFIG_NAME = "abilities";

    public static class Fireball {
        private static ConfigSection section;

        public static int cooldown() {
            return section().getInt("cooldown");
        }

        public static double radius() {
            return section().getDouble("radius");
        }

        private static ConfigSection section() {
            if (section == null) {
                section = ConfigManager.getInstance().getSection(CONFIG_NAME, "fireball");
            }
            return section;
        }

        static void reload() {
            section = null;
        }
    }

    public static void reload() {
        Fireball.reload();
    }
}
```

Nested sections become nested classes: `ScoringWrapper.Scoring.Rewards.CaptureFlag.coins()`.

## Reloading

Wrappers cache the section they read from. After reloading a config, clear that cache:

```java
ConfigManager.getInstance().reload("abilities");
AbilitiesWrapper.reload();
```

Wrappers use the shared `ConfigManager`, so `ConfigManager.init(...)` and `loadAll()` must have run before the first call.
