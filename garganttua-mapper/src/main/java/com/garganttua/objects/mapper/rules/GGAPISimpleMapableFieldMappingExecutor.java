package com.garganttua.objects.mapper.rules;

import java.lang.reflect.Field;

import com.garganttua.objects.mapper.GGMapper;
import com.garganttua.objects.mapper.GGMapperException;
import com.garganttua.objects.mapper.IGGMappingRuleExecutor;
import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

public class GGAPISimpleMapableFieldMappingExecutor implements IGGMappingRuleExecutor {

	private Field sourceFieldLeaf;
	private Field destinationFieldLeaf;
	private GGMapper mapper;

	public GGAPISimpleMapableFieldMappingExecutor(GGMapper mapper, Field sourceFieldLeaf, Field destinationFieldLeaf) {
		this.sourceFieldLeaf = sourceFieldLeaf;
		this.destinationFieldLeaf = destinationFieldLeaf;
		this.mapper = mapper;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <destination> destination doMapping(Class<destination> destinationClass, destination destinationObject, Object sourceObject) throws GGMapperException {
		if( destinationObject == null ) {
			try {
				destinationObject = GGObjectReflectionHelper.instanciateNewObject(destinationClass);
			} catch (GGReflectionException e) {
				throw new GGMapperException(e);
			}
		}
		Object sourceObjectToMap;
		try {
			sourceObjectToMap = GGObjectReflectionHelper.getObjectFieldValue(sourceObject, this.sourceFieldLeaf);
			
			if( sourceObjectToMap != null ) {
				destination destinationObjectMapped = (destination) this.mapper.map(sourceObjectToMap, this.destinationFieldLeaf.getType());
				GGObjectReflectionHelper.setObjectFieldValue(destinationObject, this.destinationFieldLeaf, destinationObjectMapped);
			}

		} catch (GGReflectionException e) {
			throw new GGMapperException(e);
		}
		
		return destinationObject;
	}

}
