package com.garganttua.core.reflection.constructors;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ReflectionException;

/**
 * Resolves {@link IConstructor}s of a type by various criteria (default, parameter types, annotation)
 * and wraps the result in a {@link ResolvedConstructor}. Stateless utility class.
 */
public class ConstructorResolver {
    private static final Logger log = Logger.getLogger(ConstructorResolver.class);

    // ========================================================================
    // Provider-based API (preferred)
    // ========================================================================

    /**
     * Resolves the no-argument constructor of {@code ownerType}.
     *
     * @param <T>       the owner type
     * @param ownerType the class whose default constructor is sought
     * @param provider  the reflection provider (must not be {@code null})
     * @return the resolved default constructor
     * @throws ReflectionException if no no-arg constructor exists
     */
    public static <T> ResolvedConstructor<T> defaultConstructor(IClass<T> ownerType,
            IReflectionProvider provider) throws ReflectionException {
        log.debug("[defaultConstructor] Resolving default constructor for {}", ownerType.getName());

        Objects.requireNonNull(ownerType, "Owner type cannot be null");
        Objects.requireNonNull(provider, "Reflection provider cannot be null");

        try {
            IConstructor<T> ctor = ownerType.getDeclaredConstructor();
            log.debug("[defaultConstructor] Found default constructor for {}", ownerType.getName());
            return new ResolvedConstructor<>(ctor);
        } catch (NoSuchMethodException e) {
            log.error("[defaultConstructor] No default constructor found for {}", ownerType.getName());
            throw new ReflectionException(
                    "No default (no-arg) constructor found for " + ownerType.getName(), e);
        }
    }

    /**
     * Resolves the single constructor of {@code ownerType} assignable from the given parameter types.
     * Falls back to {@link #defaultConstructor(IClass, IReflectionProvider)} when no types are supplied.
     *
     * @param <T>            the owner type
     * @param ownerType      the class whose constructor is sought
     * @param provider       the reflection provider (must not be {@code null})
     * @param parameterTypes the parameter types to match
     * @return the resolved constructor
     * @throws ReflectionException if no constructor matches or more than one matches ambiguously
     */
    public static <T> ResolvedConstructor<T> constructorByParameterTypes(IClass<T> ownerType,
            IReflectionProvider provider,
            IClass<?>... parameterTypes) throws ReflectionException {
        log.debug("[constructorByParameterTypes] Resolving constructor for {} with params {}",
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
            log.error("[constructorByParameterTypes] No matching constructor found for {} with params {}",
                    ownerType.getName(), Arrays.toString(parameterTypes));
            throw new ReflectionException(
                    "No constructor found for " + ownerType.getName()
                            + " with parameter types " + formatTypes(parameterTypes));
        }
        if (matches.size() > 1) {
            log.error("[constructorByParameterTypes] Multiple constructors match for {} with params {}",
                    ownerType.getName(), Arrays.toString(parameterTypes));
            throw new ReflectionException(
                    "Multiple constructors match for " + ownerType.getName()
                            + " with parameter types " + formatTypes(parameterTypes));
        }

        log.debug("[constructorByParameterTypes] Found matching constructor for {}", ownerType.getName());
        return new ResolvedConstructor<>(matches.get(0));
    }

    /**
     * Resolves the single constructor of {@code ownerType} bearing the given annotation.
     *
     * @param <T>        the owner type
     * @param ownerType  the class whose constructor is sought
     * @param provider   the reflection provider (must not be {@code null})
     * @param annotation the annotation that must be present on the constructor
     * @return the resolved annotated constructor
     * @throws ReflectionException if no annotated constructor exists or more than one does
     */
    @SuppressWarnings("unchecked")
    public static <T> ResolvedConstructor<T> constructorByAnnotation(IClass<T> ownerType,
            IReflectionProvider provider,
            IClass<? extends Annotation> annotation) throws ReflectionException {
        log.debug("[constructorByAnnotation] Resolving constructor for {} with annotation {}",
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
            log.error("[constructorByAnnotation] No constructor annotated with {} found for {}",
                    annotation.getName(), ownerType.getName());
            throw new ReflectionException(
                    "No constructor annotated with @" + annotation.getSimpleName()
                            + " found for " + ownerType.getName());
        }
        if (matches.size() > 1) {
            log.error("[constructorByAnnotation] Multiple constructors annotated with {} found for {}",
                    annotation.getName(), ownerType.getName());
            throw new ReflectionException(
                    "Multiple constructors annotated with @" + annotation.getSimpleName()
                            + " found for " + ownerType.getName());
        }

        log.debug("[constructorByAnnotation] Found annotated constructor for {}", ownerType.getName());
        return new ResolvedConstructor<>(matches.get(0));
    }

    /**
     * Lists all declared constructors of {@code ownerType}.
     *
     * @param <T>       the owner type
     * @param ownerType the class whose constructors are listed
     * @param provider  the reflection provider (must not be {@code null})
     * @return one {@link ResolvedConstructor} per declared constructor
     * @throws ReflectionException if the constructors cannot be retrieved
     */
    @SuppressWarnings("unchecked")
    public static <T> List<ResolvedConstructor<T>> allConstructors(IClass<T> ownerType,
            IReflectionProvider provider) throws ReflectionException {
        log.debug("[allConstructors] Listing all constructors for {}", ownerType.getName());

        Objects.requireNonNull(ownerType, "Owner type cannot be null");
        Objects.requireNonNull(provider, "Reflection provider cannot be null");

        IConstructor<?>[] allCtors = ownerType.getDeclaredConstructors();
        List<ResolvedConstructor<T>> result = new ArrayList<>(allCtors.length);

        for (IConstructor<?> ctor : allCtors) {
            result.add(new ResolvedConstructor<>((IConstructor<T>) ctor));
        }

        log.debug("[allConstructors] Found {} constructors for {}", result.size(), ownerType.getName());
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
