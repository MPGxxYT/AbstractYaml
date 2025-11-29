# Validation Guide

Validators automatically check config values to ensure they meet your requirements. Validation happens on load and save.

## Why Validate?

Without validation:
```java
// User sets max-players to -100 in config
int max = config.getMaxPlayers(); // -100 (causes bugs!)
```

With validation:
```java
private ConfigValue<Integer> maxPlayers =
    new ConfigValue<>("max-players", Integer.class, 100)
        .setValidator(ConfigValidator.range(1, 1000));

// User sets -100 in config
// On load: Warning logged, default (100) used instead
// On save: Exception thrown, prevents saving invalid value
```

## Built-in Validators

### Numeric Validators

```java
// Range (min and max)
.setValidator(ConfigValidator.range(1, 100))

// Minimum value only
.setValidator(ConfigValidator.min(0))

// Maximum value only
.setValidator(ConfigValidator.max(1000))
```

**Examples:**
```java
private ConfigValue<Integer> level =
    new ConfigValue<>("player.level", Integer.class, 1)
        .setValidator(ConfigValidator.range(1, 100));

private ConfigValue<Double> multiplier =
    new ConfigValue<>("economy.multiplier", Double.class, 1.0)
        .setValidator(ConfigValidator.min(0.1));

private ConfigValue<Integer> damage =
    new ConfigValue<>("weapon.damage", Integer.class, 10)
        .setValidator(ConfigValidator.max(100));
```

### String Validators

```java
// Not empty (not null and not "")
.setValidator(ConfigValidator.notEmpty())

// Length range
.setValidator(ConfigValidator.length(3, 20))

// Regex pattern
.setValidator(ConfigValidator.matches("^[A-Z_]+$"))
```

**Examples:**
```java
private ConfigValue<String> serverName =
    new ConfigValue<>("server.name", String.class, "Default")
        .setValidator(ConfigValidator.notEmpty());

private ConfigValue<String> prefix =
    new ConfigValue<>("chat.prefix", String.class, "[Server]")
        .setValidator(ConfigValidator.length(1, 10));

private ConfigValue<String> permission =
    new ConfigValue<>("permission.node", String.class, "plugin.use")
        .setValidator(ConfigValidator.matches("^[a-z.]+$"));
```

### Choice Validators

```java
// Must be one of these values
.setValidator(ConfigValidator.oneOf("red", "blue", "green"))

// Not null
.setValidator(ConfigValidator.notNull())
```

**Examples:**
```java
private ConfigValue<String> difficulty =
    new ConfigValue<>("game.difficulty", String.class, "normal")
        .setValidator(ConfigValidator.oneOf("easy", "normal", "hard"));

private ConfigValue<String> required =
    new ConfigValue<>("required.value", String.class, "default")
        .setValidator(ConfigValidator.notNull());
```

## Custom Validators

### Single Rule

```java
private ConfigValue<Integer> evenNumber =
    new ConfigValue<>("even", Integer.class, 2)
        .setValidator(
            ConfigValidator.<Integer>notNull()
                .addRule(n -> n % 2 == 0, "Must be an even number")
        );
```

### Multiple Rules

```java
private ConfigValue<String> username =
    new ConfigValue<>("username", String.class, "Player")
        .setValidator(
            ConfigValidator.<String>notEmpty()
                .addRule(s -> s.length() >= 3, "Username too short (min 3)")
                .addRule(s -> s.length() <= 16, "Username too long (max 16)")
                .addRule(s -> s.matches("^[a-zA-Z0-9_]+$"), "Only letters, numbers, and underscore allowed")
        );
```

### Complex Logic

```java
private ConfigValue<Integer> teamSize =
    new ConfigValue<>("team-size", Integer.class, 4)
        .setValidator(
            ConfigValidator.<Integer>notNull()
                .addRule(n -> n % 2 == 0, "Team size must be even")
                .addRule(n -> n >= 2, "Need at least 2 players per team")
                .addRule(n -> n <= 10, "Maximum 10 players per team")
        );
```

## Validation Behavior

### On Load (from config file)

```java
@Override
public void loadData() {
    maxPlayers = getConfigValue(maxPlayers);
    // If value is invalid:
    // 1. Warning logged to console
    // 2. Default value used
    // 3. Invalid value saved back to config
}
```

Console output:
```
[WARN] Config Value for 'max-players' failed validation: Value must be between 1 and 1000. Setting to default: 100
```

### On Save (via setter)

```java
public void setMaxPlayers(int max) {
    maxPlayers.setValue(max);
    saveValue(maxPlayers); // Throws exception if invalid!
}

// Usage:
try {
    config.setMaxPlayers(5000); // Invalid!
} catch (IllegalArgumentException e) {
    player.sendMessage("Invalid value: " + e.getMessage());
}
```

## Checking Validation

### Manual Check

```java
ConfigValue<Integer> value = getConfigValue(myValue);

if (!value.isValid()) {
    log("Validation failed!");
    log("Error: " + value.validate().getErrorMessage());
}
```

### Get Validation Result

```java
ConfigValidator.ValidationResult result = myValue.validate();

if (!result.isValid()) {
    log("Validation errors:");
    for (String error : result.getErrors()) {
        log("  - " + error);
    }
}
```

## Real-World Examples

### Example 1: Game Config

