package me.mortaldev;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Example configuration class demonstrating best practices for using the AbstractConfig framework.
 *
 * <p>This example shows:
 * <ul>
 *   <li>Proper type-safe ConfigValue declarations</li>
 *   <li>Validation usage</li>
 *   <li>Caching benefits</li>
 *   <li>Thread-safe singleton pattern</li>
 * </ul>
 */
public class ExampleConfig extends AbstractConfig {

  // === ConfigValue declarations with explicit types ===

  private ConfigValue<Integer> maxPlayers =
      new ConfigValue<>("max-players", Integer.class, 100)
          .setValidator(ConfigValidator.range(1, 1000));

  private ConfigValue<String> serverName =
      new ConfigValue<>("server-name", String.class, "My Server")
          .setValidator(
              ConfigValidator.notEmpty()
                  .addRule(name -> name.length() <= 32, "Server name too long"));

  private ConfigValue<Double> coinMultiplier =
      new ConfigValue<>("economy.coin-multiplier", Double.class, 1.0)
          .setValidator(ConfigValidator.min(0.1));

  private ConfigValue<Boolean> debugMode =
      new ConfigValue<>("debug-mode", Boolean.class, false);

  // === Singleton pattern ===

  private static class Singleton {
    private static final ExampleConfig INSTANCE = new ExampleConfig();
  }

  public static ExampleConfig getInstance() {
    return Singleton.INSTANCE;
  }

  private ExampleConfig() {}

  @Override
  public void log(String message) {
    // You can implement custom logging here if needed
    // Or leave empty for silent operation
  }

  @Override
  public String getName() {
    return "config"; // Will load config.yml
  }

  @Override
  public JavaPlugin getMain() {
    // TODO: Return your plugin instance here
    // Common approaches:
    // 1. Store in a field and set it via a setter
    // 2. Get from Bukkit.getPluginManager().getPlugin("YourPluginName")
    // 3. Pass via constructor (breaks singleton pattern)
    return null; // Replace with your implementation
  }

  @Override
  public void loadData() {
    // Load all config values - caching will prevent repeated disk reads
    maxPlayers = getConfigValue(maxPlayers);
    serverName = getConfigValue(serverName);
    coinMultiplier = getConfigValue(coinMultiplier);
    debugMode = getConfigValue(debugMode);

    // Log validation failures
    if (!maxPlayers.isValid()) {
      log("Warning: maxPlayers failed validation: " + maxPlayers.validate().getErrorMessage());
    }
  }

  // === Getters with cached values ===

  public int getMaxPlayers() {
    return maxPlayers.getValue();
  }

  public String getServerName() {
    return serverName.getValue();
  }

  public double getCoinMultiplier() {
    return coinMultiplier.getValue();
  }

  public boolean isDebugMode() {
    return debugMode.getValue();
  }

  // === Setters that update both memory and disk ===

  public void setMaxPlayers(int maxPlayers) {
    // Validation happens automatically in saveValue
    this.maxPlayers.setValue(maxPlayers);
    saveValue(this.maxPlayers);
  }

  public void setServerName(String serverName) {
    this.serverName.setValue(serverName);
    saveValue(this.serverName);
  }

  public void setCoinMultiplier(double multiplier) {
    this.coinMultiplier.setValue(multiplier);
    saveValue(this.coinMultiplier);
  }

  public void setDebugMode(boolean debugMode) {
    this.debugMode.setValue(debugMode);
    saveValue(this.debugMode);
  }
}
