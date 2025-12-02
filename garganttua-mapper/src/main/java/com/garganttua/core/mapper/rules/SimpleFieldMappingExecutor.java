package com.garganttua.core.mapper.rules;

import java.lang.reflect.Field;

import com.garganttua.core.mapper.IMappingRuleExecutor;
import com.garganttua.core.mapper.MapperException;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.utils.FieldAccessManager;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleFieldMappingExecutor implements IMappingRuleExecutor {

	private Field sourceField;
	private Field destinationField;

	public SimpleFieldMappingExecutor(Field sourceField, Field destinationField) {
		log.atTrace().log("Entering SimpleFieldMappingExecutor constructor(sourceField={}, destinationField={})", sourceField.getName(), destinationField.getName());
		log.atDebug().log("Creating SimpleFieldMappingExecutor for {} ({}) -> {} ({})", sourceField.getName(), sourceField.getType().getSimpleName(), destinationField.getName(), destinationField.getType().getSimpleName());

		this.sourceField = sourceField;
		this.destinationField = destinationField;

		log.atTrace().log("Exiting SimpleFieldMappingExecutor constructor");
	}

	@Override
	public <destination> destination doMapping(Class<destination> destinationClass, destination destinationObject, Object sourceObject) throws MapperException {
		log.atTrace().log("Entering doMapping(destinationClass={}, destinationObject={}, sourceObject={})", destinationClass, destinationObject, sourceObject);
		log.atDebug().log("Mapping simple field {} from {} to {}", this.sourceField.getName(), sourceObject.getClass().getSimpleName(), destinationClass.getSimpleName());

		if( destinationObject == null ) {
			log.atDebug().log("Destination object is null, instantiating new object of type {}", destinationClass.getSimpleName());
			try {
				destinationObject = ObjectReflectionHelper.instanciateNewObject(destinationClass);
			} catch (ReflectionException e) {
				log.atError().log("Failed to instantiate destination object: {}", e.getMessage());
				throw new MapperException(e);
			}
		}

		try ( FieldAccessManager accessor = new FieldAccessManager(destinationField) ){
			try ( FieldAccessManager accessor2 = new FieldAccessManager(this.sourceField) ){
				Object sourceValue = this.sourceField.get(sourceObject);
				log.atDebug().log("Field transformation: {} = {} -> {} = {}", this.sourceField.getName(), sourceValue, this.destinationField.getName(), sourceValue);
				this.destinationField.set(destinationObject, sourceValue);
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			log.atError().log("Failed to map field {} to {}: {}", this.sourceField.getName(), this.destinationField.getName(), e.getMessage());
			throw new MapperException(e);
		}

		log.atTrace().log("Exiting doMapping() with destinationObject={}", destinationObject);
		return destinationObject;
	}

}
