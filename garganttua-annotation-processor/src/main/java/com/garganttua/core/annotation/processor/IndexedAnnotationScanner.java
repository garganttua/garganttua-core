package com.garganttua.core.annotation.processor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.annotations.IAnnotationIndex;

/**
 * An {@link IAnnotationScanner} implementation that uses compile-time generated
 * indices for fast annotation lookups, with optional fallback to a runtime scanner.
 *
 * <p>
 * This scanner first checks if a compile-time index exists for the requested
 * annotation. If found, it uses the index for near-instantaneous lookups.
 * If no index exists, it can optionally delegate to a fallback scanner
 * (typically a runtime classpath scanner like Reflections).
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Index-only scanner (fastest, but requires all annotations to be @Indexed)
 * IAnnotationScanner scanner = new IndexedAnnotationScanner();
 *
 * // Hybrid scanner with fallback
 * IAnnotationScanner fallback = new ReflectionsAnnotationScanner();
 * IAnnotationScanner scanner = new IndexedAnnotationScanner(fallback);
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 */
public class IndexedAnnotationScanner implements IAnnotationScanner {

    private final IAnnotationIndex index;
    private final IAnnotationScanner fallbackScanner;

    /**
     * Creates an index-only scanner without fallback.
     * Annotations without a compile-time index will return empty results.
     */
    public IndexedAnnotationScanner() {
        this(null);
    }

    /**
     * Creates a hybrid scanner with optional fallback.
     *
     * @param fallbackScanner scanner to use when no index exists (may be null)
     */
    public IndexedAnnotationScanner(IAnnotationScanner fallbackScanner) {
        this.index = new AnnotationIndex();
        this.fallbackScanner = fallbackScanner;
    }

    /**
     * Creates a scanner with a custom index and optional fallback.
     *
     * @param index the annotation index to use
     * @param fallbackScanner scanner to use when no index exists (may be null)
     */
    public IndexedAnnotationScanner(IAnnotationIndex index, IAnnotationScanner fallbackScanner) {
        this.index = index;
        this.fallbackScanner = fallbackScanner;
    }

    @Override
    public List<Class<?>> getClassesWithAnnotation(String package_, Class<? extends Annotation> annotation) {
        // Try index first
        if (index.hasIndex(annotation)) {
            return index.getClassesWithAnnotation(annotation, package_);
        }

        // Fallback to runtime scanning
        if (fallbackScanner != null) {
            return fallbackScanner.getClassesWithAnnotation(package_, annotation);
        }

        return List.of();
    }

    @Override
    public List<Method> getMethodsWithAnnotation(String package_, Class<? extends Annotation> annotation) {
        // Try index first
        if (index.hasIndex(annotation)) {
            return index.getMethodsWithAnnotation(annotation, package_);
        }

        // Fallback to runtime scanning
        if (fallbackScanner != null) {
            return fallbackScanner.getMethodsWithAnnotation(package_, annotation);
        }

        return List.of();
    }

    /**
     * Checks if an index exists for the specified annotation.
     *
     * @param annotation the annotation type to check
     * @return true if a compile-time index exists
     */
    public boolean hasIndex(Class<? extends Annotation> annotation) {
        return index.hasIndex(annotation);
    }

    /**
     * Returns whether this scanner has a fallback configured.
     */
    public boolean hasFallback() {
        return fallbackScanner != null;
    }
}
