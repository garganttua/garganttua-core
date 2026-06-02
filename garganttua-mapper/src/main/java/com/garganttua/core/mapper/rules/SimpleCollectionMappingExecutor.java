package com.garganttua.core.mapper.rules;

import java.util.Collection;
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
 * Copies a collection field by reference (shallow), reusing the source elements
 * without mapping them individually. Used when source and destination element
 * types are compatible and no per-element transformation is required.
 */
public class SimpleCollectionMappingExecutor implements IMappingRuleExecutor {
    private static final Logger log = Logger.getLogger(SimpleCollectionMappingExecutor.class);

	private IReflection reflection;
	private IField sourceField;
	private IField destinationField;
	private FieldAccessor<Object> sourceFieldAccessor;
	private FieldAccessor<Object> destinationFieldAccessor;

	/**
	 * Creates an executor that shallow-copies the collection in {@code sourceField}
	 * into {@code destinationField}.
	 *
	 * @param reflection the reflection facade used to instantiate the destination
	 * @param sourceField the collection field to read from the source object
	 * @param destinationField the collection field to write on the destination object
	 * @throws ReflectionException if the field accessors cannot be resolved
	 */
	public SimpleCollectionMappingExecutor(IReflection reflection, IField sourceField, IField destinationField) throws ReflectionException {
		this.reflection = reflection;
		this.sourceField = sourceField;
		this.destinationField = destinationField;
		this.sourceFieldAccessor = (FieldAccessor<Object>) new FieldAccessor<>(
				new ResolvedField(new ObjectAddress(sourceField.getName(), false), List.of(sourceField)));
		this.destinationFieldAccessor = (FieldAccessor<Object>) new FieldAccessor<>(
				new ResolvedField(new ObjectAddress(destinationField.getName(), false), List.of(destinationField)));
	}

	/**
	 * Copies the source collection elements into the destination and sets the
	 * destination field to the source field value, creating the destination
	 * instance first if {@code destinationObject} is null.
	 *
	 * @return the populated destination object
	 */
	@SuppressWarnings({ "rawtypes" })
	@Override
	public <destination> destination doMapping(IClass<destination> destinationClass, destination destinationObject,
			Object sourceObject) throws MapperException {
		log.debug("SimpleCollection: {} -> {}", this.sourceField.getName(), this.destinationField.getName());

		if( destinationObject == null ) {
			try {
				destinationObject = this.reflection.newInstance(destinationClass);
			} catch (ReflectionException e) {
				throw new MapperException(e);
			}
		}

		Collection sourceCollection = (Collection) sourceObject;
		((Collection) destinationObject).addAll(sourceCollection);

		try {
			Object sourceValue = this.sourceFieldAccessor.getValue(sourceObject).single();
			this.destinationFieldAccessor.setValue(destinationObject,
					SingleFieldValue.of(sourceValue, (IClass<Object>) this.destinationField.getType()));
		} catch (ReflectionException e) {
			log.error("Failed to set field value for {}: {}", this.destinationField.getName(), e.getMessage());
			throw new MapperException(e);
		}

		return destinationObject;
	}

}
