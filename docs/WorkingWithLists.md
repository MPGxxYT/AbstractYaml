# Working with Lists

Guide for using `List` type config values with AbstractYaml.

## Basic List Usage

### Declaration

```java
private ConfigValue<List> items =
    new ConfigValue<>("items", List.class, Arrays.asList("sword", "shield"));
```

**Important:** Use `List.class` (raw type) due to Java's type erasure.

### Getter with Type Casting

```java
@SuppressWarnings("unchecked")
public List<String> getItems() {
    return (List<String>) (List<?>) items.getValue();
}
```

### Setter

```java
public void setItems(List<String> newItems) {
    items.setValue((List) newItems);
    saveValue(items);
}
```

## List Types

### List<String>

```java
private ConfigValue<List> commands =
    new ConfigValue<>("allowed-commands", List.class,
        Arrays.asList("help", "info", "stats"));

@SuppressWarnings("unchecked")
public List<String> getAllowedCommands() {
    return (List<String>) (List<?>) commands.getValue();
}
```

**YAML:**
```yaml
allowed-commands:
  - help
  - info
  - stats
```

### List<Integer>

```java
private ConfigValue<List> levels =
    new ConfigValue<>("reward-levels", List.class,
        Arrays.asList(1, 5, 10, 20, 50));

@SuppressWarnings("unchecked")
public List<Integer> getRewardLevels() {
    return (List<Integer>) (List<?>) levels.getValue();
}
```

**YAML:**
```yaml
reward-levels:
  - 1
  - 5
  - 10
  - 20
  - 50
```

### List<Double>

```java
private ConfigValue<List> multipliers =
    new ConfigValue<>("damage-multipliers", List.class,
        Arrays.asList(1.0, 1.5, 2.0, 2.5));

@SuppressWarnings("unchecked")
public List<Double> getDamageMultipliers() {
    return (List<Double>) (List<?>) multipliers.getValue();
}
```

**YAML:**
```yaml
damage-multipliers:
  - 1.0
  - 1.5
  - 2.0
  - 2.5
```

### List<Boolean>

```java
private ConfigValue<List> features =
    new ConfigValue<>("features.enabled", List.class,
        Arrays.asList(true, false, true, true));

@SuppressWarnings("unchecked")
public List<Boolean> getFeaturesEnabled() {
    return (List<Boolean>) (List<?>) features.getValue();
}
```

**YAML:**
```yaml
features:
  enabled:
    - true
    - false
    - true
    - true
```

## List Validation

Use `ListValidators` for list-specific validation:

### Size Validation

```java
private ConfigValue<List> players =
    new ConfigValue<>("whitelist", List.class, Arrays.asList("Steve"))
        .setValidator(ListValidators.minSize(1));

private ConfigValue<List> teams =
    new ConfigValue<>("teams", List.class, Arrays.asList("Red", "Blue"))
        .setValidator(ListValidators.sizeRange(2, 4));
```

### Type Validation

```java
private ConfigValue<List> names =
    new ConfigValue<>("player-names", List.class, Arrays.asList("Player1"))
        .setValidator(ListValidators.allInstanceOf(String.class));

private ConfigValue<List> values =
    new ConfigValue<>("values", List.class, Arrays.asList(1, 2, 3))
        .setValidator(ListValidators.allInstanceOf(Integer.class));
```

### String List Validators

```java
// All strings non-empty
private ConfigValue<List> nonEmptyList =
    new ConfigValue<>("list", List.class, Arrays.asList("a", "b"))
        .setValidator(ListValidators.allStringsNotEmpty());

// Regex pattern for all strings
private ConfigValue<List> upperCaseList =
    new ConfigValue<>("codes", List.class, Arrays.asList("ABC", "DEF"))
        .setValidator(ListValidators.allStringsMatch("^[A-Z]+$"));

// Combined: size + type + non-empty
private ConfigValue<List> items =
    new ConfigValue<>("items", List.class, Arrays.asList("sword"))
        .setValidator(ListValidators.stringList(1, 100));
```

### Numeric List Validators

```java
// All integers in range
private ConfigValue<List> levels =
    new ConfigValue<>("levels", List.class, Arrays.asList(1, 5, 10))
        .setValidator(ListValidators.allIntegersInRange(1, 100));

// All doubles in range
private ConfigValue<List> chances =
    new ConfigValue<>("drop-chances", List.class, Arrays.asList(0.1, 0.5))
        .setValidator(ListValidators.allDoublesInRange(0.0, 1.0));

// Combined: size + type + range
private ConfigValue<List> rewards =
    new ConfigValue<>("rewards", List.class, Arrays.asList(10, 25))
        .setValidator(ListValidators.integerList(1, 10, 1, 1000));
        //                                       minSize, maxSize, minValue, maxValue
```

