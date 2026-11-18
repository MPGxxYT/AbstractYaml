package me.mortaldev.abstractyaml;

/**
 * Base class for organizing configuration values into logical sections.
 *
 * <p>A container groups the values under one path prefix: a container with prefix
 * {@code "debug"} reads {@code "log-events"} from {@code "debug.log-events"}. Subclasses need a
 * public constructor taking {@code (AbstractConfig, String)} to be created by
 * {@link AbstractConfig#loadAllContainers()}.
 *
 * <p>Example usage:
 * <pre>{@code
 * public class DebugContainer extends ConfigValueContainer {
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
 *
 * @deprecated Part of the legacy config system. In {@code me.mortaldev.abstractyaml.config2}, declare
 *     sections in the schema with {@code section("debug")} and read them with
 *     {@code config.section("debug")}.
 */
@Deprecated
public abstract class ConfigValueContainer {
    /** The config file this container reads from and saves to. */
    protected final AbstractConfig config;

    /** The prefix put in front of every path in this container, without a trailing dot. */
    protected final String pathPrefix;
    private String headerComment;

    /**
     * Creates a new ValueContainer.
     *
     * @param config the parent configuration
     * @param pathPrefix the path prefix for all values in this container (e.g., "debug",
     *     "messages"); empty for no prefix
     */
    protected ConfigValueContainer(AbstractConfig config, String pathPrefix) {
        this.config = config;
        this.pathPrefix = pathPrefix;
    }

    /**
     * Loads all ConfigValues in this container. Called by
     * {@link AbstractConfig#loadAllContainers()} or {@link AbstractConfig#loadContainer}, on
     * every load and reload. Assign each field from a {@code loadValue} call here.
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
     * Helper method to load a config value with a comment.
     *
     * @param path the config path relative to this container's prefix
     * @param type the class type of the value
     * @param defaultValue the default value
     * @param comment the inline comment for this value
     * @param <T> the type of the value
     * @return the loaded ConfigValue
     */
    protected <T> ConfigValue<T> loadValue(String path, Class<T> type, T defaultValue, String comment) {
        ConfigValue<T> configValue = new ConfigValue<>(path, type, defaultValue)
            .setComment(comment);
        return getConfigValue(configValue);
    }

    /**
     * Helper method to load a config value with both a validator and a comment.
     *
     * @param path the config path relative to this container's prefix
     * @param type the class type of the value
     * @param defaultValue the default value
     * @param validator the validator to apply to this value
     * @param comment the inline comment for this value
     * @param <T> the type of the value
     * @return the loaded ConfigValue
     */
    protected <T> ConfigValue<T> loadValue(String path, Class<T> type, T defaultValue,
                                            ConfigValidator<T> validator, String comment) {
        ConfigValue<T> configValue = new ConfigValue<>(path, type, defaultValue)
            .setValidator(validator)
            .setComment(comment);
        return getConfigValue(configValue);
    }

    /**
     * Reads a value with this container's prefix put in front of its path. A new
     * {@link ConfigValue} with the full path is created and read; {@code configValue} itself is
     * not changed. Handling of invalid values is described in
     * {@link AbstractConfig#getConfigValue(ConfigValue)}.
     *
     * @param configValue the ConfigValue to load
     * @param <T> the type of the value
     * @return the loaded ConfigValue with the full path
     * @throws IllegalArgumentException if the value is invalid and its default also fails the
     *     validator
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
        if (configValue.getComment() != null) {
            prefixedValue.setComment(configValue.getComment());
        }

        // Load from parent config
        return config.getConfigValue(prefixedValue);
    }

    /**
     * Validates a config value and writes it to the file at its path, as
     * {@link AbstractConfig#saveValue(ConfigValue)} does. Pass a value returned by
     * {@code loadValue}, which already has the full path.
     *
     * @param configValue the ConfigValue to save
     * @param <T> the type of the value
     * @throws IllegalArgumentException if the value fails its validator
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

    /**
     * Sets a header comment for this container section. Call it in the constructor or in
     * {@link #load()}. The header appears above the container's section when the file is
     * generated, only for containers loaded by {@link AbstractConfig#loadAllContainers()}.
     *
     * @param headerComment the header comment
     */
    protected void setHeaderComment(String headerComment) {
        this.headerComment = headerComment;
    }

    /**
     * Gets the header comment for this container.
     *
     * @return the header comment, or null if none is set
     */
    public String getHeaderComment() {
        return headerComment;
    }
}