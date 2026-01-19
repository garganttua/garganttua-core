package com.garganttua.core.reflection;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.garganttua.core.supply.ISupplier;

/**
 * Represents the result of a method invocation that can be either a single value
 * or multiple values (when invoked on a collection/array).
 *
 * <p>
 * This interface provides a unified way to handle method invocation results without
 * requiring the caller to know in advance whether the method was invoked on a single
 * object or on a collection of objects.
 * </p>
 *
 * <p>
 * {@code IMethodReturn} extends {@link ISupplier}, making it compatible with the
 * entire supply system. The {@link #supply()} method returns the first value
 * (or only value for single results) wrapped in an Optional.
 * </p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Example 1: Single result
 * IMethodReturn<String> result = query.invoke("user.getName", String.class);
 * if (result.isSingle()) {
 *     String name = result.single();
 *     System.out.println("Name: " + name);
 * }
 *
 * // Example 2: Multiple results (invoked on collection)
 * IMethodReturn<String> result = query.invoke("users.getName", String.class);
 * if (result.isMultiple()) {
 *     List<String> names = result.multiple();
 *     names.forEach(System.out::println);
 * }
 *
 * // Example 3: Unified handling
 * IMethodReturn<String> result = query.invoke(address, String.class);
 * result.stream().forEach(System.out::println);  // Works for both cases
 *
 * // Example 4: Transformation
 * IMethodReturn<Integer> ages = query.invoke("users.getAge", Integer.class);
 * IMethodReturn<String> ageStrings = ages.map(age -> "Age: " + age);
 *
 * // Example 5: Using as ISupplier
 * IMethodReturn<String> result = query.invoke("user.getName", String.class);
 * Optional<String> supplied = result.supply();  // Returns first() as Optional
 * }</pre>
 *
 * @param <R> the type of the return value(s)
 * @since 2.0.0-ALPHA01
 * @see ISupplier
 */
public interface IMethodReturn<R> extends ISupplier<R> {

    /**
     * Checks if this return contains a single value.
     *
     * @return true if this contains a single value, false if multiple
     */
    boolean isSingle();

    /**
     * Checks if this return contains multiple values.
     *
     * @return true if this contains multiple values, false if single
     */
    default boolean isMultiple() {
        return !isSingle();
    }

    /**
     * Returns the single value.
     *
     * <p>
     * <b>Note:</b> This method should only be called when {@link #isSingle()} returns true.
     * For multiple values, use {@link #multiple()} instead.
     * </p>
     *
     * @return the single return value
     * @throws IllegalStateException if this contains multiple values
     */
    R single();

    /**
     * Returns the single value as an Optional.
     *
     * @return Optional containing the single value, or empty if multiple values
     */
    default Optional<R> singleOptional() {
        return isSingle() ? Optional.ofNullable(single()) : Optional.empty();
    }

    /**
     * Returns all values as a list.
     *
     * <p>
     * If this contains a single value, returns a list with one element.
     * If this contains multiple values, returns all of them.
     * </p>
     *
     * @return list of all return values (never null, may be empty)
     */
    List<R> multiple();

    /**
     * Returns the first value, whether single or from multiple values.
     *
     * @return the first (or only) value, or null if no values
     */
    default R first() {
        if (isSingle()) {
            return single();
        }
        List<R> list = multiple();
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Returns the first value as an Optional.
     *
     * @return Optional containing the first value, or empty if no values
     */
    default Optional<R> firstOptional() {
        return Optional.ofNullable(first());
    }

    /**
     * Returns a stream of all values.
     *
     * <p>
     * This provides a unified way to iterate over results regardless of whether
     * they are single or multiple.
     * </p>
     *
     * @return stream of all return values
     */
    default Stream<R> stream() {
        return multiple().stream();
    }

    /**
     * Returns the number of values.
     *
     * @return 1 for single value, or the size of the list for multiple values
     */
    default int size() {
        return isSingle() ? 1 : multiple().size();
    }

    /**
     * Checks if this return is empty (no values).
     *
     * @return true if no values are present
     */
    default boolean isEmpty() {
        return !isSingle() && multiple().isEmpty();
    }

    /**
     * Checks if the return value is null.
     *
     * <p>
     * For single values, returns true if the value is null.
     * For multiple values, returns true if all values are null.
     * </p>
     *
     * @return true if the value(s) is/are null
     */
    default boolean isNull() {
        if (isSingle()) {
            return single() == null;
        }
        return multiple().stream().allMatch(v -> v == null);
    }

    /**
     * Applies a transformation function to all values.
     *
     * @param <U> the type of the transformed values
     * @param mapper the transformation function
     * @return a new MethodReturn with transformed values
     */
    <U> IMethodReturn<U> map(Function<? super R, ? extends U> mapper);

    /**
     * Applies a consumer to all values.
     *
     * @param consumer the consumer to apply
     */
    default void forEach(Consumer<? super R> consumer) {
        stream().forEach(consumer);
    }

    /**
     * Supplies the first value wrapped in an Optional.
     *
     * <p>
     * This method implements {@link ISupplier#supply()} and returns the first value
     * (or the only value for single results). For single values, returns the value
     * wrapped in Optional. For multiple values, returns the first element.
     * Returns empty Optional if there are no values.
     * </p>
     *
     * @return Optional containing the first value, or empty if no values
     */
    @Override
    default Optional<R> supply() {
        return firstOptional();
    }

    /**
     * Returns the runtime type of the return value.
     *
     * <p>
     * This method must be implemented to provide type introspection for the
     * {@link ISupplier} interface. It should return the {@link Type} of the
     * return value(s).
     * </p>
     *
     * @return the Type representing the return value type
     */
    @Override
    Type getSuppliedType();

    /**
     * Checks if the method invocation threw an exception.
     *
     * @return true if an exception was thrown, false otherwise
     */
    boolean hasException();

    /**
     * Returns the exception that was thrown during method invocation.
     *
     * @return the exception, or null if no exception was thrown
     */
    Throwable getException();

    /**
     * Returns the exception as an Optional.
     *
     * @return Optional containing the exception, or empty if no exception was thrown
     */
    default Optional<Throwable> getExceptionOptional() {
        return Optional.ofNullable(getException());
    }

    /**
     * Rethrows the exception if one was thrown during method invocation.
     *
     * <p>
     * This method allows callers to propagate the original exception after
     * checking for its presence with {@link #hasException()}.
     * </p>
     *
     * @throws Throwable the original exception if one was thrown
     */
    default void rethrow() throws Throwable {
        if (hasException()) {
            throw getException();
        }
    }

    /**
     * Rethrows the exception wrapped in a RuntimeException if one was thrown.
     *
     * <p>
     * This is useful when you want to propagate the exception without declaring
     * a throws clause.
     * </p>
     *
     * @throws RuntimeException wrapping the original exception if one was thrown
     */
    default void rethrowUnchecked() {
        if (hasException()) {
            Throwable ex = getException();
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            throw new RuntimeException(ex);
        }
    }

    /**
     * Rethrows the exception if it is an instance of the specified type.
     *
     * @param <E> the exception type
     * @param exceptionType the class of the exception to rethrow
     * @throws E if the exception is an instance of the specified type
     */
    default <E extends Throwable> void rethrowIfInstanceOf(Class<E> exceptionType) throws E {
        if (hasException() && exceptionType.isInstance(getException())) {
            throw exceptionType.cast(getException());
        }
    }

}
