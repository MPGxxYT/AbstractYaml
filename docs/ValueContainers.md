# Value Containers

**ValueContainers** allow you to organize large configuration files into logical sections, reducing boilerplate and improving maintainability.

## The Problem

Large configs become unwieldy:

```java
public class CTFConfig extends AbstractConfig {
    // All values mixed together
    private ConfigValue<Boolean> debugLogPhaseTransitions = ...;
    private ConfigValue<Boolean> debugLogFlagEvents = ...;
    private ConfigValue<String> messageFlagTaken = ...;
    private ConfigValue<Integer> visualsSpawnGlowRadius = ...;

    @Override
    public void loadData() {
        // Repetitive loading code
        debugLogPhaseTransitions = getConfigValue(debugLogPhaseTransitions);
        debugLogFlagEvents = getConfigValue(debugLogFlagEvents);
        messageFlagTaken = getConfigValue(messageFlagTaken);
        visualsSpawnGlowRadius = getConfigValue(visualsSpawnGlowRadius);
        // ... 20 more lines
    }

    // Long prefixed getter names
    public boolean isDebugLogPhaseTransitions() { ... }
    public String getMessageFlagTaken() { ... }
}
```

## The Solution

Extract related values into containers:

```java
public class CTFConfig extends AbstractConfig {
    @ConfigContainer("debug")
    private DebugContainer debug;

    @ConfigContainer("messages")
    private MessagesContainer messages;

    @ConfigContainer("visuals")
    private VisualsContainer visuals;

    @Override
    public void loadData() {
        loadAllContainers(); // One line!
    }

    public DebugContainer getDebug() { return debug; }
    public MessagesContainer getMessages() { return messages; }
    public VisualsContainer getVisuals() { return visuals; }
}

// Usage
CTFConfig.getInstance().getDebug().isLogPhaseTransitions();
CTFConfig.getInstance().getMessages().getFlagTaken();
```

## Creating a Container

### Step 1: Extend ValueContainer

```java
public class DebugContainer extends ValueContainer {
    private ConfigValue<Boolean> logPhaseTransitions;
    private ConfigValue<Boolean> logFlagEvents;
    private ConfigValue<Boolean> verboseLogging;

    public DebugContainer(AbstractConfig config, String pathPrefix) {
        super(config, pathPrefix);
    }

    @Override
    protected void load() {
        // Use helper methods for cleaner code
        logPhaseTransitions = loadValue("log-phase-transitions", Boolean.class, true);
        logFlagEvents = loadValue("log-flag-events", Boolean.class, true);
        verboseLogging = loadValue("verbose-logging", Boolean.class, false);
    }

    // Clean getter names (no prefix needed!)
    public boolean isLogPhaseTransitions() {
        return logPhaseTransitions.getValue();
    }

    public boolean isLogFlagEvents() {
        return logFlagEvents.getValue();
    }

    public boolean isVerboseLogging() {
        return verboseLogging.getValue();
    }
}
```

###  Step 2: Register in Main Config

```java
public class CTFConfig extends AbstractConfig {
    @ConfigContainer("debug")
    private DebugContainer debug;

    @Override
    public void loadData() {
        loadAllContainers();
    }

    public DebugContainer getDebug() { return debug; }
}
```

### Step 3: YAML Structure

```yaml
debug:
  log-phase-transitions: true
  log-flag-events: true
  verbose-logging: false
```

## Features

### Automatic Path Prefixing

The container's path prefix is automatically prepended:

```java
public class MessagesContainer extends ValueContainer {
    public MessagesContainer(AbstractConfig config, String pathPrefix) {
        super(config, pathPrefix); // pathPrefix = "messages"
    }

    @Override
    protected void load() {
        // Loads from "messages.flag-taken" in YAML
        flagTaken = loadValue("flag-taken", String.class, "...");
    }
}
```

### Validator Support

Validators work exactly the same:

```java
@Override
protected void load() {
    spawnGlowRadius = loadValue(
        "spawn-glow-radius",
        Integer.class,
        1,
        ConfigValidator.min(0)  // Validator!
    );
}
```

### Fluent Loading

Two helper methods reduce boilerplate:

```java
// Without validator
loadValue(String path, Class<T> type, T defaultValue)

// With validator
loadValue(String path, Class<T> type, T defaultValue, ConfigValidator<T> validator)
```

## Complete Example

### Container Class

```java
public class GameContainer extends ValueContainer {
    private ConfigValue<Integer> length;
    private ConfigValue<Integer> minPlayers;
    private ConfigValue<Integer> maxPlayers;
    private ConfigValue<Boolean> enableFriendlyFire;

    public GameContainer(AbstractConfig config, String pathPrefix) {
        super(config, pathPrefix);
    }

    @Override
    protected void load() {
        length = loadValue("length", Integer.class, 900,
            ConfigValidator.min(1));
        minPlayers = loadValue("min-players", Integer.class, 2,
            ConfigValidator.min(1));
        maxPlayers = loadValue("max-players", Integer.class, 12,
            ConfigValidator.min(1));
        enableFriendlyFire = loadValue("enable-friendly-fire", Boolean.class, false);
    }

    public int getLength() { return length.getValue(); }
    public int getMinPlayers() { return minPlayers.getValue(); }
    public int getMaxPlayers() { return maxPlayers.getValue(); }
    public boolean isEnableFriendlyFire() { return enableFriendlyFire.getValue(); }
}
```

### Main Config

