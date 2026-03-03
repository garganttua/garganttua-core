package com.garganttua.core.mapper.rules;

import java.util.List;
import java.util.function.Function;

import com.garganttua.core.mapper.IMappingRuleExecutor;
import com.garganttua.core.mapper.MapperException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.fields.FieldAccessor;
import com.garganttua.core.reflection.fields.ResolvedField;
import com.garganttua.core.reflection.fields.SingleFieldValue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ImplicitConversionMappingExecutor implements IMappingRuleExecutor {

	private final IReflection reflection;
	private final IField sourceField;
	private final IField destinationField;
	private final Function<Object, Object> converter;
	private final FieldAccessor<Object> sourceFieldAccessor;
	private final FieldAccessor<Object> destinationFieldAccessor;

	public ImplicitConversionMappingExecutor(IReflection reflection, IField sourceField, IField destinationField,
			Function<Object, Object> converter) throws ReflectionException {
		this.reflection = reflection;
		this.sourceField = sourceField;
		this.destinationField = destinationField;
		this.converter = converter;
		this.sourceFieldAccessor = (FieldAccessor<Object>) new FieldAccessor<>(
				new ResolvedField(new ObjectAddress(sourceField.getName(), false), List.of(sourceField)));
		this.destinationFieldAccessor = (FieldAccessor<Object>) new FieldAccessor<>(
				new ResolvedField(new ObjectAddress(destinationField.getName(), false), List.of(destinationField)));
	}

	@Override
	public <destination> destination doMapping(IClass<destination> destinationClass, destination destinationObject,
			Object sourceObject) throws MapperException {
		log.atDebug().log("ImplicitConversion: {} ({}) -> {} ({})", this.sourceField.getName(),
				this.sourceField.getType().getSimpleName(), this.destinationField.getName(),
				this.destinationField.getType().getSimpleName());

		if (destinationObject == null) {
			try {
				destinationObject = this.reflection.newInstance(destinationClass);
			} catch (ReflectionException e) {
				throw new MapperException(e);
			}
		}

		try {
			Object sourceValue = this.sourceFieldAccessor.getValue(sourceObject).single();
			Object convertedValue = this.converter.apply(sourceValue);
			this.destinationFieldAccessor.setValue(destinationObject,
					SingleFieldValue.of(convertedValue, (IClass<Object>) this.destinationField.getType()));
		} catch (ReflectionException e) {
			log.atError().log("Implicit conversion failed {} -> {}: {}", this.sourceField.getName(),
					this.destinationField.getName(), e.getMessage());
			throw new MapperException(e);
		} catch (Exception e) {
			log.atError().log("Conversion error {} -> {}: {}", this.sourceField.getName(),
					this.destinationField.getName(), e.getMessage());
			throw new MapperException("Implicit conversion failed: " + e.getMessage(),
					e instanceof Exception ex ? ex : new RuntimeException(e));
		}

		return destinationObject;
	}
}
