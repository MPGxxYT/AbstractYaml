# Getting Started with AbstractYaml

This guide will help you create your first type-safe configuration in 5 minutes.

## Installation

Add the AbstractYaml library to your project's dependencies (already included in your project).

## Your First Config

### Step 1: Create Your Config Class

```java
package com.yourplugin.config;

import me.mortaldev.AbstractConfig;
import me.mortaldev.ConfigValue;
import me.mortaldev.ConfigValidator;
import org.bukkit.plugin.java.JavaPlugin;

public class MyConfig extends AbstractConfig {
    private JavaPlugin plugin;

    // Define your config values
    private ConfigValue<String> serverName =
        new ConfigValue<>("server.name", String.class, "My Server")
            .setValidator(ConfigValidator.notEmpty());

    private ConfigValue<Integer> maxPlayers =
        new ConfigValue<>("server.max-players", Integer.class, 100)
            .setValidator(ConfigValidator.range(1, 1000));

    private ConfigValue<Boolean> pvpEnabled =
        new ConfigValue<>("gameplay.pvp-enabled", Boolean.class, true);

    // Singleton pattern
    private static final MyConfig INSTANCE = new MyConfig();
    public static MyConfig getInstance() { return INSTANCE; }
    private MyConfig() {}

    // Plugin reference setter
    public void setPlugin(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void loadData() {
        // Load all values from config file
        serverName = getConfigValue(serverName);
        maxPlayers = getConfigValue(maxPlayers);
        pvpEnabled = getConfigValue(pvpEnabled);
    }

    @Override
    public String getName() {
        return "config"; // config.yml filename
    }

    @Override
    public JavaPlugin getMain() {
        return plugin;
    }

    @Override
    public void log(String message) {
        if (plugin != null) {
            plugin.getLogger().info(message);
        }
    }

    // Getters
    public String getServerName() {
        return serverName.getValue();
    }

    public int getMaxPlayers() {
        return maxPlayers.getValue();
    }

    public boolean isPvpEnabled() {
        return pvpEnabled.getValue();
    }

    // Setters (with automatic validation and saving)
    public void setServerName(String name) {
        serverName.setValue(name);
        saveValue(serverName);
    }

    public void setMaxPlayers(int max) {
        maxPlayers.setValue(max);
        saveValue(maxPlayers);
    }

    public void setPvpEnabled(boolean enabled) {
        pvpEnabled.setValue(enabled);
        saveValue(pvpEnabled);
    }
}
```

### Step 2: Create config.yml

Create `config.yml` in your plugin's resources folder:

```yaml
server:
  name: "My Awesome Server"
  max-players: 100

gameplay:
  pvp-enabled: true
```

### Step 3: Load in Your Plugin

```java
package com.yourplugin;

import com.yourplugin.config.MyConfig;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Load the config
        MyConfig config = MyConfig.getInstance();
        config.setPlugin(this);
        config.load();

        // Use the config
        getLogger().info("Server name: " + config.getServerName());
        getLogger().info("Max players: " + config.getMaxPlayers());
        getLogger().info("PVP enabled: " + config.isPvpEnabled());
    }

    @Override
    public void onDisable() {
        // Config is automatically saved when values change
    }
}
```

### Step 4: Test It

1. Build your plugin
2. Put it in your server's plugins folder
3. Start the server
4. Check the console for your config values
5. Edit `config.yml` and run `/reload` to see changes

## Understanding the Code

### ConfigValue

```java
private ConfigValue<String> serverName =
    new ConfigValue<>("server.name", String.class, "My Server");
```

- **"server.name"** - Path in YAML file
- **String.class** - Type of the value (required for type safety)
- **"My Server"** - Default value if not in config

### Validators

```java
.setValidator(ConfigValidator.range(1, 1000))
```

Validators automatically check values when:
- Loading from config file
- Setting via setter methods

If validation fails:
- Loading: Uses default value and logs warning
- Setting: Throws exception

### Loading

```java
@Override
public void loadData() {
    serverName = getConfigValue(serverName);
}
```

This:
1. Reads from config file
2. Validates the value
3. Caches for fast access
4. Falls back to default if invalid

### Caching

After loading, values are cached:

```java
// First call: reads from disk (~2ms)
String name = config.getServerName();

// Subsequent calls: from cache (~0.001ms)
for (int i = 0; i < 1000; i++) {
    String name = config.getServerName(); // Super fast!
}
```

## Common Patterns

### Pattern 1: Simple Value

```java
private ConfigValue<String> motd =
    new ConfigValue<>("motd", String.class, "Welcome!");

public String getMotd() { return motd.getValue(); }
```

### Pattern 2: Validated Value

```java
private ConfigValue<Integer> coins =
    new ConfigValue<>("economy.starting-coins", Integer.class, 100)
        .setValidator(ConfigValidator.min(0));

public int getStartingCoins() { return coins.getValue(); }
```

### Pattern 3: Custom Validation

```java
private ConfigValue<Integer> teamSize =
    new ConfigValue<>("team-size", Integer.class, 4)
        .setValidator(
            ConfigValidator.<Integer>notNull()
                .addRule(n -> n % 2 == 0, "Team size must be even")
                .addRule(n -> n >= 2, "Team size must be at least 2")
        );
```

### Pattern 4: Nested Paths

```java
private ConfigValue<Double> multiplier =
    new ConfigValue<>("economy.rewards.multiplier", Double.class, 1.0);
```

YAML:
```yaml
economy:
  rewards:
    multiplier: 1.5
```

## Lifecycle Methods

### load()
```java
config.load(); // Initial load from disk
```

### reload()
```java
config.reload(); // Reload from disk (clears cache)
```

### saveConfig()
```java
config.saveConfig(); // Save current values to disk
```

### clearCache()
```java
config.clearCache(); // Clear cache, next access reads from disk
```

## Next Steps

Now that you have a basic config working:

1. **Add Validation** - See [Validation.md](Validation.md) for all validators
2. **Work with Lists** - See [WorkingWithLists.md](WorkingWithLists.md) for list config values
3. **Organize Large Configs** - See [ValueContainers.md](ValueContainers.md) for the container pattern
4. **Migrate Old Configs** - See [MigrationGuide.md](MigrationGuide.md) if upgrading

## Troubleshooting

### "Cannot determine type for ConfigValue"

**Problem:** Using deprecated constructor
```java
new ConfigValue<>("path", defaultValue) // ❌
```

**Solution:** Use explicit type
```java
new ConfigValue<>("path", String.class, defaultValue) // ✅
```

### "JavaPlugin not initialized"

**Problem:** Forgot to set plugin
```java
config.load(); // ❌
```

**Solution:** Set plugin first
```java
config.setPlugin(this);
config.load(); // ✅
```

### Validation Errors on Startup

**Problem:** Config file has invalid values

**Solution:** Check console warnings, update config file or adjust validators

### Values Not Updating

**Problem:** Cache not clearing

**Solution:** Call `reload()` instead of just editing the file
```java
config.reload(); // Clears cache and reloads
```

## Tips

1. **Always use explicit types** - Prevents runtime errors
2. **Add validators** - Catch bad values early
3. **Keep caching enabled** - Massive performance boost
4. **Use singleton pattern** - Easy access from anywhere
5. **Log validation failures** - Helps debug config issues

## Complete Working Example

See [ExampleConfig.java](../src/main/java/me/mortaldev/ExampleConfig.java) for a complete, working reference implementation.

## Summary

Creating a config requires:
1. Extend `AbstractConfig`
2. Define `ConfigValue` fields
3. Implement required methods
4. Load in `onEnable()`

That's it! You now have a type-safe, validated, cached configuration system.
