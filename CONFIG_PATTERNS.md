# Configuration Patterns - JavaPlugin Reference

## The Challenge

`AbstractConfig` requires you to implement `getMain()` which returns a `JavaPlugin` instance. Here are the three main patterns for doing this with a singleton config.

---

## Pattern 1: Field with Setter (Recommended)

**Best for:** Most use cases - clean, explicit, testable

```java
public class MyConfig extends AbstractConfig {
    private JavaPlugin plugin;

    // Singleton
    private static final MyConfig INSTANCE = new MyConfig();
    public static MyConfig getInstance() { return INSTANCE; }
    private MyConfig() {}

    // Setter to inject plugin reference
    public void setPlugin(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public JavaPlugin getMain() {
        return plugin;
    }

    @Override
    public String getName() { return "config"; }

    @Override
    public void log(String message) {
        if (plugin != null) {
            plugin.getLogger().info(message);
        }
    }

    @Override
    public void loadData() {
        // Load your config values here
    }
}

// Usage in your main plugin class:
@Override
public void onEnable() {
    MyConfig config = MyConfig.getInstance();
    config.setPlugin(this);  // Inject plugin reference
    config.load();           // Load the config
}
```

**Pros:**
- ✅ Clean and explicit
- ✅ Easy to test (can inject mock plugin)
- ✅ Maintains singleton pattern
- ✅ Thread-safe if you only call `setPlugin()` once during initialization

**Cons:**
- ⚠️ Must remember to call `setPlugin()` before `load()`
- ⚠️ Not thread-safe if called multiple times

---

## Pattern 2: Static Plugin Reference

**Best for:** When you have a static plugin accessor

```java
public class MyPlugin extends JavaPlugin {
    private static MyPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        MyConfig.getInstance().load();
    }

    public static MyPlugin getInstance() {
        return instance;
    }
}

public class MyConfig extends AbstractConfig {
    private static final MyConfig INSTANCE = new MyConfig();
    public static MyConfig getInstance() { return INSTANCE; }
    private MyConfig() {}

    @Override
    public JavaPlugin getMain() {
        return MyPlugin.getInstance();  // Get from static reference
    }

    @Override
    public String getName() { return "config"; }

    @Override
    public void log(String message) {
        getMain().getLogger().info(message);
    }

    @Override
    public void loadData() {
        // Load your config values
    }
}
```

**Pros:**
- ✅ No need to call setter
- ✅ Always has plugin reference when needed
- ✅ Clean usage code

**Cons:**
- ⚠️ Couples config to specific plugin class
- ⚠️ Less testable
- ⚠️ Can return null if called before plugin enables

---

## Pattern 3: Bukkit Plugin Manager Lookup

**Best for:** Library configs used by multiple plugins

```java
public class MyConfig extends AbstractConfig {
    private static final MyConfig INSTANCE = new MyConfig();
    public static MyConfig getInstance() { return INSTANCE; }
    private MyConfig() {}

    @Override
    public JavaPlugin getMain() {
        // Look up plugin by name
        Plugin plugin = Bukkit.getPluginManager().getPlugin("MyPluginName");
        if (plugin instanceof JavaPlugin javaPlugin) {
            return javaPlugin;
        }
        throw new IllegalStateException("Plugin 'MyPluginName' not found!");
    }

    @Override
    public String getName() { return "config"; }

    @Override
    public void log(String message) {
        getMain().getLogger().info(message);
    }

    @Override
    public void loadData() {
        // Load your config values
    }
}

// Usage:
@Override
public void onEnable() {
    MyConfig.getInstance().load();  // No setup needed!
}
```

**Pros:**
- ✅ No setup code needed
- ✅ Works from anywhere
- ✅ Decoupled from plugin class

**Cons:**
- ⚠️ Requires hardcoded plugin name
- ⚠️ Lookup overhead (cache if called frequently)
- ⚠️ Can fail if plugin not loaded

---

