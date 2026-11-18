package me.mortaldev.abstractyaml;

import java.io.*;
import java.text.MessageFormat;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * File helpers used by the legacy {@link AbstractConfig}: load, save and restore YAML files in a
 * plugin's data folder with Bukkit's {@link YamlConfiguration}. Get the single instance with
 * {@link #getInstance()}.
 *
 * <p>Names passed to these methods get {@code .yml} added when they do not end with it, and
 * are resolved against {@code plugin.getDataFolder()}. Loading never throws for a bad file:
 * following {@link YamlConfiguration#loadConfiguration(File)}, a missing or invalid file is
 * logged and gives an empty configuration.
 *
 * <p>The class has no state, so it can be called from any thread, but two threads writing
 * the same file at once can still corrupt it.
 */
public class YAML {

  /**
   * Message pattern used by {@link #failedToLoad}, in {@link MessageFormat} syntax:
   * {@code {0}} is the config name, {@code {1}} the value, {@code {2}} the reason.
   */
  public static final String FAILED_TO_LOAD_CONFIG =
      "[{0}.YML] Failed to load config value: {1} ({2})";
  /** Reason used by {@link #failedToLoad(JavaPlugin, String, String)}. */
  public static final String INVALID_VALUE = "INVALID VALUE";
  /** Start of the warning logged by {@link #getOtherConfig} when the file does not exist. */
  public static final String OTHER_CONFIG_ERROR = "Error finding other config: ";
  /** Start of the warning logged by {@link #loadResource} when the jar has no such file. */
  public static final String RESOURCE_LOAD_ERROR = "Failed to load resource: ";
  private static final String YML_EXTENSION = ".yml";

  private static class Singleton {
    private static final YAML INSTANCE = new YAML();
  }

  /**
   * Returns the single instance.
   *
   * @return the instance
   */
  public static YAML getInstance() {
    return Singleton.INSTANCE;
  }

  private YAML() {}

  /**
   * Same as {@link #failedToLoad(JavaPlugin, String, String, String)} with the reason
   * {@value #INVALID_VALUE}.
   *
   * @param plugin the plugin that owns the file
   * @param configName the file name, with or without {@code .yml}
   * @param configValue the path or name of the value that failed, for the message
   * @return the logged message
   * @throws ConfigurationException if the bundled file cannot be copied
   */
  public String failedToLoad(JavaPlugin plugin, String configName, String configValue) {
    return failedToLoad(plugin, configName, configValue, INVALID_VALUE);
  }

  /**
   * Logs a warning that a value could not be loaded, then restores the file from the plugin
   * jar with {@link #loadResource(JavaPlugin, String)}, replacing the file in the data folder.
   *
   * @param plugin the plugin that owns the file
   * @param configName the file name, with or without {@code .yml}
   * @param configValue the path or name of the value that failed, for the message
   * @param failReason why it failed, shown in parentheses
   * @return the logged message, built from {@link #FAILED_TO_LOAD_CONFIG}
   * @throws ConfigurationException if the bundled file cannot be copied
   */
  public String failedToLoad(
      JavaPlugin plugin, String configName, String configValue, String failReason) {
    String message =
        MessageFormat.format(FAILED_TO_LOAD_CONFIG, configName, configValue, failReason);
    plugin.getLogger().warning(message);
    loadResource(plugin, configName);
    return message;
  }

  /**
   * Creates an empty file {@code <path>/<name>.yml}, with its folders, and returns it loaded.
   *
   * <p>If the file already exists, this returns {@link #getConfig(JavaPlugin, String)} for
   * {@code name}, which reads from the plugin's data folder, not from {@code path}.
   *
   * @param plugin the plugin, used when the file already exists
   * @param path the folder for the new file
   * @param name the file name, with or without {@code .yml}
   * @return the loaded (empty) configuration
   * @throws ConfigurationException if the file cannot be created
   */
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

  /**
   * Loads {@code <name>.yml} from the plugin's data folder. If the file does not exist, it is
   * first copied from the plugin jar with {@link #loadResource(JavaPlugin, String)}; if the jar
   * has no such file either, a warning is logged and an empty configuration is returned.
   *
   * @param plugin the plugin that owns the file
   * @param name the file name, with or without {@code .yml}; may include folders
   * @return the loaded configuration; empty if the file is missing or invalid
   * @throws ConfigurationException if copying the bundled file fails
   */
  public FileConfiguration getConfig(JavaPlugin plugin, String name) {
    name = ensureYmlExtension(name);
    File file = new File(plugin.getDataFolder(), name);
    if (!file.exists()) {
      loadResource(plugin, name);
    }
    return YamlConfiguration.loadConfiguration(file);
  }

  /**
   * Loads a YAML file anywhere on disk, such as another plugin's config. Logs a warning if the
   * file does not exist.
   *
   * @param plugin the plugin whose logger receives the warning
   * @param file the file to read
   * @return the loaded configuration; empty if the file is missing or invalid
   */
  public FileConfiguration getOtherConfig(JavaPlugin plugin, File file) {
    if (!file.exists()) {
      plugin.getLogger().warning(OTHER_CONFIG_ERROR + file);
    }
    return YamlConfiguration.loadConfiguration(file);
  }

  /**
   * Writes a configuration to a file anywhere on disk, replacing it.
   *
   * @param config the configuration to write
   * @param file the destination file
   * @throws ConfigurationException if the file cannot be written
   */
  public void saveOtherConfig(FileConfiguration config, File file) {
    try {
      config.save(file);
    } catch (IOException e) {
      throw new ConfigurationException(
          "Failed to save config to file: " + file.getAbsolutePath(), e);
    }
  }

  /**
   * Writes a configuration to {@code <name>.yml} in the plugin's data folder, replacing it and
   * creating missing folders.
   *
   * @param plugin the plugin that owns the file
   * @param config the configuration to write
   * @param name the file name, with or without {@code .yml}; may include folders
   * @throws ConfigurationException if the file cannot be written
   */
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
   * Copies {@code <name>.yml} from the plugin jar into the data folder, <b>replacing</b> any
   * existing file there. If the jar has no such file, logs a warning starting with
   * {@link #RESOURCE_LOAD_ERROR} and changes nothing.
   *
   * @param plugin the plugin whose jar contains the file
   * @param name the file name, with or without {@code .yml}; the same path inside the jar
   * @throws ConfigurationException if the file cannot be read or written
   */
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
   * Intended to copy the bundled file for {@code config} into the data folder, but a loaded
   * {@link FileConfiguration} reports its name as {@code ""}, and
   * {@link JavaPlugin#saveResource(String, boolean)} rejects an empty name. Use
   * {@code plugin.saveResource("<name>.yml", false)} or
   * {@link #loadResource(JavaPlugin, String)} instead.
   *
   * @param plugin the plugin whose jar contains the file
   * @param config the configuration whose name is used
   * @throws IllegalArgumentException always, for a configuration loaded from a file
   */
  public void saveDefaultConfig(JavaPlugin plugin, FileConfiguration config) {
    plugin.saveResource(config.getName(), false);
  }

  /**
   * Never worked: a {@link FileConfiguration} does not know which file it came from, so this
   * method read from a path built from the in-file section path and then discarded the result.
   *
   * @deprecated Load the file again with {@link #getConfig(JavaPlugin, String)}.
   * @param plugin not used
   * @param config not used
   * @throws UnsupportedOperationException always
   */
  @Deprecated
  public void reloadConfig(JavaPlugin plugin, FileConfiguration config) {
    throw new UnsupportedOperationException(
        "YAML.reloadConfig cannot locate the file; use YAML.getConfig(plugin, name) instead");
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

  /** Thrown when a file cannot be created, read, copied or written. */
  public static class ConfigurationException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message which file and what failed
     * @param cause the underlying {@link IOException}
     */
    public ConfigurationException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
