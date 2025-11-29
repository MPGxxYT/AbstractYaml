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
 *   <li>Recommended plugin reference pattern</li>
 * </ul>
 *
 * <p>Example usage in your plugin:
 * <pre>{@code
 * @Override
 * public void onEnable() {
 *     ExampleConfig config = ExampleConfig.getInstance();
 *     config.setPlugin(this);  // Set plugin reference
 *     config.load();           // Load from disk
 *
 *     getLogger().info("Max players: " + config.getMaxPlayers());
 * }
 * }</pre>
 */
public class ExampleConfig extends AbstractConfig {
  private JavaPlugin plugin;

  // === ConfigValue declarations with explicit types and validation ===

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

  // === Plugin reference (recommended pattern) ===

  /**
   * Sets the JavaPlugin instance for this config.
   * Must be called before load().
   *
   * @param plugin the plugin instance
   */
  public void setPlugin(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public JavaPlugin getMain() {
    return plugin;
  }

  @Override
  public void log(String message) {
    if (plugin != null) {
      plugin.getLogger().info("[Config] " + message);
    }
  }

  @Override
  public String getName() {
    return "config"; // Will load config.yml
  }

  @Override
  public void loadData() {
    // Load all config values - caching prevents repeated disk reads
    maxPlayers = getConfigValue(maxPlayers);
    serverName = getConfigValue(serverName);
    coinMultiplier = getConfigValue(coinMultiplier);
    debugMode = getConfigValue(debugMode);

    // Log validation failures (optional)
    if (!maxPlayers.isValid()) {
      log("Warning: maxPlayers failed validation: " + maxPlayers.validate().getErrorMessage());
    }
  }

  // === Getters (values are cached automatically) ===

  /**
   * Gets the maximum number of players.
   * Value is cached after first load.
   *
   * @return max players (1-1000)
   */
  public int getMaxPlayers() {
    return maxPlayers.getValue();
  }

  /**
   * Gets the server name.
   * Value is cached after first load.
   *
   * @return server name (max 32 characters)
   */
  public String getServerName() {
    return serverName.getValue();
  }

  /**
   * Gets the coin multiplier for economy.
   * Value is cached after first load.
   *
   * @return coin multiplier (minimum 0.1)
   */
  public double getCoinMultiplier() {
    return coinMultiplier.getValue();
  }

  /**
   * Gets whether debug mode is enabled.
   * Value is cached after first load.
   *
   * @return true if debug mode is on
   */
  public boolean isDebugMode() {
    return debugMode.getValue();
  }

  // === Setters (update memory and save to disk with validation) ===

  /**
   * Sets the maximum number of players.
   * Validates and saves to disk automatically.
   *
   * @param maxPlayers new max players (1-1000)
   * @throws IllegalArgumentException if validation fails
   */
  public void setMaxPlayers(int maxPlayers) {
    this.maxPlayers.setValue(maxPlayers);
    saveValue(this.maxPlayers); // Validates before saving
  }

  /**
   * Sets the server name.
   * Validates and saves to disk automatically.
   *
   * @param serverName new server name (max 32 characters, not empty)
   * @throws IllegalArgumentException if validation fails
   */
  public void setServerName(String serverName) {
    this.serverName.setValue(serverName);
    saveValue(this.serverName);
  }

  /**
   * Sets the coin multiplier.
   * Validates and saves to disk automatically.
   *
   * @param multiplier new multiplier (minimum 0.1)
   * @throws IllegalArgumentException if validation fails
   */
  public void setCoinMultiplier(double multiplier) {
    this.coinMultiplier.setValue(multiplier);
    saveValue(this.coinMultiplier);
  }

  /**
   * Sets debug mode.
   * Saves to disk automatically.
   *
   * @param debugMode new debug mode state
   */
  public void setDebugMode(boolean debugMode) {
    this.debugMode.setValue(debugMode);
    saveValue(this.debugMode);
  }
}
