package com.garganttua.core.mapper.rules;

import java.lang.reflect.Field;

import com.garganttua.core.mapper.IMapper;
import com.garganttua.core.mapper.IMappingRuleExecutor;
import com.garganttua.core.mapper.MapperException;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

public class SimpleMapableFieldMappingExecutor implements IMappingRuleExecutor {

	private Field sourceFieldLeaf;
	private Field destinationFieldLeaf;
	private IMapper mapper;

	public SimpleMapableFieldMappingExecutor(IMapper mapper, Field sourceFieldLeaf, Field destinationFieldLeaf) {
		this.sourceFieldLeaf = sourceFieldLeaf;
		this.destinationFieldLeaf = destinationFieldLeaf;
		this.mapper = mapper;
	}

	@Override
	public <destination> destination doMapping(Class<destination> destinationClass, destination destinationObject, Object sourceObject) throws MapperException {
		if( destinationObject == null ) {
			try {
				destinationObject = ObjectReflectionHelper.instanciateNewObject(destinationClass);
			} catch (ReflectionException e) {
				throw new MapperException(e);
			}
		}
		Object sourceObjectToMap;
		try {
			sourceObjectToMap = ObjectReflectionHelper.getObjectFieldValue(sourceObject, this.sourceFieldLeaf);
			
			if( sourceObjectToMap != null ) {
				destination destinationObjectMapped = (destination) this.mapper.map(sourceObjectToMap, this.destinationFieldLeaf.getType());
				ObjectReflectionHelper.setObjectFieldValue(destinationObject, this.destinationFieldLeaf, destinationObjectMapped);
			}

		} catch (ReflectionException e) {
			throw new MapperException(e);
		}
		
		return destinationObject;
	}

}
