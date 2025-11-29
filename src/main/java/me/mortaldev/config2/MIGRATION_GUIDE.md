# Migration Guide: Old Config System → New Config System

This guide shows how to migrate from the old `AbstractConfig` system to the new streamlined config system.

## Why Migrate?

### Old System Problems:
- ❌ 20+ lines of boilerplate per config class
- ❌ Manual container classes required
- ❌ Raw `List` types requiring unsafe casts
- ❌ Mutable state (thread-safety issues)
- ❌ Complex inheritance hierarchy
- ❌ Deprecated code still in use

### New System Benefits:
- ✅ Single schema class per config
- ✅ Fully type-safe (no casting!)
- ✅ Immutable & thread-safe
- ✅ Simpler API
- ✅ Less code to maintain
- ✅ Better IDE support

---

## Migration Steps

### Step 1: Old Config Structure

**Old: AbilitiesConfig.java** (105 lines)
```java
public class AbilitiesConfig extends AbstractConfig {
  // Boilerplate singleton
  private static class Singleton {
    private static final AbilitiesConfig INSTANCE = new AbilitiesConfig();
  }
  public static AbilitiesConfig getInstance() { return Singleton.INSTANCE; }
  private AbilitiesConfig() {}

  // Container declarations
  @ConfigContainer("medkit")
  private MedkitConfigContainer medkit;

  @ConfigContainer("wraith-water")
  private WraithWaterConfigContainer wraithWater;

  @ConfigContainer("keraunos")
  private KeraunosConfigContainer keraunos;

  // Boilerplate abstract methods
  @Override
  public void log(String message) { Main.log(message); }

  @Override
  public String getName() { return "abilities"; }

  @Override
  public JavaPlugin getMain() { return Main.getInstance(); }

  @Override
  public void loadData() { loadAllContainers(); }

  // Container getters
  public MedkitConfigContainer getMedkit() { return medkit; }
  public WraithWaterConfigContainer getWraithWater() { return wraithWater; }
  public KeraunosConfigContainer getKeraunos() { return keraunos; }

  // Legacy getters (more boilerplate!)
  public int getKeraunosCooldown() { return keraunos.getCooldown(); }
  public double getKeraunosRadius() { return keraunos.getRadius(); }
  // ... 20 more legacy getters
}
```

**Old: KeraunosConfigContainer.java** (45 lines)
```java
public class KeraunosConfigContainer extends ConfigValueContainer {
  private ConfigValue<Integer> cooldown;
  private ConfigValue<Double> radius;
  private ConfigValue<Double> horizontalStrength;
  private ConfigValue<Double> verticalStrength;

  public KeraunosConfigContainer(AbstractConfig config, String pathPrefix) {
    super(config, pathPrefix);
  }

  @Override
  protected void load() {
    cooldown = loadValue("cooldown", Integer.class, 10, ConfigValidator.min(0));
    radius = loadValue("radius", Double.class, 3.0, ConfigValidator.min(0.0));
    horizontalStrength = loadValue("horizontal-strength", Double.class, 0.8, ConfigValidator.min(0.0));
    verticalStrength = loadValue("vertical-strength", Double.class, 1.0, ConfigValidator.min(0.0));
  }

  public int getCooldown() { return cooldown.getValue(); }
  public double getRadius() { return radius.getValue(); }
  public double getHorizontalStrength() { return horizontalStrength.getValue(); }
  public double getVerticalStrength() { return verticalStrength.getValue(); }
}
```

**Total:** ~150+ lines across multiple files

---

### Step 2: New Config Structure

**New: AbilitiesConfigSchema.java** (40 lines - 75% reduction!)
```java
public class AbilitiesConfigSchema extends ConfigSchema {
  public AbilitiesConfigSchema() {
    super("abilities");

    header("====================================================================\n"
        + "                     Abilities Configuration\n"
        + "====================================================================");

    // All values in one place!
    section("keraunos")
        .intValue("cooldown", 10, Validator.range(0, 300), "Cooldown in seconds")
        .doubleValue("radius", 3.0, Validator.min(0.0), "Effect radius in blocks")
        .doubleValue("horizontal-strength", 0.8, Validator.min(0.0), "Knockback horizontal strength")
        .doubleValue("vertical-strength", 1.0, Validator.min(0.0), "Knockback vertical strength");

    section("medkit")
        .intValue("target-range", 5, Validator.range(1, 20), "Range to target allies")
        .intValue("cooldown", 30, Validator.range(0, 300), "Cooldown in seconds")
        .intValue("total-charges", 3, Validator.range(1, 10), "Total charges before depletion");

    section("wraith-water")
        .intValue("duration", 5, Validator.range(1, 60), "Invisibility duration in seconds");
  }
}
```

**No container classes needed!**

---

## Usage Comparison

### Loading Config

**Old:**
```java
AbilitiesConfig config = AbilitiesConfig.getInstance();
config.load();
```

**New:**
```java
Config abilities = ConfigLoader.builder(plugin)
    .schema(new AbilitiesConfigSchema())
    .load();
```

---

### Reading Values

**Old:**
```java
// Through legacy getters
int cooldown = config.getKeraunosCooldown();
double radius = config.getKeraunosRadius();

// Or through containers
int cooldown = config.getKeraunos().getCooldown();
double radius = config.getKeraunos().getRadius();
```

