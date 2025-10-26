# List ConfigValue Patterns

Quick reference for working with `List` types in ConfigValues.

---

## 🎯 Quick Examples

### List<String>
```java
private ConfigValue<List> allowedCommands =
    new ConfigValue<>("allowed-commands", List.class,
        Arrays.asList("help", "info", "stats"));

// With validation
private ConfigValue<List> playerNames =
    new ConfigValue<>("players", List.class,
        Arrays.asList("Steve", "Alex"))
    .setValidator(ListValidators.stringList(1, 100));
```

### List<Integer>
```java
private ConfigValue<List> levels =
    new ConfigValue<>("levels", List.class,
        Arrays.asList(1, 5, 10, 20, 50));

// With range validation
private ConfigValue<List> rewards =
    new ConfigValue<>("rewards", List.class,
        Arrays.asList(10, 25, 50))
    .setValidator(ListValidators.integerList(1, 10, 1, 1000));
    //                                      minSize, maxSize, minValue, maxValue
```

### List<Boolean>
```java
private ConfigValue<List> features =
    new ConfigValue<>("features", List.class,
        Arrays.asList(true, false, true));

// With validation
private ConfigValue<List> toggles =
    new ConfigValue<>("toggles", List.class,
        Arrays.asList(true, false, true))
    .setValidator(ListValidators.allBooleansValid());
```

### List<Double>
```java
private ConfigValue<List> multipliers =
    new ConfigValue<>("multipliers", List.class,
        Arrays.asList(1.0, 1.5, 2.0));

// With range validation
private ConfigValue<List> chances =
    new ConfigValue<>("drop-chances", List.class,
        Arrays.asList(0.1, 0.25, 0.5))
    .setValidator(ListValidators.allDoublesInRange(0.0, 1.0));
```

---

## 🛡️ Common List Validators

### Size Validators
```java
// Minimum size
.setValidator(ListValidators.minSize(1))

// Maximum size
.setValidator(ListValidators.maxSize(50))

// Size range
.setValidator(ListValidators.sizeRange(1, 100))
```

### Type Validators
```java
// All elements must be strings
.setValidator(ListValidators.allInstanceOf(String.class))

// All elements must be integers
.setValidator(ListValidators.allInstanceOf(Integer.class))

// All strings must be non-empty
.setValidator(ListValidators.allStringsNotEmpty())

// All booleans must be valid
.setValidator(ListValidators.allBooleansValid())
```

### Value Range Validators
```java
// All integers in range
.setValidator(ListValidators.allIntegersInRange(1, 100))

// All doubles in range
.setValidator(ListValidators.allDoublesInRange(0.0, 10.0))
```

### Pattern Validators
```java
// All strings match regex
.setValidator(ListValidators.allStringsMatch("^[A-Z_]+$"))

// No duplicates
.setValidator(ListValidators.noDuplicates())
```

### Combo Validators
```java
// String list with size and type validation
.setValidator(ListValidators.stringList(1, 50))

// Integer list with size and value range
.setValidator(ListValidators.integerList(1, 10, 0, 100))
//                                       minSize, maxSize, minVal, maxVal
```

---

## 📋 Complete Examples

### Example 1: Whitelist Players
```java
private ConfigValue<List> whitelistedPlayers =
    new ConfigValue<>("whitelist", List.class,
        Arrays.asList("Steve", "Alex"))
    .setValidator(
        ListValidators.stringList(1, 100)
            .addRule(
                list -> list.stream()
                    .map(obj -> (String) obj)
                    .allMatch(s -> !s.trim().isEmpty()),
                "Player names cannot be empty")
            .addRule(
                list -> list.stream()
                    .map(obj -> (String) obj)
                    .allMatch(s -> s.matches("^[a-zA-Z0-9_]{3,16}$")),
                "Player names must be 3-16 alphanumeric characters"));

// Getter with proper type casting
@SuppressWarnings("unchecked")
public List<String> getWhitelistedPlayers() {
    return (List<String>) (List<?>) whitelistedPlayers.getValue();
}

// Setter
public void setWhitelistedPlayers(List<String> players) {
    whitelistedPlayers.setValue((List) players);
    saveValue(whitelistedPlayers);
}
```

### Example 2: Reward Tiers (Ascending Order)
```java
private ConfigValue<List> experienceRewards =
    new ConfigValue<>("rewards.experience", List.class,
        Arrays.asList(10, 25, 50, 100, 250))
    .setValidator(
        ListValidators.integerList(1, 20, 1, 10000)
            .addRule(
                list -> {
                    // Validate ascending order
                    for (int i = 1; i < list.size(); i++) {
                        if ((Integer) list.get(i) <= (Integer) list.get(i - 1)) {
                            return false;
                        }
                    }
                    return true;
                },
                "Rewards must be in ascending order"));

@SuppressWarnings("unchecked")
public List<Integer> getExperienceRewards() {
    return (List<Integer>) (List<?>) experienceRewards.getValue();
}
```

### Example 3: Item Blacklist (Uppercase Only)
```java
private ConfigValue<List> bannedItems =
    new ConfigValue<>("banned-items", List.class,
        Arrays.asList("TNT", "LAVA_BUCKET", "BEDROCK"))
    .setValidator(
        ListValidators.stringList(1, 100)
            .addRule(
                list -> list.stream()
                    .map(obj -> (String) obj)
                    .allMatch(s -> s.matches("^[A-Z_]+$")),
                "Item names must be uppercase with underscores")
            .addRule(
                list -> list.size() == list.stream().distinct().count(),
                "No duplicate items allowed"));

@SuppressWarnings("unchecked")
public List<String> getBannedItems() {
    return (List<String>) (List<?>) bannedItems.getValue();
}
```

