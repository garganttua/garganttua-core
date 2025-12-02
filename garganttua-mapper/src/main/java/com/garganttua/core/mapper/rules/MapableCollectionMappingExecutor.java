package com.garganttua.core.mapper.rules;

import java.lang.reflect.Field;
import java.util.Collection;

import com.garganttua.core.mapper.IMapper;
import com.garganttua.core.mapper.IMappingRuleExecutor;
import com.garganttua.core.mapper.MapperException;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.fields.Fields;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MapableCollectionMappingExecutor implements IMappingRuleExecutor {

	private IMapper mapper;
	private Field sourceField;
	private Field destinationField;

	public MapableCollectionMappingExecutor(IMapper mapper, Field sourceField, Field destinationField) {
		log.atTrace().log("Entering MapableCollectionMappingExecutor constructor(sourceField={}, destinationField={})", sourceField.getName(), destinationField.getName());
		log.atDebug().log("Creating MapableCollectionMappingExecutor for {} -> {}", sourceField.getName(), destinationField.getName());

		this.mapper = mapper;
		this.sourceField = sourceField;
		this.destinationField = destinationField;

		log.atTrace().log("Exiting MapableCollectionMappingExecutor constructor");
	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	public <destination> destination doMapping(Class<destination> destinationClass, destination destinationObject,
			Object sourceObject) throws MapperException {
		log.atTrace().log("Entering doMapping(destinationClass={}, destinationObject={}, sourceObject={})", destinationClass, destinationObject, sourceObject);
		log.atDebug().log("Mapping collection field {} from {} to {}", this.sourceField.getName(), sourceObject.getClass().getSimpleName(), destinationClass.getSimpleName());

		if (destinationObject == null) {
			log.atDebug().log("Destination object is null, instantiating new object of type {}", destinationClass.getSimpleName());
			try {
				destinationObject = ObjectReflectionHelper.instanciateNewObject(destinationClass);
			} catch (ReflectionException e) {
				log.atError().log("Failed to instantiate destination object: {}", e.getMessage());
				throw new MapperException(e);
			}
		}

		try {
			Object sourceFieldObject = ObjectReflectionHelper.getObjectFieldValue(sourceObject, this.sourceField);
			log.atDebug().log("Retrieved source collection field value: {}", sourceFieldObject);

			Object destinationFieldObject = Fields.instanciate(this.destinationField);
			log.atDebug().log("Instantiated destination collection: {}", destinationFieldObject.getClass().getSimpleName());

			if( sourceFieldObject == null ) {
				log.atDebug().log("Source field object is null, returning destination object without mapping");
				log.atTrace().log("Exiting doMapping() with destinationObject={}", destinationObject);
				return destinationObject;
			}

			Collection sourceCollection = (Collection) sourceFieldObject;
			log.atDebug().log("Mapping collection with {} items", sourceCollection.size());

			int itemIndex = 0;
			for (Object item: sourceCollection) {
				log.atDebug().log("Mapping collection item {} of {}", ++itemIndex, sourceCollection.size());
				destination destinationItem = (destination) this.mapper.map(item, Fields.getGenericType(this.destinationField, 0));
				((Collection) destinationFieldObject).add(destinationItem);
			}

			ObjectReflectionHelper.setObjectFieldValue(destinationObject, this.destinationField, destinationFieldObject);
			log.atDebug().log("Set destination field {} with {} mapped items", this.destinationField.getName(), sourceCollection.size());
		} catch (ReflectionException e) {
			log.atError().log("Mapping failed for collection field {}: {}", this.sourceField.getName(), e.getMessage());
			throw new MapperException(e);
		}

		log.atTrace().log("Exiting doMapping() with destinationObject={}", destinationObject);
		return destinationObject;
	}

}
