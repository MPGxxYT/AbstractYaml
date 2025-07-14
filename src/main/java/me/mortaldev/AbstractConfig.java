package me.mortaldev;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public abstract class AbstractConfig {
  private static final String EXTENSION = ".yml";
  private FileConfiguration config;

  /**
   * Logs a message to the console.
   *
   * @param message the message to log
   */
  public abstract void log(String message);

  /**
   * Returns the name of this configuration file, without the extension.
   *
   * @return the name of this configuration file
   */
  public abstract String getName();

  /**
   * Returns the main plugin for this configuration.
   *
   * @return the main plugin
   */
  public abstract JavaPlugin getMain();

  public abstract void loadData();

  public AbstractConfig() {}



  public <T> ConfigValue<T> getConfigValue(ConfigValue<T> configValue) {
    Class<T> valueType = configValue.getValueType();
    String path = configValue.getId();
    T defaultValue = configValue.getDefaultValue();

    Object rawValue = config.get(path); // Get the raw value from the config

    // --- NEW: Special handling for Maps from ConfigurationSections ---
    if (Map.class.isAssignableFrom(valueType) && rawValue instanceof ConfigurationSection section) {
      // The config has a section, and we expect a map. Convert it.
      // The getValues(false) method returns a Map<String, Object>, which is what we need.
      // This is an unchecked but necessary cast that the API will now handle for the user.
      configValue.setValue((T) section.getValues(false));
      return configValue;
    }

    // --- Fallback to original logic for simple types (String, Integer, etc.) ---
    Object value = config.get(path, defaultValue);

    if (valueType.isInstance(value)) {
      configValue.setValue(valueType.cast(value));
    } else {
      getMain()
          .getLogger()
          .warning(
              "Config Value for '"
                  + path
                  + "' is invalid. Found type "
                  + (value != null ? value.getClass().getSimpleName() : "null")
                  + " but expected "
                  + valueType.getSimpleName()
                  + ". Setting to default: "
                  + configValue.getDefaultValue());
      configValue.setValue(configValue.getDefaultValue());
      saveValue(configValue);
    }
    return configValue;
  }

  public YAML getYAML() {
    YAML instance = YAML.getInstance();
    instance.setMain(getMain());
    return instance;
  }

  public String failedToLoad(String configName, String configValue) {
    return getYAML().failedToLoad(configName, configValue);
  }

  public String failedToLoad(String configName, String configValue, String failReason) {
    return getYAML().failedToLoad(configName, configValue, failReason);
  }

  /**
   * Returns the configuration for this file.
   *
   * @return the configuration for this file
   */
  public FileConfiguration getConfig() {
    return config;
  }

  /**
   * Reloads the configuration file.
   *
   * @return a message indicating that the file was reloaded
   */
  public String reload() {
    load();
    return "Reloaded " + getName() + EXTENSION;
  }

  /**
   * Saves the configuration to disk.
   *
   * <p>This method will use the last loaded configuration and any changes made to it since then.
   */
  public void saveConfig() {
    getYAML().saveConfig(getConfig(), getName());
  }

  /**
   * Saves the value of the given {@link ConfigValue} to the configuration file.
   *
   * @param configValue the {@link ConfigValue} to save
   */
  public <T> void saveValue(ConfigValue<T> configValue) {
    saveValue(configValue, true);
  }

  /**
   * Saves the value of the given {@link ConfigValue} to the configuration file.
   *
   * @param configValue the {@link ConfigValue} to save
   * @param saveToFile whether to save the value to file
   */
  public <T> void saveValue(ConfigValue<T> configValue, boolean saveToFile) {
    saveValue(configValue.getId(), configValue.getValue(), saveToFile);
  }

  /**
   * Sets the value at the given path in the config to the given value.
   *
   * <p>If {@code saveToFile} is true, the value is saved to the configuration file.
   *
   * @param path the path to the value
   * @param value the value to set
   * @param saveToFile whether to save the value to file
   */
  public void saveValue(String path, Object value, boolean saveToFile) {
    getConfig().set(path, value);
    if (saveToFile) {
      getYAML().saveConfig(getConfig(), getName());
    }
  }

  /**
   * Sets the value at the given path in the config to the given value. The value is saved to file.
   *
   * @param path the path to set the value at
   * @param value the value to set at the given path
   */
  public void saveValue(String path, Object value) {
    saveValue(path, value, true);
  }

  private void loadConfig(String configName) {
    this.config = getYAML().getConfig(configName);
    loadData();
  }

  /**
   * SHOULD ALWAYS BE RAN ON PLUGIN ENABLE
   *
   * <p>Loads the configuration file from the given name. Updates any data in memory to the new
   * values in the config file.
   *
   * <p>This method calls {@link #loadConfig(String)} with the name returned by {@link #getName()}.
   */
  public void load() {
    loadConfig(getName());
  }
}
