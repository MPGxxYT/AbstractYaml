package me.mortaldev;

/**
 * Base class for organizing configuration values into logical sections.
 *
 * <p>ValueContainers allow you to group related config values together, reducing boilerplate
 * and improving organization in large configuration files.
 *
 * <p>Example usage:
 * <pre>{@code
 * public class DebugContainer extends ValueContainer {
 *     private ConfigValue<Boolean> logEvents;
 *     private ConfigValue<Boolean> verboseMode;
 *
 *     public DebugContainer(AbstractConfig config, String pathPrefix) {
 *         super(config, pathPrefix);
 *     }
 *
 *     @Override
 *     protected void load() {
 *         logEvents = loadValue("log-events", Boolean.class, true);
 *         verboseMode = loadValue("verbose-mode", Boolean.class, false);
 *     }
 *
 *     public boolean isLogEvents() { return logEvents.getValue(); }
 *     public boolean isVerboseMode() { return verboseMode.getValue(); }
 * }
 * }</pre>
 *
 * <p>Then in your config class:
 * <pre>{@code
 * public class MyConfig extends AbstractConfig {
 *     @ConfigContainer("debug")
 *     private DebugContainer debug;
 *
 *     @Override
 *     public void loadData() {
 *         loadAllContainers();
 *     }
 *
 *     public DebugContainer getDebug() { return debug; }
 * }
 * }</pre>
 */
public abstract class ValueContainer {
    protected final AbstractConfig config;
    protected final String pathPrefix;

    /**
     * Creates a new ValueContainer.
     *
     * @param config the parent configuration
     * @param pathPrefix the path prefix for all values in this container (e.g., "debug", "messages")
     */
    protected ValueContainer(AbstractConfig config, String pathPrefix) {
        this.config = config;
        this.pathPrefix = pathPrefix;
    }

    /**
     * Loads all ConfigValues in this container.
     * Called automatically by the parent config during initialization.
     *
     * <p>Implementations should use the {@link #loadValue} helper methods to load their values.
     */
    protected abstract void load();

    /**
     * Helper method to load a config value without a validator.
     *
     * @param path the config path relative to this container's prefix
     * @param type the class type of the value
     * @param defaultValue the default value
     * @param <T> the type of the value
     * @return the loaded ConfigValue
     */
    protected <T> ConfigValue<T> loadValue(String path, Class<T> type, T defaultValue) {
        ConfigValue<T> configValue = new ConfigValue<>(path, type, defaultValue);
        return getConfigValue(configValue);
    }

    /**
     * Helper method to load a config value with a validator.
     *
     * @param path the config path relative to this container's prefix
     * @param type the class type of the value
     * @param defaultValue the default value
     * @param validator the validator to apply to this value
     * @param <T> the type of the value
     * @return the loaded ConfigValue
     */
    protected <T> ConfigValue<T> loadValue(String path, Class<T> type, T defaultValue,
                                            ConfigValidator<T> validator) {
        ConfigValue<T> configValue = new ConfigValue<>(path, type, defaultValue)
            .setValidator(validator);
        return getConfigValue(configValue);
    }

    /**
     * Gets a config value with the path prefix automatically applied.
     *
     * @param configValue the ConfigValue to load
     * @param <T> the type of the value
     * @return the loaded ConfigValue with the full path
     */
    protected <T> ConfigValue<T> getConfigValue(ConfigValue<T> configValue) {
        // Prepend the pathPrefix to the configValue's ID
        String fullPath = pathPrefix.isEmpty()
            ? configValue.getId()
            : pathPrefix + "." + configValue.getId();

        // Create a new ConfigValue with the full path
        ConfigValue<T> prefixedValue = new ConfigValue<>(
            fullPath,
            configValue.getValueType(),
            configValue.getDefaultValue()
        );
        if (configValue.getValidator() != null) {
            prefixedValue.setValidator(configValue.getValidator());
        }

        // Load from parent config
        return config.getConfigValue(prefixedValue);
    }

    /**
     * Saves a config value to the configuration file.
     *
     * @param configValue the ConfigValue to save
     * @param <T> the type of the value
     */
    protected <T> void saveValue(ConfigValue<T> configValue) {
        config.saveValue(configValue);
    }

    /**
     * Gets the path prefix for this container.
     *
     * @return the path prefix
     */
    public String getPathPrefix() {
        return pathPrefix;
    }
}