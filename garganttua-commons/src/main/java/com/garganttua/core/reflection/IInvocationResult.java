package com.garganttua.core.reflection;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.garganttua.core.supply.ISupplier;

/**
 * Common super-interface for the result of a reflective invocation
 * (method call, constructor call, or field access) that can yield
 * either a single value or multiple values.
 *
 * <p>
 * {@code IInvocationResult} extends {@link ISupplier}, making every
 * result directly usable with the supply system.  The {@link #supply()}
 * method returns the first value wrapped in an {@link Optional}.
 * </p>
 *
 * <p>
 * Concrete sub-interfaces ({@link IMethodReturn}, {@link IFieldValue})
 * add domain-specific semantics without additional abstract methods.
 * </p>
 *
 * @param <R> the type of the value(s)
 * @since 2.0.0-ALPHA01
 * @see IMethodReturn
 * @see IFieldValue
 * @see ISupplier
 */
public interface IInvocationResult<R> extends ISupplier<R> {

    // --- Cardinality ---

    /**
     * Checks if this result contains a single value.
     *
     * @return true if single, false if multiple
     */
    boolean isSingle();

    /**
     * Checks if this result contains multiple values.
     *
     * @return true if multiple, false if single
     */
    default boolean isMultiple() {
        return !isSingle();
    }

    // --- Value access ---

    /**
     * Returns the single value.
     *
     * @return the single value
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
     * @return list of all values (never null, may be empty)
     */
    List<R> multiple();

    /**
     * Returns the first value.
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

    // --- Iteration ---

    /**
     * Returns a stream of all values.
     *
     * @return stream of all values
     */
    default Stream<R> stream() {
        return multiple().stream();
    }

    /**
     * Returns the number of values.
     *
     * @return 1 for single, or the list size for multiple
     */
    default int size() {
        return isSingle() ? 1 : multiple().size();
    }

    /**
     * Checks if this result is empty.
     *
     * @return true if no values are present
     */
    default boolean isEmpty() {
        return !isSingle() && multiple().isEmpty();
    }

    /**
     * Checks if the value is null.
     *
     * @return true if single is null, or all multiple values are null
     */
    default boolean isNull() {
        if (isSingle()) {
            return single() == null;
        }
        return multiple().stream().allMatch(v -> v == null);
    }

    /**
     * Applies a consumer to all values.
     *
     * @param consumer the consumer to apply
     */
    default void forEach(Consumer<? super R> consumer) {
        stream().forEach(consumer);
    }

    // --- ISupplier integration ---

    /**
     * Supplies the first value wrapped in an Optional.
     *
     * @return Optional containing the first value, or empty
     */
    @Override
    default Optional<R> supply() {
        return firstOptional();
    }

    /**
     * Returns the runtime type of the value.
     *
     * @return the Type representing the value type
     */
    @Override
    Type getSuppliedType();

    // --- Exception support ---

    /**
     * Checks if the invocation threw an exception.
     *
     * @return true if an exception was thrown
     */
    boolean hasException();

    /**
     * Returns the exception thrown during invocation.
     *
     * @return the exception, or null if none
     */
    Throwable getException();

    /**
     * Returns the exception as an Optional.
     *
     * @return Optional containing the exception, or empty
     */
    default Optional<Throwable> getExceptionOptional() {
        return Optional.ofNullable(getException());
    }

    /**
     * Rethrows the exception if one was thrown.
     *
     * @throws Throwable the original exception
     */
    default void rethrow() throws Throwable {
        if (hasException()) {
            throw getException();
        }
    }

    /**
     * Rethrows the exception wrapped in a RuntimeException.
     *
     * @throws RuntimeException wrapping the original exception
     */
    default void rethrowUnchecked() {
        if (hasException()) {
            Throwable ex = getException();
            if (ex instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(ex);
        }
    }
}
