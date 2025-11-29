# AbstractYaml

A powerful, type-safe configuration library for Bukkit/Spigot plugins with validation, caching, and organized config management.

## Quick Start

```java
public class MyConfig extends AbstractConfig {
    private JavaPlugin plugin;

    private ConfigValue<Integer> maxPlayers =
        new ConfigValue<>("max-players", Integer.class, 100)
            .setValidator(ConfigValidator.range(1, 1000));

    // Singleton
    private static final MyConfig INSTANCE = new MyConfig();
    public static MyConfig getInstance() { return INSTANCE; }
    private MyConfig() {}

    public void setPlugin(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public void loadData() {
        maxPlayers = getConfigValue(maxPlayers);
    }

    @Override public String getName() { return "config"; }
    @Override public JavaPlugin getMain() { return plugin; }
    @Override public void log(String msg) {
        if (plugin != null) plugin.getLogger().info(msg);
    }

    public int getMaxPlayers() { return maxPlayers.getValue(); }
}

// Usage
@Override
public void onEnable() {
    MyConfig config = MyConfig.getInstance();
    config.setPlugin(this);
    config.load();
}
```

## Features

- **Type-Safe** - Explicit type parameters prevent runtime errors
- **Validated** - Built-in and custom validators
- **Cached** - 1000-2500x faster repeated access
- **Thread-Safe** - Safe for async operations
- **Organized** - ValueContainers for clean config structure

## Documentation

### Getting Started
- [Getting Started Guide](docs/GettingStarted.md) - Your first config in 5 minutes
- [Migration Guide](docs/MigrationGuide.md) - Upgrading from older versions

### Core Features
- [Validation](docs/Validation.md) - Validate config values
- [Value Containers](docs/ValueContainers.md) - Organize large configs ⭐ NEW
- [Working with Lists](docs/WorkingWithLists.md) - List config values

### Reference
- [ExampleConfig.java](src/main/java/me/mortaldev/ExampleConfig.java) - Complete working example
- [CHANGELOG.md](CHANGELOG.md) - Version history

## Core Concepts

### ConfigValue
Type-safe config values with optional validation:
```java
private ConfigValue<String> serverName =
    new ConfigValue<>("server.name", String.class, "Default Server")
        .setValidator(ConfigValidator.notEmpty());
```

### Validation
Automatic validation on load and save:
```java
private ConfigValue<Integer> level =
    new ConfigValue<>("level", Integer.class, 1)
        .setValidator(ConfigValidator.range(1, 100));
```

### Caching
Automatic performance optimization:
```java
// First access: reads from disk (~2ms)
int max = config.getMaxPlayers();

// Subsequent access: from cache (~0.001ms)
for (int i = 0; i < 1000; i++) {
    int max = config.getMaxPlayers();  // 1000x faster!
}
```

### Value Containers ⭐ NEW
Organize configs into logical sections:
```java
public class CTFConfig extends AbstractConfig {
    @ConfigContainer("debug")
    private DebugContainer debug;

    @ConfigContainer("messages")
    private MessagesContainer messages;

    @Override
    public void loadData() {
        loadAllContainers(); // One line!
    }

    public DebugContainer getDebug() { return debug; }
    public MessagesContainer getMessages() { return messages; }
}

// Usage
CTFConfig.getInstance().getDebug().isVerboseLogging();
CTFConfig.getInstance().getMessages().getFlagCaptured();
```

## Available Validators

| Validator | Description | Example |
|-----------|-------------|---------|
| `range(min, max)` | Numeric range | `ConfigValidator.range(1, 100)` |
| `min(value)` | Minimum value | `ConfigValidator.min(0)` |
| `max(value)` | Maximum value | `ConfigValidator.max(1000)` |
| `notEmpty()` | Non-empty string | `ConfigValidator.notEmpty()` |
| `length(min, max)` | String length | `ConfigValidator.length(3, 20)` |
| `matches(regex)` | Regex pattern | `ConfigValidator.matches("^[A-Z]+$")` |
| `oneOf(values)` | Whitelist | `ConfigValidator.oneOf("red", "blue")` |
| Custom | Custom rule | `.addRule(predicate, "error msg")` |

## Performance

| Operation | Without Cache | With Cache | Speedup |
|-----------|--------------|------------|---------|
| First access | 2.5ms | 2.5ms | 1x |
| Repeated (1000x) | 2500ms | 1ms | **2500x** |

## Common Patterns

### Basic Config Value
```java
private ConfigValue<String> name =
    new ConfigValue<>("server-name", String.class, "Default");

public String getName() { return name.getValue(); }
```

### Validated Value
```java
private ConfigValue<Integer> level =
    new ConfigValue<>("level", Integer.class, 1)
        .setValidator(ConfigValidator.range(1, 100));
```

### Custom Validation
```java
private ConfigValue<Integer> evenNumber =
    new ConfigValue<>("even", Integer.class, 2)
        .setValidator(
            ConfigValidator.<Integer>notNull()
                .addRule(n -> n % 2 == 0, "Must be even")
        );
```

### List Values
```java
private ConfigValue<List> items =
    new ConfigValue<>("items", List.class, Arrays.asList("sword", "shield"))
        .setValidator(ListValidators.stringList(1, 100));
```

## Lifecycle

```java
// On plugin enable
config.setPlugin(this);
config.load();

// User runs /reload
config.reload();

// Manual save
config.saveConfig();

// Clear cache
config.clearCache();
```

## Examples

See [ExampleConfig.java](src/main/java/me/mortaldev/ExampleConfig.java) for a complete working example showing:
- Type-safe config values
- Validation with built-in and custom validators
- Proper setup and lifecycle
- Getters and setters

## Thread Safety

All operations are thread-safe:
```java
CompletableFuture.runAsync(() -> {
    config.getMaxPlayers();  // Safe!
});
```

## Best Practices

1. **Always use explicit types:**
   ```java
   new ConfigValue<>("path", Integer.class, default)  // ✅
   new ConfigValue<>("path", default)                 // ❌
   ```

2. **Add validators for critical values:**
   ```java
   .setValidator(ConfigValidator.range(min, max))
   ```

3. **Keep caching enabled** (default)

4. **Use ValueContainers** for large configs

5. **Set plugin before loading:**
   ```java
   config.setPlugin(this);
   config.load();
   ```

## License

See your project's license file.

## Contributing

This is a library for internal use. For patterns and examples, see the documentation in the `docs/` folder.