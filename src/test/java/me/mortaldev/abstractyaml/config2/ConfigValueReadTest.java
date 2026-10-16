package me.mortaldev.abstractyaml.config2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfigValueReadTest {

  @Test
  void int_readsWholeNumbersAndNumericStrings() {
    ConfigValue.Int v = new ConfigValue.Int("x", 1);
    assertEquals(5, v.read(5).value());
    assertEquals(5, v.read(5L).value());
    assertEquals(5, v.read(5.0).value());
    assertEquals(5, v.read("5").value());
  }

  @Test
  void int_rejectsFractionsOverflowAndText() {
    ConfigValue.Int v = new ConfigValue.Int("x", 1);
    assertThrows(IllegalArgumentException.class, () -> v.read(5.5));
    assertThrows(IllegalArgumentException.class, () -> v.read(3_000_000_000L));
    assertThrows(IllegalArgumentException.class, () -> v.read("abc"));
    assertThrows(IllegalArgumentException.class, () -> v.read(List.of(1)));
    assertThrows(IllegalArgumentException.class, () -> v.read(null));
  }

  @Test
  void double_readsNumbers() {
    ConfigValue.Double v = new ConfigValue.Double("x", 1.0);
    assertEquals(3.0, v.read(3).value());
    assertEquals(2.5, v.read("2.5").value());
    assertThrows(IllegalArgumentException.class, () -> v.read(true));
  }

  @Test
  void string_readsScalarsButNotCollections() {
    ConfigValue.String v = new ConfigValue.String("x", "d");
    assertEquals("12", v.read(12).value());
    assertEquals("true", v.read(true).value());
    assertThrows(IllegalArgumentException.class, () -> v.read(Map.of("a", 1)));
  }

  @Test
  void boolean_readsBooleansAndTrueFalseText() {
    ConfigValue.Boolean v = new ConfigValue.Boolean("x", false);
    assertTrue(v.read(true).value());
    assertTrue(v.read("TRUE").value());
    assertThrows(IllegalArgumentException.class, () -> v.read("yes please"));
    assertThrows(IllegalArgumentException.class, () -> v.read(1));
  }

  @Test
  void lists_convertEachElementAndReportTheBadOne() {
    ConfigValue.IntList v = new ConfigValue.IntList("x", List.of());
    assertEquals(List.of(1, 2, 3), v.read(List.of(1, "2", 3.0)).value());
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> v.read(List.of(1, "two")));
    assertTrue(e.getMessage().contains("element 1"), e.getMessage());
  }

  @Test
  void lists_keepAnExplicitlyEmptyList() {
    ConfigValue.StringList v = new ConfigValue.StringList("x", List.of("default"));
    assertEquals(List.of(), v.read(List.of()).value());
  }

  @Test
  void maps_readNestedDataWithStringKeys() {
    Map<Object, Object> raw = new LinkedHashMap<>();
    raw.put("health", 20);
    raw.put(7, Map.of("deep", List.of(1, 2)));

    Map<String, Object> value = new ConfigValue.MapValue("x", Map.of()).read(raw).value();
    assertEquals(20, value.get("health"));
    assertEquals(Map.of("deep", List.of(1, 2)), value.get("7"));
  }

  @Test
  void typedMaps_convertValues() {
    assertEquals(
        Map.of("a", "1"), new ConfigValue.StringMap("x", Map.of()).read(Map.of("a", 1)).value());
    assertEquals(
        Map.of("a", 2), new ConfigValue.IntMap("x", Map.of()).read(Map.of("a", "2")).value());
    assertThrows(
        IllegalArgumentException.class,
        () -> new ConfigValue.IntMap("x", Map.of()).read(Map.of("a", "two")));
  }

  @Test
  void mapList_requiresMapElements() {
    ConfigValue.MapList v = new ConfigValue.MapList("x", List.of());
    assertEquals(1, v.read(List.of(Map.of("a", 1))).value().size());
    assertThrows(IllegalArgumentException.class, () -> v.read(List.of("not a map")));
  }

  @Test
  void validate_usesTheValuesValidator() {
    ConfigValue.Int v = new ConfigValue.Int("x", 5, Validator.max(10), null);
    assertTrue(v.validate().isValid());
    assertFalse(v.withValue(11).validate().isValid());
  }

  @Test
  void nullValidator_becomesNone() {
    assertTrue(new ConfigValue.Int("x", 5, null, null).validate().isValid());
  }
}
