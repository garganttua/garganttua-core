package com.garganttua.core.reflection.binders;

import java.util.Set;

/**
 * Interface for objects that have dependencies on other types.
 *
 * <p>
 * {@code Dependent} is implemented by binders and other components that require
 * specific types to be available for successful operation. This interface enables
 * dependency tracking and validation, particularly useful in dependency injection
 * scenarios where the framework needs to ensure all required types are registered
 * before attempting to instantiate or invoke dependent components.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // A constructor binder with dependencies
 * IConstructorBinder<UserService> binder = ConstructorBinder
 *     .forClass(UserService.class)
 *     .withParameter(UserRepository.class)
 *     .withParameter(EmailService.class)
 *     .build();
 *
 * // Check dependencies before execution
 * Set<Class<?>> deps = binder.getDependencies();
 * // Returns { UserRepository.class, EmailService.class }
 *
 * for (Class<?> dep : deps) {
 *     if (!context.hasBean(dep)) {
 *         throw new Exception("Missing dependency: " + dep.getName());
 *     }
 * }
 *
 * // Safe to execute now
 * UserService service = binder.execute().orElseThrow();
 * }</pre>
 *
 * <h2>Dependency Management</h2>
 * <p>
 * The returned set includes all types that must be available for the dependent
 * object to function correctly. This includes constructor parameters, method
 * parameters, and field types for binders. The framework uses this information
 * for dependency graph analysis, circular dependency detection, and initialization
 * order determination.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see IExecutableBinder
 * @see IFieldBinder
 */
public interface Dependent {

    /**
     * Returns the set of types this object depends on.
     *
     * <p>
     * The returned set contains all {@link Class} objects representing types that
     * must be available for this dependent to operate successfully. For executable
     * binders, this includes parameter types. For field binders, this includes the
     * field type. An empty set indicates no dependencies.
     * </p>
     *
     * @return an immutable set of dependency types (never {@code null}, may be empty)
     */
    Set<Class<?>> dependencies();
}
