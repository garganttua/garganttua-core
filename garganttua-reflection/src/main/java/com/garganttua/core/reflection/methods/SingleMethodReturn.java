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
final class SingleMethodReturn<R> implements IMethodReturn<R> {

    private final R value;
    private final Type type;

    /**
     * Creates a single-value method return.
     *
     * @param value the single return value (may be null)
     */
    SingleMethodReturn(R value) {
        this.value = value;
        this.type = value != null ? value.getClass() : Object.class;
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
