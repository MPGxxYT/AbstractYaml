package me.mortaldev;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Objects;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Thread-safe YAML configuration utility class.
 *
 * <p>This class provides methods for loading, saving, and managing YAML configuration files
 * for Bukkit/Spigot plugins. The singleton instance is thread-safe but requires the JavaPlugin
 * to be passed to methods rather than stored as mutable state.
 */
public class YAML {

  public static final String FAILED_TO_LOAD_CONFIG =
      "[{0}.YML] Failed to load config value: {1} ({2})";
  public static final String INVALID_VALUE = "INVALID VALUE";
  public static final String OTHER_CONFIG_ERROR = "Error finding other config: ";
  public static final String RESOURCE_LOAD_ERROR = "Failed to load resource: ";
  private static final String YML_EXTENSION = ".yml";

  private static class Singleton {
    private static final YAML INSTANCE = new YAML();
  }

  public static YAML getInstance() {
    return Singleton.INSTANCE;
  }

  private YAML() {}

  /**
   * @deprecated Use methods that accept JavaPlugin as a parameter instead.
   * This method maintains backwards compatibility but is not thread-safe.
   */
  @Deprecated
  private JavaPlugin main;

  /**
   * @deprecated This method is not thread-safe. Use methods that accept JavaPlugin as a parameter.
   */
  @Deprecated
  public void setMain(JavaPlugin main) {
    this.main = main;
  }

  public String failedToLoad(JavaPlugin plugin, String configName, String configValue) {
    return failedToLoad(plugin, configName, configValue, INVALID_VALUE);
  }

  public String failedToLoad(JavaPlugin plugin, String configName, String configValue, String failReason) {
    String message =
        MessageFormat.format(FAILED_TO_LOAD_CONFIG, configName, configValue, failReason);
    plugin.getLogger().warning(message);
    loadResource(plugin, configName);
    return message;
  }

  /**
   * @deprecated Use {@link #failedToLoad(JavaPlugin, String, String)} instead.
   */
  @Deprecated
  public String failedToLoad(String configName, String configValue) {
    if (main == null) {
      throw new IllegalStateException("JavaPlugin not set. Use failedToLoad(JavaPlugin, String, String) instead.");
    }
    return failedToLoad(main, configName, configValue, INVALID_VALUE);
  }

  /**
   * @deprecated Use {@link #failedToLoad(JavaPlugin, String, String, String)} instead.
   */
  @Deprecated
  public String failedToLoad(String configName, String configValue, String failReason) {
    if (main == null) {
      throw new IllegalStateException("JavaPlugin not set. Use failedToLoad(JavaPlugin, String, String, String) instead.");
    }
    return failedToLoad(main, configName, configValue, failReason);
  }

  public FileConfiguration createNewConfig(JavaPlugin plugin, String path, String name) {
    name = ensureYmlExtension(name);
    File file = new File(path, name);
    if (file.exists()) {
      return getConfig(plugin, name);
    }
    try {
      if (!file.getParentFile().exists()) {
        file.getParentFile().mkdirs();
      }
      if (!file.createNewFile()) {
        throw new IOException("Failed to create config file: " + file.getAbsolutePath());
      }
    } catch (IOException e) {
      throw new ConfigurationException("Failed to create new config file: " + name, e);
    }
    return YamlConfiguration.loadConfiguration(file);
  }

  public FileConfiguration getConfig(JavaPlugin plugin, String name) {
    name = ensureYmlExtension(name);
    File file = new File(plugin.getDataFolder(), name);
    if (!file.exists()) {
      loadResource(plugin, name);
    }
    return YamlConfiguration.loadConfiguration(file);
  }

  /**
   * @deprecated Use {@link #getConfig(JavaPlugin, String)} instead.
   */
  @Deprecated
  public FileConfiguration getConfig(String name) {
    if (main == null) {
      throw new IllegalStateException("JavaPlugin not set. Use getConfig(JavaPlugin, String) instead.");
    }
    return getConfig(main, name);
  }

  public FileConfiguration getOtherConfig(JavaPlugin plugin, File file) {
    if (!file.exists()) {
      plugin.getLogger().warning(OTHER_CONFIG_ERROR + file);
    }
    return YamlConfiguration.loadConfiguration(file);
  }

