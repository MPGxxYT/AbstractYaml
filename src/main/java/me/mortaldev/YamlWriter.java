package me.mortaldev;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Utility class for writing YAML configuration files with comments.
 * Supports both inline comments and header comments for sections.
 */
public class YamlWriter {

    /**
     * Represents a YAML entry with its value and optional comment.
     */
    public static class YamlEntry {
        private final String path;
        private final Object value;
        private final String comment;
        private final String headerComment;

        public YamlEntry(String path, Object value, String comment, String headerComment) {
            this.path = path;
            this.value = value;
            this.comment = comment;
            this.headerComment = headerComment;
        }

        public String getPath() {
            return path;
        }

        public Object getValue() {
            return value;
        }

        public String getComment() {
            return comment;
        }

        public String getHeaderComment() {
            return headerComment;
        }
    }

    /**
     * Writes YAML entries to a file with proper formatting and comments.
     *
     * @param file the file to write to
     * @param entries the entries to write
     * @throws IOException if writing fails
     */
    public static void writeYaml(File file, List<YamlEntry> entries) throws IOException {
        writeYaml(file, entries, null);
    }

    /**
     * Writes YAML entries to a file with proper formatting, comments, and an optional file header.
     *
     * @param file the file to write to
     * @param entries the entries to write
     * @param fileHeader optional file header (multi-line string, each line will be prefixed with #)
     * @throws IOException if writing fails
     */
    public static void writeYaml(File file, List<YamlEntry> entries, String fileHeader) throws IOException {
        // Ensure parent directory exists
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // Write file header if present
            if (fileHeader != null && !fileHeader.isEmpty()) {
                String[] lines = fileHeader.split("\n");
                for (String line : lines) {
                    if (line.trim().isEmpty()) {
                        writer.write("#");
                    } else if (line.trim().startsWith("#")) {
                        writer.write(line);
                    } else {
                        writer.write("# " + line);
                    }
                    writer.newLine();
                }
                writer.newLine();
            }

            Map<String, List<YamlEntry>> sections = groupBySection(entries);
            List<String> sectionKeys = new ArrayList<>(sections.keySet());

            for (int i = 0; i < sectionKeys.size(); i++) {
                String sectionKey = sectionKeys.get(i);
                List<YamlEntry> sectionEntries = sections.get(sectionKey);

                // Write header comment if present
                if (!sectionEntries.isEmpty() && sectionEntries.get(0).getHeaderComment() != null) {
                    writer.write("# " + sectionEntries.get(0).getHeaderComment());
                    writer.newLine();
                }

                // Write section key
                if (!sectionKey.isEmpty()) {
                    writer.write(sectionKey + ":");
                    writer.newLine();
                }

                // Write entries
                for (YamlEntry entry : sectionEntries) {
                    String relativePath = getRelativePath(entry.getPath(), sectionKey);
                    if (!relativePath.isEmpty()) {
                        writeEntry(writer, relativePath, entry.getValue(), entry.getComment(), sectionKey.isEmpty() ? 0 : 1);
                    }
                }

                // Add blank line between sections (except for last section)
                if (i < sectionKeys.size() - 1) {
                    writer.newLine();
                }
            }
        }
    }

    /**
     * Groups entries by their top-level section.
     */
    private static Map<String, List<YamlEntry>> groupBySection(List<YamlEntry> entries) {
        Map<String, List<YamlEntry>> sections = new LinkedHashMap<>();

        for (YamlEntry entry : entries) {
            String section = getTopLevelSection(entry.getPath());
            sections.computeIfAbsent(section, k -> new ArrayList<>()).add(entry);
        }

        return sections;
    }

    /**
     * Gets the top-level section from a path.
     * For example, "game.length" returns "game", "simple" returns "".
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
     * For example, path="game.length", section="game" returns "length".
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
     * Writes a single entry with proper indentation and optional comment.
     */
    private static void writeEntry(BufferedWriter writer, String path, Object value, String comment, int baseIndent) throws IOException {
        String[] parts = path.split("\\.");

        // Write nested structure
        for (int i = 0; i < parts.length - 1; i++) {
            String indent = getIndent(baseIndent + i);
            writer.write(indent + parts[i] + ":");
            writer.newLine();
        }

        // Write the final key-value pair
        String indent = getIndent(baseIndent + parts.length - 1);
        String valueStr = formatValue(value);
        writer.write(indent + parts[parts.length - 1] + ": " + valueStr);

        // Add inline comment if present
        if (comment != null && !comment.isEmpty()) {
            // Calculate padding for alignment
            int currentLength = indent.length() + parts[parts.length - 1].length() + 2 + valueStr.length();
            int targetColumn = 30; // Target column for comments
            int padding = Math.max(1, targetColumn - currentLength);
            writer.write(" ".repeat(padding) + "# " + comment);
        }

        writer.newLine();
    }

    /**
     * Formats a value for YAML output.
     */
    private static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            String str = (String) value;
            // Always quote strings to avoid YAML parsing issues
            return "\"" + str.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof List) {
            return formatList((List<?>) value);
        }
        return value.toString();
    }

    /**
     * Formats a list for YAML output.
     */
    private static String formatList(List<?> list) {
        if (list.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(formatValue(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Gets the indentation string for a given level.
     */
    private static String getIndent(int level) {
        return "  ".repeat(level);
    }
}
