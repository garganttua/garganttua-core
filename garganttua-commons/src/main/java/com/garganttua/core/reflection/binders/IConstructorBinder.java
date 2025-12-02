package com.garganttua.core.reflection.binders;

import java.lang.reflect.Constructor;

/**
 * Binder interface for reflective constructor invocation and object instantiation.
 *
 * <p>
 * {@code IConstructorBinder} specializes {@link IExecutableBinder} for object
 * construction scenarios. It provides a type-safe abstraction for instantiating
 * objects via reflection, handling parameter binding, constructor resolution,
 * and instance creation. This is particularly useful for dependency injection
 * frameworks and factory patterns.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // No-argument constructor
 * IConstructorBinder<StringBuilder> simpleConstructor = ConstructorBinder
 *     .forClass(StringBuilder.class)
 *     .build();
 *
 * Optional<StringBuilder> instance1 = simpleConstructor.execute();
 * // Returns Optional containing new StringBuilder()
 *
 * // Parameterized constructor
 * IConstructorBinder<Database> dbConstructor = ConstructorBinder
 *     .forClass(Database.class)
 *     .withParameter(String.class, "jdbc:mysql://localhost:3306/mydb")
 *     .withParameter(int.class, 3306)
 *     .build();
 *
 * Optional<Database> db = dbConstructor.execute();
 * // Returns Optional containing new Database("jdbc:...", 3306)
 *
 * // Check dependencies before instantiation
 * Class<Database> type = dbConstructor.getConstructedType();
 * Set<Class<?>> deps = dbConstructor.getDependencies();
 * // Returns parameter types { String.class, int.class }
 * }</pre>
 *
 * <h2>Constructor Resolution</h2>
 * <p>
 * Constructor binders resolve the target constructor based on parameter types.
 * For classes with multiple constructors, the parameter types must exactly match
 * the desired constructor signature. The binder validates parameter compatibility
 * at build time when possible.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Constructor binders are typically thread-safe if configured with immutable
 * parameters. Each invocation creates a new instance, making the binder itself
 * stateless and safe for concurrent use.
 * </p>
 *
 * @param <Constructed> the type of object this constructor creates
 * @since 2.0.0-ALPHA01
 * @see IExecutableBinder
 * @see IContextualConstructorBinder
 */
public interface IConstructorBinder<Constructed> extends IExecutableBinder<Constructed> {

    /**
     * Returns the type of object this constructor creates.
     *
     * <p>
     * This method provides type information about the class being instantiated,
     * enabling type-based logic and validation without requiring an actual
     * instance to be created.
     * </p>
     *
     * @return the {@link Class} object representing the constructed type
     */
    Class<Constructed> getConstructedType();

    /**
     * Returns the underlying Java constructor that this binder will invoke.
     *
     * <p>
     * This method provides direct access to the {@link Constructor} object that
     * will be used for object instantiation. This is useful for:
     * </p>
     * <ul>
     *   <li>Inspecting constructor metadata (annotations, modifiers, parameter types)</li>
     *   <li>Performing additional validation or security checks</li>
     *   <li>Integrating with other reflection-based frameworks</li>
     *   <li>Debugging and diagnostic purposes</li>
     * </ul>
     *
     * <p>
     * The returned constructor is the one that was resolved during binder
     * configuration based on the specified parameter types.
     * </p>
     *
     * @return the {@link Constructor} object that will be invoked during execution
     * @since 2.0.0-ALPHA01
     */
    Constructor<?> constructor();

}
