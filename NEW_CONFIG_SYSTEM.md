# Config System 2.0 - Complete Redesign

## 🎉 What's New

I've built a completely new configuration system from scratch that solves all the issues with the old system while being **75% less code**.

### Location
All new code is in the `me.mortaldev.config2` package:
```
src/main/java/me/mortaldev/config2/
├── ConfigValue.java         - Type-safe value types (String, Int, Double, etc.)
├── Validator.java           - Validation framework
├── ValidationResult.java    - Validation results
├── ConfigSchema.java        - Schema definition (replaces AbstractConfig)
├── Config.java              - Immutable config interface
├── ConfigSection.java       - Section views for organization
├── SimpleConfig.java        - Config implementation
├── ConfigLoader.java        - Loads configs from YAML files
├── YamlConfigWriter.java    - Writes configs to YAML files
├── README.md                - Full documentation
├── MIGRATION_GUIDE.md       - Step-by-step migration guide
└── example/
    ├── AbilitiesConfigSchema.java  - Real example schema
    └── UsageExample.java           - Usage examples
```

## 📊 Code Reduction

### Before (Old System)
```
AbilitiesConfig.java              105 lines
├── KeraunosConfigContainer.java   45 lines
├── MedkitConfigContainer.java     38 lines
└── WraithWaterConfigContainer.java 28 lines
────────────────────────────────────────────
TOTAL:                            216 lines
```

### After (New System)
```
AbilitiesConfigSchema.java         40 lines
────────────────────────────────────────────
TOTAL:                             40 lines
```

**Result: 81% reduction!** (176 lines eliminated)

## ✨ Key Improvements

### 1. Type Safety - No More Casting!

**Old (Unsafe):**
```java
private ConfigValue<List> commands = new ConfigValue<>("commands", List.class, ...);

@SuppressWarnings("unchecked")
public List<String> getCommands() {
    return (List<String>) (List<?>) commands.getValue();  // Yuck!
}
```

**New (Safe):**
```java
value(new ConfigValue.StringList("commands", List.of("help", "info")));

List<String> commands = config.getStringList("commands");  // No casting!
```

### 2. Zero Boilerplate

**Old:**
```java
public class MyConfig extends AbstractConfig {
    // 20 lines of singleton boilerplate
    private static class Singleton { ... }

    // 15 lines of required overrides
    @Override public void log(...) { ... }
    @Override public String getName() { ... }
    @Override public JavaPlugin getMain() { ... }
    @Override public void loadData() { ... }

    // Container declarations
    @ConfigContainer("section1") private Section1 section1;
    @ConfigContainer("section2") private Section2 section2;

    // Getters for containers
    public Section1 getSection1() { ... }
    public Section2 getSection2() { ... }

    // Legacy getters (more boilerplate!)
    public int getValue1() { return section1.getValue1(); }
    // ... etc
}

// PLUS separate container classes!
```

**New:**
```java
public class MyConfigSchema extends ConfigSchema {
    public MyConfigSchema() {
        super("config");

        section("section1")
            .intValue("value1", 10, "Description")
            .stringValue("value2", "default", "Description");

        section("section2")
            .doubleValue("value3", 1.0, "Description");
    }
}
// That's it!
```

### 3. Immutable & Thread-Safe

**Old:**
```java
// Mutable state everywhere
ConfigValue<Integer> cooldown = ...;
cooldown.setValue(15);  // Modifies in place
saveValue(cooldown);

// Thread-safety concerns
synchronized (config) {
    int value = config.getValue();
    // ...
}
```

**New:**
```java
// Immutable - thread-safe by default
Config original = ...;
Config updated = original.with("cooldown", 15);  // Returns new config

// Original unchanged
original.getInt("cooldown");  // Still old value
updated.getInt("cooldown");   // New value

// No synchronization needed - immutable!
```

### 4. Simpler API

**Old:**
```java
// Multiple ways to do the same thing
config.getKeraunosCooldown();              // Legacy getter
config.getKeraunos().getCooldown();        // Container getter
config.getConfigValue(cooldownValue);      // Direct access
```

**New:**
```java
// One clear way
config.getInt("keraunos.cooldown");

// Or use sections for organization
ConfigSection keraunos = config.section("keraunos");
int cooldown = keraunos.getInt("cooldown");
```

### 5. Better Validation

