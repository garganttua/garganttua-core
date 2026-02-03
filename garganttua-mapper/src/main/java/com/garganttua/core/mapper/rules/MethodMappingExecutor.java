package com.garganttua.core.mapper.rules;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.garganttua.core.mapper.IMappingRuleExecutor;
import com.garganttua.core.mapper.MapperException;
import com.garganttua.core.mapper.MappingDirection;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MethodMappingExecutor implements IMappingRuleExecutor {

	private Method method;
	private Field sourceField;
	private Field destinationField;
	private MappingDirection mappingDirection;

	public MethodMappingExecutor(Method method, Field sourceField, Field destinationField, MappingDirection mappingDirection) {
		log.atTrace().log("Entering MethodMappingExecutor constructor with method={}, sourceField={}, destinationField={}, direction={}", method, sourceField, destinationField, mappingDirection);
		this.method = method;
		this.sourceField = sourceField;
		this.destinationField = destinationField;
		this.mappingDirection = mappingDirection;
		log.atDebug().log("MethodMappingExecutor initialized for method {} with direction {}", method.getName(), mappingDirection);
		log.atTrace().log("Exiting MethodMappingExecutor constructor");
	}

	@Override
	public <destination> destination doMapping(Class<destination> destinationClass, destination destinationObject, Object sourceObject) throws MapperException {
		log.atTrace().log("Entering doMapping with destinationClass={}, direction={}", destinationClass, this.mappingDirection);
		try {

			if( this.mappingDirection == MappingDirection.REGULAR) {
				log.atDebug().log("Executing REGULAR mapping direction");
				Object sourceObjectToMap = ObjectReflectionHelper.getObjectFieldValue(sourceObject, this.sourceField);
				log.atDebug().log("Retrieved source field value: {}", sourceObjectToMap);
				if( sourceObjectToMap == null ) {
					log.atDebug().log("Source object is null, skipping mapping");
					return destinationObject;
				}
				Object destinationMappedObject = ObjectReflectionHelper.invokeMethod(destinationObject, this.method.getName(), this.method, this.method.getReturnType(), sourceObjectToMap);
				log.atDebug().log("Method {} invoked, result: {}", this.method.getName(), destinationMappedObject);
				ObjectReflectionHelper.setObjectFieldValue(destinationObject, this.destinationField, destinationMappedObject);
				log.atDebug().log("REGULAR mapping completed for method {}", this.method.getName());
			} else {
				log.atDebug().log("Executing REVERSE mapping direction");
				Object sourceObjectToMap = ObjectReflectionHelper.getObjectFieldValue(sourceObject, this.sourceField);
				log.atDebug().log("Retrieved source field value: {}", sourceObjectToMap);
				if( sourceObjectToMap == null ) {
					log.atDebug().log("Source object is null, skipping mapping");
					return destinationObject;
				}
				Object destinationMappedObject = ObjectReflectionHelper.invokeMethod(sourceObject, this.method.getName(), this.method, this.method.getReturnType(), sourceObjectToMap);
				log.atDebug().log("Method {} invoked on source, result: {}", this.method.getName(), destinationMappedObject);
				ObjectReflectionHelper.setObjectFieldValue(destinationObject, this.destinationField, destinationMappedObject);
				log.atDebug().log("REVERSE mapping completed for method {}", this.method.getName());
			}

		} catch (ReflectionException e) {
			log.atError().log("Mapping failed with ReflectionException: {}", e.getMessage());
			throw new MapperException(e);
		}
		log.atTrace().log("Exiting doMapping with result");
		return destinationObject;
	}

}
