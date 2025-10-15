package com.garganttua.objects.mapper.rules;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.garganttua.objects.mapper.GGMapperException;
import com.garganttua.objects.mapper.GGMappingDirection;
import com.garganttua.objects.mapper.IGGMappingRuleExecutor;
import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

public class GGAPIMethodMappingExecutor implements IGGMappingRuleExecutor {

	private Method method;
	private Field sourceField;
	private Field destinationField;
	private GGMappingDirection mappingDirection;

	public GGAPIMethodMappingExecutor(Method method, Field sourceField, Field destinationField, GGMappingDirection mappingDirection) {
		this.method = method;
		this.sourceField = sourceField;
		this.destinationField = destinationField;
		this.mappingDirection = mappingDirection;
	}

	@Override
	public <destination> destination doMapping(Class<destination> destinationClass, destination destinationObject, Object sourceObject) throws GGMapperException {
		try {
		
			if( this.mappingDirection == GGMappingDirection.REGULAR) {
				Object sourceObjectToMap = GGObjectReflectionHelper.getObjectFieldValue(sourceObject, this.sourceField);
				if( sourceObjectToMap == null ) {
					return destinationObject;
				}
				Object destinationMappedObject = GGObjectReflectionHelper.invokeMethod(destinationObject, this.method.getName(), this.method , sourceObjectToMap);
				GGObjectReflectionHelper.setObjectFieldValue(destinationObject, this.destinationField, destinationMappedObject);
			} else {
				Object sourceObjectToMap = GGObjectReflectionHelper.getObjectFieldValue(sourceObject, this.sourceField);
				if( sourceObjectToMap == null ) {
					return destinationObject;
				}
				Object destinationMappedObject = GGObjectReflectionHelper.invokeMethod(sourceObject, this.method.getName(), this.method , sourceObjectToMap);
				GGObjectReflectionHelper.setObjectFieldValue(destinationObject, this.destinationField, destinationMappedObject);
			}

		} catch (GGReflectionException e) {
			throw new GGMapperException(e);
		}
		return destinationObject;
	}

}
