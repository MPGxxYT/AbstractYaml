package me.mortaldev.config2.processor;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

/**
 * Reads a schema's structure from its constructor's syntax tree, without running it.
 *
 * <p>Understands the builder calls used in schemas: {@code super("name")}, {@code section(...)},
 * nested {@code .section(...)}, {@code .parent()}, {@code .header(...)}, the value methods such as
 * {@code .intValue(...)}, {@code value(new ConfigValue.Int(...))}, and section builders stored in
 * local variables. Keys must be string literals or string constants.
 */
final class SchemaReader {

  /** A value method on {@code SectionBuilder}, or a {@code ConfigValue} record, and its getter. */
  enum ValueType {
    INT("int", "getInt"),
    DOUBLE("double", "getDouble"),
    STRING("java.lang.String", "getString"),
    BOOLEAN("boolean", "getBoolean"),
    STRING_LIST("java.util.List<java.lang.String>", "getStringList"),
    INT_LIST("java.util.List<java.lang.Integer>", "getIntList"),
    DOUBLE_LIST("java.util.List<java.lang.Double>", "getDoubleList"),
    MAP_LIST("java.util.List<java.util.Map<java.lang.String, java.lang.Object>>", "getMapList"),
    MAP("java.util.Map<java.lang.String, java.lang.Object>", "getMap"),
    STRING_MAP("java.util.Map<java.lang.String, java.lang.String>", "getStringMap"),
    INT_MAP("java.util.Map<java.lang.String, java.lang.Integer>", "getIntMap");

    final String javaType;
    final String getter;

    ValueType(String javaType, String getter) {
      this.javaType = javaType;
      this.getter = getter;
    }
  }

  private static final Map<String, ValueType> BUILDER_METHODS = new HashMap<>();
  private static final Map<String, ValueType> RECORDS = new HashMap<>();

  static {
    BUILDER_METHODS.put("intValue", ValueType.INT);
    BUILDER_METHODS.put("doubleValue", ValueType.DOUBLE);
    BUILDER_METHODS.put("stringValue", ValueType.STRING);
    BUILDER_METHODS.put("boolValue", ValueType.BOOLEAN);
    BUILDER_METHODS.put("stringList", ValueType.STRING_LIST);
    BUILDER_METHODS.put("stringListValue", ValueType.STRING_LIST);
    BUILDER_METHODS.put("intList", ValueType.INT_LIST);
    BUILDER_METHODS.put("intListValue", ValueType.INT_LIST);
    BUILDER_METHODS.put("doubleList", ValueType.DOUBLE_LIST);
    BUILDER_METHODS.put("doubleListValue", ValueType.DOUBLE_LIST);
    BUILDER_METHODS.put("mapListValue", ValueType.MAP_LIST);
    BUILDER_METHODS.put("mapValue", ValueType.MAP);
    BUILDER_METHODS.put("stringMapValue", ValueType.STRING_MAP);
    BUILDER_METHODS.put("intMapValue", ValueType.INT_MAP);

    RECORDS.put("Int", ValueType.INT);
    RECORDS.put("Double", ValueType.DOUBLE);
    RECORDS.put("String", ValueType.STRING);
    RECORDS.put("Boolean", ValueType.BOOLEAN);
    RECORDS.put("StringList", ValueType.STRING_LIST);
    RECORDS.put("IntList", ValueType.INT_LIST);
    RECORDS.put("DoubleList", ValueType.DOUBLE_LIST);
    RECORDS.put("MapList", ValueType.MAP_LIST);
    RECORDS.put("MapValue", ValueType.MAP);
    RECORDS.put("StringMap", ValueType.STRING_MAP);
    RECORDS.put("IntMap", ValueType.INT_MAP);
  }

  /** A config section: its values and child sections, in declaration order. */
  static final class Section {
    final String key;
    final String path;
    final Map<String, Section> children = new LinkedHashMap<>();
    final List<Value> values = new ArrayList<>();

    Section(String key, String path) {
      this.key = key;
      this.path = path;
    }

    Section child(String childKey) {
      return children.computeIfAbsent(
          childKey, k -> new Section(k, path.isEmpty() ? k : path + "." + k));
    }
  }

  /** A value: its key relative to its section, type, comment and where it was declared. */
  record Value(String key, ValueType type, String comment, Tree tree) {}

  /** The result of reading a schema. */
  static final class Schema {
    final Section root = new Section("", "");
    String configName;
  }

  /** Marks a receiver that is the schema itself ({@code section(...)}, {@code this.value(...)}). */
  private static final Object SCHEMA = new Object();

  private final Trees trees;
  private final CompilationUnitTree unit;
  private final Schema schema = new Schema();
  private final Map<String, Section> locals = new HashMap<>();
  private boolean failed;

  private SchemaReader(Trees trees, CompilationUnitTree unit) {
    this.trees = trees;
    this.unit = unit;
  }

  /**
   * Reads the schema declared by {@code classTree}. Problems are reported to the compiler at the
   * offending line; returns {@code null} if any was an error.
   */
  static Schema read(Trees trees, CompilationUnitTree unit, ClassTree classTree) {
    SchemaReader reader = new SchemaReader(trees, unit);
    MethodTree constructor = null;
    for (Tree member : classTree.getMembers()) {
      if (member instanceof MethodTree method && method.getName().contentEquals("<init>")) {
        if (constructor == null || method.getParameters().isEmpty()) {
          constructor = method;
        }
      }
    }
    if (constructor == null || constructor.getBody() == null) {
      reader.error(classTree, "@GenerateWrapper needs a schema constructor that declares its values");
      return null;
    }
    reader.scan(constructor.getBody());
    return reader.failed ? null : reader.schema;
  }