## Pattern 4: Constructor Injection (No Singleton)

**Best for:** When you don't need a singleton

```java
public class MyConfig extends AbstractConfig {
    private final JavaPlugin plugin;

    public MyConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public JavaPlugin getMain() {
        return plugin;
    }

    @Override
    public String getName() { return "config"; }

    @Override
    public void log(String message) {
        plugin.getLogger().info(message);
    }

    @Override
    public void loadData() {
        // Load your config values
    }
}

// Usage:
public class MyPlugin extends JavaPlugin {
    private MyConfig config;

    @Override
    public void onEnable() {
        config = new MyConfig(this);
        config.load();
    }

    public MyConfig getConfig() {
        return config;
    }
}
```

**Pros:**
- ✅ Most testable
- ✅ Clear dependencies
- ✅ Thread-safe (immutable plugin reference)
- ✅ Can have multiple instances

**Cons:**
- ⚠️ Not a singleton
- ⚠️ Need to store and pass config reference

---

## Comparison Table

| Pattern | Setup Complexity | Testability | Thread-Safe | Singleton | Recommended For |
|---------|-----------------|-------------|-------------|-----------|-----------------|
| Field + Setter | Low | High | ⚠️ | ✅ | **Most projects** |
| Static Reference | Very Low | Medium | ✅ | ✅ | Simple plugins |
| Plugin Manager | Very Low | Medium | ✅ | ✅ | Library configs |
| Constructor | Low | Very High | ✅ | ❌ | Complex apps |

---

## Recommended Pattern for Your Use Case

Based on your existing `TestConfig.java`, I recommend **Pattern 1 (Field + Setter)**:

```java
public class GameConfig extends AbstractConfig {
    private JavaPlugin plugin;

    private ConfigValue<Integer> maxPlayers =
        new ConfigValue<>("max-players", Integer.class, 100)
            .setValidator(ConfigValidator.range(1, 1000));

    // Singleton
    private static final GameConfig INSTANCE = new GameConfig();
    public static GameConfig getInstance() { return INSTANCE; }
    private GameConfig() {}

    public void setPlugin(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public JavaPlugin getMain() { return plugin; }
    @Override
    public String getName() { return "game-config"; }
    @Override
    public void log(String msg) {
        if (plugin != null) plugin.getLogger().info(msg);
    }

    @Override
    public void loadData() {
        maxPlayers = getConfigValue(maxPlayers);
    }

    // Getters/setters
    public int getMaxPlayers() { return maxPlayers.getValue(); }
    public void setMaxPlayers(int value) {
        maxPlayers.setValue(value);
        saveValue(maxPlayers);
    }
}

// In your main plugin:
@Override
public void onEnable() {
    GameConfig config = GameConfig.getInstance();
    config.setPlugin(this);
    config.load();

    getLogger().info("Max players: " + config.getMaxPlayers());
}
```

---

## Thread-Safety Note

If you're worried about thread-safety with Pattern 1, you can make it safer:

```java
public class MyConfig extends AbstractConfig {
    private volatile JavaPlugin plugin;
    private final Object pluginLock = new Object();

    public void setPlugin(JavaPlugin plugin) {
        synchronized (pluginLock) {
            if (this.plugin != null) {
                throw new IllegalStateException("Plugin already set!");
            }
            this.plugin = plugin;
        }
    }

    @Override
    public JavaPlugin getMain() {
        JavaPlugin p = plugin;
        if (p == null) {
            throw new IllegalStateException("Plugin not initialized! Call setPlugin() first.");
        }
        return p;
    }
}
```

This ensures:
- ✅ Can only be set once
- ✅ Thread-safe initialization
- ✅ Clear error if used before initialization

---

## Summary

**Just getting started?** Use **Pattern 1 (Field + Setter)** - it's the sweet spot of simplicity and safety.

```java
config.setPlugin(this);
config.load();
```

That's it! No `initialize()` method needed - just set the plugin and load. 🎉
