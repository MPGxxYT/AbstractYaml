package me.mortaldev;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Objects;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class YAML {

  public static final String FAILED_TO_LOAD_CONFIG =
      "[{0}.YML] Failed to load config value: {1} ({2})";
  public static final String INVALID_VALUE = "INVALID VALUE";
  public static final String OTHER_CONFIG_ERROR = "Error finding other config: ";
  public static final String RESOURCE_LOAD_ERROR = "Failed to load resource: ";
  private JavaPlugin main;

  private static class Singleton {
    private static final YAML INSTANCE = new YAML();
  }

  public static YAML getInstance() {
    return Singleton.INSTANCE;
  }

  private YAML() {}

  public void setMain(JavaPlugin main) {
    this.main = main;
  }

  public String failedToLoad(String configName, String configValue) {
    return failedToLoad(configName, configValue, INVALID_VALUE);
  }

  public String failedToLoad(String configName, String configValue, String failReason) {
    String message =
        MessageFormat.format(FAILED_TO_LOAD_CONFIG, configName, configValue, failReason);
    main.getLogger().warning(message);
    loadResource(configName);
    return message;
  }

  public FileConfiguration createNewConfig(String name) {
    if (!name.contains(".yml")) {
      name = name.concat(".yml");
    }
    File file = new File(main.getDataFolder(), name);
    if (file.exists()) {
      return getConfig(name);
    }
    try {
      file.createNewFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return YamlConfiguration.loadConfiguration(file);
  }

  public FileConfiguration getConfig(String name) {
    if (!name.contains(".yml")) {
      name = name.concat(".yml");
    }
    File file = new File(main.getDataFolder(), name);
    if (!file.exists()) {
      loadResource(name);
    }
    return YamlConfiguration.loadConfiguration(file);
  }

  public FileConfiguration getOtherConfig(File file) {
    if (!file.exists()) {
      main.getLogger().warning(OTHER_CONFIG_ERROR + file);
    }
    return YamlConfiguration.loadConfiguration(file);
  }

  public void saveOtherConfig(File file, FileConfiguration config) {
    try {
      config.save(file);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void saveConfig(FileConfiguration config, String name) {
    if (!name.contains(".yml")) {
      name = name.concat(".yml");
    }
    File file = new File(main.getDataFolder(), name);
    try {
      config.save(file);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void loadResource(String name) {
    if (!name.contains(".yml")) {
      name = name.concat(".yml");
    }
    InputStream stream = main.getResource(name);
    if (stream == null) {
      main.getLogger().warning(RESOURCE_LOAD_ERROR + name);
      return;
    }
    File file = new File(main.getDataFolder(), name);
    try {
      if (!file.exists()) {
        file.createNewFile();
      }
      OutputStream outputStream = new FileOutputStream(file);
      outputStream.write(stream.readAllBytes());
      outputStream.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void saveDefaultConfig(FileConfiguration config) {
    main.saveResource(config.getName(), false);
  }

  public void reloadConfig(FileConfiguration config) {
    config = YamlConfiguration.loadConfiguration(new File(config.getCurrentPath()));
    Reader stream =
        new InputStreamReader(
            Objects.requireNonNull(main.getResource(config.getName())), StandardCharsets.UTF_8);
    YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(stream);
    config.setDefaults(defConfig);
  }
}
