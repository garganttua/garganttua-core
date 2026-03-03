package com.garganttua.core.reflection.constructors;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ReflectionException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConstructorResolver {

    // ========================================================================
    // Provider-based API (preferred)
    // ========================================================================

    public static <T> ResolvedConstructor<T> defaultConstructor(IClass<T> ownerType,
            IReflectionProvider provider) throws ReflectionException {
        log.atDebug().log("[defaultConstructor] Resolving default constructor for {}", ownerType.getName());

        Objects.requireNonNull(ownerType, "Owner type cannot be null");
        Objects.requireNonNull(provider, "Reflection provider cannot be null");

        try {
            IConstructor<T> ctor = ownerType.getDeclaredConstructor();
            log.atDebug().log("[defaultConstructor] Found default constructor for {}", ownerType.getName());
            return new ResolvedConstructor<>(ctor);
        } catch (NoSuchMethodException e) {
            log.atError().log("[defaultConstructor] No default constructor found for {}", ownerType.getName());
            throw new ReflectionException(
                    "No default (no-arg) constructor found for " + ownerType.getName(), e);
        }
    }

    public static <T> ResolvedConstructor<T> constructorByParameterTypes(IClass<T> ownerType,
            IReflectionProvider provider,
            IClass<?>... parameterTypes) throws ReflectionException {
        log.atDebug().log("[constructorByParameterTypes] Resolving constructor for {} with params {}",
                ownerType.getName(), Arrays.toString(parameterTypes));

        Objects.requireNonNull(ownerType, "Owner type cannot be null");
        Objects.requireNonNull(provider, "Reflection provider cannot be null");

        if (parameterTypes == null || parameterTypes.length == 0) {
            return defaultConstructor(ownerType, provider);
        }

        IConstructor<?>[] allCtors = ownerType.getDeclaredConstructors();
        List<IConstructor<T>> matches = new ArrayList<>();

        for (IConstructor<?> ctor : allCtors) {
            if (Constructors.parameterTypesMatch(ctor, parameterTypes)) {
                matches.add((IConstructor<T>) ctor);
            }
        }

        if (matches.isEmpty()) {
            log.atError().log("[constructorByParameterTypes] No matching constructor found for {} with params {}",
                    ownerType.getName(), Arrays.toString(parameterTypes));
            throw new ReflectionException(
                    "No constructor found for " + ownerType.getName()
                            + " with parameter types " + formatTypes(parameterTypes));
        }
        if (matches.size() > 1) {
            log.atError().log("[constructorByParameterTypes] Multiple constructors match for {} with params {}",
                    ownerType.getName(), Arrays.toString(parameterTypes));
            throw new ReflectionException(
                    "Multiple constructors match for " + ownerType.getName()
                            + " with parameter types " + formatTypes(parameterTypes));
        }

        log.atDebug().log("[constructorByParameterTypes] Found matching constructor for {}", ownerType.getName());
        return new ResolvedConstructor<>(matches.get(0));
    }

    @SuppressWarnings("unchecked")
    public static <T> ResolvedConstructor<T> constructorByAnnotation(IClass<T> ownerType,
            IReflectionProvider provider,
            IClass<? extends Annotation> annotation) throws ReflectionException {
        log.atDebug().log("[constructorByAnnotation] Resolving constructor for {} with annotation {}",
                ownerType.getName(), annotation.getName());

        Objects.requireNonNull(ownerType, "Owner type cannot be null");
        Objects.requireNonNull(provider, "Reflection provider cannot be null");
        Objects.requireNonNull(annotation, "Annotation class cannot be null");

        IConstructor<?>[] allCtors = ownerType.getDeclaredConstructors();
        List<IConstructor<T>> matches = new ArrayList<>();

        for (IConstructor<?> ctor : allCtors) {
            if (ctor.isAnnotationPresent(annotation)) {
                matches.add((IConstructor<T>) ctor);
            }
        }

        if (matches.isEmpty()) {
            log.atError().log("[constructorByAnnotation] No constructor annotated with {} found for {}",
                    annotation.getName(), ownerType.getName());
            throw new ReflectionException(
                    "No constructor annotated with @" + annotation.getSimpleName()
                            + " found for " + ownerType.getName());
        }
        if (matches.size() > 1) {
            log.atError().log("[constructorByAnnotation] Multiple constructors annotated with {} found for {}",
                    annotation.getName(), ownerType.getName());
            throw new ReflectionException(
                    "Multiple constructors annotated with @" + annotation.getSimpleName()
                            + " found for " + ownerType.getName());
        }

        log.atDebug().log("[constructorByAnnotation] Found annotated constructor for {}", ownerType.getName());
        return new ResolvedConstructor<>(matches.get(0));
    }

    @SuppressWarnings("unchecked")
    public static <T> List<ResolvedConstructor<T>> allConstructors(IClass<T> ownerType,
            IReflectionProvider provider) throws ReflectionException {
        log.atDebug().log("[allConstructors] Listing all constructors for {}", ownerType.getName());

        Objects.requireNonNull(ownerType, "Owner type cannot be null");
        Objects.requireNonNull(provider, "Reflection provider cannot be null");

        IConstructor<?>[] allCtors = ownerType.getDeclaredConstructors();
        List<ResolvedConstructor<T>> result = new ArrayList<>(allCtors.length);

        for (IConstructor<?> ctor : allCtors) {
            result.add(new ResolvedConstructor<>((IConstructor<T>) ctor));
        }

        log.atDebug().log("[allConstructors] Found {} constructors for {}", result.size(), ownerType.getName());
        return result;
    }

    // ========================================================================
    // Legacy API (without provider) — deprecated
    // ========================================================================

    // ========================================================================
    // Internal
    // ========================================================================

    private ConstructorResolver() {
        /* This utility class should not be instantiated */
    }

    private static String formatTypes(IClass<?>[] types) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            sb.append(types[i] == null ? "null" : types[i].getSimpleName());
            if (i < types.length - 1) sb.append(", ");
        }
        return sb.toString();
    }
}
