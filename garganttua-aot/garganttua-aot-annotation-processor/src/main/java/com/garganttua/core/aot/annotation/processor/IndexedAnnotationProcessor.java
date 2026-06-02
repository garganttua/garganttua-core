package com.garganttua.core.aot.annotation.processor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Compile-time annotation processor that generates annotation index files.
 *
 * <p>For each annotation type that is meta-annotated with {@code @Indexed}
 * (from garganttua-commons), this processor discovers all elements annotated
 * with it and writes index entries to
 * {@code META-INF/garganttua/index/<annotation.fqn>}.</p>
 *
 * <p>Additionally, standard JSR-330 annotations ({@code javax.inject.*} and
 * {@code jakarta.inject.*}) are indexed regardless of {@code @Indexed}
 * meta-annotation.</p>
 *
 * <h2>Index Entry Format</h2>
 * <ul>
 *   <li>Classes: {@code C:fully.qualified.ClassName}</li>
 *   <li>Methods: {@code M:fully.qualified.ClassName#methodName(Param1,Param2)}</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class IndexedAnnotationProcessor extends AbstractProcessor {

    private static final String INDEXED_ANNOTATION = "com.garganttua.core.reflection.annotations.Indexed";
    private static final String INDEX_DIR = "META-INF/garganttua/index/";

    private static final List<String> JSR330_ANNOTATIONS = List.of(
            "javax.inject.Inject",
            "javax.inject.Singleton",
            "javax.inject.Named",
            // Qualifier + Scope are meta-markers. Indexing them captures every
            // annotation TYPE that wears one — e.g. @Observer, @BeanProvider,
            // user-side custom qualifiers — so framework code that asks the
            // scanner for "classes annotated with @Qualifier" reliably finds
            // each qualifier annotation type. Without this, an AOT consumer
            // gets nothing on getClassesWithAnnotation(Qualifier).
            "javax.inject.Qualifier",
            "javax.inject.Scope",
            "jakarta.inject.Inject",
            "jakarta.inject.Singleton",
            "jakarta.inject.Named",
            "jakarta.inject.Qualifier",
            "jakarta.inject.Scope"
    );

    /** Accumulated entries across processing rounds: annotation FQN -> set of index entries. */
    private final Map<String, Set<String>> indexEntries = new HashMap<>();

    private Messager messager;

    /**
     * Captures the {@link Messager} from the processing environment for later
     * diagnostic reporting.
     *
     * @param processingEnv the processing environment supplied by the compiler
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
    }

    /**
     * Accumulates index entries for indexed/JSR-330 annotations each round, then
     * writes all index files once processing is over.
     *
     * @param annotations the annotation types requested to be processed this round
     * @param roundEnv     information about the current and prior rounds
     * @return {@code false} — this processor never claims the annotations it reads
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            writeIndexFiles();
            return false;
        }

        for (TypeElement annotation : annotations) {
            String annotationFqn = annotation.getQualifiedName().toString();

            if (isIndexedAnnotation(annotation) || JSR330_ANNOTATIONS.contains(annotationFqn)) {
                Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
                for (Element element : annotatedElements) {
                    String entry = toIndexEntry(element);
                    if (entry != null) {
                        indexEntries
                                .computeIfAbsent(annotationFqn, k -> new LinkedHashSet<>())
                                .add(entry);
                    }
                }
            }
        }

        return false;
    }

    /**
     * Checks whether the given annotation type is meta-annotated with any of
     * the markers that opt it into compile-time indexing — {@code @Indexed},
     * {@code javax.inject.Qualifier} / {@code Scope}, and their jakarta
     * equivalents.
     *
     * <p>The Qualifier/Scope inclusion is what makes user-defined custom
     * qualifier annotations work in pure-AOT mode. Without it, a consumer
     * annotation like {@code @MyQualifier} (which is itself
     * {@code @Qualifier}-meta) gets no index file — so framework code that
     * iterates known qualifiers and asks the scanner for classes annotated
     * with each one finds nothing for {@code @MyQualifier}.</p>
     */
    private boolean isIndexedAnnotation(TypeElement annotationType) {
        for (AnnotationMirror mirror : annotationType.getAnnotationMirrors()) {
            DeclaredType mirrorType = mirror.getAnnotationType();
            TypeElement mirrorElement = (TypeElement) mirrorType.asElement();
            String fqn = mirrorElement.getQualifiedName().toString();
            if (INDEXED_ANNOTATION.equals(fqn)) {
                return true;
            }
            // Treat @Qualifier and @Scope (javax + jakarta) as implicit
            // @Indexed markers. Any user-defined custom qualifier or scope
            // is therefore auto-indexed without requiring @Indexed on top.
            if (JSR330_ANNOTATIONS.contains(fqn)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts an annotated element to its index entry string.
     *
     * @return {@code "C:fqn"} for classes, {@code "M:fqn#method(params)"} for methods, or null
     */
    private String toIndexEntry(Element element) {
        ElementKind kind = element.getKind();

        // ANNOTATION_TYPE must be handled explicitly — kind.isInterface() is
        // false for it in some JDK versions, which used to silently drop
        // annotation declarations bearing meta-annotations like @Qualifier.
        // That left the Qualifier / Scope indices empty, so the AOT scanner
        // returned nothing on getClassesWithAnnotation(Qualifier) and the
        // framework's qualifier-annotation discovery (Observer, BeanProvider,
        // PropertyProvider, custom user qualifiers, …) silently no-op'd.
        if (kind.isClass() || kind.isInterface() || kind == ElementKind.ENUM
                || kind == ElementKind.RECORD || kind == ElementKind.ANNOTATION_TYPE) {
            TypeElement typeElement = (TypeElement) element;
            // Use the JVM binary name (Outer$Inner) rather than the Java
            // source name (Outer.Inner) so the runtime side's Class.forName
            // resolves nested / inner classes. Without this, an @Observer-
            // annotated nested class (common pattern in real apps, and the
            // exact shape used by the test harness) ends up in the index
            // under "Outer.Inner" which Class.forName rejects → the scanner
            // silently drops the entry → the framework never sees the class.
            return "C:" + binaryName(typeElement);
        }

        if (kind == ElementKind.METHOD || kind == ElementKind.CONSTRUCTOR) {
            ExecutableElement executableElement = (ExecutableElement) element;
            TypeElement enclosingType = (TypeElement) executableElement.getEnclosingElement();
            String className = binaryName(enclosingType);
            String methodName = executableElement.getSimpleName().toString();
            String params = formatParameters(executableElement);
            return "M:" + className + "#" + methodName + "(" + params + ")";
        }

        // Fields and other elements are not indexed
        return null;
    }

    /**
     * Returns the JVM binary name of a type element — {@code Outer$Inner$Foo}
     * for nested classes, plain {@code com.example.Foo} for top-level types.
     * This matches what {@code Class.forName} expects, so the runtime side
     * can resolve every indexed FQN. Using {@code getQualifiedName} alone
     * yields the Java source notation ({@code Outer.Inner}) which
     * {@code Class.forName} rejects.
     */
    private String binaryName(TypeElement type) {
        return processingEnv.getElementUtils().getBinaryName(type).toString();
    }

    /**
     * Formats method parameter types as a comma-separated string using simple names.
     */
    private String formatParameters(ExecutableElement method) {
        StringBuilder sb = new StringBuilder();
        List<? extends VariableElement> params = method.getParameters();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            TypeMirror paramType = params.get(i).asType();
            sb.append(toSimpleTypeName(paramType));
        }
        return sb.toString();
    }

    /**
     * Extracts a simple type name from a TypeMirror.
     * For declared types, returns the simple class name.
     * For primitives, returns the primitive name.
     * For arrays, appends "[]".
     */
    private String toSimpleTypeName(TypeMirror typeMirror) {
        switch (typeMirror.getKind()) {
            case DECLARED:
                DeclaredType declaredType = (DeclaredType) typeMirror;
                TypeElement typeElement = (TypeElement) declaredType.asElement();
                return typeElement.getSimpleName().toString();
            case ARRAY:
                javax.lang.model.type.ArrayType arrayType = (javax.lang.model.type.ArrayType) typeMirror;
                return toSimpleTypeName(arrayType.getComponentType()) + "[]";
            default:
                // Primitives and other types
                return typeMirror.toString();
        }
    }

    /**
     * Writes all accumulated index entries to resource files.
     */
    private void writeIndexFiles() {
        for (Map.Entry<String, Set<String>> entry : indexEntries.entrySet()) {
            String annotationFqn = entry.getKey();
            Set<String> entries = entry.getValue();

            if (entries.isEmpty()) {
                continue;
            }

            String resourcePath = INDEX_DIR + annotationFqn;
            try {
                // Read existing entries if the file already exists (incremental builds)
                Set<String> allEntries = new LinkedHashSet<>();
                try {
                    FileObject existing = processingEnv.getFiler().getResource(
                            StandardLocation.CLASS_OUTPUT, "", resourcePath);
                    try (var reader = existing.openReader(true)) {
                        var buffered = new java.io.BufferedReader(reader);
                        String line;
                        while ((line = buffered.readLine()) != null) {
                            String trimmed = line.trim();
                            if (!trimmed.isEmpty()) {
                                allEntries.add(trimmed);
                            }
                        }
                    }
                } catch (IOException e) {
                    // File does not exist yet, that's fine
                }

                allEntries.addAll(entries);

                FileObject fileObject = processingEnv.getFiler().createResource(
                        StandardLocation.CLASS_OUTPUT, "", resourcePath);
                try (Writer writer = fileObject.openWriter();
                     BufferedWriter bw = new BufferedWriter(writer)) {
                    for (String indexEntry : allEntries) {
                        bw.write(indexEntry);
                        bw.newLine();
                    }
                }

                messager.printMessage(Diagnostic.Kind.NOTE,
                        "[garganttua-aot] Generated index: " + resourcePath + " (" + allEntries.size() + " entries)");

            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "[garganttua-aot] Failed to write index file " + resourcePath + ": " + e.getMessage());
            }
        }
    }
}
