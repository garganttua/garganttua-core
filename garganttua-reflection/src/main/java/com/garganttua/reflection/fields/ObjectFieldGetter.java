package com.garganttua.reflection.fields;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ObjectFieldGetter {

	private Class<?> clazz;
	private List<Object> fields;
	private ObjectAddress address;

	public ObjectFieldGetter(Class<?> clazz, List<Object> fields, ObjectAddress address) throws ReflectionException {
		if( clazz == null ) {
			throw new ReflectionException("class is null");
		}
		if( fields == null ) {
			throw new ReflectionException("field is null");
		}
		if( address == null ) {
			throw new ReflectionException("address is null");
		}

		this.address = address;
		this.clazz = clazz;
		this.fields = fields;
	}

	public Object getValue(Object object) throws ReflectionException {
		if( log.isDebugEnabled() ) {
			log.debug("Getting value of object : {}, class {}, address {}", object, this.clazz, this.address);
		}
		if( object == null ) {
			throw new ReflectionException("object is null");
		}
		if( !object.getClass().isAssignableFrom(this.clazz) ) {
			throw new ReflectionException("object is not of type "+this.clazz);
		}

		Object value = null;
		if( fields.size() == 1 ) {
			Field field = (Field) fields.get(0);
			String fieldName = this.address.getElement(0);
			if( !field.getName().equals(fieldName) ) {
				throw new ReflectionException("field names of address "+fieldName+" and fields list "+field.getName()+" do not match");
			}
			value = ObjectReflectionHelper.getObjectFieldValue(object, field);
		} else {
			value = this.getValueRecursively(object, 0, 0);
		}
		
		return value;
	}

	private Object getValueRecursively(Object object, int addressIndex, int fieldIndex) throws ReflectionException {
		boolean isLastIteration = (fieldIndex+2 == this.fields.size());
		if( log.isDebugEnabled() ) {
			log.debug("Getting value of object : {}, addressIndex {}, fieldIndex {}, lastIteration {}", object, addressIndex, fieldIndex, isLastIteration);
		}
		Object value;
		String fieldName = this.address.getElement(addressIndex);
		String nextFieldName = this.address.getElement(addressIndex+1);
		Field field = (Field) this.fields.get(fieldIndex);
		Field nextField = (Field) this.fields.get(fieldIndex+1);
		
//		if( !field.getName().equals(fieldName) ) {
//			throw new ReflectionException("field names of address "+fieldName+" and fields list "+field.getName()+" do not match");
//		}
		
		Object temp = ObjectReflectionHelper.getObjectFieldValue(object, field);
		if( temp == null ) {
			return null;
		}
		
		if (Fields.isArrayOrMapOrCollectionField(field)) {
			
			value = new ArrayList<Object>();
			
			this.doIfIsCollection(value, field, nextField, temp, isLastIteration, fieldName, addressIndex, fieldIndex);
			this.doIfIsMap(value, nextFieldName, field, nextField, temp, isLastIteration, fieldName, addressIndex, fieldIndex); 
			this.doIfIsArray(value, field, nextField, temp, isLastIteration, fieldName, addressIndex, fieldIndex);
			
		} else {
			if( isLastIteration ) {
				value = ObjectReflectionHelper.getObjectFieldValue(temp, nextField);
			} else {
				value = this.getValueRecursively(temp, addressIndex+1, fieldIndex+1);
			}
		}
		return value;
	}

	private void doIfIsArray(Object value, Field field, Field nextField, Object list, boolean isLastIteration, String fieldName, int addressIndex, int fieldIndex)
			throws ReflectionException {
		if( field.getType().isArray() ){
			Object[] array = (Object[]) list;
			for(Object obj: array) {
				this.processArrayObject(value, field, nextField, isLastIteration, fieldName, addressIndex, fieldIndex, obj, 1, 1);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void doIfIsMap(Object value, String nextFieldName, Field field, Field nextField, Object list, boolean isLastIteration, String fieldName, int addressIndex, int fieldIndex)
			throws ReflectionException {
		if( Map.class.isAssignableFrom(field.getType()) ){
			Map<Object,Object> map = (Map<Object,Object>) list;
			if( nextFieldName.equals(ObjectAddress.MAP_KEY_INDICATOR) ) {
				for(Object obj: map.keySet()) {
					this.processArrayObject(value, field, nextField, isLastIteration, fieldName, addressIndex, fieldIndex, obj, 1, 2);
				}
			} else if( nextFieldName.equals(ObjectAddress.MAP_VALUE_INDICATOR) ) {
				for(Object obj: map.values()) {
					this.processArrayObject(value, field, nextField, isLastIteration, fieldName, addressIndex, fieldIndex, obj, 1, 2);
				}
			} else {
				throw new ReflectionException("Invalid address, "+nextFieldName+" should be either #key or #value");
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void doIfIsCollection(Object value, Field field, Field nextField, Object list, boolean isLastIteration, String fieldName, int addressIndex, int fieldIndex)
			throws ReflectionException {
		if( Collection.class.isAssignableFrom(field.getType()) ){
			Collection<Object> collection = (Collection<Object>) list;
			for(Object obj: collection) {
				this.processArrayObject(value, field, nextField, isLastIteration, fieldName, addressIndex, fieldIndex, obj, 1, 1);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void processArrayObject(Object value, Field field, Field nextField, boolean isLastIteration, String fieldName,
			int addressIndex, int fieldIndex, Object obj, int fieldIncrement, int addressIncrement) throws ReflectionException {
		if( isLastIteration ) {
			((List<Object>) value).add(ObjectReflectionHelper.getObjectFieldValue(obj, nextField));
		} else {
			((List<Object>) value).add(this.getValueRecursively(obj, addressIndex+addressIncrement, fieldIndex+fieldIncrement));
		}
	}
}

	