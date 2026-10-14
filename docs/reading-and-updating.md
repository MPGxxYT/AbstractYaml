# Reading and updating values

A loaded config is a `Config`: an immutable snapshot of every value in its schema. `ConfigSection` is a view of the same config under a path prefix. Both share the `ConfigView` interface, so the same getters work on either.

## Typed getters

```java
Config config = ConfigManager.getInstance().get("shop");

String title = config.getString("shop.title");
double tax = config.getDouble("shop.tax");
List<Integer> hours = config.getIntList("shop.open-hours");
List<Map<String, Object>> items = config.getMapList("shop.items");
Map<String, Integer> limits = config.getIntMap("shop.daily-limits");
```

| Getter | Value type |
| --- | --- |
| `getInt`, `getDouble`, `getString`, `getBoolean` | `int`, `double`, `String`, `boolean` |
| `getStringList`, `getIntList`, `getDoubleList` | `List<String>`, `List<Integer>`, `List<Double>` |
| `getMapList` | `List<Map<String, Object>>` |
| `getMap`, `getStringMap`, `getIntMap` | `Map<String, Object>`, `Map<String, String>`, `Map<String, Integer>` |

A getter throws `IllegalArgumentException` if the path is not in the schema, or if the value at that path has a different type (`Value at shop.title is not an integer`). Both are programming errors. A bad value in the file never reaches your code: the loader either replaces it with the default or refuses to load the config.

Treat returned lists and maps as read-only. Values read from a file are unmodifiable; defaults are returned as the schema built them.

## Sections

```java
ConfigSection shop = config.section("shop");
String title = shop.getString("title");               // shop.title

ConfigSection sounds = shop.section("sounds");         // sections nest
String buy = sounds.getString("buy");                  // shop.sounds.buy

ConfigSection same = ConfigManager.getInstance().getSection("shop", "shop.sounds");
```

`prefix()` returns the full prefix (`"shop.sounds"`) and `config()` returns the config the section belongs to.

## Optional lookups

When a path may not exist, for example when it is built at runtime, use the `Optional` forms instead of catching exceptions:

```java
Optional<String> title = config.getValue("shop.title");        // empty if the path is unknown
Optional<ConfigValue<Double>> tax = config.get("shop.tax");     // the value with its default and comment
ConfigValue<Double> required = config.getOrThrow("shop.tax");
```

`allValues()` returns every value in declaration order, keyed by path.

## Updating values

A `Config` never changes. `with(path, value)` returns a new config with one value replaced, after converting and validating it:

```java
Config updated = config.with("shop.tax", 0.1);
config.getDouble("shop.tax");   // still 0.05
updated.getDouble("shop.tax");  // 0.1

Config bulk = config.withAll(Map.of("shop.tax", 0.1, "shop.title", "Market"));
Config fromSection = config.section("shop").set("title", "Market"); // returns the whole config
```

The new value goes through the same conversion as values read from a file, so `with("shop.open-hours", List.of(9L, 21L))` stores integers. A value that can't be converted, or that fails the validator, throws `IllegalArgumentException` and nothing changes.

## Saving

`toYaml()` renders a config with all of its comments, and `save(file)` writes that text to a file:

```java
File file = new File(plugin.getDataFolder(), "shop.yml");
updated.save(file);
ConfigManager.getInstance().reload("shop");
```

> [!IMPORTANT]
> `ConfigManager` keeps the config it loaded. After saving an updated copy, call `reload(name)` so that `get(name)` returns the new values.

> [!WARNING]
> `save` rewrites the whole file from the schema. Anything a server owner added that the schema does not declare, including their own comments, is not kept.
