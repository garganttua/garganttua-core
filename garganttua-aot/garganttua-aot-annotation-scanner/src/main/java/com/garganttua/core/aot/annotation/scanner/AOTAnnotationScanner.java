package com.garganttua.core.aot.annotation.scanner;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.annotations.IAnnotationIndex;

import jakarta.annotation.Priority;

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
@Priority(20)
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

    /**
     * Returns the indexed classes annotated with the given annotation, including
     * classes carrying an annotation that is itself meta-annotated with it.
     *
     * @param annotation the annotation type to look up
     * @return the matching classes, or an empty list when no index exists
     */
    @Override
    public List<IClass<?>> getClassesWithAnnotation(IClass<? extends Annotation> annotation) {
        List<IClass<?>> direct = index.hasIndex(annotation)
                ? index.getClassesWithAnnotation(annotation)
                : List.of();
        return unionWithMetaAnnotated(annotation, direct, null);
    }

    /**
     * Returns the indexed classes within the given package annotated with the
     * specified annotation, including matches via meta-annotation.
     *
     * @param package_   the package prefix to restrict results to
     * @param annotation the annotation type to look up
     * @return the matching classes, or an empty list when no index exists
     */
    @Override
    public List<IClass<?>> getClassesWithAnnotation(String package_, IClass<? extends Annotation> annotation) {
        List<IClass<?>> direct = index.hasIndex(annotation)
                ? index.getClassesWithAnnotation(annotation, package_)
                : List.of();
        return unionWithMetaAnnotated(annotation, direct, package_);
    }

    /**
     * Returns the indexed methods annotated with the given annotation.
     *
     * @param annotation the annotation type to look up
     * @return the matching methods, or an empty list when no index exists
     */
    @Override
    public List<IMethod> getMethodsWithAnnotation(IClass<? extends Annotation> annotation) {
        return index.hasIndex(annotation)
                ? index.getMethodsWithAnnotation(annotation)
                : List.of();
    }

    /**
     * Returns the indexed methods within the given package annotated with the
     * specified annotation.
     *
     * @param package_   the package prefix to restrict results to
     * @param annotation the annotation type to look up
     * @return the matching methods, or an empty list when no index exists
     */
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

    // ─── Meta-annotation walking ─────────────────────────────────────────
    // The compile-time index files are isolated per annotation FQN — a class
    // annotated @Observer (which is itself meta-annotated @Qualifier) appears
    // ONLY in the Observer index, never in the Qualifier index. The
    // org.reflections-based runtime scanner walks meta-annotations natively;
    // the AOT scanner needs to do this explicitly. Without it, framework code
    // that asks for "classes with @Qualifier" misses every qualifier-marker
    // annotation (Observer, BeanProvider, custom user qualifiers, …).
    //
    // Strategy: on first call, enumerate every index file under
    // META-INF/garganttua/index/, resolve each FQN to a Class<?>, and check
    // whether its annotation declaration carries any other framework
    // annotation as a meta. Cache the resulting "X is meta-annotated with Y"
    // map for subsequent lookups.

    private static final String INDEX_LOCATION = "META-INF/garganttua/index/";

    /** annotation FQN → set of OTHER annotation FQNs that meta-annotate it. */
    private volatile java.util.Map<String, Set<String>> metaAnnotatedBy;

    private List<IClass<?>> unionWithMetaAnnotated(IClass<? extends Annotation> requested,
            List<IClass<?>> directHits, String packageFilter) {
        Set<String> indirect = metaAnnotatedAnnotations(requested.getName());
        if (indirect.isEmpty()) return directHits;
        LinkedHashSet<IClass<?>> merged = new LinkedHashSet<>(directHits);
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = AOTAnnotationScanner.class.getClassLoader();
        for (String metaFqn : indirect) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Annotation> metaCls = (Class<? extends Annotation>)
                        Class.forName(metaFqn, false, cl);
                IClass<? extends Annotation> metaIClass = IClass.getClass(metaCls);
                if (!index.hasIndex(metaIClass)) continue;
                List<IClass<?>> metaHits = packageFilter == null
                        ? index.getClassesWithAnnotation(metaIClass)
                        : index.getClassesWithAnnotation(metaIClass, packageFilter);
                merged.addAll(metaHits);
            } catch (ClassNotFoundException | LinkageError ignored) {
                // Annotation type not on this consumer's classpath — skip.
            }
        }
        return new ArrayList<>(merged);
    }

    /** Returns the set of OTHER annotation FQNs whose declaration carries
     *  {@code requestedAnnotationFqn} as a meta-annotation. */
    private Set<String> metaAnnotatedAnnotations(String requestedAnnotationFqn) {
        return metaIndex().getOrDefault(requestedAnnotationFqn, Set.of());
    }

    private java.util.Map<String, Set<String>> metaIndex() {
        java.util.Map<String, Set<String>> snapshot = metaAnnotatedBy;
        if (snapshot != null) return snapshot;
        synchronized (this) {
            if (metaAnnotatedBy != null) return metaAnnotatedBy;
            metaAnnotatedBy = buildMetaIndex();
            return metaAnnotatedBy;
        }
    }

    /** Walks every META-INF/garganttua/index/* resource, resolves its FQN to
     *  a Class<?>, and records each annotation it carries (other than itself).
     *  Result: "requested meta → set of annotation types that wear it". */
    private java.util.Map<String, Set<String>> buildMetaIndex() {
        java.util.Map<String, Set<String>> result = new ConcurrentHashMap<>();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = AOTAnnotationScanner.class.getClassLoader();
        Set<String> indexedFqns = enumerateIndexedFqns(cl);
        for (String indexedFqn : indexedFqns) {
            Class<?> cls;
            try {
                cls = Class.forName(indexedFqn, false, cl);
            } catch (ClassNotFoundException | LinkageError ignored) {
                continue;
            }
            if (!cls.isAnnotation()) continue;
            for (Annotation meta : cls.getAnnotations()) {
                String metaFqn = meta.annotationType().getName();
                if (metaFqn.equals(indexedFqn)) continue;
                result.computeIfAbsent(metaFqn, k -> ConcurrentHashMap.newKeySet())
                      .add(indexedFqn);
            }
        }
        return result;
    }

    private Set<String> enumerateIndexedFqns(ClassLoader cl) {
        Set<String> fqns = new LinkedHashSet<>();
        try {
            Enumeration<URL> roots = cl.getResources(INDEX_LOCATION);
            while (roots.hasMoreElements()) {
                URL root = roots.nextElement();
                String protocol = root.getProtocol();
                if ("jar".equals(protocol)) {
                    collectFromJar(root, fqns);
                } else if ("file".equals(protocol)) {
                    collectFromDir(root, fqns);
                }
            }
        } catch (IOException ignored) {
            // No index resources at all — meta-walking is a no-op.
        }
        return fqns;
    }

    private void collectFromJar(URL root, Set<String> fqns) {
        try {
            URLConnection conn = root.openConnection();
            if (!(conn instanceof JarURLConnection jar)) return;
            try (JarFile jarFile = jar.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry e = entries.nextElement();
                    String name = e.getName();
                    if (e.isDirectory() || !name.startsWith(INDEX_LOCATION)) continue;
                    if (name.length() <= INDEX_LOCATION.length()) continue;
                    fqns.add(name.substring(INDEX_LOCATION.length()));
                }
            }
        } catch (IOException ignored) {
            // skip this jar
        }
    }

    private void collectFromDir(URL root, Set<String> fqns) {
        try {
            Path dir = Path.of(root.toURI());
            if (!Files.isDirectory(dir)) return;
            try (var stream = Files.list(dir)) {
                stream.forEach(p -> fqns.add(p.getFileName().toString()));
            }
        } catch (Exception ignored) {
            // skip
        }
    }

}
