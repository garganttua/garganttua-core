package com.garganttua.core.reflection.binders;

import java.util.Optional;

import com.garganttua.core.reflection.ReflectionException;

/**
 * Base interface for binders that execute methods or constructors via reflection.
 *
 * <p>
 * {@code IExecutableBinder} provides a unified abstraction for invoking executable
 * elements (methods and constructors) through reflection. It encapsulates parameter
 * binding, invocation, and return value handling, offering a type-safe API for
 * reflective execution. The binder tracks its dependencies and provides a reference
 * identifier for debugging and logging purposes.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Method binder
 * IExecutableBinder<String> methodBinder = MethodBinder
 *     .forClass(StringUtils.class)
 *     .method("concat")
 *     .withParameter(String.class, "Hello")
 *     .withParameter(String.class, "World")
 *     .build();
 *
 * Optional<String> result = methodBinder.execute();
 * // Returns Optional.of("HelloWorld")
 *
 * // Constructor binder
 * IExecutableBinder<Database> constructorBinder = ConstructorBinder
 *     .forClass(Database.class)
 *     .withParameter(String.class, "jdbc:mysql://localhost:3306/db")
 *     .build();
 *
 * Optional<Database> db = constructorBinder.execute();
 * // Returns Optional containing new Database instance
 * }</pre>
 *
 * <h2>Return Value Semantics</h2>
 * <ul>
 *   <li>{@link Optional#empty()} - For void methods/constructors, or when execution
 *       yields no result</li>
 *   <li>{@link Optional#of(Object)} - For successful execution with a return value</li>
 *   <li>{@link ReflectionException} - For execution failures or reflection errors</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Thread safety depends on the implementation and the target method/constructor.
 * Stateless binders are typically thread-safe, but stateful binders or those
 * invoking non-thread-safe methods may require external synchronization.
 * </p>
 *
 * @param <ExecutionReturn> the return type of the executable element
 * @since 2.0.0-ALPHA01
 * @see IMethodBinder
 * @see IConstructorBinder
 * @see IContextualExecutableBinder
 * @see Dependent
 */
public interface IExecutableBinder<ExecutionReturn> extends Dependent {

    /**
     * Returns a string reference identifying the executable element.
     *
     * <p>
     * This reference is primarily used for debugging, logging, and error messages.
     * The format is implementation-specific but typically includes the class name,
     * method/constructor name, and parameter types.
     * </p>
     *
     * @return a string identifying the bound executable (never {@code null})
     */
    String getExecutableReference();

    /**
     * Executes the bound method or constructor.
     *
     * <p>
     * This method invokes the underlying executable element using pre-configured
     * parameters. For methods, the target instance (if instance method) and
     * parameters are used from the binder configuration. For constructors, a new
     * instance is created and returned.
     * </p>
     *
     * @return an {@link Optional} containing the execution result, or empty for void
     *         methods or when no result is produced
     * @throws ReflectionException if the execution fails due to illegal access,
     *                            invocation target exceptions, instantiation errors,
     *                            or parameter mismatch
     */
    Optional<ExecutionReturn> execute() throws ReflectionException;

}
