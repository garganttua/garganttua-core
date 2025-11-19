package com.garganttua.core.reflection.fields;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ObjectFieldSetter {

	private Class<?> clazz;
	private List<Object> fields;
	private ObjectAddress address;

	public ObjectFieldSetter(Class<?> clazz, List<Object> fields, ObjectAddress address)
			throws ReflectionException {
		if (clazz == null) {
			throw new ReflectionException("class is null");
		}
		if (fields == null) {
			throw new ReflectionException("fields is null");
		}
		if (address == null) {
			throw new ReflectionException("address is null");
		}

		this.clazz = clazz;
		this.fields = fields;
		this.address = address;
	}

	public Object setValue(Object fieldValue) throws ReflectionException {
		if (log.isDebugEnabled()) {
			log.debug("Setting value of object : class {}, address {}", this.clazz, this.address);
		}
		return this.setValue(ObjectReflectionHelper.instanciateNewObject(clazz), fieldValue);
	}

	public Object setValue(Object object, Object value) throws ReflectionException {
		if (log.isDebugEnabled()) {
			log.debug("Setting value of object : {}, value {}, class {}, address {}", object, value, this.clazz,
					this.address);
		}
		if (object == null) {
			throw new ReflectionException("object is null");
		}
		if( !object.getClass().isAssignableFrom(this.clazz) ) {
			throw new ReflectionException("object is not of type "+this.clazz);
		}

		if (this.fields.size() == 1) {
			Field field = (Field) this.fields.get(0);
			String fieldName = this.address.getElement(0);
			if (!field.getName().equals(fieldName)) {
				throw new ReflectionException("field names of address " + fieldName + " and fields list "
						+ field.getName() + " do not match");
			}
			ObjectReflectionHelper.setObjectFieldValue(object, field, value);
		} else {
			this.setValueRecursively(object, value, 0, 0);
		}
		return object;
	}

	private void setValueRecursively(Object object, Object value, int fieldIndex, int fieldNameIndex)
			throws ReflectionException {
		if (log.isDebugEnabled()) {
			log.debug(
					"Setting value recursively of object : {}, value {}, class {}, address {}, fieldIndex {}, fieldNameIndex {}",
					object, value, this.clazz, this.address, fieldIndex, fieldNameIndex);
		}
		boolean isLastIteration = (fieldIndex + 2 == this.fields.size());
		Field field = (Field) this.fields.get(fieldIndex);
		Field nextField = (Field) this.fields.get(fieldIndex + 1);
		String fieldName = this.address.getElement(fieldNameIndex);

		if (!field.getName().equals(fieldName)) {
			throw new ReflectionException(
					"field names of address " + fieldName + " and fields list " + field.getName() + " do not match");
		}

		Object temp = ObjectReflectionHelper.getObjectFieldValue(object, field);
		if (temp == null ) {
			temp = Fields.instanciate(field);
			ObjectReflectionHelper.setObjectFieldValue(object, field, temp);
		}

		if (Fields.isArrayOrMapOrCollectionField(field)) {
			this.doIfIsCollection(object, value, fieldIndex, fieldNameIndex, isLastIteration, field, nextField, temp);
			this.doIfIsArray(object, value, fieldIndex, fieldNameIndex, isLastIteration, field, nextField, temp);
			this.doIfIsMap(object, value, fieldIndex, fieldNameIndex, isLastIteration, field, nextField, temp);
		} else {
			if (isLastIteration) {
				ObjectReflectionHelper.setObjectFieldValue(temp, nextField, value);
			} else {
				this.setValueRecursively(temp, value, fieldIndex + 1, fieldNameIndex + 1);
			}
		}
	}
	
	private void doIfIsMap(Object object, Object value, int fieldIndex, int fieldNameIndex,
			boolean isLastIteration, Field field, Field nextField, Object temp)
			throws ReflectionException {
		if (Map.class.isAssignableFrom(field.getType())) {
			
			Map<Object,Object> collectionTarget = (Map<Object,Object>) temp;
			List<?> collectionSource = ((List<?>) value);

			if (collectionSource.size() != collectionTarget.size()) {
				int nbToCreate = collectionSource.size() - collectionTarget.size();
				if (nbToCreate > 0) {
					Class<?> keyType = Fields.getGenericType(field, 0);
					Class<?> valueType = Fields.getGenericType(field, 1);
					for (int i = 0; i < nbToCreate; i++) {
						collectionTarget.put(ObjectReflectionHelper.instanciateNewObject(keyType), ObjectReflectionHelper.instanciateNewObject(valueType));
					}
				}
			}
			
			String nextFieldName = this.address.getElement(fieldNameIndex+1);
			Iterator<?> it = null;
			if( nextFieldName.equals(ObjectAddress.MAP_KEY_INDICATOR) ) {
				it = collectionTarget.keySet().iterator();
			}
			if( nextFieldName.equals(ObjectAddress.MAP_VALUE_INDICATOR) ) {
				it = collectionTarget.values().iterator();
			}
			if( it == null ) {
				throw new ReflectionException("Invalid address, "+nextFieldName+" should be either #key or #value");
			}
			for (int i = 0; i < collectionSource.size(); i++) {
				Object tempObject = it.next();
				if (isLastIteration) {
					ObjectReflectionHelper.setObjectFieldValue(tempObject, nextField, collectionSource.get(i));
				} else {
					this.setValueRecursively(tempObject, collectionSource.get(i), fieldIndex + 1, fieldNameIndex + 2);
				}
			}
		}
	}

	private void doIfIsArray(Object object, Object value, int fieldIndex, int fieldNameIndex, boolean isLastIteration,
			Field field, Field nextField, Object temp)
			throws ReflectionException {
		if (field.getType().isArray()) {
			if (field.getType().getComponentType().isArray()) {
				this.handleMultiDimensionalArray(object, value, fieldIndex, fieldNameIndex, isLastIteration, field,
						nextField, value);
			} else {
				Object[] collectionTarget = (Object[]) temp;
				List<?> collectionSource = ((List<?>) value);

				Class<?> listObjectType = field.getType().getComponentType();

				if (collectionSource.size() != collectionTarget.length) {
					int nbToCreate = collectionSource.size() - collectionTarget.length;
					if (nbToCreate > 0) {
						collectionTarget = Arrays.copyOf(collectionTarget, collectionTarget.length + nbToCreate);
					}
					for (int i = 0; i < nbToCreate; i++) {
						collectionTarget[collectionTarget.length + i - 2] = ObjectReflectionHelper
								.instanciateNewObject(listObjectType);
					}

					ObjectReflectionHelper.setObjectFieldValue(object, field, collectionTarget);
				}

				for (int i = 0; i < collectionSource.size(); i++) {
					Object tempObject = collectionTarget[i];
					if (isLastIteration) {
						ObjectReflectionHelper.setObjectFieldValue(tempObject, nextField, collectionSource.get(i));
					} else {
						this.setValueRecursively(tempObject, collectionSource.get(i), fieldIndex + 1, fieldNameIndex + 1);
					}
				}
			}
		}
	}

	private void doIfIsCollection(Object object, Object value, int fieldIndex, int fieldNameIndex,
			boolean isLastIteration, Field field, Field nextField, Object temp)
			throws ReflectionException {
		if (Collection.class.isAssignableFrom(field.getType())) {
			
			Collection<Object> collectionTarget = (Collection<Object>) temp;
			List<?> collectionSource = ((List<?>) value);

			if (collectionSource.size() != collectionTarget.size()) {
				int nbToCreate = collectionSource.size() - collectionTarget.size();
				if (nbToCreate > 0) {
					Class<?> listObjectType = Fields.getGenericType(field, 0);
					for (int i = 0; i < nbToCreate; i++) {
						collectionTarget.add(ObjectReflectionHelper.instanciateNewObject(listObjectType));
					}
				}
			}
			Iterator<?> it = collectionTarget.iterator();
			for (int i = 0; i < collectionSource.size(); i++) {
				Object tempObject = it.next();
				if (isLastIteration) {
					ObjectReflectionHelper.setObjectFieldValue(tempObject, nextField, collectionSource.get(i));
				} else {
					this.setValueRecursively(tempObject, collectionSource.get(i), fieldIndex + 1, fieldNameIndex + 1);
				}
			}
		}
	}

	private void handleMultiDimensionalArray(Object object, Object value, int fieldIndex, int fieldNameIndex,
			boolean isLastIteration, Field field, Field nextField, Object fieldValue)
			throws ReflectionException {
		Object[] array = (Object[]) fieldValue;
		Object[] sourceArray = (Object[]) value;

		if (array == null || array.length != sourceArray.length) {
			Class<?> componentType = field.getType().getComponentType();
			array = (Object[]) Array.newInstance(componentType, sourceArray.length);
			ObjectReflectionHelper.setObjectFieldValue(object, field, array);
		}

		for (int i = 0; i < sourceArray.length; i++) {
			Object subArray = array[i];
			if (subArray == null) {
				Class<?> componentType = field.getType().getComponentType();
				subArray = Array.newInstance(componentType, sourceArray.length);
				array[i] = subArray;
			}
			if (isLastIteration) {
				Array.set(subArray, i, sourceArray[i]);
			} else {
				handleMultiDimensionalArray(subArray, sourceArray[i], fieldIndex + 1, fieldNameIndex + 1,
						isLastIteration, field, nextField, fieldValue);
			}
		}
	}
}
