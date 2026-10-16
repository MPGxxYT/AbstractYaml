package me.mortaldev.abstractyaml.config2.processor;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import me.mortaldev.abstractyaml.config2.ConfigManager;
import me.mortaldev.abstractyaml.config2.ConfigRegistry;
import me.mortaldev.abstractyaml.config2.ConfigSchema;
import me.mortaldev.abstractyaml.config2.ConfigSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WrapperProcessorTest {

  @TempDir Path dir;

  /** Compiles sources with the processor and keeps the generated files and diagnostics. */
  private record Result(boolean success, List<Diagnostic<?>> diagnostics, Path generated, Path classes) {
    String source(String qualifiedName) {
      Path file = generated.resolve(qualifiedName.replace('.', '/') + ".java");
      try {
        return Files.exists(file) ? Files.readString(file) : null;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    String messages(Diagnostic.Kind kind) {
      return diagnostics.stream()
          .filter(d -> d.getKind() == kind)
          .map(d -> "line " + d.getLineNumber() + ": " + d.getMessage(null))
          .collect(Collectors.joining("\n"));
    }

    ClassLoader loader() throws IOException {
      return new URLClassLoader(
          new URL[] {classes.toUri().toURL()}, WrapperProcessorTest.class.getClassLoader());
    }
  }

  private Result compile(Map<String, String> sources) throws IOException {
    Path src = Files.createDirectories(dir.resolve("src"));
    Path generated = Files.createDirectories(dir.resolve("generated"));
    Path classes = Files.createDirectories(dir.resolve("classes"));
    List<Path> files = new ArrayList<>();
    for (Map.Entry<String, String> entry : sources.entrySet()) {
      Path file = src.resolve(entry.getKey().replace('.', '/') + ".java");
      Files.createDirectories(file.getParent());
      Files.writeString(file, entry.getValue());
      files.add(file);
    }

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
      List<String> options = List.of(
          "-classpath", System.getProperty("java.class.path"),
          "-processor", WrapperProcessor.class.getName(),
          "-s", generated.toString(),
          "-d", classes.toString());
      boolean success = compiler
          .getTask(null, fileManager, diagnostics, options, null,
              fileManager.getJavaFileObjectsFromPaths(files))
          .call();
      return new Result(success, new ArrayList<>(diagnostics.getDiagnostics()), generated, classes);
    }
  }

  private Result compile(String qualifiedName, String source) throws IOException {
    return compile(Map.of(qualifiedName, source));
  }

  @AfterEach
  void resetShared() {
    ConfigManager.reset();
    ConfigRegistry.clear();
  }

  private static final String ABILITIES = """
      package com.example.config;

      import java.util.List;
      import java.util.Map;
      import me.mortaldev.abstractyaml.config2.*;

      @RegisterConfig(priority = 10)
      @GenerateWrapper
      public class AbilitiesConfigSchema extends ConfigSchema {
        static final String RADIUS = "radius";

        public AbilitiesConfigSchema() {
          super("abilities");
          header("Abilities");
          value(new ConfigValue.Int("version", 1, "File version"));

          section("fireball")
              .header("Fireball")
              .boolValue("enabled", true, "Turn it on or off")
              .intValue("cooldown", 10, Validator.range(1, 300), "Seconds between casts")
              .doubleValue(RADIUS, 3.0, null)
              .stringListValue("allowed-worlds", List.of("world"), "Worlds")
              .section("damage")
              .intMapValue("by-mob", Map.of("zombie", 2), "Damage per mob")
              .parent()
              .stringValue("sound", "BOOM", "Played on cast");

          ConfigSchema.SectionBuilder rewards = section("rewards");
          rewards.mapListValue("tiers", List.of(Map.of("kills", 5)), "Reward tiers");
          rewards.stringMapValue("titles", Map.of("1", "Gold"), null);
        }
      }
      """;

  @Test
  void generatesAccessorsForEveryBuilderForm() throws IOException {
    Result result = compile("com.example.config.AbilitiesConfigSchema", ABILITIES);
    assertTrue(result.success(), result.messages(Diagnostic.Kind.ERROR));

    String source = result.source("com.example.config.wrappers.AbilitiesWrapper");
    assertNotNull(source, "wrapper not generated");
    assertTrue(source.contains("public final class AbilitiesWrapper"), source);
    assertTrue(source.contains("public static int version()"), source);
    assertTrue(source.contains("public static final class Fireball"), source);
    assertTrue(source.contains("/** Seconds between casts */"), source);
    assertTrue(source.contains("public static boolean enabled()"), source);
    assertTrue(source.contains("public static double radius()"), source);
    assertTrue(
        source.contains("public static java.util.List<java.lang.String> allowedWorlds()"), source);
    assertTrue(source.contains("public static final class Damage"), source);
    assertTrue(source.contains("\"fireball.damage\""), source);
    assertTrue(
        source.contains("public static java.util.Map<java.lang.String, java.lang.Integer> byMob()"),
        source);
    assertTrue(source.contains("public static java.lang.String sound()"), source);
    assertTrue(source.contains("public static final class Rewards"), source);
    assertTrue(source.contains(" tiers()"), source);
    assertTrue(source.contains(" titles()"), source);
    assertTrue(source.contains("public static void reload() {}"), source);
  }

  @Test
  void generatedWrapperReadsTheCurrentlyLoadedConfig() throws Exception {
    Result result = compile("com.example.config.AbilitiesConfigSchema", ABILITIES);
    assertTrue(result.success(), result.messages(Diagnostic.Kind.ERROR));

    ClassLoader loader = result.loader();
    ConfigSchema schema = (ConfigSchema) loader
        .loadClass("com.example.config.AbilitiesConfigSchema").getConstructor().newInstance();
    Map<String, ConfigSource> files = new HashMap<>();
    ConfigManager manager = ConfigManager.create(
        Logger.getLogger("test"), p -> files.computeIfAbsent(p, k -> ConfigSource.inMemory(null)));
    manager.register(schema);
    ConfigManager.init(manager);
    assertTrue(manager.loadAll().isEmpty());

    Class<?> fireball =
        loader.loadClass("com.example.config.wrappers.AbilitiesWrapper$Fireball");
    Method cooldown = fireball.getMethod("cooldown");
    assertEquals(10, cooldown.invoke(null));
    Class<?> damage =
        loader.loadClass("com.example.config.wrappers.AbilitiesWrapper$Fireball$Damage");
    assertEquals(Map.of("zombie", 2), damage.getMethod("byMob").invoke(null));
    assertEquals(1, loader.loadClass("com.example.config.wrappers.AbilitiesWrapper")
        .getMethod("version").invoke(null));

    files.get("abilities.yml").write("fireball:\n  cooldown: 42\n");
    assertTrue(manager.reloadAll().isEmpty());
    assertEquals(42, cooldown.invoke(null));
  }

  @Test
  void registeredNameOverridesTheSchemaName() throws IOException {
    Result result = compile("a.ShopSchema", """
        package a;
        import me.mortaldev.abstractyaml.config2.*;
        @RegisterConfig(value = "market")
        @GenerateWrapper
        public class ShopSchema extends ConfigSchema {
          public ShopSchema() {
            super("shop");
            section("s").intValue("n", 1, null);
          }
        }
        """);
    assertTrue(result.success(), result.messages(Diagnostic.Kind.ERROR));
    assertTrue(result.source("a.wrappers.ShopWrapper").contains("CONFIG_NAME = \"market\""));
  }

  @Test
  void optionsControlNamePackageFinalAndReload() throws Exception {
    Result result = compile("a.ShopSchema", """
        package a;
        import me.mortaldev.abstractyaml.config2.*;
        @GenerateWrapper(value = "Shop", packageName = "a.gen", makeFinal = false,
            generateReload = false)
        public class ShopSchema extends ConfigSchema {
          public ShopSchema() {
            super("shop");
            section("s").intValue("n", 1, null);
          }
        }
        """);
    assertTrue(result.success(), result.messages(Diagnostic.Kind.ERROR));
    String source = result.source("a.gen.Shop");
    assertNotNull(source);
    assertFalse(source.contains("reload"), source);
    Class<?> wrapper = result.loader().loadClass("a.gen.Shop");
    assertFalse(Modifier.isFinal(wrapper.getModifiers()));
  }

  @Test
  void awkwardNamesStillCompile() throws IOException {
    Result result = compile("a.OddSchema", """
        package a;
        import me.mortaldev.abstractyaml.config2.*;
        @GenerateWrapper
        public class OddSchema extends ConfigSchema {
          public OddSchema() {
            super("odd");
            section("map").intValue("class", 1, null).intValue("2x", 2, null);
            section("string").stringValue("default", "d", "A */ tricky @comment");
            section("rewards").section("rewards").intValue("reload", 3, null);
            section("odd").boolValue("x", true, null);
          }
        }
        """);
    assertTrue(result.success(), result.messages(Diagnostic.Kind.ERROR));
    String source = result.source("a.wrappers.OddWrapper");
    assertTrue(source.contains(" class_()"), source);
    assertTrue(source.contains(" _2x()"), source);
    assertTrue(source.contains(" default_()"), source);
    assertTrue(source.contains("class RewardsSection"), source);
  }

  @Test
  void existingClassWithTheWrapperNameIsLeftAlone() throws IOException {
    Map<String, String> sources = new HashMap<>();
    sources.put("a.ShopSchema", """
        package a;
        import me.mortaldev.abstractyaml.config2.*;
        @GenerateWrapper
        public class ShopSchema extends ConfigSchema {
          public ShopSchema() {
            super("shop");
            section("s").intValue("n", 1, null);
          }
        }
        """);
    sources.put("a.wrappers.ShopWrapper", """
        package a.wrappers;
        public final class ShopWrapper {
          public static String custom() { return "hand-written"; }
        }
        """);
    Result result = compile(sources);
    assertTrue(result.success(), result.messages(Diagnostic.Kind.ERROR));
    assertNull(result.source("a.wrappers.ShopWrapper"));
    assertTrue(result.messages(Diagnostic.Kind.NOTE).contains("already exists"),
        result.messages(Diagnostic.Kind.NOTE));
  }

  @Test
  void nonConstantKeyIsReportedAtItsLine() throws IOException {
    Result result = compile("a.BadSchema", """
        package a;
        import me.mortaldev.abstractyaml.config2.*;
        @GenerateWrapper
        public class BadSchema extends ConfigSchema {
          public BadSchema() {
            super("bad");
            String key = "n" + System.nanoTime();
            section("s").intValue(key, 1, null);
          }
        }
        """);
    assertFalse(result.success());
    String errors = result.messages(Diagnostic.Kind.ERROR);
    assertTrue(errors.contains("line 8") && errors.contains("value key"), errors);
  }

  @Test
  void keysThatClashAreReported() throws IOException {
    Result result = compile("a.ClashSchema", """
        package a;
        import me.mortaldev.abstractyaml.config2.*;
        @GenerateWrapper
        public class ClashSchema extends ConfigSchema {
          public ClashSchema() {
            super("clash");
            section("s").intValue("max-hp", 1, null).intValue("max_hp", 2, null);
          }
        }
        """);
    assertFalse(result.success());
    assertTrue(result.messages(Diagnostic.Kind.ERROR).contains("maxHp"),
        result.messages(Diagnostic.Kind.ERROR));
  }

  @Test
  void annotationOnANonSchemaIsAnError() throws IOException {
    Result result = compile("a.NotASchema", """
        package a;
        @me.mortaldev.abstractyaml.config2.GenerateWrapper
        public class NotASchema {}
        """);
    assertFalse(result.success());
    assertTrue(result.messages(Diagnostic.Kind.ERROR).contains("extends ConfigSchema"));
  }
}
