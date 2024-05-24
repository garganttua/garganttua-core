package com.garganttua.reflection.fields;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.garganttua.reflection.GGObjectAddress;
import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GGObjectFieldGetter {

	private Class<?> clazz;
	private List<Object> fields;
	private GGObjectAddress address;

	public GGObjectFieldGetter(Class<?> clazz, List<Object> fields, GGObjectAddress address) throws GGReflectionException {
		if( clazz == null ) {
			throw new GGReflectionException("class is null");
		}
		if( fields == null ) {
			throw new GGReflectionException("field is null");
		}
		if( address == null ) {
			throw new GGReflectionException("address is null");
		}

		this.address = address;
		this.clazz = clazz;
		this.fields = fields;
	}

	public Object getValue(Object object) throws GGReflectionException {
		if( log.isDebugEnabled() ) {
			log.debug("Getting value of object : {}, class {}, address {}", object, this.clazz, this.address);
		}
		if( object == null ) {
			throw new GGReflectionException("object is null");
		}
		if( !object.getClass().isAssignableFrom(this.clazz) ) {
			throw new GGReflectionException("object is not of type "+this.clazz);
		}

		Object value = null;
		if( fields.size() == 1 ) {
			Field field = (Field) fields.get(0);
			String fieldName = this.address.getElement(0);
			if( !field.getName().equals(fieldName) ) {
				throw new GGReflectionException("field names of address "+fieldName+" and fields list "+field.getName()+" do not match");
			}
			value = GGObjectReflectionHelper.getObjectFieldValue(object, "", field);
		} else {
			value = this.getValueRecursively(object, 0, 0);
		}
		
		return value;
	}

	private Object getValueRecursively(Object object, int addressIndex, int fieldIndex) throws GGReflectionException {
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
//			throw new GGReflectionException("field names of address "+fieldName+" and fields list "+field.getName()+" do not match");
//		}
		
		Object temp = GGObjectReflectionHelper.getObjectFieldValue(object, fieldName, field);
		if( temp == null ) {
			return null;
		}
		
		if (GGFields.isArrayOrMapOrCollectionField(field)) {
			
			value = new ArrayList<Object>();
			
			this.doIfIsCollection(value, field, nextField, temp, isLastIteration, fieldName, addressIndex, fieldIndex);
			this.doIfIsMap(value, nextFieldName, field, nextField, temp, isLastIteration, fieldName, addressIndex, fieldIndex); 
			this.doIfIsArray(value, field, nextField, temp, isLastIteration, fieldName, addressIndex, fieldIndex);
			
		} else {
			if( isLastIteration ) {
				value = GGObjectReflectionHelper.getObjectFieldValue(temp, fieldName, nextField);
			} else {
				value = this.getValueRecursively(temp, addressIndex+1, fieldIndex+1);
			}
		}
		return value;
	}

	private void doIfIsArray(Object value, Field field, Field nextField, Object list, boolean isLastIteration, String fieldName, int addressIndex, int fieldIndex)
			throws GGReflectionException {
		if( field.getType().isArray() ){
			Object[] array = (Object[]) list;
			for(Object obj: array) {
				this.processArrayObject(value, field, nextField, isLastIteration, fieldName, addressIndex, fieldIndex, obj, 1, 1);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void doIfIsMap(Object value, String nextFieldName, Field field, Field nextField, Object list, boolean isLastIteration, String fieldName, int addressIndex, int fieldIndex)
			throws GGReflectionException {
		if( Map.class.isAssignableFrom(field.getType()) ){
			Map<Object,Object> map = (Map<Object,Object>) list;
			if( nextFieldName.equals(GGObjectAddress.MAP_KEY_INDICATOR) ) {
				for(Object obj: map.keySet()) {
					this.processArrayObject(value, field, nextField, isLastIteration, fieldName, addressIndex, fieldIndex, obj, 1, 2);
				}
			} else if( nextFieldName.equals(GGObjectAddress.MAP_VALUE_INDICATOR) ) {
				for(Object obj: map.values()) {
					this.processArrayObject(value, field, nextField, isLastIteration, fieldName, addressIndex, fieldIndex, obj, 1, 2);
				}
			} else {
				throw new GGReflectionException("Invalid address, "+nextFieldName+" should be either #key or #value");
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void doIfIsCollection(Object value, Field field, Field nextField, Object list, boolean isLastIteration, String fieldName, int addressIndex, int fieldIndex)
			throws GGReflectionException {
		if( Collection.class.isAssignableFrom(field.getType()) ){
			Collection<Object> collection = (Collection<Object>) list;
			for(Object obj: collection) {
				this.processArrayObject(value, field, nextField, isLastIteration, fieldName, addressIndex, fieldIndex, obj, 1, 1);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void processArrayObject(Object value, Field field, Field nextField, boolean isLastIteration, String fieldName,
			int addressIndex, int fieldIndex, Object obj, int fieldIncrement, int addressIncrement) throws GGReflectionException {
		if( isLastIteration ) {
			((List<Object>) value).add(GGObjectReflectionHelper.getObjectFieldValue(obj, fieldName, nextField));
		} else {
			((List<Object>) value).add(this.getValueRecursively(obj, addressIndex+addressIncrement, fieldIndex+fieldIncrement));
		}
	}
}

	