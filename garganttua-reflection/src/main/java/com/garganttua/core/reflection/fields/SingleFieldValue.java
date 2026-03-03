package com.garganttua.core.reflection.fields;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IFieldValue;

/**
 * Implementation of {@link IFieldValue} for single values.
 *
 * @param <F> the type of the field value
 * @since 2.0.0-ALPHA01
 */
public final class SingleFieldValue<F> implements IFieldValue<F> {

	private final F value;
	private final IClass<F> type;
	private final Throwable exception;

	SingleFieldValue(F value, IClass<F> type) {
		this.value = value;
		this.type = Objects.requireNonNull(type, "type cannot be null");
		this.exception = null;
	}

	SingleFieldValue(Throwable exception, IClass<F> type) {
		this.value = null;
		this.type = Objects.requireNonNull(type, "type cannot be null");
		this.exception = Objects.requireNonNull(exception, "exception cannot be null");
	}

	public static <F> SingleFieldValue<F> of(F value, IClass<F> type) {
		return new SingleFieldValue<>(value, type);
	}

	public static <F> SingleFieldValue<F> ofException(Throwable exception, IClass<F> type) {
		return new SingleFieldValue<>(exception, type);
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public F single() {
		return value;
	}

	@Override
	public List<F> multiple() {
		return Collections.singletonList(value);
	}

	@Override
	public <U> IFieldValue<U> map(Function<? super F, ? extends U> mapper, IClass<U> targetType) {
		U mapped = mapper.apply(value);
		return new SingleFieldValue<>(mapped, targetType);
	}

	@Override
	public Type getSuppliedType() {
		return this.type.getType();
	}

	@Override
	public IClass<F> getSuppliedClass() {
		return this.type;
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
		return "SingleFieldValue[value=" + value + ", type=" + type + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof SingleFieldValue<?> other)) return false;
		return value != null ? value.equals(other.value) : other.value == null;
	}

	@Override
	public int hashCode() {
		return value != null ? value.hashCode() : 0;
	}

}
