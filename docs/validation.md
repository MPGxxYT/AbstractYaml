# Validation

A validator checks a value after it has been read and converted. Pass one to any builder method, before the comment:

```java
section("game")
    .intValue("max-players", 16, Validator.range(2, 64), "Players per arena")
    .stringValue("mode", "solo", Validator.oneOf("solo", "duos", "squads"), "Game mode")
    .stringListValue("kits", List.of("archer"), Validator.minSize(1), "At least one kit");
```

## When validation runs

| Moment | On failure |
| --- | --- |
| First run, before defaults are written | `ConfigLoadException`; nothing is written. A default that fails its own validator is a bug in the schema |
| Every load and reload | `ConfigLoadException` listing every failing path |
| `config.with(path, value)` | `IllegalArgumentException`; the config is not changed |
| `config.validate()` / `ConfigManager.validateAll()` | Returns a `ValidationResult` you can inspect |

A failed load reported by `ConfigManager.loadAll()` or `reloadAll()` looks like this:

```text
Configuration validation failed for config.yml:
messages.welcome: Value cannot be empty
spawn.delay: Value must be between 0 and 60
```

Validation on load can be turned off for a single loader with `ConfigLoader.builder(...).validateOnLoad(false)`.

## Built-in validators

### Numbers

| Validator | Passes when |
| --- | --- |
| `Validator.range(min, max)` | `min <= value <= max` |
| `Validator.min(min)` | `value >= min` |
| `Validator.max(max)` | `value <= max` |

Each has an `int` and a `double` form. Use the form that matches the value: `Validator.min(0)` for an `intValue`, `Validator.min(0.0)` for a `doubleValue`.

### Strings

| Validator | Passes when |
| --- | --- |
| `Validator.notEmpty()` | not null and not blank |
| `Validator.length(min, max)` | length is between `min` and `max` |
| `Validator.matches(regex)` | the whole string matches `regex` |

### Any type

| Validator | Passes when |
| --- | --- |
| `Validator.notNull()` | the value is not null |
| `Validator.oneOf(a, b, ...)` | the value equals one of the arguments |
| `Validator.none()` | always (the default) |

### Lists

| Validator | Passes when |
| --- | --- |
| `Validator.minSize(n)`, `maxSize(n)`, `sizeRange(min, max)` | the list size is in range |
| `Validator.noDuplicates()` | no element appears twice |
| `Validator.allStringsNotEmpty()` | no element is blank |
| `Validator.allStringsMatch(regex)` | every element matches `regex` |
| `Validator.allIntegersInRange(min, max)` | every element is in range |
| `Validator.allDoublesInRange(min, max)` | every element is in range |
| `Validator.allMatch(predicate, message)` | every element passes `predicate` |

## Combining and custom validators

`and` chains validators. The first failure stops the chain, so later checks can rely on earlier ones:

```java
Validator<String> worldName =
    Validator.notEmpty().and(Validator.matches("[a-z0-9_]+"));
```

`Validator.of` builds a validator from a predicate and an error message:

```java
Validator<Integer> even = Validator.of(n -> n % 2 == 0, "Value must be even");

section("arena").intValue("teams", 2, Validator.range(2, 8).and(even), "Number of teams");
```

`Validator` is a functional interface, so a lambda that returns a `ValidationResult` also works:

```java
Validator<String> material = value ->
    org.bukkit.Material.matchMaterial(value) != null
        ? ValidationResult.valid()
        : ValidationResult.invalid("Unknown material: " + value);
```
