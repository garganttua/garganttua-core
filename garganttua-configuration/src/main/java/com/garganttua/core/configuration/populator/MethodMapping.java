package com.garganttua.core.configuration.populator;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.configuration.annotations.ConfigIgnore;
import com.garganttua.core.configuration.annotations.ConfigProperty;
import com.garganttua.core.dsl.IBuilder;

/**
 * Resolves a configuration key to a builder {@link Method} using the configured
 * {@link MethodMappingStrategy}, honouring {@link ConfigProperty} and {@link ConfigIgnore}
 * annotations and falling back through direct, {@code with}-prefixed, camelCase and
 * kebab-case name variants.
 */
public class MethodMapping {
    private static final Logger log = Logger.getLogger(MethodMapping.class);

    private final MethodMappingStrategy strategy;

    /**
     * Creates a mapping resolver using the given strategy.
     *
     * @param strategy the name-matching strategy to apply
     */
    public MethodMapping(MethodMappingStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Finds the builder method that best matches a configuration key.
     *
     * @param builderClass the builder type to search
     * @param configKey    the configuration key to map
     * @return the matching method, or empty if none matches
     */
    public Optional<Method> resolve(Class<?> builderClass, String configKey) {
        // 1. Check @ConfigProperty annotations
        var annotated = findByAnnotation(builderClass, configKey);
        if (annotated.isPresent()) {
            return annotated;
        }

        // 2. Direct name match
        var direct = findValidMethod(getMethodsByName(builderClass, configKey));
        if (direct.isPresent()) {
            return direct;
        }

        // 3. With "with" prefix
        var withPrefix = findValidMethod(getMethodsByName(builderClass, "with" + capitalize(configKey)));
        if (withPrefix.isPresent()) {
            return withPrefix;
        }

        if (this.strategy == MethodMappingStrategy.DIRECT) {
            return Optional.empty();
        }

        // 4. camelCase conversion
        var camelCase = findValidMethod(getMethodsByName(builderClass, toCamelCase(configKey)));
        if (camelCase.isPresent()) {
            return camelCase;
        }

        // 5. camelCase with "with" prefix
        var withCamelCase = findValidMethod(getMethodsByName(builderClass, "with" + capitalize(toCamelCase(configKey))));
        if (withCamelCase.isPresent()) {
            return withCamelCase;
        }

        // 6. kebab-case to camelCase
        if (configKey.contains("-")) {
            var fromKebab = kebabToCamelCase(configKey);
            var kebab = findValidMethod(getMethodsByName(builderClass, fromKebab));
            if (kebab.isPresent()) {
                return kebab;
            }
            var withKebab = findValidMethod(getMethodsByName(builderClass, "with" + capitalize(fromKebab)));
            if (withKebab.isPresent()) {
                return withKebab;
            }
        }

        log.debug("No method found for config key '{}' on {}", configKey, builderClass.getSimpleName());
        return Optional.empty();
    }

    private static List<Method> getMethodsByName(Class<?> clazz, String name) {
        return Arrays.stream(clazz.getMethods())
                .filter(m -> m.getName().equals(name))
                .collect(Collectors.toList());
    }

    private Optional<Method> findByAnnotation(Class<?> builderClass, String configKey) {
        return Arrays.stream(builderClass.getMethods())
                .filter(m -> !m.isAnnotationPresent(ConfigIgnore.class))
                .filter(MethodMapping::isMappable)
                .filter(m -> !m.getDeclaringClass().equals(Object.class))
                .filter(m -> m.isAnnotationPresent(ConfigProperty.class))
                .filter(m -> m.getAnnotation(ConfigProperty.class).value().equals(configKey))
                .findFirst();
    }

    private Optional<Method> findValidMethod(List<Method> methods) {
        return methods.stream()
                .filter(m -> !m.isAnnotationPresent(ConfigIgnore.class))
                .filter(MethodMapping::isMappable)
                .filter(m -> !m.getDeclaringClass().equals(Object.class))
                // prefer a value-consuming overload (>=1 arg) over a 0-arg flag/opener of the same name
                .sorted((a, b) -> Integer.compare(b.getParameterCount() == 0 ? 0 : 1,
                        a.getParameterCount() == 0 ? 0 : 1))
                .findFirst();
    }

    /**
     * A method is mappable from config when it either consumes a value (≥1 parameter) or is a
     * recognisable <em>no-arg config target</em>: a flag setter (returns {@code void} or the
     * builder) or a no-arg child-builder opener (returns an {@link IBuilder}). Structural ascent
     * and terminal methods ({@code build}, {@code up}, {@code setUp}, {@code and}) and plain
     * getters (which return data, not {@code void}/a builder) are excluded so config keys never
     * accidentally invoke them.
     */
    private static boolean isMappable(Method m) {
        if (m.getParameterCount() >= 1) {
            return true;
        }
        if (STRUCTURAL_METHODS.contains(m.getName())) {
            return false;
        }
        var rt = m.getReturnType();
        return rt == void.class || IBuilder.class.isAssignableFrom(rt);
    }

    private static final java.util.Set<String> STRUCTURAL_METHODS = java.util.Set.of(
            "build", "up", "setUp", "and");

    static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static String toCamelCase(String s) {
        if (s == null || !s.contains("_") && !s.contains(".")) {
            return s;
        }
        var parts = s.split("[_.]");
        var sb = new StringBuilder(parts[0].toLowerCase(java.util.Locale.ROOT));
        for (int i = 1; i < parts.length; i++) {
            sb.append(capitalize(parts[i].toLowerCase(java.util.Locale.ROOT)));
        }
        return sb.toString();
    }

    static String kebabToCamelCase(String s) {
        if (s == null || !s.contains("-")) {
            return s;
        }
        var parts = s.split("-");
        var sb = new StringBuilder(parts[0].toLowerCase(java.util.Locale.ROOT));
        for (int i = 1; i < parts.length; i++) {
            sb.append(capitalize(parts[i].toLowerCase(java.util.Locale.ROOT)));
        }
        return sb.toString();
    }
}
