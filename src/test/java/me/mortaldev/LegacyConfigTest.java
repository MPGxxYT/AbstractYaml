package me.mortaldev;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings({"deprecation", "rawtypes"})
class LegacyConfigTest {

  /** Runs without a plugin; records saves instead of writing files. */
  static class TestConfig extends AbstractConfig {
    final List<String> saved = new ArrayList<>();

    TestConfig(String yaml) throws InvalidConfigurationException {
      YamlConfiguration config = new YamlConfiguration();
      config.loadFromString(yaml);
      useConfig(config);
    }

    @Override
    public void log(String message) {}

    @Override
    public String getName() {
      return "test";
    }

    @Override
    public JavaPlugin getMain() {
      return null;
    }

    @Override
    public void loadData() {}

    @Override
    public void saveValue(String path, Object value, boolean saveToFile) {
      saved.add(path);
      super.saveValue(path, value, false);
    }
  }

  @Test
  void numbers_convertWithoutCountingAsInvalid() throws Exception {
    TestConfig config = new TestConfig("radius: 5\ncount: 3.0\nbig: 7\n");

    assertEquals(5.0, config.getConfigValue(new ConfigValue<>("radius", Double.class, 1.0)).getValue());
    assertEquals(3, config.getConfigValue(new ConfigValue<>("count", Integer.class, 1)).getValue());
    assertEquals(7L, config.getConfigValue(new ConfigValue<>("big", Long.class, 1L)).getValue());
    assertEquals(List.of(), config.saved);
  }

  @Test
  void fractionForInteger_isInvalidAndResetToDefault() throws Exception {
    TestConfig config = new TestConfig("count: 3.5\n");
    assertEquals(1, config.getConfigValue(new ConfigValue<>("count", Integer.class, 1)).getValue());
    assertEquals(List.of("count"), config.saved);
  }

  @Test
  void failedValidation_cachesTheDefaultNotTheInvalidValue() throws Exception {
    TestConfig config = new TestConfig("level: 500\n");
    ConfigValue<Integer> level =
        new ConfigValue<>("level", Integer.class, 1).setValidator(ConfigValidator.range(1, 100));

    assertEquals(1, config.getConfigValue(level).getValue());
    assertEquals(List.of("level"), config.saved);

    ConfigValue<Integer> again =
        new ConfigValue<>("level", Integer.class, 1).setValidator(ConfigValidator.range(1, 100));
    assertEquals(1, config.getConfigValue(again).getValue());
  }

  @Test
  void cachedValueOfTheWrongType_isIgnored() throws Exception {
    TestConfig config = new TestConfig("count: 4\n");
    config.saveValue("count", "not a number", false);
    config.getConfig().set("count", 4);

    assertEquals(4, config.getConfigValue(new ConfigValue<>("count", Integer.class, 1)).getValue());
  }

  @Test
  void maps_areValidatedAndNestedSectionsBecomePlainMaps() throws Exception {
    TestConfig config = new TestConfig("stats:\n  health: 20\n  nested:\n    mana: 5\n");
    ConfigValue<Map> stats =
        new ConfigValue<>("stats", Map.class, Map.of())
            .setValidator(new ConfigValidator<Map>().addRule(m -> m.size() < 5, "too big"));

    Map value = config.getConfigValue(stats).getValue();
    assertEquals(Map.of("health", 20, "nested", Map.of("mana", 5)), value);

    ConfigValue<Map> strict =
        new ConfigValue<>("stats", Map.class, Map.of("health", 1))
            .setValidator(new ConfigValidator<Map>().addRule(m -> m.size() < 2, "too big"));
    config.clearCache();
    assertEquals(Map.of("health", 1), config.getConfigValue(strict).getValue());
  }

  @Test
  void validator_stopsAtTheFirstFailingRule() {
    assertFalse(ListValidators.minSize(1).validate(null).isValid());
    assertEquals(
        List.of("All elements must be integers"),
        ListValidators.allIntegersInRange(0, 10).validate(List.of("a")).errors());
  }

  @Test
  void oneOf_worksForNonStrings() {
    ConfigValidator<Integer> validator = ConfigValidator.oneOf(1, 2, 3);
    assertTrue(validator.validate(2).isValid());
    assertEquals("Value must be one of: 1, 2, 3", validator.validate(4).getErrorMessage());
  }

  @Test
  void yamlWriter_writesNestedPathsOnceWithHeaders(@TempDir Path dir) throws IOException {
    File file = dir.resolve("out.yml").toFile();
    YamlWriter.writeYaml(
        file,
        List.of(
            new YamlWriter.YamlEntry("game.rules.a", 1, null, "Game settings"),
            new YamlWriter.YamlEntry("game.rules.b", 2, null, null),
            new YamlWriter.YamlEntry("game.name", "ctf", "Mode", null),
            new YamlWriter.YamlEntry("stats", Map.of("hp", 20), null, null)),
        "Top");

    assertEquals(
        """
        # Top

        # Game settings
        game:
          rules:
            a: 1
            b: 2
          name: "ctf"                           # Mode

        stats:
          hp: 20
        """,
        Files.readString(file.toPath()));
  }

  @Test
  void reloadConfig_saysItCannotWork() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> YAML.getInstance().reloadConfig(null, new YamlConfiguration()));
  }
}
