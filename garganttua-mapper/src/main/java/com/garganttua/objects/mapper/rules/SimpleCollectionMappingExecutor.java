package com.garganttua.objects.mapper.rules;

import java.lang.reflect.Field;
import java.util.Collection;

import com.garganttua.core.mapper.IMappingRuleExecutor;
import com.garganttua.core.mapper.MapperException;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.reflection.utils.FieldAccessManager;
import com.garganttua.reflection.utils.ObjectReflectionHelper;

public class SimpleCollectionMappingExecutor implements IMappingRuleExecutor {

	private Field sourceField;
	private Field destinationField;

	public SimpleCollectionMappingExecutor(Field sourceField, Field destinationField) {
		this.sourceField = sourceField;
		this.destinationField = destinationField;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public <destination> destination doMapping(Class<destination> destinationClass, destination destinationObject,
			Object sourceObject) throws MapperException {
		
		if( destinationObject == null ) {
			try {
				destinationObject = ObjectReflectionHelper.instanciateNewObject(destinationClass);
			} catch (ReflectionException e) {
				throw new MapperException(e);
			}
		}
		
		((Collection) destinationObject).addAll(((Collection) sourceObject));

		try ( FieldAccessManager accessor = new FieldAccessManager(destinationField) ){
			try ( FieldAccessManager accessor2 = new FieldAccessManager(this.sourceField) ){
				this.destinationField.set(destinationObject, this.sourceField.get(sourceObject));
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new MapperException(e);
		}
		
		return destinationObject;
	}

}
