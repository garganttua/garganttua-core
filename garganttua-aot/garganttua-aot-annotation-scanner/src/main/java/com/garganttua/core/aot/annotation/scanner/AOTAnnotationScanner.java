package com.garganttua.core.aot.annotation.scanner;

import java.lang.annotation.Annotation;
import java.util.List;

import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.annotations.IAnnotationIndex;

/**
 * An {@link IAnnotationScanner} that reads compile-time generated annotation indices.
 *
 * <p>This scanner looks up pre-built index files in {@code META-INF/garganttua/index/}.
 * It returns results only for annotations that have been indexed at compile time
 * (annotations meta-annotated with {@code @Indexed}).</p>
 *
 * <p>For annotations without an index, this scanner returns empty results.
 * To cover non-indexed annotations, register a fallback scanner (e.g.
 * {@code ReflectionsAnnotationScanner}) at a lower priority in the
 * {@code ReflectionBuilder}:</p>
 *
 * <pre>{@code
 * ReflectionBuilder.builder()
 *     .withProvider(new RuntimeReflectionProvider())
 *     .withScanner(new AOTAnnotationScanner(), 20)      // index-based, high priority
 *     .withScanner(new ReflectionsAnnotationScanner(), 10) // classpath scan, fallback
 *     .build();
 * }</pre>
 *
 * @since 2.0.0-ALPHA02
 */
public class AOTAnnotationScanner implements IAnnotationScanner {

    private final IAnnotationIndex index;

    /**
     * Creates a scanner using the default {@link AnnotationIndex}.
     */
    public AOTAnnotationScanner() {
        this.index = new AnnotationIndex();
    }

    /**
     * Creates a scanner with a custom annotation index.
     *
     * @param index the annotation index to use
     */
    public AOTAnnotationScanner(IAnnotationIndex index) {
        this.index = index;
    }

    @Override
    public List<IClass<?>> getClassesWithAnnotation(IClass<? extends Annotation> annotation) {
        return index.hasIndex(annotation)
                ? index.getClassesWithAnnotation(annotation)
                : List.of();
    }

    @Override
    public List<IClass<?>> getClassesWithAnnotation(String package_, IClass<? extends Annotation> annotation) {
        return index.hasIndex(annotation)
                ? index.getClassesWithAnnotation(annotation, package_)
                : List.of();
    }

    @Override
    public List<IMethod> getMethodsWithAnnotation(IClass<? extends Annotation> annotation) {
        return index.hasIndex(annotation)
                ? index.getMethodsWithAnnotation(annotation)
                : List.of();
    }

    @Override
    public List<IMethod> getMethodsWithAnnotation(String package_, IClass<? extends Annotation> annotation) {
        return index.hasIndex(annotation)
                ? index.getMethodsWithAnnotation(annotation, package_)
                : List.of();
    }

    /**
     * Checks if a compile-time index exists for the specified annotation.
     *
     * @param annotation the annotation type to check
     * @return true if an index exists
     */
    public boolean hasIndex(IClass<? extends Annotation> annotation) {
        return index.hasIndex(annotation);
    }
}
