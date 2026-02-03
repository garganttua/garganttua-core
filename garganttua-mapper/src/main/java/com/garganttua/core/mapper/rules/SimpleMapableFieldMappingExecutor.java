package com.garganttua.core.mapper.rules;

import java.lang.reflect.Field;

import com.garganttua.core.mapper.IMapper;
import com.garganttua.core.mapper.IMappingRuleExecutor;
import com.garganttua.core.mapper.MapperException;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleMapableFieldMappingExecutor implements IMappingRuleExecutor {

	private Field sourceFieldLeaf;
	private Field destinationFieldLeaf;
	private IMapper mapper;

	public SimpleMapableFieldMappingExecutor(IMapper mapper, Field sourceFieldLeaf, Field destinationFieldLeaf) {
		log.atTrace().log("Entering SimpleMapableFieldMappingExecutor constructor with mapper={}, sourceFieldLeaf={}, destinationFieldLeaf={}", mapper, sourceFieldLeaf, destinationFieldLeaf);
		this.sourceFieldLeaf = sourceFieldLeaf;
		this.destinationFieldLeaf = destinationFieldLeaf;
		this.mapper = mapper;
		log.atDebug().log("SimpleMapableFieldMappingExecutor initialized for fields {} -> {}", sourceFieldLeaf.getName(), destinationFieldLeaf.getName());
		log.atTrace().log("Exiting SimpleMapableFieldMappingExecutor constructor");
	}

	@Override
	public <destination> destination doMapping(Class<destination> destinationClass, destination destinationObject, Object sourceObject) throws MapperException {
		log.atTrace().log("Entering doMapping with destinationClass={}", destinationClass);
		if( destinationObject == null ) {
			log.atDebug().log("Destination object is null, instantiating new object of type {}", destinationClass);
			try {
				destinationObject = ObjectReflectionHelper.instanciateNewObject(destinationClass);
				log.atDebug().log("New destination object created: {}", destinationObject);
			} catch (ReflectionException e) {
				log.atError().log("Failed to instantiate destination object: {}", e.getMessage());
				throw new MapperException(e);
			}
		}
		Object sourceObjectToMap;
		try {
			sourceObjectToMap = ObjectReflectionHelper.getObjectFieldValue(sourceObject, this.sourceFieldLeaf);
			log.atDebug().log("Retrieved source field value: {}", sourceObjectToMap);

			if( sourceObjectToMap != null ) {
				log.atDebug().log("Mapping source object to destination type {}", this.destinationFieldLeaf.getType());
				destination destinationObjectMapped = (destination) this.mapper.map(sourceObjectToMap, this.destinationFieldLeaf.getType());
				log.atDebug().log("Object mapped, setting destination field");
				ObjectReflectionHelper.setObjectFieldValue(destinationObject, this.destinationFieldLeaf, destinationObjectMapped);
				log.atDebug().log("Mapable field mapping completed for {} -> {}", this.sourceFieldLeaf.getName(), this.destinationFieldLeaf.getName());
			} else {
				log.atDebug().log("Source object is null, skipping mapping");
			}

		} catch (ReflectionException e) {
			log.atError().log("Mapping failed with ReflectionException: {}", e.getMessage());
			throw new MapperException(e);
		}

		log.atTrace().log("Exiting doMapping with result");
		return destinationObject;
	}

}
