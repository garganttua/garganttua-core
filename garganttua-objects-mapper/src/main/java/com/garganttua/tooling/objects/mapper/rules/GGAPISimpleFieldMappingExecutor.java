package com.garganttua.tooling.objects.mapper.rules;

import java.lang.reflect.Field;

import com.garganttua.reflection.utils.GGFieldAccessManager;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;
import com.garganttua.tooling.objects.mapper.IGGAPIMappingRuleExecutor;

public class GGAPISimpleFieldMappingExecutor implements IGGAPIMappingRuleExecutor {

	private Field sourceField;
	private Field destinationField;

	public GGAPISimpleFieldMappingExecutor(Field sourceField, Field destinationField) {
		this.sourceField = sourceField;
		this.destinationField = destinationField;
	}

	@Override
	public <destination> destination doMapping(Class<destination> destinationClass, destination destinationObject, Object sourceObject) throws GGAPIMappingRuleExecutorException {
		if( destinationObject == null ) {
			try {
				destinationObject = GGObjectReflectionHelper.instanciateNewObject(destinationClass);
			} catch (GGAPIObjectReflectionHelperExcpetion e) {
				throw new GGAPIMappingRuleExecutorException(e);
			}
		}

		try ( GGFieldAccessManager accessor = new GGFieldAccessManager(destinationField) ){
			try ( GGFieldAccessManager accessor2 = new GGFieldAccessManager(this.sourceField) ){
				this.destinationField.set(destinationObject, this.sourceField.get(sourceObject));
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new GGAPIMappingRuleExecutorException(e);
		}
		
		return destinationObject;
	}

}
