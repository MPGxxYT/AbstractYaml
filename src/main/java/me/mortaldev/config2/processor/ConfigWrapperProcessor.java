package me.mortaldev.config2.processor;

import me.mortaldev.config2.ConfigSchema;
import me.mortaldev.config2.GenerateWrapper;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Annotation processor that generates type-safe wrapper classes for ConfigSchema.
 *
 * <p>Runs at compile-time and generates static wrapper classes based on
 * ConfigSchema definitions annotated with @GenerateWrapper.
 */
@SupportedAnnotationTypes("me.mortaldev.config2.GenerateWrapper")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class ConfigWrapperProcessor extends AbstractProcessor {

  private Messager messager;
  private Filer filer;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.messager = processingEnv.getMessager();
    this.filer = processingEnv.getFiler();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getElementsAnnotatedWith(GenerateWrapper.class)) {
      if (!(element instanceof TypeElement)) {
        continue;
      }

      TypeElement typeElement = (TypeElement) element;
      GenerateWrapper annotation = element.getAnnotation(GenerateWrapper.class);

      try {
        // Instantiate the schema to read its structure
        SchemaInfo schemaInfo = analyzeSchema(typeElement, annotation);
        generateWrapperClass(schemaInfo);

        messager.printMessage(
            Diagnostic.Kind.NOTE,
            "Generated wrapper class: " + schemaInfo.wrapperClassName);

      } catch (Exception e) {
        messager.printMessage(
            Diagnostic.Kind.ERROR,
            "Failed to generate wrapper for " + typeElement.getSimpleName() + ": " + e.getMessage(),
            element);
        e.printStackTrace();
      }
    }

    return true;
  }

  /**
   * Analyzes a schema class by instantiating it and reading its structure.
   */
  private SchemaInfo analyzeSchema(TypeElement typeElement, GenerateWrapper annotation)
      throws Exception {
    String schemaClassName = typeElement.getQualifiedName().toString();
    String packageName = annotation.packageName().isEmpty()
        ? getPackageName(schemaClassName)
        : annotation.packageName();

    String wrapperClassName = annotation.value().isEmpty()
        ? deriveWrapperName(typeElement.getSimpleName().toString())
        : annotation.value();

    // Load and instantiate the schema class
    Class<?> schemaClass = Class.forName(schemaClassName);
    ConfigSchema schema = (ConfigSchema) schemaClass.getDeclaredConstructor().newInstance();

    // Extract sections and values from the schema
    Map<String, SectionInfo> sections = extractSections(schema);

    return new SchemaInfo(
        schemaClassName,
        packageName,
        wrapperClassName,
        schema.name(),
        sections,
        annotation.generateReload(),
        annotation.makeFinal());
  }

  /**
   * Extracts section information from a ConfigSchema instance.
   */
  private Map<String, SectionInfo> extractSections(ConfigSchema schema) {
    Map<String, SectionInfo> sections = new LinkedHashMap<>();

    // Get all values from the schema
    for (var configValue : schema.values()) {
      String path = configValue.path();
      String[] parts = path.split("\\.", 2);

      if (parts.length == 2) {
        // It's a nested value (section.key)
        String sectionName = parts[0];
        String key = parts[1];

        SectionInfo section = sections.computeIfAbsent(
            sectionName,
            name -> new SectionInfo(name, new LinkedHashMap<>()));

        section.values.put(key, new ValueInfo(
            key,
            getValueType(configValue),
            configValue.comment()));
      } else {
        // Top-level value (no section)
        SectionInfo topLevel = sections.computeIfAbsent(
            "",
            name -> new SectionInfo("", new LinkedHashMap<>()));

        topLevel.values.put(path, new ValueInfo(
            path,
            getValueType(configValue),
            configValue.comment()));
      }
    }

    return sections;
  }

  /**
   * Determines the Java type of a ConfigValue.
   */
  private String getValueType(me.mortaldev.config2.ConfigValue<?> configValue) {
    if (configValue instanceof me.mortaldev.config2.ConfigValue.Int) {
      return "int";
    } else if (configValue instanceof me.mortaldev.config2.ConfigValue.Double) {
      return "double";
    } else if (configValue instanceof me.mortaldev.config2.ConfigValue.String) {
      return "String";
    } else if (configValue instanceof me.mortaldev.config2.ConfigValue.Boolean) {
      return "boolean";
    } else if (configValue instanceof me.mortaldev.config2.ConfigValue.StringList) {
      return "List<String>";
    } else if (configValue instanceof me.mortaldev.config2.ConfigValue.IntList) {
      return "List<Integer>";
    } else if (configValue instanceof me.mortaldev.config2.ConfigValue.DoubleList) {
      return "List<Double>";
    }
    return "Object";
  }

  /**
   * Generates the wrapper class source code.
   */
  private void generateWrapperClass(SchemaInfo info) throws IOException {
    String fullClassName = info.packageName + "." + info.wrapperClassName;

    JavaFileObject file = filer.createSourceFile(fullClassName);
    try (PrintWriter writer = new PrintWriter(file.openWriter())) {
      writeWrapperClass(writer, info);
    }
  }

  /**
   * Writes the wrapper class code.
   */
  private void writeWrapperClass(PrintWriter w, SchemaInfo info) {
    // Package and imports
    w.println("package " + info.packageName + ";");
    w.println();
    w.println("import me.mortaldev.config2.Config;");
    w.println("import me.mortaldev.config2.ConfigSection;");
    w.println("import me.mortaldev.config2.ConfigManager;");
    w.println("import java.util.List;");
    w.println();

    // Class header with documentation
    w.println("/**");
    w.println(" * Type-safe wrapper for " + info.configName + " config.");
    w.println(" * AUTO-GENERATED - DO NOT EDIT!");
    w.println(" *");
    w.println(" * Generated from: " + info.schemaClassName);
    w.println(" * Generated at: " + new Date());
    w.println(" */");

    String finalMod = info.makeFinal ? "final " : "";
    w.println("public " + finalMod + "class " + info.wrapperClassName + " {");
    w.println("    private static final String CONFIG_NAME = \"" + info.configName + "\";");
    w.println();

    // Generate nested classes for sections
    for (SectionInfo section : info.sections.values()) {
      if (section.name.isEmpty()) {
        // Top-level values
        writeTopLevelValues(w, section);
      } else {
        // Section class
        writeSectionClass(w, section);
      }
    }

    // Reload method
    if (info.generateReload) {
      writeReloadMethod(w, info);
    }

    // Close class
    w.println("}");
  }

  /**
   * Writes top-level values (not in a section).
   */
  private void writeTopLevelValues(PrintWriter w, SectionInfo section) {
    for (ValueInfo value : section.values.values()) {
      if (value.comment != null && !value.comment.isEmpty()) {
        w.println("    /** " + value.comment + " */");
      }
      String methodName = toCamelCase(value.key);
      w.println("    public static " + value.type + " " + methodName + "() {");
      w.println("        return ConfigManager.getInstance().get(CONFIG_NAME)." + getAccessor(value) + ";");
      w.println("    }");
      w.println();
    }
  }

  /**
   * Writes a section class.
   */
  private void writeSectionClass(PrintWriter w, SectionInfo section) {
    String className = toPascalCase(section.name);

    w.println("    public static class " + className + " {");
    w.println("        private static ConfigSection section;");
    w.println();

    // Methods for each value
    for (ValueInfo value : section.values.values()) {
      if (value.comment != null && !value.comment.isEmpty()) {
        w.println("        /** " + value.comment + " */");
      }
      String methodName = toCamelCase(value.key);
      w.println("        public static " + value.type + " " + methodName + "() {");
      w.println("            return section()." + getAccessor(value) + ";");
      w.println("        }");
      w.println();
    }

    // Section getter
    w.println("        private static ConfigSection section() {");
    w.println("            if (section == null) {");
    w.println("                section = ConfigManager.getInstance().getSection(CONFIG_NAME, \"" + section.name + "\");");
    w.println("            }");
    w.println("            return section;");
    w.println("        }");
    w.println();

    // Reload
    w.println("        static void reload() { section = null; }");
    w.println("    }");
    w.println();
  }

  /**
   * Writes the reload method.
   */
  private void writeReloadMethod(PrintWriter w, SchemaInfo info) {
    w.println("    /**");
    w.println("     * Reloads all cached config sections.");
    w.println("     * Call this after reloading the config.");
    w.println("     */");
    w.println("    public static void reload() {");
    for (SectionInfo section : info.sections.values()) {
      if (!section.name.isEmpty()) {
        w.println("        " + toPascalCase(section.name) + ".reload();");
      }
    }
    w.println("    }");
  }

  /**
   * Gets the ConfigSection accessor for a value type.
   */
  private String getAccessor(ValueInfo value) {
    return switch (value.type) {
      case "int" -> "getInt(\"" + value.key + "\")";
      case "double" -> "getDouble(\"" + value.key + "\")";
      case "String" -> "getString(\"" + value.key + "\")";
      case "boolean" -> "getBoolean(\"" + value.key + "\")";
      case "List<String>" -> "getStringList(\"" + value.key + "\")";
      case "List<Integer>" -> "getIntList(\"" + value.key + "\")";
      case "List<Double>" -> "getDoubleList(\"" + value.key + "\")";
      default -> "get(\"" + value.key + "\")";
    };
  }

  // Utility methods
  private String getPackageName(String fullClassName) {
    int lastDot = fullClassName.lastIndexOf('.');
    return lastDot > 0 ? fullClassName.substring(0, lastDot) : "";
  }

  private String deriveWrapperName(String schemaName) {
    return schemaName
        .replace("ConfigSchema", "")
        .replace("Schema", "");
  }

  private String toCamelCase(String kebabCase) {
    StringBuilder result = new StringBuilder();
    boolean capitalizeNext = false;

    for (char c : kebabCase.toCharArray()) {
      if (c == '-' || c == '_') {
        capitalizeNext = true;
      } else if (capitalizeNext) {
        result.append(Character.toUpperCase(c));
        capitalizeNext = false;
      } else {
        result.append(c);
      }
    }

    return result.toString();
  }

  private String toPascalCase(String kebabCase) {
    String camel = toCamelCase(kebabCase);
    return camel.isEmpty() ? camel : Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
  }

  // Data classes
  private record SchemaInfo(
      String schemaClassName,
      String packageName,
      String wrapperClassName,
      String configName,
      Map<String, SectionInfo> sections,
      boolean generateReload,
      boolean makeFinal) {}

  private record SectionInfo(String name, Map<String, ValueInfo> values) {}

  private record ValueInfo(String key, String type, String comment) {}
}
