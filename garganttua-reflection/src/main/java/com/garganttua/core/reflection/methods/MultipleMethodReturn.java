package com.garganttua.core.reflection.methods;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.garganttua.core.reflection.IMethodReturn;

/**
 * Implementation of {@link IMethodReturn} for multiple values.
 *
 * <p>
 * This class represents the result of a method invocation on a collection,
 * array, or map, where the method was invoked on each element.
 * </p>
 *
 * @param <R> the type of the return values
 * @since 2.0.0-ALPHA01
 */
final class MultipleMethodReturn<R> implements IMethodReturn<R> {

    private final List<SingleMethodReturn<R>> returns;
    private final Type type;

    /**
     * Creates a multiple-value method return.
     *
     * @param values the list of return values (must not be null)
     * @throws NullPointerException if values is null
     */
    MultipleMethodReturn(List<R> values) {
        Objects.requireNonNull(values, "Values list cannot be null");
        // Wrap each value in a SingleMethodReturn
        List<SingleMethodReturn<R>> wrapped = new ArrayList<>(values.size());
        for (R value : values) {
            wrapped.add(new SingleMethodReturn<>(value, (Class<R>) (value != null ? value.getClass() : Object.class)));
        }
        this.returns = Collections.unmodifiableList(wrapped);
        // Infer type from first non-null value
        Type inferredType = values.stream()
            .filter(Objects::nonNull)
            .findFirst()
            .map(v -> (Type) v.getClass())
            .orElse(Object.class);
        this.type = inferredType;
    }

    /**
     * Creates a multiple-value method return with explicit type.
     *
     * @param values the list of return values (must not be null)
     * @param type the runtime type of the values
     * @throws NullPointerException if values is null
     */
    MultipleMethodReturn(List<R> values, Class<R> type) {
        Objects.requireNonNull(values, "Values list cannot be null");
        // Wrap each value in a SingleMethodReturn with the provided type
        List<SingleMethodReturn<R>> wrapped = new ArrayList<>(values.size());
        for (R value : values) {
            wrapped.add(new SingleMethodReturn<>(value, type));
        }
        this.returns = Collections.unmodifiableList(wrapped);
        this.type = type != null ? type : Object.class;
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public R single() {
        throw new IllegalStateException(
            "Cannot call single() on multiple values. Use multiple() or first() instead. " +
            "Value count: " + returns.size()
        );
    }

    @Override
    public List<R> multiple() {
        // Extract values from SingleMethodReturn wrappers
        return returns.stream()
            .map(SingleMethodReturn::single)
            .collect(Collectors.toList());
    }

    @Override
    public <U> IMethodReturn<U> map(Function<? super R, ? extends U> mapper) {
        List<U> mapped = returns.stream()
            .map(SingleMethodReturn::single)
            .map(mapper)
            .collect(Collectors.toList());
        return new MultipleMethodReturn<>(mapped);
    }

    @Override
    public Type getSuppliedType() {
        return type;
    }

    /**
     * Returns the list of SingleMethodReturn instances.
     *
     * @return unmodifiable list of SingleMethodReturn instances
     */
    public List<SingleMethodReturn<R>> getReturns() {
        return returns;
    }

    @Override
    public String toString() {
        return "MultipleMethodReturn[count=" + returns.size() + ", type=" + type + ", values=" + multiple() + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MultipleMethodReturn)) return false;
        MultipleMethodReturn<?> other = (MultipleMethodReturn<?>) obj;
        return returns.equals(other.returns);
    }

    @Override
    public int hashCode() {
        return returns.hashCode();
    }

}