```java
public class GameConfig extends AbstractConfig {
    private JavaPlugin plugin;

    private ConfigValue<Integer> roundTime =
        new ConfigValue<>("game.round-time", Integer.class, 300)
            .setValidator(ConfigValidator.range(60, 3600)); // 1-60 minutes

    private ConfigValue<Integer> minPlayers =
        new ConfigValue<>("game.min-players", Integer.class, 2)
            .setValidator(ConfigValidator.range(1, 100));

    private ConfigValue<Integer> maxPlayers =
        new ConfigValue<>("game.max-players", Integer.class, 12)
            .setValidator(ConfigValidator.range(1, 100));

    private ConfigValue<String> mapName =
        new ConfigValue<>("game.map", String.class, "default")
            .setValidator(ConfigValidator.notEmpty());

    // ... (standard methods)
}
```

### Example 2: Economy Config

```java
public class EconomyConfig extends AbstractConfig {
    private JavaPlugin plugin;

    private ConfigValue<Double> startingBalance =
        new ConfigValue<>("economy.starting-balance", Double.class, 100.0)
            .setValidator(ConfigValidator.min(0.0));

    private ConfigValue<Double> taxRate =
        new ConfigValue<>("economy.tax-rate", Double.class, 0.05)
            .setValidator(ConfigValidator.range(0.0, 1.0)); // 0-100%

    private ConfigValue<String> currencySymbol =
        new ConfigValue<>("economy.currency-symbol", String.class, "$")
            .setValidator(ConfigValidator.length(1, 3));

    // ... (standard methods)
}
```

### Example 3: Chat Config with Custom Validation

```java
public class ChatConfig extends AbstractConfig {
    private JavaPlugin plugin;

    private ConfigValue<String> chatFormat =
        new ConfigValue<>("chat.format", String.class, "{player}: {message}")
            .setValidator(
                ConfigValidator.<String>notEmpty()
                    .addRule(
                        s -> s.contains("{player}") && s.contains("{message}"),
                        "Format must contain {player} and {message} placeholders"
                    )
            );

    private ConfigValue<Integer> maxMessageLength =
        new ConfigValue<>("chat.max-length", Integer.class, 256)
            .setValidator(
                ConfigValidator.<Integer>range(10, 1000)
                    .addRule(n -> n % 10 == 0, "Must be a multiple of 10")
            );

    private ConfigValue<String> colorCode =
        new ConfigValue<>("chat.color-code", String.class, "&")
            .setValidator(ConfigValidator.oneOf("&", "§", "%"));

    // ... (standard methods)
}
```

### Example 4: Permission Nodes

```java
private ConfigValue<String> adminPermission =
    new ConfigValue<>("permissions.admin", String.class, "plugin.admin")
        .setValidator(
            ConfigValidator.<String>notEmpty()
                .addRule(
                    s -> s.matches("^[a-z.]+$"),
                    "Permission must be lowercase with dots only"
                )
                .addRule(
                    s -> !s.startsWith(".") && !s.endsWith("."),
                    "Permission cannot start or end with a dot"
                )
        );
```

## List Validators

For List config values, use specialized list validators:

```java
private ConfigValue<List> playerNames =
    new ConfigValue<>("players", List.class, Arrays.asList("Steve", "Alex"))
        .setValidator(ListValidators.stringList(1, 100));

private ConfigValue<List> rewards =
    new ConfigValue<>("rewards", List.class, Arrays.asList(10, 25, 50))
        .setValidator(ListValidators.integerList(1, 10, 1, 1000));
        //                                       minSize, maxSize, minValue, maxValue
```

See [WorkingWithLists.md](WorkingWithLists.md) for complete list validation guide.

## Combining Validators

Validators are chainable:

```java
private ConfigValue<String> code =
    new ConfigValue<>("server.code", String.class, "ABC123")
        .setValidator(
            ConfigValidator.<String>notNull()          // Not null
                .addRule(s -> !s.isEmpty(), "Cannot be empty")     // Not empty
                .addRule(s -> s.length() == 6, "Must be 6 characters")  // Exact length
                .addRule(s -> s.matches("^[A-Z0-9]+$"), "Must be uppercase alphanumeric")  // Pattern
        );
```

## Performance Notes

- Validation is **fast** - runs in microseconds
- Validated values are **cached** - validation only happens on load/save
- Failed validation on load **doesn't slow down** the server
- Use validation liberally - the performance cost is negligible

## Best Practices

1. **Validate user-facing configs** - Anything users edit should be validated
2. **Provide clear error messages** - Make it easy to fix config errors
3. **Use appropriate validators** - `range()` is better than custom rules for numeric bounds
4. **Test edge cases** - Try invalid values to ensure validators work
5. **Document valid values** - Add comments in default config.yml

## Validation vs Runtime Checks

**Use validators for:**
- Config file values
- User-editable settings
- Values that must meet specific criteria

**Use runtime checks for:**
- Dynamic values (player input, calculations, etc.)
- Complex business logic
- Values that depend on game state

## Summary

| Validator | Use Case | Example |
|-----------|----------|---------|
| `range(min, max)` | Numeric bounds | Player level (1-100) |
| `min(value)` | Minimum only | Positive numbers |
| `max(value)` | Maximum only | Upper limits |
| `notEmpty()` | Required strings | Server name |
| `length(min, max)` | String length | Username (3-16 chars) |
| `matches(regex)` | Pattern matching | Permission nodes |
| `oneOf(values)` | Fixed choices | Difficulty (easy/normal/hard) |
| Custom rules | Complex logic | Even numbers, custom formats |

Validation makes your configs **safer**, **more reliable**, and **easier to debug**!
