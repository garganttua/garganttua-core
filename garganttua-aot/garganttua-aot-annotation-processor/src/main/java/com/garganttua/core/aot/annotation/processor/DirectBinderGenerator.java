package com.garganttua.core.aot.annotation.processor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

/**
 * Compile-time annotation processor that generates AOTClass implementations
 * for types annotated with {@code @Reflected}.
 *
 * <p>This processor is controlled by the compiler option
 * {@code -Agarganttua.direct.binders=true}. If the option is not set or
 * not equal to "true", this processor does nothing.</p>
 *
 * <p>For each class annotated with {@code @Reflected}, the processor generates:</p>
 * <ul>
 *   <li>An {@code AOTClass_<SimpleName>.java} source file containing pre-computed
 *       metadata (fields, methods, constructors, annotations, modifiers)</li>
 *   <li>A listing entry in {@code META-INF/garganttua/aot/classes/<fqn>}</li>
 * </ul>
 *
 * <p>The generated class registers itself with {@code AOTRegistry} in a static
 * initializer block, enabling automatic discovery at runtime.</p>
 *
 * @since 2.0.0-ALPHA01
 */
@SupportedAnnotationTypes("com.garganttua.core.reflection.annotations.Reflected")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedOptions("garganttua.direct.binders")
public class DirectBinderGenerator extends AbstractProcessor {

    private static final String REFLECTED_ANNOTATION = "com.garganttua.core.reflection.annotations.Reflected";
    private static final String AOT_CLASSES_DIR = "META-INF/garganttua/aot/classes/";

    private Messager messager;
    private boolean enabled;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        String option = processingEnv.getOptions().get("garganttua.direct.binders");
        this.enabled = "true".equalsIgnoreCase(option);

        if (enabled) {
            messager.printMessage(Diagnostic.Kind.NOTE,
                    "[garganttua-aot] DirectBinderGenerator enabled");
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!enabled || roundEnv.processingOver()) {
            return false;
        }

        for (TypeElement annotation : annotations) {
            if (!REFLECTED_ANNOTATION.equals(annotation.getQualifiedName().toString())) {
                continue;
            }

            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element element : annotatedElements) {
                if (element instanceof TypeElement typeElement) {
                    processReflectedType(typeElement);
                }
            }
        }

        return false;
    }

    /**
     * Processes a single @Reflected type element: generates the AOTClass source
     * and writes the listing entry.
     */
    private void processReflectedType(TypeElement typeElement) {
        String qualifiedName = typeElement.getQualifiedName().toString();

        try {
            // Extract @Reflected annotation values
            boolean queryAllDeclaredConstructors = getAnnotationBooleanValue(typeElement, "queryAllDeclaredConstructors", false);
            boolean queryAllPublicConstructors = getAnnotationBooleanValue(typeElement, "queryAllPublicConstructors", false);
            boolean queryAllDeclaredMethods = getAnnotationBooleanValue(typeElement, "queryAllDeclaredMethods", false);
            boolean queryAllPublicMethods = getAnnotationBooleanValue(typeElement, "queryAllPublicMethods", false);
            boolean allDeclaredFields = getAnnotationBooleanValue(typeElement, "allDeclaredFields", false);
            boolean allDeclaredClasses = getAnnotationBooleanValue(typeElement, "allDeclaredClasses", false);

            // Generate source code
            AOTClassSourceGenerator generator = new AOTClassSourceGenerator(
                    typeElement, processingEnv,
                    queryAllDeclaredConstructors, queryAllPublicConstructors,
                    queryAllDeclaredMethods, queryAllPublicMethods,
                    allDeclaredFields, allDeclaredClasses);

            String sourceCode = generator.generate();
            String generatedFqn = generator.getGeneratedQualifiedName();

            // Write Java source file
            JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(generatedFqn, typeElement);
            try (Writer writer = sourceFile.openWriter()) {
                writer.write(sourceCode);
            }

            messager.printMessage(Diagnostic.Kind.NOTE,
                    "[garganttua-aot] Generated AOT descriptor: " + generatedFqn);

            // Write listing entry
            writeListingEntry(qualifiedName, generatedFqn);

        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "[garganttua-aot] Failed to generate AOT class for " + qualifiedName + ": " + e.getMessage(),
                    typeElement);
        }
    }

    /**
     * Writes a listing entry to META-INF/garganttua/aot/classes/<fqn>.
     */
    private void writeListingEntry(String originalFqn, String generatedFqn) throws IOException {
        String resourcePath = AOT_CLASSES_DIR + originalFqn;
        FileObject fileObject = processingEnv.getFiler().createResource(
                StandardLocation.CLASS_OUTPUT, "", resourcePath);
        try (Writer writer = fileObject.openWriter();
             BufferedWriter bw = new BufferedWriter(writer)) {
            bw.write(generatedFqn);
            bw.newLine();
        }
    }

    /**
     * Extracts a boolean value from the @Reflected annotation on the given element.
     */
    private boolean getAnnotationBooleanValue(TypeElement typeElement, String attributeName, boolean defaultValue) {
        for (AnnotationMirror mirror : typeElement.getAnnotationMirrors()) {
            TypeElement annoElement = (TypeElement) mirror.getAnnotationType().asElement();
            if (REFLECTED_ANNOTATION.equals(annoElement.getQualifiedName().toString())) {
                for (var entry : mirror.getElementValues().entrySet()) {
                    ExecutableElement key = entry.getKey();
                    AnnotationValue value = entry.getValue();
                    if (attributeName.equals(key.getSimpleName().toString())) {
                        Object val = value.getValue();
                        if (val instanceof Boolean b) {
                            return b;
                        }
                    }
                }
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
