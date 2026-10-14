# Testing

Configs can be loaded in plain unit tests: no server, no `JavaPlugin`, no files. The examples use JUnit 5.

## ConfigSource

A `ConfigSource` is where a config's YAML text is stored. The library provides two:

| Source | Use |
| --- | --- |
| `ConfigSource.file(File)` | A file on disk. Used by `ConfigLoader.builder(plugin)` and `ConfigManager.forPlugin` |
| `ConfigSource.inMemory(String)` | A string held in memory. Pass `null` to start empty. Writes replace the string, so a test can inspect what was generated |

Any other storage (a database row, a resource in the jar) can implement the three methods `read()`, `write(String)` and `name()`.

## Testing a schema

`ConfigLoader.builder()` without a plugin needs a source:

```java
class ShopSchemaTest {

  private Config load(String yaml) {
    return ConfigLoader.builder()
        .schema(new ShopSchema())
        .source(ConfigSource.inMemory(yaml))
        .load();
  }

  @Test
  void defaultsAreValidAndGenerated() {
    ConfigSource source = ConfigSource.inMemory(null);
    Config config = ConfigLoader.builder().schema(new ShopSchema()).source(source).load();

    assertEquals(0.05, config.getDouble("shop.tax"));
    assertTrue(source.read().contains("# Items for sale"));
  }

  @Test
  void readsServerOwnerValues() {
    Config config = load("shop:\n  tax: 0.2\n  open-hours: [9, 21]\n");
    assertEquals(0.2, config.getDouble("shop.tax"));
    assertEquals(List.of(9, 21), config.getIntList("shop.open-hours"));
  }

  @Test
  void rejectsOutOfRangeTax() {
    assertThrows(ConfigLoader.ConfigLoadException.class, () -> load("shop:\n  tax: 3\n"));
  }
}
```

Running the "defaults" test for every schema catches a default that fails its own validator before a server does.

## Testing code that uses ConfigManager

Create a manager per test instead of using the shared instance:

```java
Map<String, ConfigSource> files = new HashMap<>();
ConfigManager manager = ConfigManager.create(
    Logger.getLogger("test"),
    path -> files.computeIfAbsent(path, p -> ConfigSource.inMemory(null)));

manager.register(ShopSchema.class);
assertTrue(manager.loadAll().isEmpty());

files.get("shop.yml").write("shop:\n  tax: -1\n");
assertEquals(Set.of("shop"), manager.reloadAll().keySet());
assertEquals(0.05, manager.get("shop").getDouble("shop.tax")); // previous values kept
```

If code under test calls `ConfigManager.getInstance()`, clear the shared state after each test:

```java
@AfterEach
void resetConfigs() {
  ConfigManager.reset();
  ConfigRegistry.clear();
}
```
