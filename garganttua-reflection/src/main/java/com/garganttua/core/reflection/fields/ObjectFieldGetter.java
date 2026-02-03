package com.garganttua.core.reflection.fields;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ObjectFieldGetter {

	private Class<?> clazz;
	private List<Object> fields;
	private ObjectAddress address;

	public ObjectFieldGetter(Class<?> clazz, List<Object> fields, ObjectAddress address) throws ReflectionException {
		log.atTrace().log("Creating ObjectFieldGetter for class={}, address={}, fields count={}", clazz, address, fields != null ? fields.size() : 0);
		if( clazz == null ) {
			log.atError().log("class parameter is null");
			throw new ReflectionException("class is null");
		}
		if( fields == null ) {
			log.atError().log("fields parameter is null");
			throw new ReflectionException("field is null");
		}
		if( address == null ) {
			log.atError().log("address parameter is null");
			throw new ReflectionException("address is null");
		}

		this.address = address;
		this.clazz = clazz;
		this.fields = fields;
		log.atDebug().log("ObjectFieldGetter initialized for class={}, address={}", clazz.getName(), address);
	}

	public Object getValue(Object object) throws ReflectionException {
		log.atTrace().log("getValue entry: object={}, class={}, address={}", object, this.clazz, this.address);
		log.atDebug().log("Getting value of object: class={}, address={}", this.clazz.getName(), this.address);
		if( object == null ) {
			log.atError().log("object parameter is null");
			throw new ReflectionException("object is null");
		}
		if( !object.getClass().isAssignableFrom(this.clazz) ) {
			log.atError().log("object type {} is not assignable from {}", object.getClass(), this.clazz);
			throw new ReflectionException("object is not of type "+this.clazz);
		}

		Object value = null;
		if( fields.size() == 1 ) {
			Field field = (Field) fields.get(0);
			String fieldName = this.address.getElement(0);
			log.atDebug().log("Single field access: fieldName={}", fieldName);
			if( !field.getName().equals(fieldName) ) {
				log.atError().log("field names mismatch: address={}, field={}", fieldName, field.getName());
				throw new ReflectionException("field names of address "+fieldName+" and fields list "+field.getName()+" do not match");
			}
			value = ObjectReflectionHelper.getObjectFieldValue(object, field);
		} else {
			log.atDebug().log("Recursive field access with {} fields", fields.size());
			value = this.getValueRecursively(object, 0, 0);
		}

		log.atDebug().log("Successfully retrieved value for address={}", this.address);
		return value;
	}

	private Object getValueRecursively(Object object, int addressIndex, int fieldIndex) throws ReflectionException {
		boolean isLastIteration = (fieldIndex+2 == this.fields.size());
		log.atTrace().log("getValueRecursively: addressIndex={}, fieldIndex={}, lastIteration={}", addressIndex, fieldIndex, isLastIteration);
		log.atDebug().log("Getting value recursively: addressIndex={}, fieldIndex={}, lastIteration={}", addressIndex, fieldIndex, isLastIteration);
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
			log.atDebug().log("Field {} value is null, returning null", fieldName);
			return null;
		}

		if (Fields.isArrayOrMapOrCollectionField(field)) {
			log.atDebug().log("Field {} is array/map/collection type", fieldName);
			value = new ArrayList<Object>();

			this.doIfIsCollection(value, field, nextField, temp, isLastIteration, fieldName, addressIndex, fieldIndex);
			this.doIfIsMap(value, nextFieldName, field, nextField, temp, isLastIteration, fieldName, addressIndex, fieldIndex);
			this.doIfIsArray(value, field, nextField, temp, isLastIteration, fieldName, addressIndex, fieldIndex);

		} else {
			if( isLastIteration ) {
				log.atDebug().log("Last iteration, getting final field value");
				value = ObjectReflectionHelper.getObjectFieldValue(temp, nextField);
			} else {
				log.atTrace().log("Continuing recursive traversal");
				value = this.getValueRecursively(temp, addressIndex+1, fieldIndex+1);
			}
		}
		return value;
	}

	private void doIfIsArray(Object value, Field field, Field nextField, Object list, boolean isLastIteration, String fieldName, int addressIndex, int fieldIndex)
			throws ReflectionException {
		if( field.getType().isArray() ){
			log.atDebug().log("Processing array field: {}", fieldName);
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
			log.atDebug().log("Processing map field: {}, nextFieldName={}", fieldName, nextFieldName);
			Map<Object,Object> map = (Map<Object,Object>) list;
			if( nextFieldName.equals(ObjectAddress.MAP_KEY_INDICATOR) ) {
				log.atDebug().log("Processing map keys for field {}", fieldName);
				for(Object obj: map.keySet()) {
					this.processArrayObject(value, field, nextField, isLastIteration, fieldName, addressIndex, fieldIndex, obj, 1, 2);
				}
			} else if( nextFieldName.equals(ObjectAddress.MAP_VALUE_INDICATOR) ) {
				log.atDebug().log("Processing map values for field {}", fieldName);
				for(Object obj: map.values()) {
					this.processArrayObject(value, field, nextField, isLastIteration, fieldName, addressIndex, fieldIndex, obj, 1, 2);
				}
			} else {
				log.atError().log("Invalid map address element: {}, expected #key or #value", nextFieldName);
				throw new ReflectionException("Invalid address, "+nextFieldName+" should be either #key or #value");
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void doIfIsCollection(Object value, Field field, Field nextField, Object list, boolean isLastIteration, String fieldName, int addressIndex, int fieldIndex)
			throws ReflectionException {
		if( Collection.class.isAssignableFrom(field.getType()) ){
			log.atDebug().log("Processing collection field: {}", fieldName);
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

	