package me.mortaldev.config2;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigRoundTripTest {
  private static final Logger LOGGER = Logger.getLogger("test");

  @TempDir Path dir;

  private Config load(ConfigSource source) {
    return ConfigLoader.builder().schema(new AllTypesSchema()).source(source).logger(LOGGER).load();
  }

  private Config load(String yaml) {
    return load(ConfigSource.inMemory(yaml));
  }

  @Test
  void generatedText_readsBackEveryType() {
    ConfigSource source = ConfigSource.inMemory(null);
    Config generated = load(source);
    assertNotNull(source.read());

    Config reloaded = load(source);
    for (ConfigValue<?> value : generated.allValues().values()) {
      assertEquals(
          value.value(),
          reloaded.getOrThrow(value.path()).value(),
          "round trip changed " + value.path());
    }
  }

  @Test
  void fileSource_generatesAndReloadsFromDisk() {
    File file = dir.resolve("nested/dir/all-types.yml").toFile();
    Config generated = load(ConfigSource.file(file));
    assertTrue(file.exists());
    Config reloaded = load(ConfigSource.file(file));
    for (ConfigValue<?> value : generated.allValues().values()) {
      assertEquals(value.value(), reloaded.getOrThrow(value.path()).value());
    }
  }

  @Test
  void userValues_areReadForEveryType() {
    Config config =
        load(
        """
        scalars:
          count: 42
          radius: 7
          name: custom
          enabled: false
        lists:
          names: []
          numbers: [9, 8]
          weights: [1]
        maps:
          points:
            - name: base
              radius: 1.5
          stats:
            health: 5
          colors:
            green: "#00FF00"
          levels:
            rogue: 3
        """);
    assertEquals(42, config.getInt("scalars.count"));
    assertEquals(7.0, config.getDouble("scalars.radius"));
    assertEquals("custom", config.getString("scalars.name"));
    assertFalse(config.getBoolean("scalars.enabled"));
    assertEquals(List.of(), config.getStringList("lists.names"));
    assertEquals(List.of(9, 8), config.getIntList("lists.numbers"));
    assertEquals(List.of(1.0), config.getDoubleList("lists.weights"));
    assertEquals(List.of(Map.of("name", "base", "radius", 1.5)), config.getMapList("maps.points"));
    assertEquals(Map.of("health", 5), config.getMap("maps.stats"));
    assertEquals(Map.of("green", "#00FF00"), config.getStringMap("maps.colors"));
    assertEquals(Map.of("rogue", 3), config.getIntMap("maps.levels"));
  }

  @Test
  void invalidValue_fallsBackToDefault() {
    assertEquals(5, load("scalars:\n  count: lots\n").getInt("scalars.count"));
  }

  @Test
  void malformedYaml_usesDefaultsAndLeavesTheTextAlone() {
    ConfigSource source = ConfigSource.inMemory("scalars: [unclosed");
    assertEquals(5, load(source).getInt("scalars.count"));
    assertEquals("scalars: [unclosed", source.read());
  }

  @Test
  void outOfRangeValue_failsValidation() {
    assertThrows(ConfigLoader.ConfigLoadException.class, () -> load("scalars:\n  count: 500\n"));
  }

  @Test
  void invalidDefaults_failValidationOnFirstRun() {
    ConfigSchema broken =
        new ConfigSchema("broken") {
          {
            section("a").intValue("n", -1, Validator.min(0), "must be positive");
          }
        };
    ConfigSource source = ConfigSource.inMemory(null);
    assertThrows(
        ConfigLoader.ConfigLoadException.class,
        () -> ConfigLoader.builder().schema(broken).source(source).logger(LOGGER).load());
    assertNull(source.read());
  }

  @Test
  void generatedText_followsDeclarationOrderWithHeaders() {
    ConfigSchema schema =
        new ConfigSchema("ordered") {
          {
            section("zeta")
                .header("Last letter, declared first")
                .intValue("b", 1, null)
                .intValue("a", 2, null);
            section("alpha").section("inner").header("Nested header").boolValue("x", true, null);
            section("middle").stringValue("s", "v", null);
          }
        };
    String yaml =
        ConfigLoader.builder()
            .schema(schema)
            .source(ConfigSource.inMemory(null))
            .logger(LOGGER)
            .load()
            .toYaml();

    assertEquals(
        """
        # Last letter, declared first
        zeta:
          b: 1
          a: 2

        alpha:
          # Nested header
          inner:
            x: true

        middle:
          s: "v"
        """,
        yaml);
  }

  @Test
  void with_convertsCompatibleValuesAndRejectsOthers() {
    Config config = load((String) null);

    Config updated = config.with("scalars.count", 7L);
    assertEquals(7, updated.getInt("scalars.count"));
    assertEquals(5, config.getInt("scalars.count"));

    assertThrows(IllegalArgumentException.class, () -> config.with("scalars.count", "seven"));
    assertThrows(IllegalArgumentException.class, () -> config.with("scalars.count", 500));
  }

  @Test
  void typedGetter_onWrongType_throws() {
    Config config = load((String) null);
    assertThrows(IllegalArgumentException.class, () -> config.getInt("scalars.name"));
    assertThrows(IllegalArgumentException.class, () -> config.getInt("missing"));
  }
}
