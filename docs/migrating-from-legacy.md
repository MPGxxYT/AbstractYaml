# Migrating from the legacy API

The legacy API (`AbstractConfig`, `ConfigValue`, `ConfigValueContainer` in the `me.mortaldev.abstractyaml` package) is deprecated but still works. You can migrate one config at a time: both APIs can be used in the same plugin, as long as each file is managed by only one of them.

## Side by side

A legacy config with one container:

```java
public class MainConfig extends AbstractConfig {
  private JavaPlugin plugin;

  @ConfigContainer("arena")
  private ArenaContainer arena;

  private static final MainConfig INSTANCE = new MainConfig();
  public static MainConfig getInstance() { return INSTANCE; }
  private MainConfig() {}

  public void setPlugin(JavaPlugin plugin) { this.plugin = plugin; }

  @Override public void loadData() { loadAllContainers(); }
  @Override public String getName() { return "config"; }
  @Override public JavaPlugin getMain() { return plugin; }
  @Override public void log(String message) { plugin.getLogger().info(message); }

  public ArenaContainer getArena() { return arena; }
}

public class ArenaContainer extends ConfigValueContainer {
  private ConfigValue<Integer> maxPlayers;
  private ConfigValue<Boolean> spectators;

  public ArenaContainer(AbstractConfig config, String pathPrefix) { super(config, pathPrefix); }

  @Override
  protected void load() {
    maxPlayers = loadValue("max-players", Integer.class, 16, ConfigValidator.range(2, 64), "Players per arena");
    spectators = loadValue("allow-spectators", Boolean.class, true, "Let players watch");
  }

  public int getMaxPlayers() { return maxPlayers.getValue(); }
  public boolean isSpectators() { return spectators.getValue(); }
}
```

The same config as a schema:

```java
@RegisterConfig
public class MainSchema extends ConfigSchema {
  public MainSchema() {
    super("config");
    section("arena")
        .intValue("max-players", 16, Validator.range(2, 64), "Players per arena")
        .boolValue("allow-spectators", true, "Let players watch");
  }
}
```

Paths and file name are unchanged, so existing `config.yml` files keep working.

## Mapping

| Legacy | Schema API |
| --- | --- |
| `extends AbstractConfig` + singleton + `getName()` | `extends ConfigSchema`, `super("config")` |
| `getMain()` / `setPlugin(...)` | `ConfigManager.init(plugin)` once for all configs |
| `@ConfigContainer("arena")` + container class | `section("arena")` in the schema |
| `new ConfigValue<>(path, Integer.class, 16)` | `.intValue(path, 16, comment)` |
| `.setValidator(ConfigValidator.range(2, 64))` | `Validator.range(2, 64)` as the third argument |
| `.setComment("...")` | the last argument of the builder method |
| `ListValidators.stringList(1, 10)` | `Validator.sizeRange(1, 10)` with `stringListValue` |
| `setFileHeader(...)` / `setHeaderComment(...)` | `header(...)` on the schema / on a section |
| `config.load()` in `onEnable` | `ConfigRegistry.register(...)` + `ConfigManager.getInstance().loadAll()` |
| `MainConfig.getInstance().getArena().getMaxPlayers()` | `ConfigManager.getInstance().getSection("config", "arena").getInt("max-players")` |
| `reload()` | `ConfigManager.getInstance().reload("config")` |
| `saveValue(value)` | `config.with(path, value).save(file)`, then `reload(name)` |
| `clearCache()` / `setCacheEnabled(...)` | not needed: a loaded `Config` is already in memory |

To keep typed getters like `getArena().getMaxPlayers()`, either write them as small methods over `getSection(...)` or use [generated wrappers](generated-wrappers.md).

## Behaviour differences

| | Legacy | Schema API |
| --- | --- | --- |
| Invalid value in the file | Replaced by the default, and the default is saved over the file | Wrong types fall back to the default with a warning; validator failures stop the load and are reported. The file is not changed |
| Missing key | Default used | Default used, with a warning naming the key |
| Changing a value | `saveValue` changes the value in place and writes the file | `with(...)` returns a new config; you choose when to save |
| Missing file | Generated from the values read in `loadData()` | Generated from the schema, with section headers |
| Type of `5` read as a Double | Converted | Converted |

## Steps

1. Write a schema with the same name and paths as the legacy config.
2. Register it with `ConfigRegistry.register(...)` and call `ConfigManager.init(this)` and `loadAll()` in `onEnable`, if the plugin does not already.
3. Replace calls to the legacy getters with `ConfigManager` lookups, or keep the getter names and re-implement them over `getSection(...)`.
4. Delete the `AbstractConfig` class and its containers, and remove the old `load()` call.
5. Start the server with an existing file and with no file, and check the log for warnings about missing keys.