### Other Validators

```java
// No duplicates
private ConfigValue<List> unique =
    new ConfigValue<>("unique-items", List.class, Arrays.asList("a", "b"))
        .setValidator(ListValidators.noDuplicates());

// All booleans valid
private ConfigValue<List> flags =
    new ConfigValue<>("flags", List.class, Arrays.asList(true, false))
        .setValidator(ListValidators.allBooleansValid());
```

## Complete Examples

### Example 1: Whitelist

```java
private ConfigValue<List> whitelist =
    new ConfigValue<>("whitelist", List.class,
        Arrays.asList("Steve", "Alex"))
        .setValidator(
            ListValidators.stringList(1, 100)
                .addRule(
                    list -> list.stream()
                        .map(obj -> (String) obj)
                        .allMatch(s -> s.matches("^[a-zA-Z0-9_]{3,16}$")),
                    "Player names must be 3-16 alphanumeric characters"
                )
        );

@SuppressWarnings("unchecked")
public List<String> getWhitelist() {
    return (List<String>) (List<?>) whitelist.getValue();
}

public void addToWhitelist(String player) {
    List<String> current = getWhitelist();
    current.add(player);
    whitelist.setValue((List) current);
    saveValue(whitelist);
}
```

**YAML:**
```yaml
whitelist:
  - Steve
  - Alex
  - Notch
```

### Example 2: Reward Tiers

```java
private ConfigValue<List> rewards =
    new ConfigValue<>("rewards.experience", List.class,
        Arrays.asList(10, 25, 50, 100))
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
                    "Rewards must be in ascending order"
                )
        );

@SuppressWarnings("unchecked")
public List<Integer> getExperienceRewards() {
    return (List<Integer>) (List<?>) rewards.getValue();
}
```

**YAML:**
```yaml
rewards:
  experience:
    - 10
    - 25
    - 50
    - 100
```

### Example 3: Item Blacklist

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
                    "Item names must be uppercase with underscores"
                )
                .addRule(
                    list -> list.size() == list.stream().distinct().count(),
                    "No duplicate items allowed"
                )
        );

@SuppressWarnings("unchecked")
public List<String> getBannedItems() {
    return (List<String>) (List<?>) bannedItems.getValue();
}
```

**YAML:**
```yaml
banned-items:
  - TNT
  - LAVA_BUCKET
  - BEDROCK
  - COMMAND_BLOCK
```

## Why the Casting?

Java's type erasure means `List<String>` becomes just `List` at runtime. Therefore:

```java
// Declaration - use List.class (raw type)
private ConfigValue<List> names =
    new ConfigValue<>("names", List.class, Arrays.asList("a", "b"));

// Getter - cast from List to List<T>
@SuppressWarnings("unchecked")
public List<String> getNames() {
    return (List<String>) (List<?>) names.getValue();
}

// Setter - cast from List<T> to List
public void setNames(List<String> names) {
    this.names.setValue((List) names);
    saveValue(this.names);
}
```

The `@SuppressWarnings("unchecked")` is safe because we control the types.

## Common Patterns

### Accessing by Index

```java
List<Integer> levels = config.getRewardLevels();
int firstLevel = levels.get(0);
int lastLevel = levels.get(levels.size() - 1);
```

### Iterating

```java
for (String item : config.getBannedItems()) {
    // Process each item
}
```

### Modifying

```java
List<String> items = config.getItems();
items.add("new_item");
config.setItems(items); // Save back
```

### Checking Contains

```java
if (config.getWhitelist().contains(playerName)) {
    // Player is whitelisted
}
```

## Best Practices

1. **Always use explicit List.class** in declaration
2. **Add @SuppressWarnings("unchecked")** to getters
3. **Validate list size** - prevent empty or huge lists
4. **Validate element types** - ensure all elements are correct type
5. **Document expected format** in config.yml comments

## Summary

| List Type | Declaration | Getter Cast | Validator |
|-----------|-------------|-------------|-----------|
| `List<String>` | `List.class` | `(List<String>) (List<?>)` | `ListValidators.stringList()` |
| `List<Integer>` | `List.class` | `(List<Integer>) (List<?>)` | `ListValidators.integerList()` |
| `List<Double>` | `List.class` | `(List<Double>) (List<?>)` | `ListValidators.allDoublesInRange()` |
| `List<Boolean>` | `List.class` | `(List<Boolean>) (List<?>)` | `ListValidators.allBooleansValid()` |

Lists are fully supported with proper validation - just remember the casting pattern!
