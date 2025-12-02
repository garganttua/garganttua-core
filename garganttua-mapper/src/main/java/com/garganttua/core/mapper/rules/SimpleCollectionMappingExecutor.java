package com.garganttua.core.mapper.rules;

import java.lang.reflect.Field;
import java.util.Collection;

import com.garganttua.core.mapper.IMappingRuleExecutor;
import com.garganttua.core.mapper.MapperException;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.utils.FieldAccessManager;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleCollectionMappingExecutor implements IMappingRuleExecutor {

	private Field sourceField;
	private Field destinationField;

	public SimpleCollectionMappingExecutor(Field sourceField, Field destinationField) {
		log.atTrace().log("Entering SimpleCollectionMappingExecutor constructor(sourceField={}, destinationField={})", sourceField.getName(), destinationField.getName());
		log.atDebug().log("Creating SimpleCollectionMappingExecutor for {} -> {}", sourceField.getName(), destinationField.getName());

		this.sourceField = sourceField;
		this.destinationField = destinationField;

		log.atTrace().log("Exiting SimpleCollectionMappingExecutor constructor");
	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	public <destination> destination doMapping(Class<destination> destinationClass, destination destinationObject,
			Object sourceObject) throws MapperException {
		log.atTrace().log("Entering doMapping(destinationClass={}, destinationObject={}, sourceObject={})", destinationClass, destinationObject, sourceObject);
		log.atDebug().log("Mapping simple collection field {} from {} to {}", this.sourceField.getName(), sourceObject.getClass().getSimpleName(), destinationClass.getSimpleName());

		if( destinationObject == null ) {
			log.atDebug().log("Destination object is null, instantiating new object of type {}", destinationClass.getSimpleName());
			try {
				destinationObject = ObjectReflectionHelper.instanciateNewObject(destinationClass);
			} catch (ReflectionException e) {
				log.atError().log("Failed to instantiate destination object: {}", e.getMessage());
				throw new MapperException(e);
			}
		}

		Collection sourceCollection = (Collection) sourceObject;
		log.atDebug().log("Adding all {} items from source collection to destination", sourceCollection.size());
		((Collection) destinationObject).addAll(sourceCollection);

		try ( FieldAccessManager accessor = new FieldAccessManager(destinationField) ){
			try ( FieldAccessManager accessor2 = new FieldAccessManager(this.sourceField) ){
				log.atDebug().log("Setting destination field {} with source field value", this.destinationField.getName());
				this.destinationField.set(destinationObject, this.sourceField.get(sourceObject));
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			log.atError().log("Failed to set field value for {}: {}", this.destinationField.getName(), e.getMessage());
			throw new MapperException(e);
		}

		log.atTrace().log("Exiting doMapping() with destinationObject={}", destinationObject);
		return destinationObject;
	}

}
