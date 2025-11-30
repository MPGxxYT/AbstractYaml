package me.mortaldev.config2.example;

import me.mortaldev.config2.*;

import java.util.List;

/**
 * Example demonstrating the @GenerateWrapper annotation.
 *
 * <p>This shows how to use the annotation processor to automatically
 * generate type-safe wrapper classes from your ConfigSchema definitions.
 */
public class GeneratedWrapperExample {

  // ===== Step 1: Annotate Your Schema =====

  @RegisterConfig(priority = 10)
  @GenerateWrapper // ← Add this annotation!
  public static class AbilitiesConfigSchema extends ConfigSchema {
    public AbilitiesConfigSchema() {
      super("abilities");

      header("Abilities Configuration");

      section("keraunos")
          .intValue("cooldown", 10, Validator.range(0, 300), "Cooldown in seconds")
          .doubleValue("radius", 3.0, Validator.min(0.0), "Effect radius in blocks")
          .doubleValue("horizontal-strength", 0.8, "Horizontal knockback strength")
          .doubleValue("vertical-strength", 1.0, "Vertical knockback strength");

      section("medkit")
          .intValue("target-range", 5, Validator.range(1, 20), "Range to target allies")
          .intValue("cooldown", 30, "Cooldown in seconds")
          .intValue("total-charges", 3, "Total charges before depletion");

      section("wraith-water")
          .intValue("duration", 5, Validator.range(1, 60), "Invisibility duration in seconds");

      value(
          new ConfigValue.StringList(
              "disabled-abilities", List.of(), "List of disabled ability names"));
    }
  }

  // ===== Step 2: Compile =====
  // When you compile your project (mvn compile or ./gradlew build),
  // the annotation processor automatically generates: Abilities.java
  //
  // Generated file structure:
  // public class Abilities {
  //     public static class Keraunos {
  //         public static int cooldown() { ... }
  //         public static double radius() { ... }
  //         public static double horizontalStrength() { ... }
  //         public static double verticalStrength() { ... }
  //     }
  //
  //     public static class Medkit {
  //         public static int targetRange() { ... }
  //         public static int cooldown() { ... }
  //         public static int totalCharges() { ... }
  //     }
  //
  //     public static class WraithWater {
  //         public static int duration() { ... }
  //     }
  //
  //     public static List<String> disabledAbilities() { ... }
  //
  //     public static void reload() { ... }
  // }

  // ===== Step 3: Use the Generated Wrapper =====
  // NOTE: The examples below show what your code would look like.
  // The Abilities class will be generated when YOU compile YOUR project with @GenerateWrapper.
  // This example file is in the AbstractYaml library (which has proc=none), so it can't
  // demonstrate the actual usage. Copy this pattern into your own project!

  /* EXAMPLE USAGE - Copy this to your project after adding @GenerateWrapper:

  public static class KeraunosAbility {
    public void onUse() {
      // Type-safe access! No strings! IDE autocomplete works!
      int cooldown = Abilities.Keraunos.cooldown();
      double radius = Abilities.Keraunos.radius();
      double hStrength = Abilities.Keraunos.horizontalStrength();
      double vStrength = Abilities.Keraunos.verticalStrength();

      // Use the values...
      System.out.println("Keraunos cooldown: " + cooldown);
      System.out.println("Keraunos radius: " + radius);
    }
  }

  public static class MedkitAbility {
    public void onUse() {
      int range = Abilities.Medkit.targetRange();
      int cooldown = Abilities.Medkit.cooldown();
      int charges = Abilities.Medkit.totalCharges();

      System.out.println("Medkit range: " + range);
      System.out.println("Medkit charges: " + charges);
    }
  }

  public static class WraithWaterAbility {
    public void onUse() {
      int duration = Abilities.WraithWater.duration();
      System.out.println("Wraith Water duration: " + duration);
    }
  }

  // Top-level values (not in a section)
  public static class AbilityManager {
    public void checkDisabled(String abilityName) {
      List<String> disabled = Abilities.disabledAbilities();

      if (disabled.contains(abilityName)) {
        System.out.println("Ability " + abilityName + " is disabled!");
      }
    }
  }

  */ // End of example usage

  // ===== Advanced: Custom Wrapper Name =====

  @RegisterConfig
  @GenerateWrapper(value = "CTFSettings") // Custom name
  public static class CTFConfigSchema extends ConfigSchema {
    public CTFConfigSchema() {
      super("ctf");

      section("game")
          .intValue("length", 900, "Game length in seconds")
          .intValue("max-players", 12, "Maximum players");
    }
  }
  // Generates: CTFSettings.java (not "CTF.java")
  // Usage: int length = CTFSettings.Game.length();

  // ===== Advanced: Custom Package =====

  @RegisterConfig
  @GenerateWrapper(packageName = "me.mortaldev.crusaders.api")
  public static class PublicAPISchema extends ConfigSchema {
    public PublicAPISchema() {
      super("api");
    }
  }
  // Generates: me.mortaldev.crusaders.api.PublicAPI.java

  // ===== Advanced: No Reload Methods =====

  @RegisterConfig
  @GenerateWrapper(generateReload = false)
  public static class StaticConfigSchema extends ConfigSchema {
    public StaticConfigSchema() {
      super("static-config");
    }
  }
  // Generated class won't have reload() methods

  // ===== Benefits =====
  // Copy this pattern to your project after @GenerateWrapper generates the wrapper class

  /* EXAMPLE - Benefits demonstration:

  public static void demonstrateBenefits() {
    // ✅ Type-safe - compile errors instead of runtime errors
    int cooldown = Abilities.Keraunos.cooldown(); // Works!
    // int cooldown = Abilities.Keraunos.cooldwon(); // Compile error! Typo caught!

    // ✅ IDE autocomplete - just type "Abilities." and see all options
    // Abilities.Keraunos. → shows: cooldown(), radius(), horizontalStrength(), etc.

    // ✅ Refactoring - renaming works across entire codebase
    // Rename "cooldown" in schema → all usages update automatically

    // ✅ Documentation - JavaDoc from schema comments
    // Hover over Abilities.Keraunos.cooldown() → shows "Cooldown in seconds"

    // ✅ No string typos - impossible to make this mistake:
    // config.getInt("keraunos.cooldwon"); // Runtime error with old system
    // Abilities.Keraunos.cooldown(); // Compile-time safety!
  }

  */

  // ===== Reload Example =====
  // Copy this pattern to your project

  /* EXAMPLE - Reload pattern:

  public static void handleReload() {
    // Reload the config through ConfigManager
    ConfigManager.getInstance().reload("abilities");

    // Clear wrapper's cached sections
    Abilities.reload();

    // Next access will fetch fresh values
    int newCooldown = Abilities.Keraunos.cooldown();
    System.out.println("Reloaded cooldown: " + newCooldown);
  }

  */
}

// ===== IMPORTANT: Generated Classes =====
// The Abilities.java, CTFSettings.java, etc. classes are AUTO-GENERATED.
// They will appear in your source directory after compilation.
// DO NOT edit them manually - your changes will be overwritten!
// Instead, modify the schema and recompile.
