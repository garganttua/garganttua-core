package com.garganttua.core.reflection.methods;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.garganttua.core.reflection.IMethodReturn;

/**
 * Implementation of {@link IMethodReturn} for single values.
 *
 * <p>
 * This class represents the result of a method invocation on a single object.
 * </p>
 *
 * @param <R> the type of the return value
 * @since 2.0.0-ALPHA01
 */
public final  class SingleMethodReturn<R> implements IMethodReturn<R> {

    private final R value;
    private final Type type;
    private final Throwable exception;

    /**
     * Creates a single-value method return.
     *
     * @param value the single return value (may be null)
     */
    SingleMethodReturn(R value) {
        this.value = value;
        this.type = value != null ? value.getClass() : Object.class;
        this.exception = null;
    }

    /**
     * Creates a single-value method return with explicit type.
     *
     * @param value the single return value (may be null)
     * @param type the runtime type of the value
     */
    SingleMethodReturn(R value, Class<R> type) {
        this.value = value;
        this.type = type != null ? type : Object.class;
        this.exception = null;
    }

    /**
     * Creates a method return representing an exception.
     *
     * @param exception the exception that was thrown
     * @param type the expected return type
     */
    SingleMethodReturn(Throwable exception, Class<R> type) {
        this.value = null;
        this.type = type != null ? type : Object.class;
        this.exception = exception;
    }

    /**
     * Creates a single-value method return with a successful result.
     *
     * @param <R> the type of the return value
     * @param value the return value (may be null)
     * @return a new SingleMethodReturn containing the value
     */
    public static <R> SingleMethodReturn<R> of(R value) {
        return new SingleMethodReturn<>(value);
    }

    /**
     * Creates a single-value method return with a successful result and explicit type.
     *
     * @param <R> the type of the return value
     * @param value the return value (may be null)
     * @param type the runtime type of the value
     * @return a new SingleMethodReturn containing the value
     */
    public static <R> SingleMethodReturn<R> of(R value, Class<R> type) {
        return new SingleMethodReturn<>(value, type);
    }

    /**
     * Creates a method return representing an exception.
     *
     * @param <R> the type of the expected return value
     * @param exception the exception that was thrown
     * @param type the expected return type
     * @return a new SingleMethodReturn containing the exception
     */
    public static <R> SingleMethodReturn<R> ofException(Throwable exception, Class<R> type) {
        return new SingleMethodReturn<>(exception, type);
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public R single() {
        return value;
    }

    @Override
    public List<R> multiple() {
        return Collections.singletonList(value);
    }

    @Override
    public <U> IMethodReturn<U> map(Function<? super R, ? extends U> mapper) {
        return new SingleMethodReturn<>(mapper.apply(value));
    }

    @Override
    public Type getSuppliedType() {
        return type;
    }

    @Override
    public boolean hasException() {
        return exception != null;
    }

    @Override
    public Throwable getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "SingleMethodReturn[value=" + value + ", type=" + type + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SingleMethodReturn)) return false;
        SingleMethodReturn<?> other = (SingleMethodReturn<?>) obj;
        return value != null ? value.equals(other.value) : other.value == null;
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

}
