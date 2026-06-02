package com.garganttua.core.mapper.rules;

import java.util.List;

import com.garganttua.core.observability.Logger;
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

/**
 * Copies a single source field value directly into the matching destination
 * field, without any type conversion or recursive mapping. Used when source and
 * destination field types are compatible leaf types.
 */
public class SimpleFieldMappingExecutor implements IMappingRuleExecutor {
    private static final Logger log = Logger.getLogger(SimpleFieldMappingExecutor.class);

	private IReflection reflection;
	private IField sourceField;
	private IField destinationField;
	private FieldAccessor<Object> sourceFieldAccessor;
	private FieldAccessor<Object> destinationFieldAccessor;

	/**
	 * Creates an executor that copies {@code sourceField} into {@code destinationField}.
	 *
	 * @param reflection the reflection facade used to instantiate the destination
	 * @param sourceField the field to read from the source object
	 * @param destinationField the field to write on the destination object
	 * @throws ReflectionException if the field accessors cannot be resolved
	 */
	public SimpleFieldMappingExecutor(IReflection reflection, IField sourceField, IField destinationField) throws ReflectionException {
		this.reflection = reflection;
		this.sourceField = sourceField;
		this.destinationField = destinationField;
		this.sourceFieldAccessor = (FieldAccessor<Object>) new FieldAccessor<>(
				new ResolvedField(new ObjectAddress(sourceField.getName(), false), List.of(sourceField)));
		this.destinationFieldAccessor = (FieldAccessor<Object>) new FieldAccessor<>(
				new ResolvedField(new ObjectAddress(destinationField.getName(), false), List.of(destinationField)));
	}

	/**
	 * Reads the source field value and writes it as-is onto the destination,
	 * creating the destination instance first if {@code destinationObject} is null.
	 *
	 * @return the populated destination object
	 */
	@Override
	public <destination> destination doMapping(IClass<destination> destinationClass, destination destinationObject, Object sourceObject) throws MapperException {
		log.debug("SimpleField: {} -> {}", this.sourceField.getName(), this.destinationField.getName());

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
			log.error("Failed to map field {} to {}: {}", this.sourceField.getName(), this.destinationField.getName(), e.getMessage());
			throw new MapperException(e);
		}

		return destinationObject;
	}

}