### Example 4: Drop Chances (0.0 to 1.0)
```java
private ConfigValue<List> dropChances =
    new ConfigValue<>("loot.drop-chances", List.class,
        Arrays.asList(0.1, 0.25, 0.5, 0.75, 1.0))
    .setValidator(
        ListValidators.allDoublesInRange(0.0, 1.0)
            .addRule(list -> list.size() >= 1, "Must have at least one drop tier"));

@SuppressWarnings("unchecked")
public List<Double> getDropChances() {
    return (List<Double>) (List<?>) dropChances.getValue();
}
```

### Example 5: Feature Flags
```java
private ConfigValue<List> featureFlags =
    new ConfigValue<>("features.enabled", List.class,
        Arrays.asList(true, false, true, true))
    .setValidator(
        ListValidators.allBooleansValid()
            .addRule(list -> list.size() == 4,
                "Must have exactly 4 feature flags: [PVP, Economy, Chat, Teleport]"));

@SuppressWarnings("unchecked")
public List<Boolean> getFeatureFlags() {
    return (List<Boolean>) (List<?>) featureFlags.getValue();
}

// Helper getters for specific features
public boolean isPvpEnabled() {
    return getFeatureFlags().get(0);
}

public boolean isEconomyEnabled() {
    return getFeatureFlags().get(1);
}
```

---

## 🎨 Custom Validators for Lists

### Validate List Contains Specific Item
```java
.setValidator(
    ConfigValidator.<List>notNull()
        .addRule(
            list -> list.contains("required_item"),
            "List must contain 'required_item'"))
```

### Validate All Items Start With Prefix
```java
.setValidator(
    ListValidators.allStringsNotEmpty()
        .addRule(
            list -> list.stream()
                .map(obj -> (String) obj)
                .allMatch(s -> s.startsWith("prefix_")),
            "All items must start with 'prefix_'"))
```

### Validate Even Numbers Only
```java
.setValidator(
    ListValidators.allInstanceOf(Integer.class)
        .addRule(
            list -> list.stream()
                .map(obj -> (Integer) obj)
                .allMatch(i -> i % 2 == 0),
            "All numbers must be even"))
```

### Validate Total Sum
```java
.setValidator(
    ListValidators.allIntegersInRange(1, 100)
        .addRule(
            list -> {
                int sum = list.stream()
                    .map(obj -> (Integer) obj)
                    .mapToInt(Integer::intValue)
                    .sum();
                return sum == 100;
            },
            "List values must sum to exactly 100"))
```

---

## ⚠️ Type Casting Notes

### Why the weird casting?
Because Java's type erasure means `List<String>` becomes just `List` at runtime, you need to use this pattern:

```java
// In ConfigValue declaration - use List.class (raw type)
private ConfigValue<List> names =
    new ConfigValue<>("names", List.class, Arrays.asList("a", "b"));

// In getter - cast from List to List<T>
@SuppressWarnings("unchecked")
public List<String> getNames() {
    return (List<String>) (List<?>) names.getValue();
}

// In setter - cast from List<T> to List
public void setNames(List<String> names) {
    this.names.setValue((List) names);
    saveValue(this.names);
}
```

### Alternative: Type-Safe Wrapper (Advanced)
```java
// Create a wrapper class for type-safe lists
public class TypedListValue<T> extends ConfigValue<List> {
    private final Class<T> elementType;

    public TypedListValue(String id, Class<T> elementType, List<T> defaultValue) {
        super(id, List.class, defaultValue);
        this.elementType = elementType;
    }

    @SuppressWarnings("unchecked")
    public List<T> getTypedValue() {
        return (List<T>) (List<?>) getValue();
    }

    public void setTypedValue(List<T> value) {
        setValue((List) value);
    }
}

// Usage:
private TypedListValue<String> names =
    new TypedListValue<>("names", String.class, Arrays.asList("a", "b"));

public List<String> getNames() {
    return names.getTypedValue();  // No casting needed!
}
```

---

## 📊 YAML Format Examples

### List<String> in YAML
```yaml
allowed-commands:
  - help
  - info
  - stats

whitelist:
  - Steve
  - Alex
  - Notch
```

### List<Integer> in YAML
```yaml
reward-levels:
  - 1
  - 5
  - 10
  - 25
  - 50

team-sizes:
  - 2
  - 4
  - 8
```

### List<Boolean> in YAML
```yaml
features:
  - true
  - false
  - true
  - true
```

### List<Double> in YAML
```yaml
drop-chances:
  - 0.1
  - 0.25
  - 0.5
  - 0.75
  - 1.0

multipliers:
  - 1.0
  - 1.5
  - 2.0
```

---

## 🔍 Validation Error Checking

```java
@Override
public void loadData() {
    experienceRewards = getConfigValue(experienceRewards);

    // Check if validation failed
    if (!experienceRewards.isValid()) {
        log("Experience rewards validation failed:");
        log(experienceRewards.validate().getErrorMessage());
        // Value will be set to default automatically
    }
}
```

---

## 📝 Summary

| Type | Declaration | Getter Cast | Common Validators |
|------|-------------|-------------|-------------------|
| `List<String>` | `List.class` | `(List<String>) (List<?>)` | `stringList()`, `allStringsNotEmpty()` |
| `List<Integer>` | `List.class` | `(List<Integer>) (List<?>)` | `integerList()`, `allIntegersInRange()` |
| `List<Boolean>` | `List.class` | `(List<Boolean>) (List<?>)` | `allBooleansValid()` |
| `List<Double>` | `List.class` | `(List<Double>) (List<?>)` | `allDoublesInRange()` |

**Pro Tip:** See `ListExampleConfig.java` for complete working examples of all patterns!
