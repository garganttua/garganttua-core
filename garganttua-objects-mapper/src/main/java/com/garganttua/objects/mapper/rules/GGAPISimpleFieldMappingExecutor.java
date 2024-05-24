package com.garganttua.objects.mapper.rules;

import java.lang.reflect.Field;

import com.garganttua.objects.mapper.GGMapperException;
import com.garganttua.objects.mapper.IGGMappingRuleExecutor;
import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.utils.GGFieldAccessManager;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

public class GGAPISimpleFieldMappingExecutor implements IGGMappingRuleExecutor {

	private Field sourceField;
	private Field destinationField;

	public GGAPISimpleFieldMappingExecutor(Field sourceField, Field destinationField) {
		this.sourceField = sourceField;
		this.destinationField = destinationField;
	}

	@Override
	public <destination> destination doMapping(Class<destination> destinationClass, destination destinationObject, Object sourceObject) throws GGMapperException {
		if( destinationObject == null ) {
			try {
				destinationObject = GGObjectReflectionHelper.instanciateNewObject(destinationClass);
			} catch (GGReflectionException e) {
				throw new GGMapperException(e);
			}
		}

		try ( GGFieldAccessManager accessor = new GGFieldAccessManager(destinationField) ){
			try ( GGFieldAccessManager accessor2 = new GGFieldAccessManager(this.sourceField) ){
				this.destinationField.set(destinationObject, this.sourceField.get(sourceObject));
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new GGMapperException(e);
		}
		
		return destinationObject;
	}

}