  /**
   * @deprecated Use {@link #getOtherConfig(JavaPlugin, File)} instead.
   */
  @Deprecated
  public FileConfiguration getOtherConfig(File file) {
    if (main == null) {
      throw new IllegalStateException("JavaPlugin not set. Use getOtherConfig(JavaPlugin, File) instead.");
    }
    return getOtherConfig(main, file);
  }

  public void saveOtherConfig(FileConfiguration config, File file) {
    try {
      config.save(file);
    } catch (IOException e) {
      throw new ConfigurationException("Failed to save config to file: " + file.getAbsolutePath(), e);
    }
  }

  public void saveConfig(JavaPlugin plugin, FileConfiguration config, String name) {
    name = ensureYmlExtension(name);
    File file = new File(plugin.getDataFolder(), name);
    try {
      if (!file.getParentFile().exists()) {
        file.getParentFile().mkdirs();
      }
      config.save(file);
    } catch (IOException e) {
      throw new ConfigurationException("Failed to save config: " + name, e);
    }
  }

  /**
   * @deprecated Use {@link #saveConfig(JavaPlugin, FileConfiguration, String)} instead.
   */
  @Deprecated
  public void saveConfig(FileConfiguration config, String name) {
    if (main == null) {
      throw new IllegalStateException("JavaPlugin not set. Use saveConfig(JavaPlugin, FileConfiguration, String) instead.");
    }
    saveConfig(main, config, name);
  }

  public void loadResource(JavaPlugin plugin, String name) {
    name = ensureYmlExtension(name);
    try (InputStream stream = plugin.getResource(name)) {
      if (stream == null) {
        plugin.getLogger().warning(RESOURCE_LOAD_ERROR + name);
        return;
      }
      File file = new File(plugin.getDataFolder(), name);
      if (!file.getParentFile().exists()) {
        file.getParentFile().mkdirs();
      }
      if (!file.exists()) {
        file.createNewFile();
      }
      try (OutputStream outputStream = new FileOutputStream(file)) {
        stream.transferTo(outputStream);
      }
    } catch (IOException e) {
      throw new ConfigurationException("Failed to load resource: " + name, e);
    }
  }

  /**
   * @deprecated Use {@link #loadResource(JavaPlugin, String)} instead.
   */
  @Deprecated
  public void loadResource(String name) {
    if (main == null) {
      throw new IllegalStateException("JavaPlugin not set. Use loadResource(JavaPlugin, String) instead.");
    }
    loadResource(main, name);
  }

  public void saveDefaultConfig(JavaPlugin plugin, FileConfiguration config) {
    plugin.saveResource(config.getName(), false);
  }

  /**
   * @deprecated Use {@link #saveDefaultConfig(JavaPlugin, FileConfiguration)} instead.
   */
  @Deprecated
  public void saveDefaultConfig(FileConfiguration config) {
    if (main == null) {
      throw new IllegalStateException("JavaPlugin not set. Use saveDefaultConfig(JavaPlugin, FileConfiguration) instead.");
    }
    saveDefaultConfig(main, config);
  }

  public void reloadConfig(JavaPlugin plugin, FileConfiguration config) {
    config = YamlConfiguration.loadConfiguration(new File(config.getCurrentPath()));
    try (Reader stream = new InputStreamReader(
        Objects.requireNonNull(plugin.getResource(config.getName())), StandardCharsets.UTF_8)) {
      YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(stream);
      config.setDefaults(defConfig);
    } catch (IOException e) {
      throw new ConfigurationException("Failed to reload config: " + config.getName(), e);
    }
  }

  /**
   * @deprecated Use {@link #reloadConfig(JavaPlugin, FileConfiguration)} instead.
   */
  @Deprecated
  public void reloadConfig(FileConfiguration config) {
    if (main == null) {
      throw new IllegalStateException("JavaPlugin not set. Use reloadConfig(JavaPlugin, FileConfiguration) instead.");
    }
    reloadConfig(main, config);
  }

  /**
   * Ensures that the given name has the .yml extension.
   *
   * @param name the name to check
   * @return the name with .yml extension
   */
  private String ensureYmlExtension(String name) {
    if (!name.endsWith(YML_EXTENSION)) {
      return name + YML_EXTENSION;
    }
    return name;
  }

  /**
   * Custom exception for configuration-related errors.
   */
  public static class ConfigurationException extends RuntimeException {
    public ConfigurationException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
