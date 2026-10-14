# Schemas

A schema describes one YAML file: every value it contains, with a type, a default, an optional validator and an optional comment. It is the only place a value's path is written down.

## Anatomy of a schema

```java
public class ShopSchema extends ConfigSchema {
  public ShopSchema() {
    super("shop");                                   // file name: shop.yml
    header("Shop settings\nPrices are in coins.");   // comment at the top of the file

    section("shop")
        .header("Items for sale")                    // comment above the section
        .stringValue("title", "Item Shop", "Inventory title")
        .doubleValue("tax", 0.05, Validator.range(0.0, 1.0), "Fraction added to prices")
        .intListValue("open-hours", List.of(8, 22), Validator.sizeRange(2, 2), "Opening and closing hour")
        .mapListValue("items", List.of(sword, bow), "Items, in display order")
        .intMapValue("daily-limits", limits, "Purchases per day by rank")
        .section("sounds")                           // nested section: shop.sounds
        .stringValue("buy", "ENTITY_EXPERIENCE_ORB_PICKUP", "Played on purchase");
  }
}
```

The generated `shop.yml`:

```yaml
# Shop settings
# Prices are in coins.

# Items for sale
shop:
  title: "Item Shop"                    # Inventory title
  tax: 0.05                             # Fraction added to prices
  open-hours: [8, 22]                   # Opening and closing hour
  items:  # Items, in display order
    - material: "DIAMOND_SWORD"
      price: 250
    - material: "BOW"
      price: 120
  daily-limits:  # Purchases per day by rank
    vip: 10
    default: 3
  sounds:
    buy: "ENTITY_EXPERIENCE_ORB_PICKUP"  # Played on purchase
```

## Value types

Every builder method takes a key, a default and a comment (`null` for none). Most also have an overload with a `Validator` before the comment.

| Builder method | YAML | Read with |
| --- | --- | --- |
| `intValue(key, int, comment)` | `count: 5` | `getInt` |
| `doubleValue(key, double, comment)` | `radius: 2.5` | `getDouble` |
| `stringValue(key, String, comment)` | `name: "Arena"` | `getString` |
| `boolValue(key, boolean, comment)` | `enabled: true` | `getBoolean` |
| `stringListValue(key, List<String>, comment)` | a block list, one item per line | `getStringList` |
| `intListValue(key, List<Integer>, validator, comment)` | `levels: [1, 5, 10]` | `getIntList` |
| `doubleListValue(key, List<Double>, validator, comment)` | `weights: [0.5, 1.5]` | `getDoubleList` |
| `mapListValue(key, List<Map<String, Object>>, comment)` | a list of objects | `getMapList` |
| `mapValue(key, Map<String, Object>, comment)` | a nested object with any values | `getMap` |
| `stringMapValue(key, Map<String, String>, comment)` | `red: "#FF0000"` entries | `getStringMap` |
| `intMapValue(key, Map<String, Integer>, comment)` | `vip: 10` entries | `getIntMap` |

`intList(key, default, comment)`, `doubleList(...)` and `stringList(...)` are the no-validator forms of the list methods.

> [!TIP]
> `Map.of(...)` has no fixed iteration order, so keys in the generated file can come out shuffled. Build map defaults with a `LinkedHashMap` when the order matters to readers.

## Sections

`section(name)` starts a group of values under a common prefix. Calling `.section(name)` on a section nests another level; `.parent()` goes back up one level.

```java
section("arena")
    .intValue("max-players", 16, null)
    .section("lobby")
    .intValue("countdown", 30, null)          // arena.lobby.countdown
    .parent()
    .boolValue("allow-spectators", true, null); // arena.allow-spectators
```

Values can also be added at the top level with `value(...)` and a `ConfigValue` record, for example `value(new ConfigValue.Int("version", 1))`.

## Comments and layout

- `header(text)` on the schema writes a comment block at the top of the file. Each line of `text` becomes a `#` line.
- `.header(text)` on a section writes a comment above that section.
- The last argument of every value method is an inline comment. Scalars align comments at column 40; lists and maps put the comment after the key.
- Values appear in the order they were declared, and top-level sections are separated by a blank line.
- Strings are always quoted, so values such as `yes`, `on` or `#FF0000` keep their meaning.

## How a file is read

The loader goes through the schema's values one by one:

| Situation | Result |
| --- | --- |
| The file does not exist | Defaults are validated, written to the file, and used |
| The file is not valid YAML | An error is logged and defaults are used. The file is left as it is |
| A key is missing | A warning is logged and the default is used. The file is not changed |
| A value has the wrong type | A warning naming the path and the problem is logged, and the default is used |
| A value fails its validator | Loading fails with a `ConfigLoadException` that lists every failing path |

Conversions that lose nothing are accepted:

| Type | Accepts | Rejects |
| --- | --- | --- |
| Int | `5`, `5.0`, `"5"` | `5.5`, values outside the `int` range, text |
| Double | any number, `"2.5"` | text, booleans |
| String | text, numbers, booleans | lists and maps |
| Boolean | `true`, `false`, `"true"`, `"false"` (any case) | numbers, other text |
| Lists | a list whose elements all convert | a list with one bad element (the whole list falls back to its default) |
| String and int maps | a map whose values all convert; keys become strings | a map with one bad value |
| Maps and map lists | any nested YAML; keys become strings | non-map elements in a map list |

An explicit empty list (`worlds: []`) is kept as an empty list.

> [!NOTE]
> Keys added to a schema later are not written into existing files. The default is used and a warning names the missing key. Server owners can add the key by hand, or delete the file to regenerate it.

## Registration annotation

`@RegisterConfig` tells [ConfigManager](config-manager.md) how to load the schema:

```java
@RegisterConfig(priority = 10, value = "shop", path = "economy/shop.yml")
```

| Attribute | Default | Meaning |
| --- | --- | --- |
| `priority` | `100` | Lower numbers load first |
| `value` | the name passed to `super(...)` | The name used with `ConfigManager.get(name)` |
| `path` | `<name>.yml` | File path relative to the plugin's data folder |
