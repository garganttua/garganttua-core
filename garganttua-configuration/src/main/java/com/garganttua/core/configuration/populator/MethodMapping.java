package com.garganttua.core.configuration.populator;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.garganttua.core.configuration.annotations.ConfigIgnore;
import com.garganttua.core.configuration.annotations.ConfigProperty;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MethodMapping {

    private final MethodMappingStrategy strategy;

    public MethodMapping(MethodMappingStrategy strategy) {
        this.strategy = strategy;
    }

    public Optional<Method> resolve(Class<?> builderClass, String configKey) {
        // 1. Check @ConfigProperty annotations
        var annotated = findByAnnotation(builderClass, configKey);
        if (annotated.isPresent()) {
            return annotated;
        }

        // 2. Direct name match
        var direct = findValidMethod(ObjectReflectionHelper.getMethods(builderClass, configKey));
        if (direct.isPresent()) {
            return direct;
        }

        // 3. With "with" prefix
        var withPrefix = findValidMethod(ObjectReflectionHelper.getMethods(builderClass, "with" + capitalize(configKey)));
        if (withPrefix.isPresent()) {
            return withPrefix;
        }

        if (this.strategy == MethodMappingStrategy.DIRECT) {
            return Optional.empty();
        }

        // 4. camelCase conversion
        var camelCase = findValidMethod(ObjectReflectionHelper.getMethods(builderClass, toCamelCase(configKey)));
        if (camelCase.isPresent()) {
            return camelCase;
        }

        // 5. camelCase with "with" prefix
        var withCamelCase = findValidMethod(ObjectReflectionHelper.getMethods(builderClass, "with" + capitalize(toCamelCase(configKey))));
        if (withCamelCase.isPresent()) {
            return withCamelCase;
        }

        // 6. kebab-case to camelCase
        if (configKey.contains("-")) {
            var fromKebab = kebabToCamelCase(configKey);
            var kebab = findValidMethod(ObjectReflectionHelper.getMethods(builderClass, fromKebab));
            if (kebab.isPresent()) {
                return kebab;
            }
            var withKebab = findValidMethod(ObjectReflectionHelper.getMethods(builderClass, "with" + capitalize(fromKebab)));
            if (withKebab.isPresent()) {
                return withKebab;
            }
        }

        log.atDebug().log("No method found for config key '{}' on {}", configKey, builderClass.getSimpleName());
        return Optional.empty();
    }

    private Optional<Method> findByAnnotation(Class<?> builderClass, String configKey) {
        return Arrays.stream(builderClass.getMethods())
                .filter(m -> !m.isAnnotationPresent(ConfigIgnore.class))
                .filter(m -> m.getParameterCount() >= 1)
                .filter(m -> !m.getDeclaringClass().equals(Object.class))
                .filter(m -> m.isAnnotationPresent(ConfigProperty.class))
                .filter(m -> m.getAnnotation(ConfigProperty.class).value().equals(configKey))
                .findFirst();
    }

    private Optional<Method> findValidMethod(List<Method> methods) {
        return methods.stream()
                .filter(m -> !m.isAnnotationPresent(ConfigIgnore.class))
                .filter(m -> m.getParameterCount() >= 1)
                .filter(m -> !m.getDeclaringClass().equals(Object.class))
                .findFirst();
    }

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
        var sb = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            sb.append(capitalize(parts[i].toLowerCase()));
        }
        return sb.toString();
    }

    static String kebabToCamelCase(String s) {
        if (s == null || !s.contains("-")) {
            return s;
        }
        var parts = s.split("-");
        var sb = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            sb.append(capitalize(parts[i].toLowerCase()));
        }
        return sb.toString();
    }
}
