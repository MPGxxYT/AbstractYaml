<div align="center">

# AbstractYaml

[![JitPack](https://jitpack.io/v/MPGxxYT/AbstractYaml.svg)](https://jitpack.io/#MPGxxYT/AbstractYaml)
![Java 21](https://img.shields.io/badge/Java-21-orange)
![Paper API](https://img.shields.io/badge/Paper%20API-1.19.4-blue)

Typed, validated YAML configuration for Paper plugins.<br>
Declare a schema once and get a commented config file, checked values and typed getters.

[Overview](#overview) • [Installation](#installation) • [Quick start](#quick-start) • [Documentation](#documentation) • [Building from source](#building-from-source)

</div>

## Overview

Plugin configs tend to repeat the same work for every value: pick a path, read it, check its type, fall back to a default, write a comment into the default file. AbstractYaml moves that work into a **schema**. You list each value once, with its type, default, validator and comment, and the library does the rest:

```java
@RegisterConfig(priority = 10)
public class AbilitiesSchema extends ConfigSchema {
  public AbilitiesSchema() {
    super("abilities");
    header("Ability settings for MyPlugin");

    section("fireball")
        .header("Fireball ability")
        .boolValue("enabled", true, "Turn the ability on or off")
        .intValue("cooldown", 10, Validator.range(1, 300), "Seconds between casts")
        .doubleValue("radius", 3.0, Validator.min(0.5), "Explosion radius in blocks")
        .stringListValue("worlds", List.of("world", "arena"), "Worlds where it can be cast");
  }
}
```

On first run this writes `plugins/MyPlugin/abilities.yml`:

```yaml
# Ability settings for MyPlugin

# Fireball ability
fireball:
  enabled: true                         # Turn the ability on or off
  cooldown: 10                          # Seconds between casts
  radius: 3.0                           # Explosion radius in blocks
  worlds:  # Worlds where it can be cast
    - "world"
    - "arena"
```

After that, server owners edit the file and your code reads typed values:

```java
ConfigSection fireball = ConfigManager.getInstance().getSection("abilities", "fireball");
int cooldown = fireball.getInt("cooldown");
List<String> worlds = fireball.getStringList("worlds");
```

## Features

- **One declaration per value.** Type, default, validator and comment sit together in the schema.
- **Generated files.** Missing files are written with your header, section headers and inline comments, in the order you declared them.
- **Typed access.** `getInt`, `getDouble`, `getStringList`, `getMap`, `getIntMap` and more, with no casting. Sections can be nested.
- **Forgiving reads.** A missing or malformed value logs a warning naming the path and falls back to its default. `5.0` reads as an int, `"5"` reads as a number.
- **Validation at load time.** Out-of-range values fail with every error listed at once, before your plugin uses them.
- **Immutable configs.** A loaded `Config` never changes, so it is safe to read from async tasks. `with(...)` returns an updated copy.
- **Load order and reloads.** `ConfigManager` loads configs by priority, reports which ones failed and why, and keeps the previous values when a reload fails.
- **Testable without a server.** Configs can load from an in-memory string, with no `JavaPlugin` and no files.
- **Generated wrappers.** Add `@GenerateWrapper` and the bundled annotation processor turns a schema into static accessors such as `AbilitiesWrapper.Fireball.cooldown()` at compile time, with Maven or Gradle.

## Installation

AbstractYaml is published through [JitPack](https://jitpack.io/#MPGxxYT/AbstractYaml). Add the repository and the dependency to your plugin's `pom.xml`:

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.github.MPGxxYT</groupId>
    <artifactId>AbstractYaml</artifactId>
    <version>v2.0.1</version>
  </dependency>
</dependencies>
```

Or with Gradle:

```groovy
repositories {
  maven { url 'https://jitpack.io' }
}

dependencies {
  implementation 'com.github.MPGxxYT:AbstractYaml:v2.0.1'
}
```

Other [releases](https://github.com/MPGxxYT/AbstractYaml/releases) are listed on GitHub; `master-SNAPSHOT` builds the latest commit. To generate [wrapper classes](docs/generated-wrappers.md), also add the library to the annotation processor path.

> [!NOTE]
> The library is not a plugin: it has no `plugin.yml`. Shade it into your plugin jar (for example with `maven-shade-plugin`) and relocate `me.mortaldev` if several plugins on the same server bundle different versions.

> [!IMPORTANT]
> These docs describe v2.0.1. `v1.0.0` predates `ConfigSource`, `ConfigView`, the map getters, the instance-based `ConfigManager` and the wrapper processor. See [CHANGELOG.md](CHANGELOG.md) for what changed.

## Quick start

**1. Declare a schema.** Annotate it with `@RegisterConfig` so the manager can find its file name and load order.

```java
@RegisterConfig(priority = 10)
public class AbilitiesSchema extends ConfigSchema {
  public AbilitiesSchema() {
    super("abilities"); // file: abilities.yml
    section("fireball")
        .intValue("cooldown", 10, Validator.range(1, 300), "Seconds between casts");
  }
}
```

**2. Register and load it** from your plugin class.

```java
public final class MyPlugin extends JavaPlugin {
  static {
    ConfigRegistry.register(AbilitiesSchema.class);
  }

  @Override
  public void onEnable() {
    ConfigManager.init(this);
    Map<String, Exception> failures = ConfigManager.getInstance().loadAll();
    failures.forEach((name, error) ->
        getLogger().severe(name + ".yml was not loaded: " + error.getMessage()));
  }
}
```

**3. Read values** anywhere.

```java
int cooldown = ConfigManager.getInstance().get("abilities").getInt("fireball.cooldown");
```

**4. Reload** after server owners edit the file, for example from a command.

```java
Map<String, Exception> failures = ConfigManager.getInstance().reloadAll();
```

> [!TIP]
> A config that fails to reload keeps its previous values, so a typo in a file never leaves your plugin without settings.

## Documentation

| Guide | Covers |
| --- | --- |
| [Getting started](docs/getting-started.md) | A first config from schema to reload command |
| [Schemas](docs/schemas.md) | Value types, sections, comments, file layout and how values are read |
| [Reading and updating values](docs/reading-and-updating.md) | `Config`, `ConfigSection`, typed getters, `with(...)` and saving |
| [Validation](docs/validation.md) | Built-in validators, combining them, and what happens when a value fails |
| [ConfigManager](docs/config-manager.md) | Registration, priorities, file paths, failures and reloads |
| [Testing](docs/testing.md) | Loading configs in unit tests without a server |
| [Generated wrappers](docs/generated-wrappers.md) | Maven and Gradle setup for compile-time accessor classes |
| [Migrating from the legacy API](docs/migrating-from-legacy.md) | Moving an `AbstractConfig` class to a schema |

### Legacy API

The original `AbstractConfig` API in the `me.mortaldev` package is still shipped so existing plugins keep working, but it is deprecated. New plugins should use the schema API in `me.mortaldev.config2`. Its guides are in [docs/legacy](docs/legacy/README.md).

## Building from source

Requires Java 21 and Maven.

```bash
mvn test       # run the test suite
mvn install    # install to your local Maven repository
```

After `mvn install`, other projects on the same machine can depend on `me.mortaldev:AbstractYaml:1.0-SNAPSHOT`.
