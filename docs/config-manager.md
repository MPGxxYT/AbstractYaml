# ConfigManager

`ConfigManager` owns a plugin's configs: it knows which schemas exist, where their files are, in which order to load them, and which ones failed.

## The shared instance

Most plugins use one manager, set up in two places:

```java
public final class MyPlugin extends JavaPlugin {
  static {
    ConfigRegistry.register(MainSchema.class);
    ConfigRegistry.register(ShopSchema.class);
  }

  @Override
  public void onEnable() {
    ConfigManager.init(this);
    ConfigManager.getInstance().loadAll();
  }
}
```

- `ConfigRegistry.register(Class)` checks that the class has `@RegisterConfig` and remembers it. Classes registered after `init` are added to the running manager straight away.
- `ConfigManager.init(plugin)` creates the shared manager with files in the plugin's data folder and registers every class seen so far. It throws if a schema can't be created (for example, no public no-argument constructor), so mistakes show up at startup. Calling it twice throws.
- `ConfigManager.init(manager)` installs a manager you built yourself (see below) as the shared one, for example in tests or to store files elsewhere. Classes passed to `ConfigRegistry.register(...)` are added to it too.
- `ConfigManager.getInstance()` returns the shared manager, or throws if `init` has not run.

## Registration

Each schema becomes one named config. The name, file and load order come from `@RegisterConfig`:

```java
@RegisterConfig(priority = 10, path = "economy/shop.yml")
public class ShopSchema extends ConfigSchema { ... }
```

| Attribute | Default | Meaning |
| --- | --- | --- |
| `priority` | `100` | Lower numbers load first. Use it when one config's values are needed while loading another |
| `value` | the schema's name | The name used with `get(name)`, `reload(name)` and `getSection(name, path)` |
| `path` | `<name>.yml` | File path relative to the data folder; folders are created as needed |

Two schemas with the same name are rejected.

## Loading and reloading

```java
ConfigManager manager = ConfigManager.getInstance();

Map<String, Exception> failures = manager.loadAll();    // empty when everything loaded
Map<String, Exception> reloadFailures = manager.reloadAll();
manager.reload("shop");                                // throws if this one fails
```

| Method | Behaviour |
| --- | --- |
| `loadAll()` | Loads every config in priority order. A config that fails is logged and skipped, and the others still load. Returns the failures by name |
| `reloadAll()` | Same as `loadAll()`. A config that fails keeps its previous values |
| `reload(name)` | Reloads one config. Throws `ConfigLoader.ConfigLoadException` on failure and keeps the previous values; throws `IllegalArgumentException` for an unknown name |

## Accessing configs

| Method | Returns |
| --- | --- |
| `get(name)` | The loaded `Config`. Throws `IllegalStateException` with the original error if it failed to load, or `IllegalArgumentException` if no such config is registered |
| `getSection(name, path)` | `get(name).section(path)` |
| `isLoaded(name)` | Whether a config with that name is loaded |
| `getConfigNames()` | Names of the loaded configs |
| `count()` | Number of loaded configs |
| `validateAll()` | A `ValidationResult` per loaded config |

## Managers without the shared instance

`ConfigManager` is an ordinary object, so you can create as many as you need, for example one per module or one per test:

```java
ConfigManager manager = ConfigManager.forPlugin(plugin)
    .register(MainSchema.class)       // annotated class
    .register(new ArenaSchema("pvp")); // or an instance
manager.loadAll();
```

- `ConfigManager.forPlugin(plugin)` stores files in the plugin's data folder and logs through the plugin's logger.
- `ConfigManager.create(logger, sources)` takes a logger and a function from a relative path (such as `"shop.yml"`) to a [`ConfigSource`](testing.md#configsource). Use it to keep configs somewhere other than the data folder, or in memory.
- `register(ConfigSchema)` accepts instances, so one schema class can be registered several times with different names. Without `@RegisterConfig`, the name comes from the schema, the file is `<name>.yml` and the priority is 100.

## Using ConfigLoader directly

`ConfigManager` is built on `ConfigLoader`, which loads a single schema. Use it when you don't need registration or load order:

```java
Config arena = ConfigLoader.builder(plugin)
    .schema(new ArenaSchema("pvp"))
    .file("arenas/pvp.yml")       // relative to the data folder
    .autoGenerate(true)           // write defaults when the file is missing (default: true)
    .validateOnLoad(true)         // throw ConfigLoadException on invalid values (default: true)
    .load();
```

`file(File)` takes an absolute file, `source(ConfigSource)` takes any source, and `build()` returns a loader you can call `load()` or `reload()` on later.
