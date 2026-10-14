# AbstractYaml Library - Migration Guide

## Overview of Changes

This update fixes critical bugs and adds powerful new features to the AbstractYaml library:

### **Critical Bug Fixes**
1. ✅ **Type Erasure Fixed** - ConfigValue now requires explicit type information
2. ✅ **Thread-Safety Fixed** - YAML singleton no longer has mutable shared state
3. ✅ **Resource Leaks Fixed** - All I/O operations now use try-with-resources

### **New Features**
1. ✨ **Validation Framework** - Validate config values with built-in or custom rules
2. ✨ **Caching System** - Automatic caching reduces disk I/O operations
3. ✨ **Better Error Handling** - Custom exceptions with meaningful error messages

---

## Migration Steps

### Step 1: Update ConfigValue Declarations

**BEFORE (Old Pattern - Deprecated):**
```java
private ConfigValue<Integer> maxPlayers = new ConfigValue<>("max-players", 100);
```

**AFTER (New Pattern - Recommended):**
```java
private ConfigValue<Integer> maxPlayers =
    new ConfigValue<>("max-players", Integer.class, 100);
```

**Why?** The old pattern uses runtime type inference which fails for generic types like `List<String>`. The new pattern provides compile-time type safety.

---

### Step 2: Update YAML Method Calls

The YAML class now accepts JavaPlugin as a parameter instead of storing it as mutable state.

**BEFORE:**
```java
YAML yaml = YAML.getInstance();
yaml.setMain(plugin);
FileConfiguration config = yaml.getConfig("config");
```

**AFTER:**
```java
YAML yaml = YAML.getInstance();
FileConfiguration config = yaml.getConfig(plugin, "config");
```

**Note:** The old `setMain()` method is deprecated but still works for backward compatibility. However, it's not thread-safe and should be avoided.

---

### Step 3: Add Validation (Optional but Recommended)

Add validators to ensure config values meet your requirements:

```java
private ConfigValue<Integer> maxPlayers =
    new ConfigValue<>("max-players", Integer.class, 100)
        .setValidator(ConfigValidator.range(1, 1000));

private ConfigValue<String> serverName =
    new ConfigValue<>("server-name", String.class, "Default")
        .setValidator(ConfigValidator.<String>notEmpty()
            .addRule(name -> name.length() <= 32, "Name too long"));
```

**Available Validators:**
- `ConfigValidator.range(min, max)` - Numeric ranges
- `ConfigValidator.min(value)` - Minimum value
- `ConfigValidator.max(value)` - Maximum value
- `ConfigValidator.notEmpty()` - Non-empty strings
- `ConfigValidator.length(min, max)` - String length
- `ConfigValidator.matches(regex)` - Regex pattern
- `ConfigValidator.oneOf(values...)` - Whitelist
- `ConfigValidator.notNull()` - Non-null values
- Custom validators with `.addRule(predicate, errorMessage)`

---

### Step 4: Use Caching Features (Automatic)

Caching is **enabled by default** and requires no code changes. However, you can control it:

```java
// Disable caching if needed (e.g., for configs that change externally)
config.setCacheEnabled(false);

// Clear cache to force reload from disk
config.clearCache();

// Clear specific value from cache
config.clearCacheValue("max-players");
```

**Benefits:**
- Multiple calls to `getValue()` don't re-read from disk
- Significant performance improvement for frequently accessed values
- Cache automatically cleared on `reload()`

---

## Example: Complete Migration

### BEFORE (Old Code)
```java
public class MyConfig extends AbstractConfig {

    private ConfigValue<Integer> amount = new ConfigValue<>("amount", 5);

    @Override
    public void loadData() {
        amount = getConfigValue(amount);
    }

    public int getAmount() {
        return amount.getValue();
    }

    public void setAmount(int value) {
        saveValue(amount.getId(), value);
        amount.setValue(value);
    }
}
```

### AFTER (New Code)
```java
public class MyConfig extends AbstractConfig {
    private JavaPlugin plugin;

    // Add explicit type and optional validation
    private ConfigValue<Integer> amount =
        new ConfigValue<>("amount", Integer.class, 5)
            .setValidator(ConfigValidator.range(1, 100));

    // Singleton
    private static final MyConfig INSTANCE = new MyConfig();
    public static MyConfig getInstance() { return INSTANCE; }
    private MyConfig() {}

    // Setter for plugin reference
    public void setPlugin(JavaPlugin plugin) { this.plugin = plugin; }

    @Override
    public JavaPlugin getMain() { return plugin; }

    @Override
    public void loadData() {
        // Validation happens automatically
        amount = getConfigValue(amount);

        // Check if validation failed
        if (!amount.isValid()) {
            log("Warning: amount failed validation");
        }
    }

    public int getAmount() {
        // Value is cached automatically
        return amount.getValue();
    }

    public void setAmount(int value) {
        amount.setValue(value);
        // Validation happens automatically in saveValue
        saveValue(amount);
    }
}

// Usage:
@Override
public void onEnable() {
    MyConfig config = MyConfig.getInstance();
    config.setPlugin(this);
    config.load();
}
```

---

## Backward Compatibility

All changes maintain backward compatibility:

- ✅ Old `new ConfigValue<>(id, default)` constructors still work (marked `@Deprecated`)
- ✅ Old `YAML.setMain()` pattern still works (marked `@Deprecated`)
- ✅ No breaking changes to AbstractConfig

**However**, you should migrate to the new patterns to:
1. Fix type erasure bugs
2. Ensure thread-safety
3. Get validation support
4. Benefit from performance improvements

---

## Testing Checklist

After migration, verify:

- [ ] All config values load correctly on plugin enable
- [ ] Config values with validators reject invalid input
- [ ] Changes persist correctly to disk
- [ ] No errors in console during config operations
- [ ] Performance is improved (check with large configs)

---

## Common Issues

### Issue: "Cannot determine type for ConfigValue"
**Cause:** Using deprecated constructor with null values
**Solution:** Use new constructor with explicit type: `new ConfigValue<>(id, Type.class, default)`

### Issue: "JavaPlugin not set"
**Cause:** Using deprecated YAML methods without calling `setMain()`
**Solution:** Use new method signatures that accept JavaPlugin as parameter

### Issue: Validation errors on startup
**Cause:** Existing config values don't meet new validation rules
**Solution:** Update config.yml or adjust validation rules to be more lenient initially

---

## Need Help?

If you encounter issues during migration:
1. Check the `ExampleConfig.java` for reference implementation
2. Review validation rules - they may be too strict
3. Enable debug logging to see detailed error messages
4. Check console warnings for specific config values failing validation

---

## Performance Tips

1. **Keep caching enabled** unless you have external config modifications
2. **Use validators** to catch bad values early instead of checking in code
3. **Batch saves** by passing `false` to `saveValue()` and calling `saveConfig()` once
4. **Reload only when necessary** - cache makes repeated access fast

---

## What's Next?

Future improvements planned:
- Builder pattern for cleaner config creation
- Change listeners for reactive updates
- Support for custom type adapters
- YAML comment preservation
- Config migration utilities
