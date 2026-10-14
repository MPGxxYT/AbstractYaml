package me.mortaldev.config2.processor;

import com.sun.source.tree.ClassTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import me.mortaldev.config2.GenerateWrapper;
import me.mortaldev.config2.RegisterConfig;

/**
 * Generates wrapper classes for schemas annotated with {@link GenerateWrapper}.
 *
 * <p>Registered through {@code META-INF/services}, so it runs whenever AbstractYaml is on the
 * compiler's annotation processor path. It needs javac's syntax tree API, which Maven, Gradle and
 * IntelliJ all use.
 */
public final class WrapperProcessor extends AbstractProcessor {
  private static final String SCHEMA_TYPE = "me.mortaldev.config2.ConfigSchema";

  private Trees trees;

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(GenerateWrapper.class.getName());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public synchronized void init(ProcessingEnvironment env) {
    super.init(env);
    trees = treesFor(env);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
    for (Element element : round.getElementsAnnotatedWith(GenerateWrapper.class)) {
      if (element instanceof TypeElement type) {
        generate(type);
      }
    }
    return true;
  }

  private void generate(TypeElement type) {
    TypeElement schemaType = processingEnv.getElementUtils().getTypeElement(SCHEMA_TYPE);
    TypeMirror erased =
        schemaType == null ? null : processingEnv.getTypeUtils().erasure(schemaType.asType());
    if (type.getKind() != ElementKind.CLASS
        || erased == null
        || !processingEnv.getTypeUtils().isSubtype(type.asType(), erased)) {
      error(type, "@GenerateWrapper can only be used on a class that extends ConfigSchema");
      return;
    }
    if (trees == null) {
      error(type, "@GenerateWrapper needs javac's syntax tree API, which this compiler does not"
          + " provide; build with javac (Maven, Gradle or IntelliJ)");
      return;
    }

    TreePath path = trees.getPath(type);
    if (path == null || !(path.getLeaf() instanceof ClassTree classTree)) {
      error(type, "Source code for " + type.getQualifiedName() + " is not available");
      return;
    }
    SchemaReader.Schema schema = SchemaReader.read(trees, path.getCompilationUnit(), classTree);
    if (schema == null) {
      return;
    }

    RegisterConfig registration = type.getAnnotation(RegisterConfig.class);
    if (registration != null && !registration.value().isEmpty()) {
      schema.configName = registration.value();
    }
    if (schema.configName == null) {
      error(type, "@GenerateWrapper could not find super(\"config-name\") in the constructor");
      return;
    }

    GenerateWrapper options = type.getAnnotation(GenerateWrapper.class);
    String className =
        options.value().isEmpty() ? defaultClassName(type) : options.value();
    String packageName = options.packageName().isEmpty()
        ? subPackage(processingEnv.getElementUtils().getPackageOf(type), "wrappers")
        : options.packageName();
    String qualifiedName = packageName.isEmpty() ? className : packageName + "." + className;

    if (!SourceVersion.isName(qualifiedName)) {
      error(type, "'" + qualifiedName + "' is not a valid class name for the wrapper");
      return;
    }
    if (processingEnv.getElementUtils().getTypeElement(qualifiedName) != null) {
      processingEnv.getMessager().printMessage(
          Diagnostic.Kind.NOTE,
          qualifiedName + " already exists in the sources, so it was not generated."
              + " Delete it to have AbstractYaml generate it from " + type.getSimpleName() + ".",
          type);
      return;
    }

    String source;
    try {
      source = WrapperWriter.write(
          packageName,
          className,
          type.getQualifiedName().toString(),
          schema,
          options.makeFinal(),
          options.generateReload());
    } catch (WrapperWriter.NameClashException e) {
      trees.printMessage(
          Diagnostic.Kind.ERROR, e.getMessage(), e.value.tree(), path.getCompilationUnit());
      return;
    }

    Filer filer = processingEnv.getFiler();
    try (Writer writer = filer.createSourceFile(qualifiedName, type).openWriter()) {
      writer.write(source);
    } catch (FilerException e) {
      error(type, "Could not create " + qualifiedName + ": " + e.getMessage());
    } catch (IOException e) {
      error(type, "Could not write " + qualifiedName + ": " + e);
    }
  }

  /** {@code AbilitiesConfigSchema} becomes {@code AbilitiesWrapper}. */
  static String defaultClassName(TypeElement type) {
    String name = type.getSimpleName().toString();
    for (String suffix : new String[] {"ConfigSchema", "Schema"}) {
      if (name.endsWith(suffix) && name.length() > suffix.length()) {
        return name.substring(0, name.length() - suffix.length()) + "Wrapper";
      }
    }
    return name + "Wrapper";
  }

  private static String subPackage(PackageElement pkg, String name) {
    return pkg.isUnnamed() ? name : pkg.getQualifiedName() + "." + name;
  }

  private void error(Element element, String message) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
  }

  /**
   * Gets javac's {@link Trees} for this compilation. Some build tools hand processors a wrapped
   * environment; this unwraps the common forms (a field holding the delegate, or a dynamic proxy)
   * until javac's own environment is found.
   */
  private static Trees treesFor(ProcessingEnvironment env) {
    for (int depth = 0; env != null && depth < 5; depth++) {
      try {
        return Trees.instance(env);
      } catch (IllegalArgumentException e) {
        env = unwrap(env);
      }
    }
    return null;
  }

  private static ProcessingEnvironment unwrap(ProcessingEnvironment env) {
    Object holder = Proxy.isProxyClass(env.getClass()) ? Proxy.getInvocationHandler(env) : env;
    for (Class<?> c = holder.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
      for (Field field : c.getDeclaredFields()) {
        try {
          field.setAccessible(true);
          if (field.get(holder) instanceof ProcessingEnvironment inner && inner != env) {
            return inner;
          }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
          // Not accessible; try the next field
        }
      }
    }
    return null;
  }
}