**New:**
```java
// Direct access (type-safe!)
int cooldown = abilities.getInt("keraunos.cooldown");
double radius = abilities.getDouble("keraunos.radius");

// Or use sections for cleaner code
ConfigSection keraunos = abilities.section("keraunos");
int cooldown = keraunos.getInt("cooldown");
double radius = keraunos.getDouble("radius");
```

---

### Updating Values

**Old:**
```java
config.getKeraunos().getCooldown().setValue(15);
config.saveValue(config.getKeraunos().getCooldown());

// Or if legacy setters exist:
config.setKeraunosCooldown(15);
```

**New (Immutable):**
```java
Config updated = abilities.with("keraunos.cooldown", 15);
updated.save(file);

// Or through section
ConfigSection keraunos = abilities.section("keraunos");
Config updated = keraunos.set("cooldown", 15);
```

---

### Working with Lists

**Old (Ugly Casting!):**
```java
private ConfigValue<List> allowedCommands =
    new ConfigValue<>("allowed-commands", List.class, Arrays.asList("help", "info"));

@SuppressWarnings("unchecked")
public List<String> getAllowedCommands() {
    return (List<String>) (List<?>) allowedCommands.getValue();
}
```

**New (Type-Safe!):**
```java
// In schema:
value(new ConfigValue.StringList(
    "allowed-commands",
    List.of("help", "info"),
    "Commands players can use"));

// Usage (no casting!):
List<String> commands = config.getStringList("allowed-commands");
```

---

## Step-by-Step Migration Process

### 1. Create Schema Class

Replace your `AbilitiesConfig extends AbstractConfig` with:

```java
public class AbilitiesConfigSchema extends ConfigSchema {
    public AbilitiesConfigSchema() {
        super("abilities");

        // Copy your container value definitions here
        // Use section() builder for clean code
    }
}
```

### 2. Copy Container Values to Schema

For each container like `KeraunosConfigContainer`:

```java
// OLD:
@Override
protected void load() {
    cooldown = loadValue("cooldown", Integer.class, 10, ConfigValidator.min(0));
}

// NEW:
section("keraunos")
    .intValue("cooldown", 10, Validator.min(0), "Cooldown in seconds");
```

### 3. Update Load Sites

Find all places where you load the config:

```java
// OLD:
AbilitiesConfig config = AbilitiesConfig.getInstance();
config.load();

// NEW:
Config abilities = ConfigLoader.builder(plugin)
    .schema(new AbilitiesConfigSchema())
    .load();
```

### 4. Update Read Sites

Find all places where you read values:

```java
// OLD:
int cooldown = config.getKeraunosCooldown();

// NEW:
int cooldown = abilities.getInt("keraunos.cooldown");
// Or: abilities.section("keraunos").getInt("cooldown");
```

### 5. Update Write Sites

```java
// OLD:
config.setKeraunosCooldown(15);

// NEW:
Config updated = abilities.with("keraunos.cooldown", 15);
updated.save(file);
```

### 6. (Optional) Create Wrapper

If you want to keep the old API temporarily:

```java
public class AbilitiesConfig {
    private final Config config;
    private final ConfigSection keraunos;

    public AbilitiesConfig(JavaPlugin plugin) {
        this.config = ConfigLoader.builder(plugin)
            .schema(new AbilitiesConfigSchema())
            .load();
        this.keraunos = config.section("keraunos");
    }

    // Keep old API
    public int getKeraunosCooldown() {
        return keraunos.getInt("cooldown");
    }

    public void setKeraunosCooldown(int value) {
        config = keraunos.set("cooldown", value);
    }
}
```

---

## Validation Comparison

**Old:**
```java
ConfigValidator<Integer> validator = ConfigValidator.range(1, 100)
    .addRule(n -> n % 5 == 0, "Must be multiple of 5");

configValue.setValidator(validator);
```

**New (Same API!):**
```java
Validator<Integer> validator = Validator.range(1, 100)
    .and(Validator.of(n -> n % 5 == 0, "Must be multiple of 5"));

section("example")
    .intValue("value", 10, validator, "Comment");
```

---

## Benefits Summary

| Feature | Old System | New System |
|---------|-----------|------------|
| Lines of code | ~150+ per config | ~40 per config |
| Type safety | ❌ Raw lists, casting | ✅ Fully type-safe |
| Boilerplate | ❌ High | ✅ Minimal |
| Thread safety | ⚠️ Mutable | ✅ Immutable |
| Container classes | ❌ Required | ✅ Not needed |
| Validation | ✅ Good | ✅ Same + fail-fast |
| IDE support | ⚠️ String paths | ✅ Better autocomplete |

---

## Troubleshooting

### "How do I handle plugin instance?"

**Old:** Stored in AbstractConfig, accessed via `getMain()`

**New:** Pass to ConfigLoader:
```java
Config config = ConfigLoader.builder(plugin)  // ← plugin here
    .schema(schema)
    .load();
```

### "How do I reload configs?"

**Old:**
```java
config.reload();
```

**New:**
```java
ConfigLoader loader = ConfigLoader.builder(plugin)
    .schema(schema)
    .build();

Config reloaded = loader.reload();
```

### "Can I keep my old API temporarily?"

Yes! Create a wrapper class (see Step 6 above). This lets you migrate incrementally.

---

## Full Example

See `example/UsageExample.java` for complete working examples.
