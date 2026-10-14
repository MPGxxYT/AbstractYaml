package me.mortaldev.config2;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigRoundTripTest {
  private static final Logger LOGGER = Logger.getLogger("test");

  @TempDir Path dir;

  private Config load(File file) {
    return ConfigLoader.builder(null)
        .schema(new AllTypesSchema())
        .file(file)
        .logger(LOGGER)
        .load();
  }

  @Test
  void generatedFile_readsBackEveryType() {
    File file = dir.resolve("all-types.yml").toFile();
    Config generated = load(file);
    assertTrue(file.exists());

    Config reloaded = load(file);
    for (ConfigValue<?> value : generated.allValues().values()) {
      assertEquals(
          value.value(),
          reloaded.getOrThrow(value.path()).value(),
          "round trip changed " + value.path());
    }
  }

  @Test
  void userValues_areReadForEveryType() throws IOException {
    File file = dir.resolve("all-types.yml").toFile();
    Files.writeString(
        file.toPath(),
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

    Config config = load(file);
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
  void invalidValue_fallsBackToDefault() throws IOException {
    File file = dir.resolve("all-types.yml").toFile();
    Files.writeString(file.toPath(), "scalars:\n  count: lots\n");
    assertEquals(5, load(file).getInt("scalars.count"));
  }

  @Test
  void with_convertsCompatibleValuesAndRejectsOthers() {
    Config config = load(dir.resolve("all-types.yml").toFile());

    Config updated = config.with("scalars.count", 7L);
    assertEquals(7, updated.getInt("scalars.count"));
    assertEquals(5, config.getInt("scalars.count"));

    assertThrows(IllegalArgumentException.class, () -> config.with("scalars.count", "seven"));
    assertThrows(IllegalArgumentException.class, () -> config.with("scalars.count", 500));
  }

  @Test
  void typedGetter_onWrongType_throws() {
    Config config = load(dir.resolve("all-types.yml").toFile());
    assertThrows(IllegalArgumentException.class, () -> config.getInt("scalars.name"));
    assertThrows(IllegalArgumentException.class, () -> config.getInt("missing"));
  }
}
