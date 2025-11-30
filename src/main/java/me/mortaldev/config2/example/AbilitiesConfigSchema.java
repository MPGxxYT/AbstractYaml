package me.mortaldev.config2.example;

import me.mortaldev.config2.ConfigSchema;
import me.mortaldev.config2.ConfigValue;
import me.mortaldev.config2.Validator;

/**
 * Example configuration schema for abilities. This demonstrates the new streamlined config system.
 *
 * <p>Compare this to the old system which required: - AbstractConfig class - Multiple container
 * classes - Manual field declarations - Boilerplate getters/setters
 *
 * <p>The new system requires only this single schema class!
 */
public class AbilitiesConfigSchema extends ConfigSchema {

  public AbilitiesConfigSchema() {
    super("abilities");

    // Set file header
    header(
        "====================================================================\n"
            + "                     Abilities Configuration\n"
            + "====================================================================\n"
            + "Configure cooldowns, ranges, and other ability settings.\n"
            + "====================================================================");

    // Keraunos ability (lightning strike)
    section("keraunos")
        .intValue("cooldown", 10, Validator.range(0, 300), "Cooldown in seconds")
        .doubleValue("radius", 3.0, Validator.min(0.0), "Effect radius in blocks")
        .doubleValue(
            "horizontal-strength", 0.8, Validator.min(0.0), "Knockback horizontal strength")
        .doubleValue("vertical-strength", 1.0, Validator.min(0.0), "Knockback vertical strength");

    // Medkit ability
    section("medkit")
        .intValue("target-range", 5, Validator.range(1, 20), "Range to target allies")
        .intValue("cooldown", 30, Validator.range(0, 300), "Cooldown in seconds")
        .intValue("total-charges", 3, Validator.range(1, 10), "Total charges before depletion");

    // Wraith Water ability
    section("wraith-water")
        .intValue("duration", 5, Validator.range(1, 60), "Invisibility duration in seconds");

    // You can also add top-level values without a section
    value(new ConfigValue.Boolean("abilities-enabled", true, "Master toggle for all abilities"));
  }
}
