package me.mortaldev.config2;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A schema with one value of every ConfigValue type. */
class AllTypesSchema extends ConfigSchema {

  AllTypesSchema() {
    super("all-types");
    header("All types test file");

    section("scalars")
        .intValue("count", 5, Validator.range(0, 100), "An int")
        .doubleValue("radius", 2.5, "A double")
        .stringValue("name", "default", "A string")
        .boolValue("enabled", true, "A boolean");

    section("lists")
        .stringListValue("names", List.of("a", "b"), "Strings")
        .intListValue("numbers", List.of(1, 2, 3), Validator.notNull(), "Ints")
        .doubleListValue("weights", List.of(0.5, 1.5), Validator.notNull(), "Doubles");

    Map<String, Object> point = new LinkedHashMap<>();
    point.put("name", "spawn");
    point.put("radius", 3.0);
    point.put("tags", List.of("safe", "start"));

    Map<String, Object> stats = new LinkedHashMap<>();
    stats.put("health", 20);
    stats.put("nested", Map.of("mana", 100));

    Map<String, String> colors = new LinkedHashMap<>();
    colors.put("red", "#FF0000");
    colors.put("blue", "#0000FF");

    Map<String, Integer> levels = new LinkedHashMap<>();
    levels.put("warrior", 10);
    levels.put("mage", 8);

    section("maps")
        .mapListValue("points", List.of(point), "Map list")
        .mapValue("stats", stats, "Map")
        .stringMapValue("colors", colors, "String map")
        .intMapValue("levels", levels, "Int map");
  }
}
