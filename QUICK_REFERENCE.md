# AbstractYaml - Quick Reference Card

## 🚀 Quick Start

```java
// 1. Create your config class
public class MyConfig extends AbstractConfig {
    private JavaPlugin plugin;

    private ConfigValue<Integer> maxPlayers =
        new ConfigValue<>("max-players", Integer.class, 100)
            .setValidator(ConfigValidator.range(1, 1000));

    // Singleton
    private static final MyConfig INSTANCE = new MyConfig();
    public static MyConfig getInstance() { return INSTANCE; }
    private MyConfig() {}

    // Setter for plugin reference
    public void setPlugin(JavaPlugin plugin) { this.plugin = plugin; }

    @Override
    public void loadData() {
        maxPlayers = getConfigValue(maxPlayers);
    }

    @Override
    public String getName() { return "config"; }

    @Override
    public JavaPlugin getMain() { return plugin; }

    @Override
    public void log(String msg) {
        if (plugin != null) plugin.getLogger().info(msg);
    }

    // Getter with caching
    public int getMaxPlayers() { return maxPlayers.getValue(); }

    // Setter with validation
    public void setMaxPlayers(int value) {
        maxPlayers.setValue(value);
        saveValue(maxPlayers);
    }
}

// 2. Setup on plugin enable
@Override
public void onEnable() {
    MyConfig config = MyConfig.getInstance();
    config.setPlugin(this);  // Set plugin reference
    config.load();           // Load config from disk
}
```

---

## 📝 ConfigValue Creation

### Basic Types
```java
// String
new ConfigValue<>("path", String.class, "default")

// Numbers
new ConfigValue<>("count", Integer.class, 10)
new ConfigValue<>("multiplier", Double.class, 1.5)

// Boolean
new ConfigValue<>("enabled", Boolean.class, true)

// List
new ConfigValue<>("items", List.class, Arrays.asList("a", "b"))

// Map
new ConfigValue<>("data", Map.class, new HashMap<>())
```

---

## ✅ Validation Cheat Sheet

### Numeric Validators
```java
ConfigValidator.range(1, 100)           // Between 1 and 100
ConfigValidator.min(0)                  // At least 0
ConfigValidator.max(1000)               // At most 1000
```

### String Validators
```java
ConfigValidator.notEmpty()              // Not null or empty
ConfigValidator.length(3, 20)           // Length between 3-20
ConfigValidator.matches("^[A-Z]+$")     // Regex pattern
```

### Choice Validators
```java
ConfigValidator.oneOf("red", "blue")    // Must be one of these
ConfigValidator.notNull()               // Cannot be null
```

### Custom Validators
```java
ConfigValidator.<Integer>notNull()
    .addRule(n -> n % 2 == 0, "Must be even")
    .addRule(n -> n > 0, "Must be positive")
```

---

## 🎯 Common Patterns

### Pattern 1: Simple Config Value
```java
private ConfigValue<String> name =
    new ConfigValue<>("server.name", String.class, "Default");

public String getName() { return name.getValue(); }
```

### Pattern 2: Validated Range
```java
private ConfigValue<Integer> level =
    new ConfigValue<>("player.level", Integer.class, 1)
        .setValidator(ConfigValidator.range(1, 100));

public int getLevel() { return level.getValue(); }

public void setLevel(int lvl) {
    level.setValue(lvl);
    saveValue(level); // Auto-validates
}
```

### Pattern 3: Optional Cache Control
```java
@Override
public void loadData() {
    // Disable cache if config changes externally
    setCacheEnabled(false);

    // Or clear cache before loading
    clearCache();

    values = getConfigValue(values);
}
```

### Pattern 4: Batch Save Operations
```java
public void updateMultiple(int a, int b, int c) {
    saveValue("value-a", a, false); // Don't save yet
    saveValue("value-b", b, false); // Don't save yet
    saveValue("value-c", c, false); // Don't save yet
    saveConfig();                   // Save all at once
}
```

---

## 🔄 Lifecycle Methods

```java
// On plugin enable
config.load();              // Initial load

// When user runs /reload
config.reload();            // Reload from disk (clears cache)

// Manual save
config.saveConfig();        // Save current state to disk

// Clear cache only
config.clearCache();        // Force next access to read disk

// Cache specific value
config.clearCacheValue("max-players");
```

---

## 🛠️ YAML Utility Methods

### Thread-Safe (New API)
```java
JavaPlugin plugin = getMain();

// Get config
FileConfiguration fc = YAML.getInstance().getConfig(plugin, "config");

// Save config
YAML.getInstance().saveConfig(plugin, fc, "config");

// Load resource from jar
YAML.getInstance().loadResource(plugin, "config");

// Create new config
FileConfiguration fc = YAML.getInstance()
    .createNewConfig(plugin, plugin.getDataFolder().getPath(), "custom");
```

### Deprecated (Old API - Still Works)
```java
YAML yaml = YAML.getInstance();
yaml.setMain(plugin);                    // Not thread-safe!
FileConfiguration fc = yaml.getConfig("config");
```

