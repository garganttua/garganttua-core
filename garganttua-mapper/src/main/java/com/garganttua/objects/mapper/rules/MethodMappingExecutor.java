package com.garganttua.objects.mapper.rules;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.garganttua.core.mapper.IMappingRuleExecutor;
import com.garganttua.core.mapper.MapperException;
import com.garganttua.core.mapper.MappingDirection;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.reflection.utils.ObjectReflectionHelper;

public class MethodMappingExecutor implements IMappingRuleExecutor {

	private Method method;
	private Field sourceField;
	private Field destinationField;
	private MappingDirection mappingDirection;

	public MethodMappingExecutor(Method method, Field sourceField, Field destinationField, MappingDirection mappingDirection) {
		this.method = method;
		this.sourceField = sourceField;
		this.destinationField = destinationField;
		this.mappingDirection = mappingDirection;
	}

	@Override
	public <destination> destination doMapping(Class<destination> destinationClass, destination destinationObject, Object sourceObject) throws MapperException {
		try {
		
			if( this.mappingDirection == MappingDirection.REGULAR) {
				Object sourceObjectToMap = ObjectReflectionHelper.getObjectFieldValue(sourceObject, this.sourceField);
				if( sourceObjectToMap == null ) {
					return destinationObject;
				}
				Object destinationMappedObject = ObjectReflectionHelper.invokeMethod(destinationObject, this.method.getName(), this.method , sourceObjectToMap);
				ObjectReflectionHelper.setObjectFieldValue(destinationObject, this.destinationField, destinationMappedObject);
			} else {
				Object sourceObjectToMap = ObjectReflectionHelper.getObjectFieldValue(sourceObject, this.sourceField);
				if( sourceObjectToMap == null ) {
					return destinationObject;
				}
				Object destinationMappedObject = ObjectReflectionHelper.invokeMethod(sourceObject, this.method.getName(), this.method , sourceObjectToMap);
				ObjectReflectionHelper.setObjectFieldValue(destinationObject, this.destinationField, destinationMappedObject);
			}

		} catch (ReflectionException e) {
			throw new MapperException(e);
		}
		return destinationObject;
	}

}
