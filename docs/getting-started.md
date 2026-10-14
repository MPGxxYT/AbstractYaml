# Getting started

This guide builds one config for a plugin, from the schema to a reload command. It assumes the dependency is already added (see [Installation](../README.md#installation)).

## 1. Write a schema

A schema is a class that extends `ConfigSchema` and declares its values in the constructor. The name passed to `super(...)` becomes the file name.

```java
package com.example.myplugin.config;

import java.util.List;
import me.mortaldev.config2.ConfigSchema;
import me.mortaldev.config2.RegisterConfig;
import me.mortaldev.config2.Validator;

@RegisterConfig
public class MainSchema extends ConfigSchema {
  public MainSchema() {
    super("config"); // plugins/MyPlugin/config.yml
    header("MyPlugin configuration");

    section("messages")
        .stringValue("prefix", "&6[MyPlugin] ", "Shown before every message")
        .stringValue("welcome", "Welcome, %player%!", Validator.notEmpty(), "Sent on join");

    section("spawn")
        .boolValue("teleport-on-join", true, "Send players to spawn when they join")
        .intValue("delay", 3, Validator.range(0, 60), "Seconds before teleporting")
        .stringListValue("blocked-worlds", List.of("world_nether"), "Worlds that skip the teleport");
  }
}
```

The schema needs a public no-argument constructor. Keep one builder call per line: the optional [wrapper generator](generated-wrappers.md) reads schema source that way.

## 2. Register and load

Register schema classes in a static block, then initialize the manager in `onEnable`:

```java
public final class MyPlugin extends JavaPlugin {
  static {
    ConfigRegistry.register(MainSchema.class);
  }

  @Override
  public void onEnable() {
    ConfigManager.init(this);
    Map<String, Exception> failures = ConfigManager.getInstance().loadAll();
    if (failures.containsKey("config")) {
      getLogger().severe("config.yml is invalid, disabling: " + failures.get("config").getMessage());
      getServer().getPluginManager().disablePlugin(this);
    }
  }
}
```

On the first start there is no `config.yml`, so the defaults are validated and written:

```yaml
# MyPlugin configuration

messages:
  prefix: "&6[MyPlugin] "               # Shown before every message
  welcome: "Welcome, %player%!"         # Sent on join

spawn:
  teleport-on-join: true                # Send players to spawn when they join
  delay: 3                              # Seconds before teleporting
  blocked-worlds:  # Worlds that skip the teleport
    - "world_nether"
```

## 3. Read values

Get the config by name, or a section of it:

```java
Config config = ConfigManager.getInstance().get("config");
String prefix = config.getString("messages.prefix");

ConfigSection spawn = config.section("spawn");
if (spawn.getBoolean("teleport-on-join")
    && !spawn.getStringList("blocked-worlds").contains(player.getWorld().getName())) {
  int delay = spawn.getInt("delay");
  // ...
}
```

A `Config` is immutable. Fetch it from the manager when you need it instead of keeping a copy in a field, so a reload is picked up.

## 4. Add a reload command

```java
@Override
public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
  Map<String, Exception> failures = ConfigManager.getInstance().reloadAll();
  if (failures.isEmpty()) {
    sender.sendMessage("Configs reloaded.");
  } else {
    failures.forEach((name, error) ->
        sender.sendMessage(name + " kept its previous values: " + error.getMessage()));
  }
  return true;
}
```

If a server owner sets `delay: 500`, the reload reports `Value must be between 0 and 60` and the plugin keeps using the last valid config.

## Next steps

- [Schemas](schemas.md) lists every value type and explains how files are read and written.
- [Validation](validation.md) covers the built-in validators.
- [ConfigManager](config-manager.md) covers load order, custom file paths and several configs.
