package me.mortaldev.abstractyaml.config2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class ConfigSectionTest {

  private final Config config =
      ConfigLoader.builder()
          .schema(
              new ConfigSchema("sections") {
                {
                  section("scoring")
                      .intValue("base", 10, null)
                      .section("rewards")
                      .intValue("kill", 5, Validator.min(0), null)
                      .stringMapValue("titles", Map.of("1", "Gold"), null);
                }
              })
          .source(ConfigSource.inMemory(null))
          .logger(Logger.getLogger("test"))
          .load();

  @Test
  void typedGetters_resolveRelativeToThePrefix() {
    ConfigSection scoring = config.section("scoring");
    assertEquals("scoring", scoring.prefix());
    assertEquals(10, scoring.getInt("base"));
    assertEquals(5, scoring.getInt("rewards.kill"));
    assertEquals(Map.of("1", "Gold"), scoring.getStringMap("rewards.titles"));
  }

  @Test
  void sections_nest() {
    ConfigSection rewards = config.section("scoring").section("rewards");
    assertEquals("scoring.rewards", rewards.prefix());
    assertEquals(5, rewards.getInt("kill"));
    assertEquals(5, rewards.get("kill").value());
    assertEquals(Optional.of(5), rewards.getValue("kill"));
    assertEquals(Optional.empty(), rewards.getValue("missing"));
  }

  @Test
  void set_returnsUpdatedRootConfig() {
    Config updated = config.section("scoring").section("rewards").set("kill", 8);
    assertEquals(8, updated.getInt("scoring.rewards.kill"));
    assertEquals(5, config.getInt("scoring.rewards.kill"));
    assertThrows(
        IllegalArgumentException.class, () -> config.section("scoring").set("rewards.kill", -1));
  }

  @Test
  void wrongType_errorNamesTheFullPath() {
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> config.section("scoring").getStringList("base"));
    assertTrue(e.getMessage().contains("scoring.base"), e.getMessage());
  }

  @Test
  void configAndSection_shareTheViewInterface() {
    List<ConfigView> views = List.of(config, config.section("scoring"));
    assertEquals(10, views.get(0).section("scoring").getInt("base"));
    assertEquals(10, views.get(1).getInt("base"));
  }
}
