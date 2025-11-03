package com.garganttua.core.mapper.rules;

import java.lang.reflect.Field;
import java.util.Collection;

import com.garganttua.core.mapper.IMapper;
import com.garganttua.core.mapper.IMappingRuleExecutor;
import com.garganttua.core.mapper.MapperException;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.fields.Fields;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

public class MapableCollectionMappingExecutor implements IMappingRuleExecutor {

	private IMapper mapper;
	private Field sourceField;
	private Field destinationField;

	public MapableCollectionMappingExecutor(IMapper mapper, Field sourceField, Field destinationField) {
		this.mapper = mapper;
		this.sourceField = sourceField;
		this.destinationField = destinationField;
	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	public <destination> destination doMapping(Class<destination> destinationClass, destination destinationObject,
			Object sourceObject) throws MapperException {

		if (destinationObject == null) {
			try {
				destinationObject = ObjectReflectionHelper.instanciateNewObject(destinationClass);
			} catch (ReflectionException e) {
				throw new MapperException(e);
			}
		}

		try {
			Object sourceFieldObject = ObjectReflectionHelper.getObjectFieldValue(sourceObject, this.sourceField);
			Object destinationFieldObject = Fields.instanciate(this.destinationField);
			
			if( sourceFieldObject == null ) {
				return destinationObject;
			}
			
			for (Object item: ((Collection) sourceFieldObject)) {
				destination destinationItem = (destination) this.mapper.map(item, Fields.getGenericType(this.destinationField, 0));
				((Collection) destinationFieldObject).add(destinationItem);	
			}
			ObjectReflectionHelper.setObjectFieldValue(destinationObject, this.destinationField, destinationFieldObject);
		} catch (ReflectionException e) {
			throw new MapperException(e);
		}
		
		return destinationObject;
	}

}