```java
public class CTFConfig extends AbstractConfig {
    @ConfigContainer("game")
    private GameContainer game;

    @ConfigContainer("debug")
    private DebugContainer debug;

    @ConfigContainer("messages")
    private MessagesContainer messages;

    private static final CTFConfig INSTANCE = new CTFConfig();
    public static CTFConfig getInstance() { return INSTANCE; }
    private CTFConfig() {}

    @Override
    public void loadData() {
        loadAllContainers();
    }

    @Override public String getName() { return "ctf"; }
    @Override public JavaPlugin getMain() { return Main.getInstance(); }
    @Override public void log(String msg) { Main.log(msg); }

    public GameContainer getGame() { return game; }
    public DebugContainer getDebug() { return debug; }
    public MessagesContainer getMessages() { return messages; }
}
```

### YAML File (ctf.yml)

```yaml
game:
  length: 900
  min-players: 2
  max-players: 12
  enable-friendly-fire: false

debug:
  log-phase-transitions: true
  log-flag-events: true
  verbose-logging: false

messages:
  flag-taken: "&a{player} &ftook the flag!"
  flag-returned: "&aFlag returned!"
```

### Usage

```java
CTFConfig config = CTFConfig.getInstance();
config.load();

// Access values through containers
int gameLength = config.getGame().getLength();
boolean debugMode = config.getDebug().isVerboseLogging();
String message = config.getMessages().getFlagTaken();
```

## Backwards Compatibility

Keep legacy getters for existing code:

```java
public class CTFConfig extends AbstractConfig {
    @ConfigContainer("game")
    private GameContainer game;

    // ... container setup ...

    // New way (preferred)
    public GameContainer getGame() { return game; }

    // Old way (backwards compatible)
    public int getGameLength() {
        return game.getLength();
    }
}

// Both work:
config.getGame().getLength();        // New
config.getGameLength();              // Old (still works)
```

## Benefits

### Organization
- Each logical section gets its own file
- Main config becomes much shorter
- Easy to find where values are defined

### Reduced Boilerplate
- No prefixed variable names
- Shorter getter/setter names within container
- Path prefix handled automatically
- One-line loading with `loadAllContainers()`

### Maintainability
- Changes to one section isolated to its container
- Clear separation of concerns
- Easier to test individual sections

### Flexibility
- Mix containers and direct values
- Can nest containers if needed
- Works with all existing features (validation, caching, etc.)

## Patterns

### Pattern 1: Ability Configs

Perfect for games with multiple abilities:

```java
public class AbilitiesConfig extends AbstractConfig {
    @ConfigContainer("keraunos")
    private KeraunosContainer keraunos;

    @ConfigContainer("medkit")
    private MedkitContainer medkit;

    @ConfigContainer("wraith-water")
    private WraithWaterContainer wraithWater;

    @Override
    public void loadData() {
        loadAllContainers();
    }
}

// Usage
AbilitiesConfig.getInstance().getKeraunos().getCooldown();
AbilitiesConfig.getInstance().getMedkit().getTotalCharges();
```

### Pattern 2: Feature Sections

Group related features:

```java
@ConfigContainer("economy")
private EconomyContainer economy;

@ConfigContainer("chat")
private ChatContainer chat;

@ConfigContainer("teleport")
private TeleportContainer teleport;
```

### Pattern 3: Nested Organization

For very large configs:

```java
public class VisualsContainer extends ValueContainer {
    @ConfigContainer("particles")
    private ParticlesContainer particles;

    @ConfigContainer("sounds")
    private SoundsContainer sounds;

    // ... (Note: nested containers require manual loading)
}
```

## Alternative: Manual Loading

If you prefer not to use annotations:

```java
public class CTFConfig extends AbstractConfig {
    private GameContainer game;
    private DebugContainer debug;

    @Override
    public void loadData() {
        // Manual loading
        game = loadContainer(new GameContainer(this, "game"));
        debug = loadContainer(new DebugContainer(this, "debug"));
    }
}
```

## Tips

1. **Group by functionality**, not by type (e.g., "debug" not "booleans")
2. **Keep containers focused** - if a container has 20+ values, consider splitting it
3. **Use descriptive names** - "messages" is better than "msg"
4. **Maintain backwards compatibility** with legacy getters during migration
5. **Document your containers** - add JavaDoc to explain what each section controls

## When to Use Containers

### Use containers when:
- ✅ Config has 10+ values
- ✅ Values naturally group into logical sections
- ✅ You want cleaner organization
- ✅ Multiple people work on the config

### Skip containers when:
- ❌ Config is very small (< 5 values)
- ❌ No clear logical groupings
- ❌ Values are truly independent

## Summary

ValueContainers transform this:

```java
private ConfigValue<Boolean> debugLogPhaseTransitions = new ConfigValue<>(...);
private ConfigValue<Boolean> debugLogFlagEvents = new ConfigValue<>(...);
private ConfigValue<String> messageFlagTaken = new ConfigValue<>(...);
private ConfigValue<String> messageFlagReturned = new ConfigValue<>(...);

@Override
public void loadData() {
    debugLogPhaseTransitions = getConfigValue(debugLogPhaseTransitions);
    debugLogFlagEvents = getConfigValue(debugLogFlagEvents);
    messageFlagTaken = getConfigValue(messageFlagTaken);
    messageFlagReturned = getConfigValue(messageFlagReturned);
}
```

Into this:

```java
@ConfigContainer("debug")
private DebugContainer debug;

@ConfigContainer("messages")
private MessagesContainer messages;

@Override
public void loadData() {
    loadAllContainers();
}
```

Much cleaner!
