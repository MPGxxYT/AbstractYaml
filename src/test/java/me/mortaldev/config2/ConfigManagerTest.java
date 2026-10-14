package me.mortaldev.config2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ConfigManagerTest {
  private static final Logger LOGGER = Logger.getLogger("test");

  @RegisterConfig(priority = 20)
  public static class LateSchema extends ConfigSchema {
    public LateSchema() {
      super("late");
      section("a").intValue("n", 1, null);
    }
  }

  @RegisterConfig(priority = 10, path = "configs/early.yml")
  public static class EarlySchema extends ConfigSchema {
    public EarlySchema() {
      super("early");
      section("a").intValue("n", 2, Validator.min(0), null);
    }
  }

  public static class UnannotatedSchema extends ConfigSchema {
    public UnannotatedSchema() {
      super("plain");
    }
  }

  @RegisterConfig
  public static class BrokenSchema extends ConfigSchema {
    public BrokenSchema(String required) {
      super("broken");
    }
  }

  /** In-memory storage that records the order sources are read in. */
  private final Map<String, ConfigSource> store = new HashMap<>();
  private final List<String> reads = new ArrayList<>();

  private ConfigSource source(String path) {
    ConfigSource memory = store.computeIfAbsent(path, p -> ConfigSource.inMemory(null));
    return new ConfigSource() {
      @Override
      public String read() {
        reads.add(path);
        return memory.read();
      }

      @Override
      public void write(String yaml) {
        memory.write(yaml);
      }

      @Override
      public String name() {
        return path;
      }
    };
  }

  private ConfigManager manager() {
    return ConfigManager.create(LOGGER, this::source);
  }

  @AfterEach
  void resetShared() {
    ConfigManager.reset();
    ConfigRegistry.clear();
  }

  @Test
  void loadAll_followsPriorityAndAnnotationPaths() {
    ConfigManager manager = manager().register(LateSchema.class).register(EarlySchema.class);
    assertTrue(manager.loadAll().isEmpty());

    assertEquals(List.of("configs/early.yml", "late.yml"), reads);
    assertEquals(2, manager.get("early").getInt("a.n"));
    assertEquals(1, manager.getSection("late", "a").getInt("n"));
    assertEquals(2, manager.count());
  }

  @Test
  void loadAll_reportsFailuresAndLoadsTheRest() {
    store.put("configs/early.yml", ConfigSource.inMemory("a:\n  n: -5\n"));
    ConfigManager manager = manager().register(EarlySchema.class).register(LateSchema.class);

    Map<String, Exception> failures = manager.loadAll();

    assertEquals(List.of("early"), List.copyOf(failures.keySet()));
    assertInstanceOf(ConfigLoader.ConfigLoadException.class, failures.get("early"));
    assertTrue(manager.isLoaded("late"));
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> manager.get("early"));
    assertTrue(e.getMessage().contains("failed to load"), e.getMessage());
    assertThrows(IllegalArgumentException.class, () -> manager.get("nope"));
  }

  @Test
  void failedReload_keepsThePreviousValues() {
    ConfigManager manager = manager().register(EarlySchema.class);
    manager.loadAll();
    store.get("configs/early.yml").write("a:\n  n: -5\n");

    assertThrows(ConfigLoader.ConfigLoadException.class, () -> manager.reload("early"));
    assertEquals(2, manager.get("early").getInt("a.n"));

    store.get("configs/early.yml").write("a:\n  n: 7\n");
    assertTrue(manager.reloadAll().isEmpty());
    assertEquals(7, manager.get("early").getInt("a.n"));
  }

  @Test
  void register_rejectsBadSchemasImmediately() {
    ConfigManager manager = manager();
    assertThrows(IllegalArgumentException.class, () -> manager.register(UnannotatedSchema.class));
    assertThrows(IllegalArgumentException.class, () -> manager.register(BrokenSchema.class));
    manager.register(LateSchema.class);
    assertThrows(IllegalArgumentException.class, () -> manager.register(LateSchema.class));
  }

  @Test
  void register_acceptsUnannotatedInstances() {
    ConfigManager manager = manager().register(new UnannotatedSchema());
    assertTrue(manager.loadAll().isEmpty());
    assertTrue(manager.isLoaded("plain"));
    assertEquals(List.of("plain.yml"), reads);
  }

  @Test
  void sharedRegistry_checksAnnotationsAndCounts() {
    assertThrows(
        IllegalArgumentException.class, () -> ConfigRegistry.register(UnannotatedSchema.class));
    ConfigRegistry.register(LateSchema.class);
    assertEquals(1, ConfigRegistry.count());
    assertThrows(IllegalStateException.class, ConfigManager::getInstance);
  }
}
