package com.garganttua.objects.mapper.rules;

import java.lang.reflect.Field;
import java.util.Collection;

import com.garganttua.objects.mapper.GGMapper;
import com.garganttua.objects.mapper.GGMapperException;
import com.garganttua.objects.mapper.IGGMappingRuleExecutor;
import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.fields.GGFields;
import com.garganttua.reflection.utils.GGFieldAccessManager;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

public class GGAPIMapableCollectionMappingExecutor implements IGGMappingRuleExecutor {

	private GGMapper mapper;
	private Field sourceField;
	private Field destinationField;

	public GGAPIMapableCollectionMappingExecutor(GGMapper mapper, Field sourceField, Field destinationField) {
		this.mapper = mapper;
		this.sourceField = sourceField;
		this.destinationField = destinationField;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <destination> destination doMapping(Class<destination> destinationClass, destination destinationObject,
			Object sourceObject) throws GGMapperException {

		if (destinationObject == null) {
			try {
				destinationObject = GGObjectReflectionHelper.instanciateNewObject(destinationClass);
			} catch (GGReflectionException e) {
				throw new GGMapperException(e);
			}
		}

		try {
			Object sourceFieldObject = GGObjectReflectionHelper.getObjectFieldValue(sourceObject, this.sourceField);
			Object destinationFieldObject = GGFields.instanciate(this.destinationField);
			
			if( sourceFieldObject == null ) {
				return destinationObject;
			}
			
			for (Object item: ((Collection) sourceFieldObject)) {
				destination destinationItem = (destination) this.mapper.map(item, GGFields.getGenericType(this.destinationField, 0));
				((Collection) destinationFieldObject).add(destinationItem);	
			}
			GGObjectReflectionHelper.setObjectFieldValue(destinationObject, this.destinationField, destinationFieldObject);
		} catch (GGReflectionException e) {
			throw new GGMapperException(e);
		}
		
		return destinationObject;
	}

}
