package com.garganttua.core.mapper.rules;

import java.lang.reflect.Field;

import com.garganttua.core.mapper.IMappingRuleExecutor;
import com.garganttua.core.mapper.MapperException;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.utils.FieldAccessManager;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

public class SimpleFieldMappingExecutor implements IMappingRuleExecutor {

	private Field sourceField;
	private Field destinationField;

	public SimpleFieldMappingExecutor(Field sourceField, Field destinationField) {
		this.sourceField = sourceField;
		this.destinationField = destinationField;
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
