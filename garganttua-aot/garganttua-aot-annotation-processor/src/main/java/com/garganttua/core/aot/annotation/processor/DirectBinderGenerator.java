package com.garganttua.core.aot.annotation.processor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

/**
 * Annotation processor that generates direct-call binder classes for:
 * <ul>
 *   <li>{@code @Expression}-annotated static methods → {@code IMethodBinder} (direct method call)</li>
 *   <li>Injection bean classes ({@code @Prototype}, {@code @Singleton}, {@code @Inject}) →
 *       {@code IConstructorBinder} (direct {@code new} call)</li>
 * </ul>
 *
 * <p>
 * Generated classes extend {@code ExecutableBinder} and call the target directly
 * (without {@code Method.invoke()} or {@code Constructor.newInstance()}),
 * eliminating reflection overhead on hot paths.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
@SupportedAnnotationTypes({
    "com.garganttua.core.expression.annotations.Expression",
    "com.garganttua.core.injection.annotations.Prototype",
    "javax.inject.Singleton",
    "jakarta.inject.Singleton",
    "javax.inject.Inject",
    "jakarta.inject.Inject"
})
@SupportedOptions(DirectBinderGenerator.OPTION_ENABLED)
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class DirectBinderGenerator extends AbstractProcessor {

    /**
     * Compiler option to enable/disable direct binder generation.
     * Pass {@code -Agarganttua.direct.binders=false} to disable.
     * Defaults to {@code true} (enabled).
     */
    static final String OPTION_ENABLED = "garganttua.direct.binders";

    private static final String EXPRESSION_ANNOTATION_FQN = "com.garganttua.core.expression.annotations.Expression";
    private static final String GENERATED_PACKAGE = "com.garganttua.core.reflection.binders.generated";
    private static final String METHOD_INDEX_RESOURCE = "META-INF/garganttua/generated-binders";
    private static final String CONSTRUCTOR_INDEX_RESOURCE = "META-INF/garganttua/generated-constructor-binders";

    /** Annotations that mark a class as a DI-managed bean (constructor binder target). */
    private static final Set<String> BEAN_CLASS_ANNOTATIONS = Set.of(
        "com.garganttua.core.injection.annotations.Prototype",
        "javax.inject.Singleton",
        "jakarta.inject.Singleton"
    );

    /** Annotations on constructors that indicate DI-selected constructor. */
    private static final Set<String> INJECT_CONSTRUCTOR_ANNOTATIONS = Set.of(
        "javax.inject.Inject",
        "jakarta.inject.Inject"
    );

    private Filer filer;
    private Messager messager;

    /** Accumulated method binder index entries across rounds. */
    private final Set<String> methodIndexEntries = new HashSet<>();

    /** Accumulated constructor binder index entries across rounds. */
    private final Set<String> constructorIndexEntries = new HashSet<>();

    /** Track already-generated constructor binders to avoid duplicates. */
    private final Set<String> generatedConstructorKeys = new HashSet<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Check if generation is disabled via -Agarganttua.direct.binders=false
        String enabled = processingEnv.getOptions().get(OPTION_ENABLED);
        if ("false".equalsIgnoreCase(enabled)) {
            if (roundEnv.processingOver()) {
                messager.printMessage(Diagnostic.Kind.NOTE,
                        "[DirectBinderGenerator] Direct binder generation is DISABLED via -A" + OPTION_ENABLED + "=false");
            }
            return false;
        }

        if (roundEnv.processingOver()) {
            writeIndexFile(METHOD_INDEX_RESOURCE, methodIndexEntries);
            writeIndexFile(CONSTRUCTOR_INDEX_RESOURCE, constructorIndexEntries);
            return false;
        }

        for (TypeElement annotation : annotations) {
            String annotationFqn = annotation.getQualifiedName().toString();

            if (EXPRESSION_ANNOTATION_FQN.equals(annotationFqn)) {
                processExpressionMethods(annotation, roundEnv);
            } else if (BEAN_CLASS_ANNOTATIONS.contains(annotationFqn)) {
                processBeanClasses(annotation, roundEnv);
            } else if (INJECT_CONSTRUCTOR_ANNOTATIONS.contains(annotationFqn)) {
                processInjectConstructors(annotation, roundEnv);
            }
        }

        return false;
    }

    // ========== Expression Method Binders ==========

    private void processExpressionMethods(TypeElement annotation, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
            if (element.getKind() != ElementKind.METHOD) {
                continue;
            }

            ExecutableElement method = (ExecutableElement) element;

            if (!method.getModifiers().contains(Modifier.STATIC)) {
                messager.printMessage(Diagnostic.Kind.NOTE,
                        "[DirectBinderGenerator] Skipping non-static method: " + method.getSimpleName());
                continue;
            }

            try {
                generateMethodBinder(method);
            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "[DirectBinderGenerator] Failed to generate method binder for " + method.getSimpleName()
                                + ": " + e.getMessage());
            }
        }
    }

    // ========== Bean Constructor Binders ==========

    /**
     * Processes classes annotated with bean scope annotations (@Prototype, @Singleton).
     * Generates direct constructor binders for all public constructors.
     */
    private void processBeanClasses(TypeElement annotation, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
            if (element.getKind() != ElementKind.CLASS) {
                continue;
            }

            TypeElement classElement = (TypeElement) element;

            // Skip abstract classes — cannot be instantiated
            if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
                messager.printMessage(Diagnostic.Kind.NOTE,
                        "[DirectBinderGenerator] Skipping abstract class: " + classElement.getQualifiedName());
                continue;
            }

            // Generate binders for all public constructors
            for (Element enclosed : classElement.getEnclosedElements()) {
                if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
                    ExecutableElement ctor = (ExecutableElement) enclosed;
                    if (ctor.getModifiers().contains(Modifier.PUBLIC)) {
                        tryGenerateConstructorBinder(classElement, ctor);
                    }
                }
            }
        }
    }

    /**
     * Processes constructors annotated with @Inject.
     * Generates a direct constructor binder for the annotated constructor.
     */
    private void processInjectConstructors(TypeElement annotation, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
            if (element.getKind() != ElementKind.CONSTRUCTOR) {
                continue;
            }

            ExecutableElement ctor = (ExecutableElement) element;
            TypeElement classElement = (TypeElement) ctor.getEnclosingElement();

            // Skip abstract classes
            if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
                continue;
            }

            // Must be public for direct-call from generated package
            if (!ctor.getModifiers().contains(Modifier.PUBLIC)) {
                messager.printMessage(Diagnostic.Kind.NOTE,
                        "[DirectBinderGenerator] Skipping non-public @Inject constructor in: "
                                + classElement.getQualifiedName());
                continue;
            }

            tryGenerateConstructorBinder(classElement, ctor);
        }
    }

    private void tryGenerateConstructorBinder(TypeElement classElement, ExecutableElement ctor) {
        String key = buildConstructorKey(classElement, ctor);
        if (generatedConstructorKeys.contains(key)) {
            return; // Already generated (e.g., class has both @Singleton and @Inject on same ctor)
        }
        generatedConstructorKeys.add(key);

        try {
            generateConstructorBinder(classElement, ctor);
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "[DirectBinderGenerator] Failed to generate constructor binder for "
                            + classElement.getQualifiedName() + ": " + e.getMessage());
        }
    }

    private String buildConstructorKey(TypeElement classElement, ExecutableElement ctor) {
        StringBuilder sb = new StringBuilder();
        sb.append(classElement.getQualifiedName()).append('(');
        List<? extends VariableElement> params = ctor.getParameters();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(getSimpleTypeName(params.get(i).asType()));
        }
        sb.append(')');
        return sb.toString();
    }

    // ========== Code Generation: Method Binders ==========

    private void generateMethodBinder(ExecutableElement method) throws IOException {
        TypeElement declaringClass = (TypeElement) method.getEnclosingElement();
        String ownerFqn = declaringClass.getQualifiedName().toString();
        String ownerSimple = declaringClass.getSimpleName().toString();
        String methodName = method.getSimpleName().toString();
        List<? extends VariableElement> params = method.getParameters();

        // Build class name
        StringBuilder classNameBuilder = new StringBuilder("DirectBinder_");
        classNameBuilder.append(ownerSimple).append('_').append(methodName);
        for (VariableElement param : params) {
            classNameBuilder.append('_');
            classNameBuilder.append(getSimpleTypeName(param.asType()));
        }
        String className = classNameBuilder.toString();
        String fqClassName = GENERATED_PACKAGE + "." + className;

        // Build index key
        StringBuilder keyBuilder = new StringBuilder("M:");
        keyBuilder.append(ownerFqn).append('#').append(methodName).append('(');
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) keyBuilder.append(',');
            keyBuilder.append(getSimpleTypeName(params.get(i).asType()));
        }
        keyBuilder.append(')');
        String indexKey = keyBuilder.toString();

        methodIndexEntries.add(indexKey + "=" + fqClassName);

        messager.printMessage(Diagnostic.Kind.NOTE,
                "[DirectBinderGenerator] Generating method binder " + className + " for " + indexKey);

        TypeMirror returnType = method.getReturnType();
        String returnTypeFqn = getBoxedTypeName(returnType);
        boolean isVoid = returnType.getKind() == TypeKind.VOID;

        JavaFileObject sourceFile = filer.createSourceFile(fqClassName);
        try (Writer writer = sourceFile.openWriter()) {
            writer.write("package " + GENERATED_PACKAGE + ";\n\n");

            writer.write("import java.lang.reflect.Type;\n");
            writer.write("import java.util.List;\n");
            writer.write("import java.util.Optional;\n");
            writer.write("import java.util.Set;\n\n");
            writer.write("import com.garganttua.core.reflection.IMethodReturn;\n");
            writer.write("import com.garganttua.core.reflection.ReflectionException;\n");
            writer.write("import com.garganttua.core.reflection.binders.ExecutableBinder;\n");
            writer.write("import com.garganttua.core.reflection.binders.IMethodBinder;\n");
            writer.write("import com.garganttua.core.reflection.methods.SingleMethodReturn;\n");
            writer.write("import com.garganttua.core.reflection.IClass;\n");
            writer.write("import com.garganttua.core.supply.ISupplier;\n");
            writer.write("import com.garganttua.core.supply.SupplyException;\n");
            writer.write("import " + ownerFqn + ";\n\n");

            writer.write("/**\n");
            writer.write(" * Generated direct-call binder for {@link " + ownerSimple + "#" + methodName + "}.\n");
            writer.write(" * Calls the method directly without reflection.\n");
            writer.write(" *\n");
            writer.write(" * @generated by DirectBinderGenerator\n");
            writer.write(" */\n");
            writer.write("@SuppressWarnings(\"unchecked\")\n");
            writer.write("public class " + className + "\n");
            writer.write("        extends ExecutableBinder<" + returnTypeFqn + ">\n");
            writer.write("        implements IMethodBinder<" + returnTypeFqn + "> {\n\n");

            writer.write("    private final ISupplier<?> objectSupplier;\n\n");

            writer.write("    public " + className + "(ISupplier<?> objectSupplier, List<ISupplier<?>> params) {\n");
            writer.write("        super(params);\n");
            writer.write("        this.objectSupplier = objectSupplier;\n");
            writer.write("    }\n\n");

            writer.write("    @Override\n");
            writer.write("    public Optional<IMethodReturn<" + returnTypeFqn + ">> execute() throws ReflectionException {\n");
            writer.write("        Object[] args = buildArguments();\n");
            writer.write("        try {\n");

            if (isVoid) {
                writer.write("            " + ownerSimple + "." + methodName + "(");
                writeArgCasts(writer, params);
                writer.write(");\n");
                writer.write("            return Optional.of(SingleMethodReturn.of(null, IClass.getClass(Void.class)));\n");
            } else {
                writer.write("            " + returnTypeFqn + " result = " + ownerSimple + "." + methodName + "(");
                writeArgCasts(writer, params);
                writer.write(");\n");
                writer.write("            return Optional.of(SingleMethodReturn.of(result, IClass.getClass(" + returnTypeFqn + ".class)));\n");
            }

            writer.write("        } catch (Exception e) {\n");
            writer.write("            return Optional.of(SingleMethodReturn.ofException(e, IClass.getClass(" + returnTypeFqn + ".class)));\n");
            writer.write("        }\n");
            writer.write("    }\n\n");

            writer.write("    @Override\n");
            writer.write("    public String getExecutableReference() {\n");
            writer.write("        return \"" + ownerFqn + "#" + methodName + " [direct]\";\n");
            writer.write("    }\n\n");

            writer.write("    @Override\n");
            writer.write("    public Optional<IMethodReturn<" + returnTypeFqn + ">> supply() throws SupplyException {\n");
            writer.write("        try {\n");
            writer.write("            return this.execute();\n");
            writer.write("        } catch (ReflectionException e) {\n");
            writer.write("            throw new SupplyException(e);\n");
            writer.write("        }\n");
            writer.write("    }\n\n");

            writer.write("    @Override\n");
            writer.write("    public Type getSuppliedType() {\n");
            writer.write("        return this.objectSupplier.getSuppliedType();\n");
            writer.write("    }\n\n");

            writer.write("    @Override\n");
            writer.write("    public IClass<IMethodReturn<" + returnTypeFqn + ">> getSuppliedClass() {\n");
            writer.write("        return (IClass<IMethodReturn<" + returnTypeFqn + ">>) (IClass<?>) IClass.getClass(IMethodReturn.class);\n");
            writer.write("    }\n");

            writer.write("}\n");
        }
    }

    // ========== Code Generation: Constructor Binders ==========

    private void generateConstructorBinder(TypeElement classElement, ExecutableElement ctor) throws IOException {
        String classFqn = classElement.getQualifiedName().toString();
        String classSimple = classElement.getSimpleName().toString();
        List<? extends VariableElement> params = ctor.getParameters();

        // Build class name: DirectConstructorBinder_ClassName_ParamSimples
        StringBuilder classNameBuilder = new StringBuilder("DirectConstructorBinder_");
        classNameBuilder.append(classSimple);
        for (VariableElement param : params) {
            classNameBuilder.append('_');
            classNameBuilder.append(getSimpleTypeName(param.asType()));
        }
        String className = classNameBuilder.toString();
        String fqClassName = GENERATED_PACKAGE + "." + className;

        // Build index key: C:com.example.MyClass(ParamType1,ParamType2)
        StringBuilder keyBuilder = new StringBuilder("C:");
        keyBuilder.append(classFqn).append('(');
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) keyBuilder.append(',');
            keyBuilder.append(getSimpleTypeName(params.get(i).asType()));
        }
        keyBuilder.append(')');
        String indexKey = keyBuilder.toString();

        constructorIndexEntries.add(indexKey + "=" + fqClassName);

        messager.printMessage(Diagnostic.Kind.NOTE,
                "[DirectBinderGenerator] Generating constructor binder " + className + " for " + indexKey);

        // Collect all param type FQNs for imports
        Set<String> extraImports = new HashSet<>();
        for (VariableElement param : params) {
            String fqn = getRawTypeFqn(param.asType());
            if (fqn != null && !fqn.startsWith("java.lang.") && fqn.contains(".")) {
                extraImports.add(fqn);
            }
        }

        JavaFileObject sourceFile = filer.createSourceFile(fqClassName);
        try (Writer writer = sourceFile.openWriter()) {
            writer.write("package " + GENERATED_PACKAGE + ";\n\n");

            writer.write("import java.lang.reflect.Type;\n");
            writer.write("import java.util.List;\n");
            writer.write("import java.util.Optional;\n\n");
            writer.write("import com.garganttua.core.reflection.IConstructor;\n");
            writer.write("import com.garganttua.core.reflection.IMethodReturn;\n");
            writer.write("import com.garganttua.core.reflection.ReflectionException;\n");
            writer.write("import com.garganttua.core.reflection.binders.ExecutableBinder;\n");
            writer.write("import com.garganttua.core.reflection.binders.IConstructorBinder;\n");
            writer.write("import com.garganttua.core.reflection.methods.SingleMethodReturn;\n");
            writer.write("import com.garganttua.core.reflection.IClass;\n");
            writer.write("import com.garganttua.core.supply.ISupplier;\n");
            writer.write("import com.garganttua.core.supply.SupplyException;\n");
            writer.write("import " + classFqn + ";\n");
            for (String imp : extraImports) {
                writer.write("import " + imp + ";\n");
            }
            writer.write("\n");

            writer.write("/**\n");
            writer.write(" * Generated direct-call constructor binder for {@link " + classSimple + "}.\n");
            writer.write(" * Instantiates via {@code new " + classSimple + "(...)} without reflection.\n");
            writer.write(" *\n");
            writer.write(" * @generated by DirectBinderGenerator\n");
            writer.write(" */\n");
            writer.write("public class " + className + "\n");
            writer.write("        extends ExecutableBinder<" + classFqn + ">\n");
            writer.write("        implements IConstructorBinder<" + classFqn + "> {\n\n");

            // Constructor
            writer.write("    public " + className + "(List<ISupplier<?>> params) {\n");
            writer.write("        super(params);\n");
            writer.write("    }\n\n");

            // execute()
            writer.write("    @Override\n");
            writer.write("    public Optional<IMethodReturn<" + classFqn + ">> execute() throws ReflectionException {\n");
            writer.write("        Object[] args = buildArguments();\n");
            writer.write("        try {\n");
            writer.write("            " + classFqn + " instance = new " + classFqn + "(");
            writeArgCasts(writer, params);
            writer.write(");\n");
            writer.write("            return Optional.of(SingleMethodReturn.of(instance, IClass.getClass(" + classFqn + ".class)));\n");
            writer.write("        } catch (Exception e) {\n");
            writer.write("            return Optional.of(SingleMethodReturn.ofException(e, IClass.getClass(" + classFqn + ".class)));\n");
            writer.write("        }\n");
            writer.write("    }\n\n");

            // getConstructedType()
            writer.write("    @Override\n");
            writer.write("    public IClass<" + classFqn + "> getConstructedType() {\n");
            writer.write("        return IClass.getClass(" + classFqn + ".class);\n");
            writer.write("    }\n\n");

            // constructor() - deprecated, not needed for direct call binders
            writer.write("    @Override\n");
            writer.write("    public IConstructor<?> constructor() {\n");
            writer.write("        throw new UnsupportedOperationException(\"Direct binder does not expose constructor\");\n");
            writer.write("    }\n\n");

            // getExecutableReference()
            writer.write("    @Override\n");
            writer.write("    public String getExecutableReference() {\n");
            writer.write("        return \"" + classFqn + ".<init> [direct]\";\n");
            writer.write("    }\n\n");

            // supply()
            writer.write("    @Override\n");
            writer.write("    public Optional<IMethodReturn<" + classFqn + ">> supply() throws SupplyException {\n");
            writer.write("        try {\n");
            writer.write("            return this.execute();\n");
            writer.write("        } catch (ReflectionException e) {\n");
            writer.write("            throw new SupplyException(e);\n");
            writer.write("        }\n");
            writer.write("    }\n\n");

            // getSuppliedType()
            writer.write("    @Override\n");
            writer.write("    public Type getSuppliedType() {\n");
            writer.write("        return " + classFqn + ".class;\n");
            writer.write("    }\n\n");

            // getSuppliedClass()
            writer.write("    @SuppressWarnings(\"unchecked\")\n");
            writer.write("    @Override\n");
            writer.write("    public IClass<IMethodReturn<" + classFqn + ">> getSuppliedClass() {\n");
            writer.write("        return (IClass<IMethodReturn<" + classFqn + ">>) (IClass<?>) IClass.getClass(IMethodReturn.class);\n");
            writer.write("    }\n");

            writer.write("}\n");
        }
    }

    // ========== Shared Utilities ==========

    private void writeArgCasts(Writer writer, List<? extends VariableElement> params) throws IOException {
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) writer.write(", ");
            TypeMirror paramType = params.get(i).asType();
            String castType = getCastExpression(paramType, i);
            writer.write(castType);
        }
    }

    private String getCastExpression(TypeMirror type, int index) {
        String argExpr = "args[" + index + "]";
        return switch (type.getKind()) {
            case BOOLEAN -> "(" + argExpr + " instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(" + argExpr + ")))";
            case BYTE -> "((Number) " + argExpr + ").byteValue()";
            case SHORT -> "((Number) " + argExpr + ").shortValue()";
            case INT -> "((Number) " + argExpr + ").intValue()";
            case LONG -> "((Number) " + argExpr + ").longValue()";
            case FLOAT -> "((Number) " + argExpr + ").floatValue()";
            case DOUBLE -> "((Number) " + argExpr + ").doubleValue()";
            case CHAR -> "(Character) " + argExpr;
            case TYPEVAR -> "(Object) " + argExpr; // Unresolved type variable → cast to Object
            default -> "(" + getRawTypeFqn(type) + ") " + argExpr; // Strip generics from cast
        };
    }

    private String getSimpleTypeName(TypeMirror type) {
        String fullName = type.toString();
        int genericStart = fullName.indexOf('<');
        if (genericStart > 0) {
            fullName = fullName.substring(0, genericStart);
        }
        int lastDot = fullName.lastIndexOf('.');
        if (lastDot >= 0) {
            return fullName.substring(lastDot + 1);
        }
        return fullName;
    }

    /**
     * Returns the fully-qualified raw type name (without generics) for a TypeMirror.
     */
    private String getRawTypeFqn(TypeMirror type) {
        String fullName = type.toString();
        int genericStart = fullName.indexOf('<');
        if (genericStart > 0) {
            fullName = fullName.substring(0, genericStart);
        }
        return fullName;
    }

    private String getBoxedTypeName(TypeMirror type) {
        return switch (type.getKind()) {
            case BOOLEAN -> "Boolean";
            case BYTE -> "Byte";
            case SHORT -> "Short";
            case INT -> "Integer";
            case LONG -> "Long";
            case FLOAT -> "Float";
            case DOUBLE -> "Double";
            case CHAR -> "Character";
            case VOID -> "Void";
            case TYPEVAR -> "Object"; // Unresolved type variable → use Object
            default -> {
                String name = type.toString();
                int genericStart = name.indexOf('<');
                if (genericStart > 0) {
                    name = name.substring(0, genericStart);
                }
                yield name;
            }
        };
    }

    private void writeIndexFile(String resource, Set<String> entries) {
        if (entries.isEmpty()) {
            return;
        }

        try {
            Set<String> allEntries = new HashSet<>(entries);
            try {
                FileObject existing = filer.getResource(StandardLocation.CLASS_OUTPUT, "", resource);
                try (var reader = existing.openReader(true);
                     var bufferedReader = new java.io.BufferedReader(reader)) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (!line.isBlank()) {
                            allEntries.add(line.trim());
                        }
                    }
                }
            } catch (IOException e) {
                // File doesn't exist yet
            }

            FileObject indexFile = filer.createResource(StandardLocation.CLASS_OUTPUT, "", resource);
            try (BufferedWriter writer = new BufferedWriter(indexFile.openWriter())) {
                for (String entry : allEntries.stream().sorted().toList()) {
                    writer.write(entry);
                    writer.newLine();
                }
            }

            messager.printMessage(Diagnostic.Kind.NOTE,
                    "[DirectBinderGenerator] Wrote " + allEntries.size() + " entries to " + resource);

        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "[DirectBinderGenerator] Failed to write index file " + resource + ": " + e.getMessage());
        }
    }
}