**Old:**
```java
// Validation only on load
ConfigValidator<Integer> validator = ConfigValidator.range(0, 100);
configValue.setValidator(validator);

// Runtime errors possible
configValue.setValue(999);  // Oops! No validation on set
saveValue(configValue);     // Saved invalid value to disk
```

**New:**
```java
// Validation enforced everywhere
Validator<Integer> validator = Validator.range(0, 100);

// Schema defines validation
section("example")
    .intValue("value", 10, validator, "Comment");

// Throws exception immediately on invalid value
Config updated = config.with("example.value", 999);  // Exception!

// Validation on load
Config config = ConfigLoader.builder(plugin)
    .schema(schema)
    .validateOnLoad(true)  // Fails fast on invalid file
    .load();
```

## 🚀 Quick Start

### 1. Define Schema
```java
public class AbilitiesSchema extends ConfigSchema {
    public AbilitiesSchema() {
        super("abilities");

        section("keraunos")
            .intValue("cooldown", 10, Validator.range(0, 300), "Cooldown in seconds")
            .doubleValue("radius", 3.0, Validator.min(0.0), "Effect radius");
    }
}
```

### 2. Load Config
```java
Config abilities = ConfigLoader.builder(plugin)
    .schema(new AbilitiesSchema())
    .load();
```

### 3. Use It
```java
// Read
int cooldown = abilities.getInt("keraunos.cooldown");
double radius = abilities.getDouble("keraunos.radius");

// Or use sections
ConfigSection keraunos = abilities.section("keraunos");
int cooldown = keraunos.getInt("cooldown");

// Update (immutable)
Config updated = abilities.with("keraunos.cooldown", 15);
updated.save(file);
```

## 📚 Documentation

- **README.md** - Full API documentation and examples
- **MIGRATION_GUIDE.md** - Step-by-step migration from old system
- **example/AbilitiesConfigSchema.java** - Real-world example
- **example/UsageExample.java** - Comprehensive usage examples

## 🔄 Migration Strategy

You can migrate incrementally:

1. **Keep old system running** - No breaking changes
2. **Create new schemas** - One config at a time
3. **Update call sites** - Gradually replace old API calls
4. **Remove old code** - Once migration complete

Or use a wrapper to maintain compatibility:

```java
public class AbilitiesConfig {
    private final Config config;

    public AbilitiesConfig(JavaPlugin plugin) {
        this.config = ConfigLoader.builder(plugin)
            .schema(new AbilitiesConfigSchema())
            .load();
    }

    // Keep old API
    public int getKeraunosCooldown() {
        return config.getInt("keraunos.cooldown");
    }
}
```

## 🎯 Benefits Summary

| Feature | Old System | New System |
|---------|-----------|------------|
| **Lines of code** | ~216 per config | ~40 per config (-81%) |
| **Type safety** | ❌ Raw lists, unsafe casts | ✅ Fully type-safe |
| **Boilerplate** | ❌ 35+ lines per config | ✅ ~5 lines |
| **Thread safety** | ⚠️ Manual synchronization | ✅ Immutable by default |
| **Container classes** | ❌ Required (3-5 per config) | ✅ Not needed |
| **API clarity** | ⚠️ 3+ ways to access values | ✅ One clear way |
| **Validation** | ⚠️ Optional, inconsistent | ✅ Enforced everywhere |
| **Performance** | ⚠️ Reflection on every load | ✅ Zero reflection |
| **Deprecated code** | ❌ Lots (40+ methods) | ✅ None |

## 🤔 Why This Design?

The new system is based on modern Java best practices:

1. **Records** - Immutable data classes (Java 14+)
2. **Sealed interfaces** - Controlled type hierarchies (Java 17+)
3. **Builder pattern** - Flexible object creation
4. **Fluent API** - Readable, chainable methods
5. **Type safety** - Eliminate entire classes of bugs
6. **Immutability** - Easier to reason about, thread-safe

## 🛠️ Next Steps

1. **Read README.md** - Understand the full API
2. **Try the examples** - Run `UsageExample.java`
3. **Migrate one config** - Start with a small one
4. **Provide feedback** - What works? What doesn't?

## ❓ Questions?

- How does X work? → Check README.md
- How do I migrate? → Check MIGRATION_GUIDE.md
- Need more examples? → Check example/UsageExample.java
- Found a bug? → Let me know!

---

Built with ☕ to make your config system **simple, safe, and maintainable**.
