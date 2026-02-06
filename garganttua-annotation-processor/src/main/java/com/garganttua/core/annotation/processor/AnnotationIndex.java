package com.garganttua.core.annotation.processor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.garganttua.core.reflection.annotations.IAnnotationIndex;

/**
 * Runtime implementation of {@link IAnnotationIndex} that reads compile-time
 * generated index files.
 *
 * <p>
 * This implementation provides fast annotation lookups by reading pre-computed
 * index files from {@code META-INF/garganttua/index/}. Index files are
 * generated
 * at compile-time by {@link IndexedAnnotationProcessor}.
 * </p>
 *
 * <h2>Performance Characteristics</h2>
 * <ul>
 * <li>First lookup: O(n) where n is the number of entries in the index</li>
 * <li>Subsequent lookups: O(1) due to caching</li>
 * <li>No classpath scanning required</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is thread-safe. Index loading is performed lazily and cached.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
public class AnnotationIndex implements IAnnotationIndex {

    private static final String INDEX_LOCATION = "META-INF/garganttua/index/";
    private static final String CLASS_PREFIX = "C:";
    private static final String METHOD_PREFIX = "M:";

    /** Cache of loaded indices: annotation FQN -> list of entries */
    private final Map<String, IndexData> indexCache = new ConcurrentHashMap<>();

    /**
     * Holds parsed index data for an annotation.
     */
    private static class IndexData {
        final List<Class<?>> classes = new ArrayList<>();
        final List<Method> methods = new ArrayList<>();
        final boolean loaded;

        IndexData(boolean loaded) {
            this.loaded = loaded;
        }
    }

    @Override
    public List<Class<?>> getClassesWithAnnotation(Class<? extends Annotation> annotation) {
        IndexData data = getOrLoadIndex(annotation);
        return data.loaded ? new ArrayList<>(data.classes) : Collections.emptyList();
    }

    @Override
    public List<Method> getMethodsWithAnnotation(Class<? extends Annotation> annotation) {
        IndexData data = getOrLoadIndex(annotation);
        return data.loaded ? new ArrayList<>(data.methods) : Collections.emptyList();
    }

    @Override
    public List<Class<?>> getClassesWithAnnotation(Class<? extends Annotation> annotation, String packagePrefix) {
        return getClassesWithAnnotation(annotation).stream()
                .filter(clazz -> clazz.getName().startsWith(packagePrefix))
                .toList();
    }

    @Override
    public List<Method> getMethodsWithAnnotation(Class<? extends Annotation> annotation, String packagePrefix) {
        return getMethodsWithAnnotation(annotation).stream()
                .filter(method -> method.getDeclaringClass().getName().startsWith(packagePrefix))
                .toList();
    }

    @Override
    public boolean hasIndex(Class<? extends Annotation> annotation) {
        IndexData data = getOrLoadIndex(annotation);
        return data.loaded;
    }

    /**
     * Gets or loads the index for the specified annotation.
     */
    private IndexData getOrLoadIndex(Class<? extends Annotation> annotation) {
        String annotationFqn = annotation.getName();
        return indexCache.computeIfAbsent(annotationFqn, this::loadIndex);
    }

    /**
     * Loads the index for the specified annotation from classpath resources.
     */
    private IndexData loadIndex(String annotationFqn) {
        String resourcePath = INDEX_LOCATION + annotationFqn;
        IndexData data = new IndexData(false);

        try {
            // Load from all JARs in the classpath (aggregated index)
            Enumeration<URL> resources = Thread.currentThread()
                    .getContextClassLoader()
                    .getResources(resourcePath);

            boolean foundAny = false;
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                foundAny = true;
                loadIndexFromUrl(url, data);
            }

            if (foundAny) {
                return new LoadedIndexData(data.classes, data.methods);
            }
        } catch (IOException e) {
            // Index not found or error reading
        }

        return new IndexData(false);
    }

    /**
     * Loads index entries from a single URL.
     */
    private void loadIndexFromUrl(URL url, IndexData data) {
        try (InputStream is = url.openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith(CLASS_PREFIX)) {
                    String className = line.substring(CLASS_PREFIX.length());
                    try {
                        Class<?> clazz = Class.forName(className, false,
                                Thread.currentThread().getContextClassLoader());
                        data.classes.add(clazz);
                    } catch (ClassNotFoundException e) {
                        // Class no longer exists, skip
                    }
                } else if (line.startsWith(METHOD_PREFIX)) {
                    Method method = parseMethodEntry(line.substring(METHOD_PREFIX.length()));
                    if (method != null) {
                        data.methods.add(method);
                    }
                }
            }
        } catch (IOException e) {
            // Error reading index file
        }
    }

    /**
     * Parses a method entry from the index.
     *
     * @param entry format: "fully.qualified.ClassName#methodName(param1,param2)"
     * @return the Method object, or null if not found
     */
    private Method parseMethodEntry(String entry) {
        int hashIndex = entry.indexOf('#');
        if (hashIndex < 0) {
            return null;
        }

        String className = entry.substring(0, hashIndex);
        String methodPart = entry.substring(hashIndex + 1);

        int parenIndex = methodPart.indexOf('(');
        if (parenIndex < 0) {
            return null;
        }

        String methodName = methodPart.substring(0, parenIndex);
        String paramsPart = methodPart.substring(parenIndex + 1, methodPart.length() - 1);

        try {
            Class<?> clazz = Class.forName(className, false,
                    Thread.currentThread().getContextClassLoader());

            // Find method by name and parameter count/types
            Class<?>[] paramTypes = parseParamTypes(paramsPart);

            if (paramTypes != null) {
                return clazz.getDeclaredMethod(methodName, paramTypes);
            } else {
                // Fallback: find by name and param count
                int expectedParamCount = paramsPart.isEmpty() ? 0 : paramsPart.split(",").length;
                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.getName().equals(methodName) && m.getParameterCount() == expectedParamCount) {
                        return m;
                    }
                }
            }
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // Method no longer exists, skip
        }

        return null;
    }

    /**
     * Parses parameter types from a comma-separated string.
     *
     * @param paramsPart comma-separated parameter type names
     * @return array of Class objects, or null if any type cannot be resolved
     */
    private Class<?>[] parseParamTypes(String paramsPart) {
        if (paramsPart.isEmpty()) {
            return new Class<?>[0];
        }

        String[] typeNames = paramsPart.split(",");
        Class<?>[] types = new Class<?>[typeNames.length];

        for (int i = 0; i < typeNames.length; i++) {
            String typeName = typeNames[i].trim();
            try {
                types[i] = resolveType(typeName);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        return types;
    }

    /**
     * Resolves a type name to a Class object, handling primitives.
     */
    private Class<?> resolveType(String typeName) throws ClassNotFoundException {
        return switch (typeName) {
            case "boolean" -> boolean.class;
            case "byte" -> byte.class;
            case "char" -> char.class;
            case "short" -> short.class;
            case "int" -> int.class;
            case "long" -> long.class;
            case "float" -> float.class;
            case "double" -> double.class;
            case "void" -> void.class;
            default -> Class.forName(typeName, false, Thread.currentThread().getContextClassLoader());
        };
    }

    /**
     * Marker subclass indicating the index was successfully loaded.
     */
    private static class LoadedIndexData extends IndexData {
        LoadedIndexData(List<Class<?>> classes, List<Method> methods) {
            super(true);
            this.classes.addAll(classes);
            this.methods.addAll(methods);
        }
    }
}