---

## 🐛 Error Handling

### Check Validation Results
```java
ConfigValue<Integer> value = getConfigValue(myValue);

if (!value.isValid()) {
    log("Validation failed: " + value.validate().getErrorMessage());
    // Value will be set to default automatically
}
```

### Handle Save Validation
```java
try {
    saveValue(myValue); // Throws if validation fails
} catch (IllegalArgumentException e) {
    log("Cannot save invalid value: " + e.getMessage());
}
```

### Custom Exception Handling
```java
try {
    YAML.getInstance().saveConfig(plugin, config, "config");
} catch (YAML.ConfigurationException e) {
    log("Failed to save: " + e.getMessage());
    e.getCause().printStackTrace();
}
```

---

## ⚡ Performance Tips

1. **Keep caching enabled** (default):
   ```java
   setCacheEnabled(true); // Default, don't need to call
   ```

2. **Access values directly**:
   ```java
   // ✅ Fast - cached
   for (int i = 0; i < 1000; i++) {
       int max = getMaxPlayers();
   }

   // ❌ Slow - repeated disk I/O
   for (int i = 0; i < 1000; i++) {
       int max = getConfig().getInt("max-players");
   }
   ```

3. **Batch saves**:
   ```java
   saveValue(path1, val1, false);
   saveValue(path2, val2, false);
   saveConfig(); // One disk write instead of two
   ```

4. **Reload only when needed**:
   ```java
   // Cache makes repeated access fast
   // Only reload if file changed externally
   ```

---

## 🔍 Debugging

### Enable detailed logging
```java
@Override
public void log(String message) {
    getMain().getLogger().info("[Config] " + message);
}
```

### Check cache status
```java
boolean cacheEnabled = isCacheEnabled();
log("Cache enabled: " + cacheEnabled);
```

### Inspect config values
```java
log(configValue.toString());
// Output: ConfigValue{id='max-players', value=100, type=Integer}
```

---

## 📦 Complete Example

```java
public class GameConfig extends AbstractConfig {
    private JavaPlugin plugin;

    // Config values with validation
    private ConfigValue<Integer> roundTime =
        new ConfigValue<>("game.round-time", Integer.class, 300)
            .setValidator(ConfigValidator.range(60, 3600));

    private ConfigValue<String> mapName =
        new ConfigValue<>("game.map", String.class, "default")
            .setValidator(ConfigValidator.notEmpty());

    private ConfigValue<Boolean> pvpEnabled =
        new ConfigValue<>("game.pvp", Boolean.class, true);

    private ConfigValue<Double> rewardMultiplier =
        new ConfigValue<>("economy.multiplier", Double.class, 1.0)
            .setValidator(ConfigValidator.min(0.1));

    // Singleton
    private static final GameConfig INSTANCE = new GameConfig();
    public static GameConfig getInstance() { return INSTANCE; }
    private GameConfig() {}

    public void setPlugin(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void loadData() {
        roundTime = getConfigValue(roundTime);
        mapName = getConfigValue(mapName);
        pvpEnabled = getConfigValue(pvpEnabled);
        rewardMultiplier = getConfigValue(rewardMultiplier);
    }

    @Override public String getName() { return "config"; }
    @Override public JavaPlugin getMain() { return plugin; }
    @Override public void log(String msg) {
        if (plugin != null) plugin.getLogger().info(msg);
    }

    // Getters
    public int getRoundTime() { return roundTime.getValue(); }
    public String getMapName() { return mapName.getValue(); }
    public boolean isPvpEnabled() { return pvpEnabled.getValue(); }
    public double getRewardMultiplier() { return rewardMultiplier.getValue(); }

    // Setters
    public void setRoundTime(int time) {
        roundTime.setValue(time);
        saveValue(roundTime);
    }

    public void setMapName(String name) {
        mapName.setValue(name);
        saveValue(mapName);
    }
}

// Usage in plugin:
@Override
public void onEnable() {
    GameConfig config = GameConfig.getInstance();
    config.setPlugin(this);
    config.load();

    // Access values (cached automatically)
    int time = config.getRoundTime();
    getLogger().info("Round time: " + time);
}
```

---

## 📚 See Also

- **MIGRATION_GUIDE.md** - Step-by-step migration from old API
- **IMPROVEMENTS_SUMMARY.md** - Detailed technical improvements
- **ExampleConfig.java** - Complete working example
- **ConfigValidator.java** - Validation framework source

---

## 🆘 Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Type erasure error | Old constructor | Use `new ConfigValue<>(id, Type.class, default)` |
| JavaPlugin not set | Old YAML API | Use `YAML.getInstance().method(plugin, ...)` |
| Validation fails | Invalid default | Check validator rules or update default value |
| Cache not updating | External changes | Call `clearCache()` or disable cache |
| Slow access | Cache disabled | Enable cache with `setCacheEnabled(true)` |

---

**💡 Remember:** All new code should use explicit types, validators, and thread-safe YAML methods!
