package me.mortaldev.config2.example;

import me.mortaldev.config2.*;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Example showing how to use the new config system.
 *
 * <p><b>Benefits over the old system:</b>
 * <ul>
 *   <li>No boilerplate - just define your schema
 *   <li>Type-safe - no casting needed
 *   <li>Immutable - thread-safe by default
 *   <li>Easy to update - builder-style updates
 *   <li>Auto-validates - catch errors early
 *   <li>Auto-generates - files created with comments
 * </ul>
 */
public class UsageExample {

  public static void example(JavaPlugin plugin) {

    // ===== 1. Load Configuration =====

    // Create schema and load config in one line
    Config abilities = ConfigLoader.builder(plugin)
        .schema(new AbilitiesConfigSchema())
        .load();

    // ===== 2. Read Values (Type-Safe!) =====

    // Direct access - no casting!
    int keraunosCooldown = abilities.getInt("keraunos.cooldown");
    double keraunosRadius = abilities.getDouble("keraunos.radius");
    boolean abilitiesEnabled = abilities.getBoolean("abilities-enabled");

    plugin.getLogger().info("Keraunos cooldown: " + keraunosCooldown);
    plugin.getLogger().info("Keraunos radius: " + keraunosRadius);

    // ===== 3. Using Sections (Cleaner Access) =====

    ConfigSection keraunos = abilities.section("keraunos");
    int cooldown = keraunos.getInt("cooldown");  // Auto-prefixes with "keraunos."
    double radius = keraunos.getDouble("radius");
    double hStrength = keraunos.getDouble("horizontal-strength");
    double vStrength = keraunos.getDouble("vertical-strength");

    plugin.getLogger().info(String.format(
        "Keraunos: cooldown=%d, radius=%.1f, strength=(%.1f, %.1f)",
        cooldown, radius, hStrength, vStrength));

    ConfigSection medkit = abilities.section("medkit");
    int medkitRange = medkit.getInt("target-range");
    int medkitCooldown = medkit.getInt("cooldown");
    int medkitCharges = medkit.getInt("total-charges");

    // ===== 4. Updating Values (Immutable Pattern) =====

    // Update a single value - returns new config
    Config updated = abilities.with("keraunos.cooldown", 15);

    // Original is unchanged (immutable)
    plugin.getLogger().info("Original cooldown: " + abilities.getInt("keraunos.cooldown")); // 10
    plugin.getLogger().info("Updated cooldown: " + updated.getInt("keraunos.cooldown"));   // 15

    // Update through section
    Config updated2 = keraunos.set("cooldown", 20);

    // Save changes
    updated2.save(plugin.getDataFolder().toPath().resolve("abilities.yml").toFile());

    // ===== 5. Validation =====

    // Validate all values
    ValidationResult validation = abilities.validate();
    if (!validation.isValid()) {
      plugin.getLogger().severe("Config validation failed:");
      validation.errors().forEach(error -> plugin.getLogger().severe("  - " + error));
    }

    // Validation happens automatically when setting values
    try {
      Config invalid = abilities.with("keraunos.cooldown", 999); // Max is 300
    } catch (IllegalArgumentException e) {
      plugin.getLogger().warning("Invalid value: " + e.getMessage());
    }

    // ===== 6. Reloading =====

    ConfigLoader loader = ConfigLoader.builder(plugin)
        .schema(new AbilitiesConfigSchema())
        .build();

    Config reloaded = loader.reload();
    plugin.getLogger().info("Config reloaded!");

    // ===== 7. Working with Lists =====

    // Example with a list config
    ConfigSchema listsSchema = new ConfigSchema("example") {
      {
        value(new ConfigValue.StringList(
            "allowed-commands",
            java.util.List.of("help", "info", "stats"),
            "Commands players can use"));

        value(new ConfigValue.IntList(
            "reward-levels",
            java.util.List.of(1, 5, 10, 25, 50),
            Validator.allIntegersInRange(1, 100),
            "Levels that give rewards"));
      }
    };

    Config listConfig = ConfigLoader.builder(plugin)
        .schema(listsSchema)
        .file("example.yml")
        .load();

    java.util.List<String> commands = listConfig.getStringList("allowed-commands");
    java.util.List<Integer> levels = listConfig.getIntList("reward-levels");

    plugin.getLogger().info("Allowed commands: " + commands);
    plugin.getLogger().info("Reward levels: " + levels);
  }

  /**
   * Example of migration from old system to new system.
   */
  public static void migrationExample(JavaPlugin plugin) {

    // OLD SYSTEM:
    // ----------------------------------------
    // AbilitiesConfig config = AbilitiesConfig.getInstance();
    // config.load();
    // int cooldown = config.getKeraunosCooldown();
    // double radius = config.getKeraunosRadius();
    // config.setKeraunosCooldown(15);
    // config.saveConfig();

    // NEW SYSTEM (Much Simpler!):
    // ----------------------------------------
    Config abilities = ConfigLoader.builder(plugin)
        .schema(new AbilitiesConfigSchema())
        .load();

    int cooldown = abilities.getInt("keraunos.cooldown");
    double radius = abilities.getDouble("keraunos.radius");

    Config updated = abilities.with("keraunos.cooldown", 15);
    updated.save(plugin.getDataFolder().toPath().resolve("abilities.yml").toFile());

    // Or use sections for cleaner code:
    ConfigSection keraunos = abilities.section("keraunos");
    int cooldown2 = keraunos.getInt("cooldown");
    Config updated2 = keraunos.set("cooldown", 15);
  }

  /**
   * Advanced example: Creating a wrapper for easier access.
   */
  public static class AbilitiesConfig {
    private final Config config;
    private final ConfigSection keraunos;
    private final ConfigSection medkit;
    private final ConfigSection wraithWater;

    public AbilitiesConfig(JavaPlugin plugin) {
      this.config = ConfigLoader.builder(plugin)
          .schema(new AbilitiesConfigSchema())
          .load();
      this.keraunos = config.section("keraunos");
      this.medkit = config.section("medkit");
      this.wraithWater = config.section("wraith-water");
    }

    // Keraunos getters
    public int getKeraunosCooldown() { return keraunos.getInt("cooldown"); }
    public double getKeraunosRadius() { return keraunos.getDouble("radius"); }
    public double getKeraunosHorizontalStrength() { return keraunos.getDouble("horizontal-strength"); }
    public double getKeraunosVerticalStrength() { return keraunos.getDouble("vertical-strength"); }

    // Medkit getters
    public int getMedkitRange() { return medkit.getInt("target-range"); }
    public int getMedkitCooldown() { return medkit.getInt("cooldown"); }
    public int getMedkitCharges() { return medkit.getInt("total-charges"); }

    // Wraith Water getters
    public int getWraithWaterDuration() { return wraithWater.getInt("duration"); }

    // Update methods
    public void setKeraunosCooldown(int value) {
      Config updated = keraunos.set("cooldown", value);
      updated.save(/* file */);
    }

    public void reload(JavaPlugin plugin) {
      // Reload logic here
    }
  }
}
