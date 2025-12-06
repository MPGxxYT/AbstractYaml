# Config System 2.0

A modern, type-safe, immutable configuration system for Bukkit/Spigot plugins.

## Table of Contents

- [Installation](#installation) ⚠️ **Start here!**
- [Quick Start](#quick-start)
- [Generated Type-Safe Wrappers](#generated-type-safe-wrappers) - Recommended!
- [Troubleshooting](#troubleshooting-generatewrapper)
- [API Documentation](#api-documentation)
- [Migration Guide](MIGRATION_GUIDE.md)

## Features

- ✅ **Fully Type-Safe** - No casting, no `@SuppressWarnings`
- ✅ **Immutable** - Thread-safe by default
- ✅ **Simple API** - Minimal boilerplate
- ✅ **Auto-Registration** - Automatic schema discovery and loading
- ✅ **Auto-Generation** - Creates YAML files with comments automatically
- ✅ **Validation** - Catch errors at load time, not runtime
- ✅ **Sections** - Organize related values
- ✅ **Nested Sections** - **NEW!** Unlimited nesting depth with `.section().section()...`
- ✅ **Lists** - Type-safe lists (String, Integer, Double)
- ✅ **Generated Wrappers** - **IMPROVED!** Auto-generated type-safe wrappers with `Wrapper` suffix in `.wrappers` package
- ✅ **Nested Wrapper Classes** - **NEW!** Deeply nested configs generate matching nested static classes

## Installation

### Step 1: Add AbstractYaml Dependency

Add to your project's `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>me.mortaldev</groupId>
        <artifactId>AbstractYaml</artifactId>
        <version>1.0-SNAPSHOT</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```

### Step 2: Add Config Wrapper Maven Plugin (Required for @GenerateWrapper)

**⚠️ IMPORTANT:** If you want to use `@GenerateWrapper` for type-safe config access, you **MUST** add the wrapper generator plugin to your `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>me.mortaldev</groupId>
            <artifactId>config-wrapper-maven-plugin</artifactId>
            <version>1.0-SNAPSHOT</version>
            <executions>
                <execution>
                    <phase>process-classes</phase>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

**Without this plugin:**
- `@GenerateWrapper` will not work
- Type-safe wrapper classes will not be generated

**With this plugin:**
- Wrapper classes are generated after compilation (can instantiate schemas)
- No runtime overhead
- Full IDE autocomplete support
- Compile-time type safety
- Runs automatically during build

### Step 3: Build AbstractYaml and Plugin

Before using in your project, build and install both AbstractYaml and the plugin to your local Maven repository:

```bash
# Build AbstractYaml
cd AbstractYaml
mvn clean install

# Build the wrapper generator plugin
cd ../config-wrapper-maven-plugin
mvn clean install
```

You only need to do this once, or whenever AbstractYaml/plugin is updated.

## Quick Start

### 1. Define Your Schema

```java
public class MyConfigSchema extends ConfigSchema {
    public MyConfigSchema() {
        super("config");

        // Add a file header (optional)
        header("""
            ====================================================================
                             My Plugin Configuration
            ====================================================================
            This is your main configuration file.
            ====================================================================""");

        // Define values
        value(new ConfigValue.Int("max-players", 10, "Maximum players"));
        value(new ConfigValue.String("server-name", "My Server", "Server name"));
        value(new ConfigValue.Boolean("debug-mode", false, "Enable debug logging"));

        // Use sections for organization
        section("economy")
            .header("Economy Settings")
            .doubleValue("coin-multiplier", 1.0, Validator.min(0.1), "Coin earn multiplier")
            .intValue("starting-balance", 100, Validator.min(0), "Starting balance");

        section("features")
            .header("Feature Toggles")
            .boolValue("pvp-enabled", true, "Enable PvP")
            .boolValue("trading-enabled", true, "Enable trading");
    }
}
```

### 2. Load Your Config

```java
public class MyPlugin extends JavaPlugin {
    private Config config;

    @Override
    public void onEnable() {
        // Load config
        config = ConfigLoader.builder(this)
            .schema(new MyConfigSchema())
            .load();  // Auto-generates file if missing!

        getLogger().info("Max players: " + config.getInt("max-players"));
    }
}
```

### 3. Read Values

```java
// Direct access (type-safe!)
int maxPlayers = config.getInt("max-players");
String serverName = config.getString("server-name");
boolean debugMode = config.getBoolean("debug-mode");

// Use sections for cleaner code
ConfigSection economy = config.section("economy");
double multiplier = economy.getDouble("coin-multiplier");
int startingBalance = economy.getInt("starting-balance");
```

### 4. Update Values

```java
// Configs are immutable - updating returns a new config
Config updated = config.with("max-players", 20);
updated.save(new File(getDataFolder(), "config.yml"));

// Or update through a section
ConfigSection economy = config.section("economy");
Config updated = economy.set("coin-multiplier", 1.5);
```

## Complete Example

```java
// 1. Define schema
public class AbilitiesSchema extends ConfigSchema {
    public AbilitiesSchema() {
        super("abilities");

        section("fireball")
            .intValue("cooldown", 10, Validator.range(0, 300), "Cooldown in seconds")
            .doubleValue("damage", 5.0, Validator.min(0.0), "Damage dealt")
            .intValue("range", 50, "Maximum range");

        section("teleport")
            .intValue("cooldown", 30, "Cooldown in seconds")
            .intValue("max-distance", 100, "Maximum teleport distance");
    }
}

// 2. Load and use
public class MyPlugin extends JavaPlugin {
    private Config abilities;

    @Override
    public void onEnable() {
        abilities = ConfigLoader.builder(this)
            .schema(new AbilitiesSchema())
            .load();

        ConfigSection fireball = abilities.section("fireball");
        getLogger().info("Fireball cooldown: " + fireball.getInt("cooldown"));
        getLogger().info("Fireball damage: " + fireball.getDouble("damage"));
    }

    public void updateFireballCooldown(int newCooldown) {
        ConfigSection fireball = abilities.section("fireball");
        abilities = fireball.set("cooldown", newCooldown);
        abilities.save(new File(getDataFolder(), "abilities.yml"));
    }
}
```

## Validation

```java
// Built-in validators
section("player")
    .intValue("level", 1, Validator.range(1, 100), "Player level")
    .stringValue("name", "Steve", Validator.notEmpty(), "Player name")
    .doubleValue("balance", 0.0, Validator.min(0.0), "Account balance");

// String validators
value(new ConfigValue.String(
    "item-code",
    "ITEM_001",
    Validator.matches("^ITEM_\\d{3}$"),
    "Item code (must match ITEM_XXX)"
));

// List validators
value(new ConfigValue.StringList(
    "allowed-commands",
    List.of("help", "info"),
    Validator.minSize(1).and(Validator.allStringsNotEmpty()),
    "Allowed commands"
));

// Custom validators
Validator<Integer> multipleOf5 = Validator.of(
    n -> n % 5 == 0,
    "Value must be a multiple of 5"
);

section("timers")
    .intValue("interval", 10, Validator.range(5, 60).and(multipleOf5), "Timer interval");
```

## Working with Lists

```java
// String lists
section("chat")
    .header("Chat Settings")
    .stringListValue(
        "blocked-words",
        List.of("spam", "hack"),
        Validator.minSize(1),
        "Words to block in chat");

List<String> blockedWords = config.section("chat").getStringList("blocked-words");

// Integer lists
section("rewards")
    .header("Reward Configuration")
    .intListValue(
        "levels",
        List.of(1, 5, 10, 25, 50),
        Validator.allIntegersInRange(1, 100),
        "Levels that give rewards");

List<Integer> levels = config.section("rewards").getIntList("levels");

// Double lists
section("difficulty")
    .header("Difficulty Multipliers")
    .doubleListValue(
        "multipliers",
        List.of(0.5, 1.0, 1.5, 2.0),
        Validator.allDoublesInRange(0.1, 5.0),
        "Difficulty multipliers");

List<Double> multipliers = config.section("difficulty").getDoubleList("multipliers");
```

## Nested Sections

**NEW:** Sections can now be nested multiple levels deep!

```java
public class ScoringConfigSchema extends ConfigSchema {
    public ScoringConfigSchema() {
        super("scoring");

        section("scoring")
            .header("Win Conditions")
            .intValue("points-to-win", 3, Validator.min(1), "First team to X points wins")
            .intValue("points-per-capture", 1, Validator.min(1), "Points per flag capture")
            .header("Rewards")
            .section("rewards")             // ← Nested section
                .section("capture-flag")    // ← Nested even deeper!
                    .intValue("coins", 10, Validator.min(0), "Coins for capturing")
                    .intValue("xp", 15, Validator.min(0), "XP for capturing")
                    .parent()               // ← Return to parent section
                .section("win-game")
                    .intValue("coins", 30, Validator.min(0), "Coins for winning")
                    .intValue("xp", 20, Validator.min(0), "XP for winning")
                    .parent()
                .section("lose-game")
                    .intValue("coins", 5, Validator.min(0), "Coins for losing")
                    .intValue("xp", 5, Validator.min(0), "XP for losing");
    }
}
```

This creates paths like:
- `scoring.points-to-win`
- `scoring.points-per-capture`
- `scoring.rewards.capture-flag.coins`
- `scoring.rewards.capture-flag.xp`
- `scoring.rewards.win-game.coins`
- etc.

**Access nested values:**
```java
// Access deeply nested values
ConfigSection rewards = config.section("scoring").section("rewards");
int captureCoins = rewards.section("capture-flag").getInt("coins");

// Or use the full path
int captureCoins = config.getInt("scoring.rewards.capture-flag.coins");
```

## Advanced Features

### Reload Config

```java
ConfigLoader loader = ConfigLoader.builder(plugin)
    .schema(new MyConfigSchema())
    .build();

// Later...
Config reloaded = loader.reload();
```

### Validation on Load

```java
// Automatically validates when loading (default: true)
Config config = ConfigLoader.builder(plugin)
    .schema(schema)
    .validateOnLoad(true)  // Throws exception if invalid
    .load();

// Or validate manually
ValidationResult result = config.validate();
if (!result.isValid()) {
    result.errors().forEach(error ->
        plugin.getLogger().severe("Config error: " + error));
}
```

### Custom File Location

```java
Config config = ConfigLoader.builder(plugin)
    .schema(schema)
    .file(new File(plugin.getDataFolder(), "custom/path/config.yml"))
    .load();

// Or by name (relative to data folder)
Config config = ConfigLoader.builder(plugin)
    .schema(schema)
    .file("custom-config.yml")
    .load();
```

### Disable Auto-Generation

```java
Config config = ConfigLoader.builder(plugin)
    .schema(schema)
    .autoGenerate(false)  // Don't create file if missing
    .load();
```

## Comparison: Old vs New

### Old System
```java
// 105 lines of boilerplate
public class MyConfig extends AbstractConfig {
    private static class Singleton { ... }
    public static MyConfig getInstance() { ... }

    @ConfigContainer("section1")
    private Section1Container section1;

    @Override public void log(String msg) { ... }
    @Override public String getName() { ... }
    @Override public JavaPlugin getMain() { ... }
    @Override public void loadData() { loadAllContainers(); }

    // Plus separate container classes...
}

// Container class (45 lines each!)
public class Section1Container extends ConfigValueContainer {
    private ConfigValue<Integer> value1;
    private ConfigValue<String> value2;

    @Override protected void load() { ... }

    public int getValue1() { return value1.getValue(); }
    public String getValue2() { return value2.getValue(); }
}
```

### New System
```java
// 15 lines total!
public class MyConfigSchema extends ConfigSchema {
    public MyConfigSchema() {
        super("config");

        section("section1")
            .intValue("value1", 10, "Description")
            .stringValue("value2", "default", "Description");
    }
}

// Load and use
Config config = ConfigLoader.builder(plugin)
    .schema(new MyConfigSchema())
    .load();

int value1 = config.section("section1").getInt("value1");
```

**Result: 75% less code!**

## Migration Guide

See [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md) for detailed migration instructions.

## API Documentation

### ConfigValue Types

- `ConfigValue.Int` - Integer values
- `ConfigValue.Double` - Double values
- `ConfigValue.String` - String values
- `ConfigValue.Boolean` - Boolean values
- `ConfigValue.StringList` - List of strings
- `ConfigValue.IntList` - List of integers
- `ConfigValue.DoubleList` - List of doubles

### Validator Methods

**Numeric:**
- `Validator.range(min, max)` - Value must be in range (inclusive)
- `Validator.min(min)` - Value must be at least min
- `Validator.max(max)` - Value must be at most max

**String:**
- `Validator.notEmpty()` - String cannot be empty
- `Validator.matches(regex)` - String must match pattern
- `Validator.length(min, max)` - String length in range

**List:**
- `Validator.minSize(n)` - List must have at least n elements
- `Validator.maxSize(n)` - List cannot exceed n elements
- `Validator.sizeRange(min, max)` - List size in range
- `Validator.noDuplicates()` - List cannot contain duplicates
- `Validator.allStringsNotEmpty()` - All strings must be non-empty
- `Validator.allStringsMatch(regex)` - All strings must match pattern
- `Validator.allIntegersInRange(min, max)` - All integers in range
- `Validator.allDoublesInRange(min, max)` - All doubles in range

**General:**
- `Validator.notNull()` - Value cannot be null
- `Validator.oneOf(values...)` - Value must be one of the allowed values
- `Validator.of(predicate, error)` - Custom validator from predicate

**Chaining:**
```java
Validator<Integer> validator = Validator.range(0, 100)
    .and(Validator.of(n -> n % 5 == 0, "Must be multiple of 5"));
```

## Auto-Registration with ConfigManager

For managing multiple configs, use the auto-registration system:

### Step 1: Annotate Your Schemas

```java
@RegisterConfig(priority = 10)  // Lower priority = loads first
public class AbilitiesConfigSchema extends ConfigSchema {
    public AbilitiesConfigSchema() {
        super("abilities");
        // ... define values
    }
}

@RegisterConfig(priority = 20)
public class CTFConfigSchema extends ConfigSchema {
    public CTFConfigSchema() {
        super("ctf");
        // ... define values
    }
}
```

### Step 2: Register in Plugin

```java
public class MyPlugin extends JavaPlugin {
    // Register all schemas in static block
    static {
        ConfigRegistry.register(AbilitiesConfigSchema.class);
        ConfigRegistry.register(CTFConfigSchema.class);
        ConfigRegistry.register(UltimateConfigSchema.class);
    }

    @Override
    public void onEnable() {
        // Initialize and load all configs
        ConfigManager.init(this);
        ConfigManager.getInstance().loadAll();
    }
}
```

### Step 3: Access Anywhere

```java
// Get config by name
Config abilities = ConfigManager.getInstance().get("abilities");
int cooldown = abilities.getInt("keraunos.cooldown");

// Or get section directly
ConfigSection keraunos = ConfigManager.getInstance()
    .getSection("abilities", "keraunos");
int cooldown = keraunos.getInt("cooldown");
```

### Reload Commands

```java
ConfigManager cm = ConfigManager.getInstance();

// Reload all configs
cm.reloadAll();

// Reload specific config
cm.reload("abilities");

// Check which configs are loaded
Set<String> names = cm.getConfigNames();
```

### Benefits

- **Centralized** - All configs managed in one place
- **Priority control** - Load order specified with `priority` parameter
- **Type-safe** - Compile-time checking of schema classes
- **Simple** - One-time registration, automatic loading
- **No dependencies** - Uses manual registration (can add classpath scanning later)

See `example/AutoRegisterExample.java` for a complete working example.

## Generated Type-Safe Wrappers

Eliminate string-based config access with auto-generated wrapper classes!

**⚠️ PREREQUISITE:** You **MUST** configure annotation processing in your `pom.xml` first! See [Installation](#installation) section above.

### Problem: String-Based Access

```java
// Error-prone - typos cause runtime errors
int cooldown = config.getInt("keraunos.cooldown");
double radius = config.getDouble("keraunos.raduis");  // Typo! Runtime error!
```

### Solution: @GenerateWrapper

Add `@GenerateWrapper` to your schema:

```java
@RegisterConfig(priority = 10)
@GenerateWrapper  // ← Add this!
public class AbilitiesConfigSchema extends ConfigSchema {
    public AbilitiesConfigSchema() {
        super("abilities");

        section("keraunos")
            .intValue("cooldown", 10, "Cooldown in seconds")
            .doubleValue("radius", 3.0, "Effect radius");
    }
}
```

**Compiling automatically generates:**

```java
// Generated in me.mortaldev.yourplugin.configs.wrappers package
public final class AbilitiesWrapper {
    private static final String CONFIG_NAME = "abilities";

    public static class Keraunos {
        private static ConfigSection section;

        /** Cooldown in seconds */
        public static int cooldown() {
            return section().getInt("cooldown");
        }

        /** Effect radius */
        public static double radius() {
            return section().getDouble("radius");
        }

        private static ConfigSection section() {
            if (section == null) {
                section = ConfigManager.getInstance().getSection(CONFIG_NAME, "keraunos");
            }
            return section;
        }

        static void reload() { section = null; }
    }

    /** Reloads all cached config sections */
    public static void reload() {
        Keraunos.reload();
    }
}
```

### Usage - Type-Safe!

```java
// No strings! IDE autocomplete works! Compile-time safety!
int cooldown = AbilitiesWrapper.Keraunos.cooldown();
double radius = AbilitiesWrapper.Keraunos.radius();

// Typo = compile error (caught immediately!)
// double radius = AbilitiesWrapper.Keraunos.raduis();  // Doesn't compile!
```

### Nested Sections in Wrappers

Nested sections generate nested static classes:

```java
@RegisterConfig
@GenerateWrapper
public class ScoringConfigSchema extends ConfigSchema {
    public ScoringConfigSchema() {
        super("scoring");

        section("scoring")
            .section("rewards")
                .section("capture-flag")
                    .intValue("coins", 10, Validator.min(0), "Coins for capturing")
                    .intValue("xp", 15, Validator.min(0), "XP for capturing");
    }
}
```

**Generates:**

```java
public final class ScoringWrapper {
    public static class Scoring {
        public static class Rewards {
            public static class CaptureFlag {
                /** Coins for capturing */
                public static int coins() {
                    return section().getInt("coins");
                }

                /** XP for capturing */
                public static int xp() {
                    return section().getInt("xp");
                }

                private static ConfigSection section() { ... }
                static void reload() { section = null; }
            }

            static void reload() { CaptureFlag.reload(); }
        }

        static void reload() { Rewards.reload(); }
    }

    public static void reload() { Scoring.reload(); }
}
```

**Usage:**
```java
// Clean, type-safe nested access!
int coins = ScoringWrapper.Scoring.Rewards.CaptureFlag.coins();
int xp = ScoringWrapper.Scoring.Rewards.CaptureFlag.xp();
```

### Benefits

- ✅ **No typos** - IDE catches errors at compile-time
- ✅ **Autocomplete** - Type `Abilities.Keraunos.` and see all options
- ✅ **Refactor-safe** - Rename works across entire codebase
- ✅ **Documentation** - JavaDoc from schema comments
- ✅ **No plugin running** - Generates during compilation

### Advanced Options

```java
// Custom wrapper name
@GenerateWrapper(value = "CTFSettings")
public class CTFConfigSchema extends ConfigSchema { }
// Generates: CTFSettingsWrapper.java in ...configs.wrappers package
// Note: "Wrapper" suffix is automatically added

// Custom package (wrappers subpackage is still added)
@GenerateWrapper(packageName = "me.mortaldev.api")
public class APIConfigSchema extends ConfigSchema { }
// Generates: me.mortaldev.api.wrappers.APIWrapper.java

// No reload methods
@GenerateWrapper(generateReload = false)
public class StaticConfigSchema extends ConfigSchema { }

// Non-final class (allows extension, though not recommended)
@GenerateWrapper(makeFinal = false)
public class ExtendableConfigSchema extends ConfigSchema { }
```

### Wrapper Naming Convention

**Automatic naming:**
- `AbilitiesConfigSchema` → `AbilitiesWrapper` (in `.wrappers` subpackage)
- `CTFConfigSchema` → `CTFWrapper` (in `.wrappers` subpackage)
- `MainConfigSchema` → `MainWrapper` (in `.wrappers` subpackage)

**All wrappers:**
- Automatically get "Wrapper" suffix to avoid naming conflicts
- Are placed in a `.wrappers` subpackage
- Are final by default (can be changed with `makeFinal = false`)

### Reload Pattern

```java
// Reload config
ConfigManager.getInstance().reload("abilities");

// Clear wrapper cache
AbilitiesWrapper.reload();

// Get fresh values
int newCooldown = AbilitiesWrapper.Keraunos.cooldown();
```

See `example/GeneratedWrapperExample.java` for complete examples.

### Troubleshooting @GenerateWrapper

**Generated classes not appearing?**

1. Make sure you've built AbstractYaml and the plugin first:
   ```bash
   cd AbstractYaml && mvn clean install
   cd ../config-wrapper-maven-plugin && mvn clean install
   ```

2. Verify the plugin is configured in your `pom.xml` (see [Installation](#installation))

3. Clean and rebuild your project: `mvn clean compile`

4. Check the build output for generation messages:
   ```
   [INFO] Generating config wrapper classes...
   [INFO] Found 3 schema(s) to process
   [INFO] Generated AbilitiesWrapper.java
   [INFO] Generated CTFWrapper.java
   [INFO] Generated PhaseWrapper.java
   ```

5. Look for generated files in `src/main/java/[your-package]/wrappers/`
   - Example: If your schemas are in `me.mortaldev.yourplugin.configs`
   - Wrappers will be in `me.mortaldev.yourplugin.configs.wrappers`
   - Files will be named `*Wrapper.java`

**Generated classes have wrong package?**

Use the `packageName` parameter:
```java
@GenerateWrapper(packageName = "me.mortaldev.yourplugin.config")
public class YourConfigSchema extends ConfigSchema { }
```

**Plugin not running?**

Check that you have the `<executions>` block in your plugin configuration:
```xml
<executions>
    <execution>
        <phase>process-classes</phase>
        <goals>
            <goal>generate</goal>
        </goals>
    </execution>
</executions>
```

## Design Philosophy

1. **Type Safety** - Eliminate runtime type errors with compile-time checks
2. **Immutability** - Thread-safe by default, easier to reason about
3. **Simplicity** - Less code = fewer bugs
4. **Fail Fast** - Catch errors at load time, not during gameplay
5. **Minimal Reflection** - Fast and predictable performance

## License

Same as parent project.
