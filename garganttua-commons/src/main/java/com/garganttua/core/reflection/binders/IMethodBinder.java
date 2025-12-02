package com.garganttua.core.reflection.binders;

/**
 * Binder interface for reflective method invocation.
 *
 * <p>
 * {@code IMethodBinder} specializes {@link IExecutableBinder} for method invocation
 * scenarios. It provides a type-safe abstraction for invoking methods (static or
 * instance) via reflection, handling parameter binding, target instance management,
 * and return value extraction.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Static method invocation
 * IMethodBinder<Integer> staticMethod = MethodBinder
 *     .forClass(Math.class)
 *     .method("max")
 *     .withParameter(int.class, 10)
 *     .withParameter(int.class, 20)
 *     .build();
 *
 * Optional<Integer> result = staticMethod.execute();
 * // Returns Optional.of(20)
 *
 * // Instance method invocation
 * StringBuilder target = new StringBuilder("Hello");
 * IMethodBinder<StringBuilder> instanceMethod = MethodBinder
 *     .forInstance(target)
 *     .method("append")
 *     .withParameter(String.class, " World")
 *     .build();
 *
 * Optional<StringBuilder> updated = instanceMethod.execute();
 * // Returns Optional containing the StringBuilder with "Hello World"
 * }</pre>
 *
 * <h2>Method Resolution</h2>
 * <p>
 * Method binders resolve target methods based on name and parameter types. For
 * overloaded methods, the exact parameter types must match the target method
 * signature. The binder handles both static and instance methods, automatically
 * determining the invocation mode based on the builder configuration.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * The binder itself is typically thread-safe if configured with immutable parameters.
 * However, thread safety of the actual invocation depends on the target method and
 * instance (if applicable).
 * </p>
 *
 * @param <ExecutionReturned> the return type of the bound method
 * @since 2.0.0-ALPHA01
 * @see IExecutableBinder
 * @see IContextualMethodBinder
 */
public interface IMethodBinder<ExecutionReturned> extends IExecutableBinder<ExecutionReturned> {

}
