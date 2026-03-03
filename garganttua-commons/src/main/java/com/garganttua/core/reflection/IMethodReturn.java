package com.garganttua.core.reflection;

import java.util.function.Function;

/**
 * Represents the result of a method invocation that can be either a single value
 * or multiple values (when invoked on a collection/array).
 *
 * <p>
 * This interface extends {@link IInvocationResult} and adds method-specific
 * capabilities such as typed mapping and type-checked exception rethrowing.
 * </p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Single result
 * IMethodReturn<String> result = query.invoke("user.getName", String.class);
 * if (result.isSingle()) {
 *     String name = result.single();
 * }
 *
 * // Multiple results
 * IMethodReturn<String> result = query.invoke("users.getName", String.class);
 * result.stream().forEach(System.out::println);
 *
 * // Transformation
 * IMethodReturn<Integer> ages = query.invoke("users.getAge", Integer.class);
 * IMethodReturn<String> ageStrings = ages.map(age -> "Age: " + age);
 * }</pre>
 *
 * @param <R> the type of the return value(s)
 * @since 2.0.0-ALPHA01
 * @see IInvocationResult
 */
public interface IMethodReturn<R> extends IInvocationResult<R> {

    /**
     * Applies a transformation function to all values.
     *
     * @param <U> the type of the transformed values
     * @param mapper the transformation function
     * @param targetType the target IClass type
     * @return a new IMethodReturn with transformed values
     */
    <U> IMethodReturn<U> map(Function<? super R, ? extends U> mapper, IClass<U> targetType);

    /**
     * Rethrows the exception if it is an instance of the specified type.
     *
     * @param <E> the exception type
     * @param exceptionType the class of the exception to rethrow
     * @throws E if the exception is an instance of the specified type
     */
    default <E extends Throwable> void rethrowIfInstanceOf(IClass<E> exceptionType) throws E {
        if (hasException() && exceptionType.isInstance(getException())) {
            throw exceptionType.cast(getException());
        }
    }

}
