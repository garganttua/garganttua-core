package com.garganttua.objects.mapper.rules;

import java.lang.reflect.Field;
import java.util.Collection;

import com.garganttua.objects.mapper.GGMapperException;
import com.garganttua.objects.mapper.IGGMappingRuleExecutor;
import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.utils.GGFieldAccessManager;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

public class GGAPISimpleCollectionMappingExecutor implements IGGMappingRuleExecutor {

	private Field sourceField;
	private Field destinationField;

	public GGAPISimpleCollectionMappingExecutor(Field sourceField, Field destinationField) {
		this.sourceField = sourceField;
		this.destinationField = destinationField;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public <destination> destination doMapping(Class<destination> destinationClass, destination destinationObject,
			Object sourceObject) throws GGMapperException {
		
		if( destinationObject == null ) {
			try {
				destinationObject = GGObjectReflectionHelper.instanciateNewObject(destinationClass);
			} catch (GGReflectionException e) {
				throw new GGMapperException(e);
			}
		}
		
		((Collection) destinationObject).addAll(((Collection) sourceObject));

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