  private void scan(BlockTree body) {
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitExpressionStatement(ExpressionStatementTree node, Void unused) {
        eval(node.getExpression());
        return null;
      }

      @Override
      public Void visitVariable(VariableTree node, Void unused) {
        if (node.getInitializer() != null && eval(node.getInitializer()) instanceof Section s) {
          locals.put(node.getName().toString(), s);
        }
        return null;
      }

      @Override
      public Void visitClass(ClassTree node, Void unused) {
        return null;
      }

      @Override
      public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
        return null;
      }
    }.scan(body, null);
  }

  /**
   * Evaluates a builder expression. Returns the section it leaves the builder in, {@link #SCHEMA}
   * for {@code this}, or {@code null} for anything else.
   */
  private Object eval(ExpressionTree expression) {
    if (expression instanceof ParenthesizedTree parenthesized) {
      return eval(parenthesized.getExpression());
    }
    if (expression instanceof IdentifierTree identifier) {
      String name = identifier.getName().toString();
      return name.equals("this") ? SCHEMA : locals.get(name);
    }
    if (!(expression instanceof MethodInvocationTree call)) {
      return null;
    }

    String method;
    Object receiver;
    if (call.getMethodSelect() instanceof IdentifierTree identifier) {
      method = identifier.getName().toString();
      receiver = SCHEMA;
    } else if (call.getMethodSelect() instanceof MemberSelectTree select) {
      method = select.getIdentifier().toString();
      receiver = eval(select.getExpression());
    } else {
      return null;
    }
    List<? extends ExpressionTree> args = call.getArguments();

    if (receiver == SCHEMA) {
      switch (method) {
        case "super" -> schema.configName = string(args, 0, call, "config name");
        case "section" -> {
          String key = string(args, 0, call, "section name");
          return key == null ? null : schema.root.child(key);
        }
        case "value" -> readRecord(args, call);
        default -> {}
      }
      return null;
    }

    if (receiver instanceof Section section) {
      if (method.equals("section")) {
        String key = string(args, 0, call, "section name");
        return key == null ? null : section.child(key);
      }
      if (method.equals("parent")) {
        Section parent = parentOf(section);
        if (parent == null) {
          error(call, "parent() called on a top-level section");
        }
        return parent;
      }
      if (method.equals("header")) {
        return section;
      }
      ValueType type = BUILDER_METHODS.get(method);
      if (type != null) {
        String key = string(args, 0, call, "value key");
        if (key != null) {
          add(section, key, type, comment(args), call);
        }
        return section;
      }
    }
    return null;
  }

  /** {@code value(new ConfigValue.Int("path", ...))} at the schema level. */
  private void readRecord(List<? extends ExpressionTree> args, Tree call) {
    if (args.size() != 1 || !(args.get(0) instanceof NewClassTree created)) {
      return;
    }
    String recordName =
        created.getIdentifier() instanceof MemberSelectTree select
            ? select.getIdentifier().toString()
            : created.getIdentifier().toString();
    ValueType type = RECORDS.get(recordName);
    if (type == null) {
      return;
    }
    String path = string(created.getArguments(), 0, call, "value path");
    if (path == null) {
      return;
    }
    Section section = schema.root;
    String[] parts = path.split("\\.");
    for (int i = 0; i < parts.length - 1; i++) {
      section = section.child(parts[i]);
    }
    add(section, parts[parts.length - 1], type, comment(created.getArguments()), call);
  }

  private void add(Section section, String key, ValueType type, String comment, Tree tree) {
    for (Value existing : section.values) {
      if (existing.key().equals(key)) {
        error(tree, "Value '" + key + "' is declared twice in section '" + section.path + "'");
        return;
      }
    }
    section.values.add(new Value(key, type, comment, tree));
  }

  private Section parentOf(Section section) {
    int dot = section.path.lastIndexOf('.');
    if (dot < 0) {
      return null;
    }
    Section parent = schema.root;
    for (String part : section.path.substring(0, dot).split("\\.")) {
      parent = parent.child(part);
    }
    return parent;
  }

  /** The comment is the last argument when it is a string and not the key itself. */
  private String comment(List<? extends ExpressionTree> args) {
    if (args.size() < 3) {
      return null;
    }
    return constant(args.get(args.size() - 1));
  }

  private String string(List<? extends ExpressionTree> args, int index, Tree call, String what) {
    if (args.size() <= index) {
      return null;
    }
    String value = constant(args.get(index));
    if (value == null) {
      error(args.get(index), "The " + what + " must be a string literal or constant for @GenerateWrapper");
    }
    return value;
  }

  /** A string literal, or a reference to a {@code static final String} constant. */
  private String constant(ExpressionTree expression) {
    if (expression instanceof LiteralTree literal) {
      return literal.getValue() instanceof String s ? s : null;
    }
    if (expression instanceof IdentifierTree || expression instanceof MemberSelectTree) {
      TreePath path = TreePath.getPath(unit, expression);
      Element element = path == null ? null : trees.getElement(path);
      if (element instanceof VariableElement variable
          && variable.getConstantValue() instanceof String s) {
        return s;
      }
    }
    return null;
  }

  private void error(Tree tree, String message) {
    failed = true;
    trees.printMessage(Diagnostic.Kind.ERROR, message, tree, unit);
  }
}
