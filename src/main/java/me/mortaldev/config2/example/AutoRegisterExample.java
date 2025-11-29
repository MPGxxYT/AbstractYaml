package me.mortaldev.config2.example;

import me.mortaldev.config2.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Complete example of using the auto-registration system.
 */
public class AutoRegisterExample {

  // ===== Step 1: Define Your Schemas =====

  @RegisterConfig(priority = 10) // Loads first
  public static class GameConfigSchema extends ConfigSchema {
    public GameConfigSchema() {
      super("game");

      header("Main Game Configuration");

      section("general")
          .intValue("max-players", 12, Validator.range(1, 100), "Maximum players")
          .stringValue("server-name", "My Server", "Server name")
          .boolValue("pvp-enabled", true, "Enable PvP");

      section("economy")
          .doubleValue("coin-multiplier", 1.0, Validator.min(0.1), "Coin earn rate")
          .intValue("starting-balance", 100, Validator.min(0), "Starting balance");
    }
  }

  @RegisterConfig(priority = 20) // Loads second (may depend on game config)
  public static class AbilitiesConfigSchema extends ConfigSchema {
    public AbilitiesConfigSchema() {
      super("abilities");

      header("Abilities Configuration");

      section("fireball")
          .intValue("cooldown", 10, Validator.range(0, 300), "Cooldown in seconds")
          .doubleValue("damage", 5.0, Validator.min(0.0), "Damage dealt")
          .intValue("range", 50, "Maximum range");

      section("teleport")
          .intValue("cooldown", 30, "Cooldown in seconds")
          .intValue("max-distance", 100, "Max teleport distance");

      value(
          new ConfigValue.StringList(
              "disabled-abilities",
              List.of(),
              Validator.allStringsNotEmpty(),
              "Abilities to disable"));
    }
  }

  // ===== Step 2: Register Schemas in Your Plugin =====

  public static class ExamplePlugin extends JavaPlugin {

    // Register all schemas in static block (runs once on class load)
    static {
      ConfigRegistry.register(GameConfigSchema.class);
      ConfigRegistry.register(AbilitiesConfigSchema.class);
    }

    @Override
    public void onEnable() {
      // Initialize ConfigManager
      ConfigManager.init(this);

      // Load all configs
      ConfigManager.getInstance().loadAll();

      // Use configs...
      exampleUsage();
    }

    private void exampleUsage() {
      ConfigManager cm = ConfigManager.getInstance();

      // ===== Reading Values =====

      // Get config by name
      Config game = cm.get("game");
      int maxPlayers = game.getInt("general.max-players");
      String serverName = game.getString("general.server-name");

      getLogger().info("Max players: " + maxPlayers);
      getLogger().info("Server: " + serverName);

      // Use sections for cleaner code
      ConfigSection economy = game.section("economy");
      double multiplier = economy.getDouble("coin-multiplier");
      int startingBalance = economy.getInt("starting-balance");

      getLogger().info("Coin multiplier: " + multiplier);

      // Get section directly from manager
      ConfigSection fireball = cm.getSection("abilities", "fireball");
      int fireballCooldown = fireball.getInt("cooldown");
      double fireballDamage = fireball.getDouble("damage");

      getLogger().info("Fireball cooldown: " + fireballCooldown);
      getLogger().info("Fireball damage: " + fireballDamage);

      // Lists
      List<String> disabled = cm.get("abilities").getStringList("disabled-abilities");
      getLogger().info("Disabled abilities: " + disabled);

      // ===== Updating Values =====

      // Update and save
      Config updatedGame = game.with("general.max-players", 16);
      updatedGame.save(getDataFolder().toPath().resolve("game.yml").toFile());

      // Or update through section
      Config updatedFireball = fireball.set("cooldown", 15);

      // ===== Reloading =====

      // Reload all configs
      cm.reloadAll();

      // Reload specific config
      cm.reload("abilities");

      // ===== Validation =====

      // Validate all configs
      var results = cm.validateAll();
      results.forEach(
          (name, result) -> {
            if (!result.isValid()) {
              getLogger().severe("Config '" + name + "' has errors:");
              result.errors().forEach(error -> getLogger().severe("  - " + error));
            }
          });

      // Check which configs are loaded
      getLogger().info("Loaded configs: " + cm.getConfigNames());
      getLogger().info("Total configs: " + cm.count());
    }

    // ===== Reload Command Example =====

    public void onReloadCommand(String[] args) {
      ConfigManager cm = ConfigManager.getInstance();

      if (args.length == 0) {
        // Reload all
        cm.reloadAll();
        getLogger().info("Reloaded all configurations!");
      } else {
        // Reload specific
        String configName = args[0];
        if (cm.isLoaded(configName)) {
          cm.reload(configName);
          getLogger().info("Reloaded " + configName + ".yml!");
        } else {
          getLogger().warning("Unknown config: " + configName);
          getLogger().info("Available: " + cm.getConfigNames());
        }
      }
    }
  }

  // ===== Step 3: Access From Anywhere =====

  public static class AbilityHandler {
    public void useFireball() {
      // Access config from anywhere
      ConfigSection fireball = ConfigManager.getInstance().getSection("abilities", "fireball");

      int cooldown = fireball.getInt("cooldown");
      double damage = fireball.getDouble("damage");
      int range = fireball.getInt("range");

      // Use the values...
    }
  }

  // ===== Advanced: Priority Example =====

  @RegisterConfig(priority = 5) // Loads very early
  public static class DatabaseConfigSchema extends ConfigSchema {
    public DatabaseConfigSchema() {
      super("database");

      value(new ConfigValue.String("host", "localhost", "Database host"));
      value(new ConfigValue.Int("port", 3306, "Database port"));
    }
  }

  @RegisterConfig(priority = 100) // Loads late (default priority)
  public static class FeatureConfigSchema extends ConfigSchema {
    public FeatureConfigSchema() {
      super("features");
      // May depend on database config being loaded first
    }
  }
}
