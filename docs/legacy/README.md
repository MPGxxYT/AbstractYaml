# Legacy API

> [!WARNING]
> These guides describe the original `AbstractConfig` API in the `me.mortaldev.abstractyaml` package. It is deprecated and kept so that existing plugins keep working. New plugins should use the schema API; see the [main README](../../README.md) and [Migrating from the legacy API](../migrating-from-legacy.md).

| Guide | Covers |
| --- | --- |
| [Getting started](GettingStarted.md) | A first `AbstractConfig` class |
| [Validation](Validation.md) | `ConfigValidator` and custom rules |
| [Working with lists](WorkingWithLists.md) | List values and `ListValidators` |
| [Value containers](ValueContainers.md) | Splitting a config with `@ConfigContainer` |
| [Migration guide](MigrationGuide.md) | Upgrading from versions before explicit `Class<T>` types |

## Changes since these guides were written

- Numbers convert between `Integer`, `Long`, `Float` and `Double` when nothing is lost, so `radius: 5` reads into a `Double` value.
- Validator rules run in order and stop at the first failure, so only one error is reported per value.
- `regenerateConfigFile()` writes current values instead of defaults.
- Generated files use the same writer as the schema API: comments align at column 40 and string lists are written one item per line.
- `YAML.reloadConfig` throws `UnsupportedOperationException`; load the file again with `YAML.getInstance().getConfig(plugin, name)`.
