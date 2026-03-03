package com.garganttua.core.reflection.fields;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IFieldValue;

/**
 * Implementation of {@link IFieldValue} for multiple values.
 *
 * <p>
 * This class represents the result of a field access through a collection,
 * array, or map, where the field was read from each element.
 * </p>
 *
 * @param <F> the type of the field values
 * @since 2.0.0-ALPHA01
 */
public final class MultipleFieldValue<F> implements IFieldValue<F> {

	private final List<SingleFieldValue<F>> values;
	private final IClass<F> type;

	MultipleFieldValue(List<F> values, IClass<F> type) {
		Objects.requireNonNull(values, "Values list cannot be null");
		this.type = Objects.requireNonNull(type, "Type cannot be null");
		List<SingleFieldValue<F>> wrapped = new ArrayList<>(values.size());
		for (F value : values) {
			wrapped.add(new SingleFieldValue<>(value, type));
		}
		this.values = Collections.unmodifiableList(wrapped);
	}

	MultipleFieldValue(IClass<F> type, List<SingleFieldValue<F>> values) {
		Objects.requireNonNull(values, "Values list cannot be null");
		this.values = Collections.unmodifiableList(values);
		this.type = Objects.requireNonNull(type, "Type cannot be null");
	}

	public static <F> MultipleFieldValue<F> of(List<F> values, IClass<F> type) {
		return new MultipleFieldValue<>(values, type);
	}

	public static <F> MultipleFieldValue<F> ofValues(IClass<F> type, List<SingleFieldValue<F>> values) {
		return new MultipleFieldValue<>(type, values);
	}

	@SuppressWarnings("unchecked")
	public static <F> MultipleFieldValue<F> ofFieldValues(List<IFieldValue<F>> fieldValues, IClass<?> type) {
		Objects.requireNonNull(fieldValues, "Field values list cannot be null");
		List<SingleFieldValue<F>> collected = new ArrayList<>();
		for (IFieldValue<F> fv : fieldValues) {
			if (fv.hasException()) {
				collected.add(SingleFieldValue.ofException(fv.getException(), (IClass<F>) type));
			} else if (fv.isSingle()) {
				collected.add(SingleFieldValue.of(fv.single(), (IClass<F>) type));
			} else if (fv instanceof MultipleFieldValue<F> multiple) {
				collected.addAll(multiple.getValues());
			} else {
				for (F value : fv.multiple()) {
					collected.add(SingleFieldValue.of(value, (IClass<F>) type));
				}
			}
		}
		return new MultipleFieldValue<>((IClass<F>) type, collected);
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public F single() {
		throw new IllegalStateException(
				"Cannot call single() on multiple values. Use multiple() or first() instead. "
						+ "Value count: " + values.size());
	}

	@Override
	public List<F> multiple() {
		return values.stream()
				.map(SingleFieldValue::single)
				.toList();
	}

	@Override
	public <U> IFieldValue<U> map(Function<? super F, ? extends U> mapper, IClass<U> targetType) {
		List<U> mapped = values.stream()
				.map(SingleFieldValue::single)
				.map(mapper)
				.collect(Collectors.toList());
		return new MultipleFieldValue<>(mapped, targetType);
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
		return values.stream().anyMatch(SingleFieldValue::hasException);
	}

	@Override
	public Throwable getException() {
		return values.stream()
				.filter(SingleFieldValue::hasException)
				.map(SingleFieldValue::getException)
				.findFirst()
				.orElse(null);
	}

	public List<SingleFieldValue<F>> getValues() {
		return values;
	}

	@Override
	public String toString() {
		return "MultipleFieldValue[count=" + values.size() + ", type=" + type + ", values=" + multiple() + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof MultipleFieldValue<?> other)) return false;
		return values.equals(other.values);
	}

	@Override
	public int hashCode() {
		return values.hashCode();
	}

}
