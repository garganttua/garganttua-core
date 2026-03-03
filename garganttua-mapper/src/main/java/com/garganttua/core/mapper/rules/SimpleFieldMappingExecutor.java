package com.garganttua.core.mapper.rules;

import java.util.List;

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
public class SimpleFieldMappingExecutor implements IMappingRuleExecutor {

	private IReflection reflection;
	private IField sourceField;
	private IField destinationField;
	private FieldAccessor<Object> sourceFieldAccessor;
	private FieldAccessor<Object> destinationFieldAccessor;

	public SimpleFieldMappingExecutor(IReflection reflection, IField sourceField, IField destinationField) throws ReflectionException {
		this.reflection = reflection;
		this.sourceField = sourceField;
		this.destinationField = destinationField;
		this.sourceFieldAccessor = (FieldAccessor<Object>) new FieldAccessor<>(
				new ResolvedField(new ObjectAddress(sourceField.getName(), false), List.of(sourceField)));
		this.destinationFieldAccessor = (FieldAccessor<Object>) new FieldAccessor<>(
				new ResolvedField(new ObjectAddress(destinationField.getName(), false), List.of(destinationField)));
	}

	@Override
	public <destination> destination doMapping(IClass<destination> destinationClass, destination destinationObject, Object sourceObject) throws MapperException {
		log.atDebug().log("SimpleField: {} -> {}", this.sourceField.getName(), this.destinationField.getName());

		if( destinationObject == null ) {
			try {
				destinationObject = this.reflection.newInstance(destinationClass);
			} catch (ReflectionException e) {
				throw new MapperException(e);
			}
		}

		try {
			Object sourceValue = this.sourceFieldAccessor.getValue(sourceObject).single();
			this.destinationFieldAccessor.setValue(destinationObject,
					SingleFieldValue.of(sourceValue, (IClass<Object>) this.destinationField.getType()));
		} catch (ReflectionException e) {
			log.atError().log("Failed to map field {} to {}: {}", this.sourceField.getName(), this.destinationField.getName(), e.getMessage());
			throw new MapperException(e);
		}

		return destinationObject;
	}

}
