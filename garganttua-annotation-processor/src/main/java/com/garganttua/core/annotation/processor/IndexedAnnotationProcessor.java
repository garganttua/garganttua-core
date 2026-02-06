package com.garganttua.core.annotation.processor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Annotation processor that generates compile-time indices for annotations
 * marked with {@code @Indexed}.
 *
 * <p>
 * This processor scans for all annotations that are themselves annotated with
 * {@code @Indexed}, then collects all classes and methods annotated with those
 * annotations. The results are written to index files in
 * {@code META-INF/garganttua/index/}.
 * </p>
 *
 * <h2>Index File Format</h2>
 * <p>Each index file is named after the fully qualified annotation name and contains:</p>
 * <ul>
 *   <li>{@code C:fully.qualified.ClassName} - for annotated classes</li>
 *   <li>{@code M:fully.qualified.ClassName#methodName(param1,param2)} - for annotated methods</li>
 * </ul>
 *
 * <h2>Incremental Compilation Support</h2>
 * <p>
 * The processor aggregates entries from all compilation rounds and writes
 * the complete index at the end of processing. It also attempts to read
 * and merge with existing index files to support incremental compilation.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class IndexedAnnotationProcessor extends AbstractProcessor {

    private static final String INDEX_LOCATION = "META-INF/garganttua/index/";
    private static final String CLASS_PREFIX = "C:";
    private static final String METHOD_PREFIX = "M:";
    private static final String INDEXED_ANNOTATION_FQN = "com.garganttua.core.reflection.annotations.Indexed";

    /**
     * Hardcoded third-party annotations that should be indexed even though
     * they don't have @Indexed (since we can't modify third-party code).
     */
    private static final Set<String> HARDCODED_INDEXED_ANNOTATIONS = Set.of(
        // javax.inject annotations
        "javax.inject.Singleton",
        "javax.inject.Inject",
        "javax.inject.Named",
        "javax.inject.Qualifier",
        // jakarta.inject annotations (for future compatibility)
        "jakarta.inject.Singleton",
        "jakarta.inject.Inject",
        "jakarta.inject.Named",
        "jakarta.inject.Qualifier"
    );

    private Filer filer;
    private Messager messager;

    /** Map from indexed annotation FQN to set of indexed entries */
    private final Map<String, Set<String>> indexEntries = new HashMap<>();

    /** Cache of annotation types we've already checked for @Indexed */
    private final Set<String> checkedAnnotations = new HashSet<>();

    /** Set of annotation FQNs that are marked with @Indexed */
    private final Set<String> indexedAnnotations = new HashSet<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            writeIndexFiles();
            return false;
        }

        // Process each annotation type present in this round
        for (TypeElement annotation : annotations) {
            String annotationFqn = annotation.getQualifiedName().toString();

            // Check if this annotation is marked with @Indexed or is hardcoded (only check once)
            if (!checkedAnnotations.contains(annotationFqn)) {
                checkedAnnotations.add(annotationFqn);

                if (hasIndexedAnnotation(annotation) || HARDCODED_INDEXED_ANNOTATIONS.contains(annotationFqn)) {
                    indexedAnnotations.add(annotationFqn);
                    indexEntries.putIfAbsent(annotationFqn, new HashSet<>());
                    String reason = HARDCODED_INDEXED_ANNOTATIONS.contains(annotationFqn)
                        ? "hardcoded third-party"
                        : "@Indexed";
                    messager.printMessage(Diagnostic.Kind.NOTE,
                        "[IndexedAnnotationProcessor] Found " + reason + " annotation: " + annotationFqn);
                }
            }

            // If this is an indexed annotation, collect all elements annotated with it
            if (indexedAnnotations.contains(annotationFqn)) {
                Set<String> entries = indexEntries.get(annotationFqn);

                for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                    String entry = createIndexEntry(element);
                    if (entry != null) {
                        entries.add(entry);
                        messager.printMessage(Diagnostic.Kind.NOTE,
                            "[IndexedAnnotationProcessor] Indexed: " + entry);
                    }
                }
            }
        }

        return false;
    }

    /**
     * Checks if a TypeElement (annotation type) has the @Indexed annotation.
     *
     * @param annotationType the annotation type to check
     * @return true if the annotation is marked with @Indexed
     */
    private boolean hasIndexedAnnotation(TypeElement annotationType) {
        List<? extends AnnotationMirror> annotationMirrors = annotationType.getAnnotationMirrors();

        for (AnnotationMirror mirror : annotationMirrors) {
            DeclaredType annotationDeclaredType = mirror.getAnnotationType();
            TypeElement annotationElement = (TypeElement) annotationDeclaredType.asElement();
            String annotationName = annotationElement.getQualifiedName().toString();

            if (INDEXED_ANNOTATION_FQN.equals(annotationName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Creates an index entry for the given element.
     *
     * @param element the annotated element
     * @return the index entry string, or null if not indexable
     */
    private String createIndexEntry(Element element) {
        switch (element.getKind()) {
            case CLASS:
            case INTERFACE:
            case ENUM:
            case RECORD:
                return CLASS_PREFIX + ((TypeElement) element).getQualifiedName().toString();

            case METHOD:
                ExecutableElement method = (ExecutableElement) element;
                TypeElement declaringClass = (TypeElement) method.getEnclosingElement();
                String methodSignature = buildMethodSignature(method);
                return METHOD_PREFIX + declaringClass.getQualifiedName().toString()
                    + "#" + methodSignature;

            default:
                return null;
        }
    }

    /**
     * Builds a method signature string including parameter types.
     */
    private String buildMethodSignature(ExecutableElement method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getSimpleName().toString());
        sb.append("(");

        boolean first = true;
        for (VariableElement param : method.getParameters()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            TypeMirror paramType = param.asType();
            sb.append(getSimpleTypeName(paramType));
        }

        sb.append(")");
        return sb.toString();
    }

    /**
     * Gets a simple type name suitable for the index.
     */
    private String getSimpleTypeName(TypeMirror type) {
        String fullName = type.toString();
        // Remove generic parameters for simpler matching
        int genericStart = fullName.indexOf('<');
        if (genericStart > 0) {
            fullName = fullName.substring(0, genericStart);
        }
        return fullName;
    }

    /**
     * Writes all collected index entries to their respective files.
     */
    private void writeIndexFiles() {
        for (Map.Entry<String, Set<String>> entry : indexEntries.entrySet()) {
            String annotationFqn = entry.getKey();
            Set<String> entries = entry.getValue();

            if (entries.isEmpty()) {
                messager.printMessage(Diagnostic.Kind.NOTE,
                    "[IndexedAnnotationProcessor] No entries for @" + annotationFqn + ", skipping index file");
                continue;
            }

            String fileName = INDEX_LOCATION + annotationFqn;

            try {
                // Merge with existing entries if file exists
                Set<String> allEntries = new HashSet<>(entries);
                try {
                    FileObject existing = filer.getResource(StandardLocation.CLASS_OUTPUT, "", fileName);
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
                    // File doesn't exist yet, that's fine
                }

                // Write the merged index
                FileObject indexFile = filer.createResource(StandardLocation.CLASS_OUTPUT, "", fileName);
                try (BufferedWriter writer = new BufferedWriter(indexFile.openWriter())) {
                    for (String indexEntry : allEntries.stream().sorted().toList()) {
                        writer.write(indexEntry);
                        writer.newLine();
                    }
                }

                messager.printMessage(Diagnostic.Kind.NOTE,
                    "[IndexedAnnotationProcessor] Generated index for @" + annotationFqn + " with " + allEntries.size() + " entries");

            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                    "[IndexedAnnotationProcessor] Failed to write index file for " + annotationFqn + ": " + e.getMessage());
            }
        }
    }
}
