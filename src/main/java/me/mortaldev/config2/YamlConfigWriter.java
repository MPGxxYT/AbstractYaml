package me.mortaldev.config2;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Writes configuration to YAML files with proper formatting and comments.
 */
class YamlConfigWriter {
  private static final int COMMENT_COLUMN = 40;
  private static final Pattern PLAIN_KEY = Pattern.compile("[A-Za-z0-9_-]+");

  /**
   * Writes a config to a YAML file.
   */
  static void write(Config config, File file) {
    try {
      // Ensure parent directory exists
      if (file.getParentFile() != null && !file.getParentFile().exists()) {
        file.getParentFile().mkdirs();
      }
      Files.writeString(file.toPath(), render(config), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Failed to write config to " + file, e);
    }
  }

  /**
   * Renders a config as YAML text.
   */
  static String render(Config config) {
    StringBuilder out = new StringBuilder();

    // Write file header if present
    String header = config.schema().fileHeader();
    if (header != null && !header.isEmpty()) {
      writeHeader(out, header);
      out.append('\n');
    }

    // Group values by section
    Map<String, List<ConfigValue<?>>> sections = groupBySection(config);

    // Write each section
    boolean first = true;
    for (Map.Entry<String, List<ConfigValue<?>>> entry : sections.entrySet()) {
      if (!first) {
        out.append('\n'); // Blank line between sections
      }
      first = false;

      String sectionKey = entry.getKey();
      List<ConfigValue<?>> values = entry.getValue();

      // Write section header if it's a nested section
      if (!sectionKey.isEmpty()) {
        out.append(key(sectionKey)).append(":\n");
      }

      // Track written section headers to avoid duplicates
      Set<String> writtenSections = new HashSet<>();

      // Write values
      for (ConfigValue<?> value : values) {
        writeValue(out, value, sectionKey, writtenSections);
      }
    }
    return out.toString();
  }

  /**
   * Writes the file header comment.
   */
  private static void writeHeader(StringBuilder out, String header) {
    for (String line : header.split("\n")) {
      if (line.trim().isEmpty()) {
        out.append("#");
      } else if (line.trim().startsWith("#")) {
        out.append(line);
      } else {
        out.append("# ").append(line);
      }
      out.append('\n');
    }
  }

  /**
   * Groups config values by their top-level section.
   */
  private static Map<String, List<ConfigValue<?>>> groupBySection(Config config) {
    Map<String, List<ConfigValue<?>>> sections = new LinkedHashMap<>();

    for (ConfigValue<?> value : config.allValues().values()) {
      String section = getTopLevelSection(value.path());
      sections.computeIfAbsent(section, k -> new ArrayList<>()).add(value);
    }

    // Sort values within each section by their full path to ensure proper nesting
    for (List<ConfigValue<?>> values : sections.values()) {
      values.sort(Comparator.comparing(ConfigValue::path));
    }

    return sections;
  }

  /**
   * Gets the top-level section from a path.
   * For example, "keraunos.cooldown" returns "keraunos", "simple" returns "".
   */
  private static String getTopLevelSection(String path) {
    int dotIndex = path.indexOf('.');
    if (dotIndex == -1) {
      return "";
    }
    return path.substring(0, dotIndex);
  }

  /**
   * Gets the relative path after removing the section prefix.
   */
  private static String getRelativePath(String path, String section) {
    if (section.isEmpty()) {
      return path;
    }
    if (path.startsWith(section + ".")) {
      return path.substring(section.length() + 1);
    }
    return path;
  }

  /**
   * Writes a single config value.
   */
  private static void writeValue(
      StringBuilder out, ConfigValue<?> value, String section, Set<String> writtenSections) {
    String relativePath = getRelativePath(value.path(), section);
    String[] parts = relativePath.split("\\.");
    int baseIndent = section.isEmpty() ? 0 : 1;

    // Write nested structure, but only if not already written
    StringBuilder sectionPath = new StringBuilder();
    for (int i = 0; i < parts.length - 1; i++) {
      if (i > 0) {
        sectionPath.append(".");
      }
      sectionPath.append(parts[i]);
      String currentSection = sectionPath.toString();

      // Only write the section header if we haven't written it yet
      if (writtenSections.add(currentSection)) {
        out.append(getIndent(baseIndent + i)).append(key(parts[i])).append(":\n");
      }
    }

    String key = parts[parts.length - 1];
    try {
      writeEntry(out, baseIndent + parts.length - 1, key, value.value(), value.comment());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Cannot write " + value.path() + ": " + e.getMessage(), e);
    }
  }

  /**
   * Writes {@code key: value}, using block style for non-empty maps and for lists of strings or
   * maps, and inline style for everything else.
   */
  private static void writeEntry(
      StringBuilder out, int level, String key, Object value, String comment) {
    String indent = getIndent(level);
    if (value instanceof Map<?, ?> map && !map.isEmpty()) {
      out.append(indent).append(key(key)).append(':');
      appendComment(out, comment, 2);
      out.append('\n');
      for (Map.Entry<?, ?> e : map.entrySet()) {
        writeEntry(out, level + 1, String.valueOf(e.getKey()), e.getValue(), null);
      }
    } else if (value instanceof List<?> list && useBlockList(list)) {
      out.append(indent).append(key(key)).append(':');
      appendComment(out, comment, 2);
      out.append('\n');
      for (Object item : list) {
        writeListItem(out, level + 1, item);
      }
    } else {
      String line = indent + key(key) + ": " + formatValue(value);
      out.append(line);
      appendComment(out, comment, Math.max(2, COMMENT_COLUMN - line.length()));
      out.append('\n');
    }
  }

  /**
   * Writes one block-list item. A map item puts its first entry on the dash line.
   */
  private static void writeListItem(StringBuilder out, int level, Object item) {
    if (item instanceof Map<?, ?> map && !map.isEmpty()) {
      StringBuilder entries = new StringBuilder();
      for (Map.Entry<?, ?> e : map.entrySet()) {
        writeEntry(entries, level + 1, String.valueOf(e.getKey()), e.getValue(), null);
      }
      // Replace the first entry's indent with "- " at this level
      out.append(getIndent(level)).append("- ").append(entries, getIndent(level + 1).length(),
          entries.length());
    } else {
      out.append(getIndent(level)).append("- ").append(formatValue(item)).append('\n');
    }
  }

  private static void appendComment(StringBuilder out, String comment, int padding) {
    if (comment != null && !comment.isEmpty()) {
      out.append(" ".repeat(padding)).append("# ").append(comment);
    }
  }

  /**
   * Lists of strings or maps use one item per line; number and boolean lists stay inline.
   */
  private static boolean useBlockList(List<?> list) {
    for (Object item : list) {
      if (item instanceof String || item instanceof Map || item instanceof List) {
        return true;
      }
    }
    return false;
  }

  /**
   * Formats a value in YAML flow style.
   */
  private static String formatValue(Object value) {
    if (value == null) {
      return "null";
    }
    if (value instanceof String str) {
      return quote(str);
    }
    if (value instanceof Boolean || value instanceof Number) {
      return value.toString();
    }
    if (value instanceof List<?> list) {
      StringJoiner joiner = new StringJoiner(", ", "[", "]");
      for (Object item : list) {
        joiner.add(formatValue(item));
      }
      return joiner.toString();
    }
    if (value instanceof Map<?, ?> map) {
      StringJoiner joiner = new StringJoiner(", ", "{", "}");
      for (Map.Entry<?, ?> e : map.entrySet()) {
        joiner.add(key(String.valueOf(e.getKey())) + ": " + formatValue(e.getValue()));
      }
      return joiner.toString();
    }
    throw new IllegalArgumentException(
        "unsupported YAML type " + value.getClass().getName());
  }

  /** Quotes strings to avoid YAML parsing issues. */
  private static String quote(String str) {
    return "\"" + str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
  }

  private static String key(String key) {
    return PLAIN_KEY.matcher(key).matches() ? key : quote(key);
  }

  /**
   * Gets the indentation string for a given level.
   */
  private static String getIndent(int level) {
    return "  ".repeat(level);
  }
}
