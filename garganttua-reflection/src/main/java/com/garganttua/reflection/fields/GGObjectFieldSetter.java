package com.garganttua.reflection.fields;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.garganttua.reflection.GGObjectAddress;
import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GGObjectFieldSetter {

	private Class<?> clazz;
	private List<Object> fields;
	private GGObjectAddress address;

	public GGObjectFieldSetter(Class<?> clazz, List<Object> fields, GGObjectAddress address)
			throws GGReflectionException {
		if (clazz == null) {
			throw new GGReflectionException("class is null");
		}
		if (fields == null) {
			throw new GGReflectionException("fields is null");
		}
		if (address == null) {
			throw new GGReflectionException("address is null");
		}

		this.clazz = clazz;
		this.fields = fields;
		this.address = address;
	}

	public Object setValue(Object fieldValue) throws GGReflectionException {
		if (log.isDebugEnabled()) {
			log.debug("Setting value of object : class {}, address {}", this.clazz, this.address);
		}
		return this.setValue(GGObjectReflectionHelper.instanciateNewObject(clazz), fieldValue);
	}

	public Object setValue(Object object, Object value) throws GGReflectionException {
		if (log.isDebugEnabled()) {
			log.debug("Setting value of object : {}, value {}, class {}, address {}", object, value, this.clazz,
					this.address);
		}
		if (object == null) {
			throw new GGReflectionException("object is null");
		}
		if( !object.getClass().isAssignableFrom(this.clazz) ) {
			throw new GGReflectionException("object is not of type "+this.clazz);
		}

		if (this.fields.size() == 1) {
			Field field = (Field) this.fields.get(0);
			String fieldName = this.address.getElement(0);
			if (!field.getName().equals(fieldName)) {
				throw new GGReflectionException("field names of address " + fieldName + " and fields list "
						+ field.getName() + " do not match");
			}
			GGObjectReflectionHelper.setObjectFieldValue(object, field, value);
		} else {
			this.setValueRecursively(object, value, 0, 0);
		}
		return object;
	}

	private void setValueRecursively(Object object, Object value, int fieldIndex, int fieldNameIndex)
			throws GGReflectionException {
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
			throw new GGReflectionException(
					"field names of address " + fieldName + " and fields list " + field.getName() + " do not match");
		}

		Object temp = GGObjectReflectionHelper.getObjectFieldValue(object, field);
		if (temp == null ) {
			temp = GGFields.instanciate(field);
			GGObjectReflectionHelper.setObjectFieldValue(object, field, temp);
		}

		if (GGFields.isArrayOrMapOrCollectionField(field)) {
			this.doIfIsCollection(object, value, fieldIndex, fieldNameIndex, isLastIteration, field, nextField, temp);
			this.doIfIsArray(object, value, fieldIndex, fieldNameIndex, isLastIteration, field, nextField, temp);
			this.doIfIsMap(object, value, fieldIndex, fieldNameIndex, isLastIteration, field, nextField, temp);
		} else {
			if (isLastIteration) {
				GGObjectReflectionHelper.setObjectFieldValue(temp, nextField, value);
			} else {
				this.setValueRecursively(temp, value, fieldIndex + 1, fieldNameIndex + 1);
			}
		}
	}
	
	private void doIfIsMap(Object object, Object value, int fieldIndex, int fieldNameIndex,
			boolean isLastIteration, Field field, Field nextField, Object temp)
			throws GGReflectionException {
		if (Map.class.isAssignableFrom(field.getType())) {
			
			Map<?,?> collectionTarget = (Map<?,?>) temp;
			List<?> collectionSource = ((List<?>) value);

			if (collectionSource.size() != collectionTarget.size()) {
				int nbToCreate = collectionSource.size() - collectionTarget.size();
				if (nbToCreate > 0) {
					Class<?> keyType = GGFields.getGenericType(field, 0);
					Class<?> valueType = GGFields.getGenericType(field, 1);
					for (int i = 0; i < nbToCreate; i++) {
						collectionTarget.put(GGObjectReflectionHelper.instanciateNewObject(keyType), GGObjectReflectionHelper.instanciateNewObject(valueType));
					}
				}
			}
			
			String nextFieldName = this.address.getElement(fieldNameIndex+1);
			Iterator<?> it = null;
			if( nextFieldName.equals(GGObjectAddress.MAP_KEY_INDICATOR) ) {
				it = collectionTarget.keySet().iterator();
			}
			if( nextFieldName.equals(GGObjectAddress.MAP_VALUE_INDICATOR) ) {
				it = collectionTarget.values().iterator();
			}
			if( it == null ) {
				throw new GGReflectionException("Invalid address, "+nextFieldName+" should be either #key or #value");
			}
			for (int i = 0; i < collectionSource.size(); i++) {
				Object tempObject = it.next();
				if (isLastIteration) {
					GGObjectReflectionHelper.setObjectFieldValue(tempObject, nextField, collectionSource.get(i));
				} else {
					this.setValueRecursively(tempObject, collectionSource.get(i), fieldIndex + 1, fieldNameIndex + 2);
				}
			}
		}
	}

	private void doIfIsArray(Object object, Object value, int fieldIndex, int fieldNameIndex, boolean isLastIteration,
			Field field, Field nextField, Object temp)
			throws GGReflectionException {
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
						collectionTarget[collectionTarget.length + i - 2] = GGObjectReflectionHelper
								.instanciateNewObject(listObjectType);
					}

					GGObjectReflectionHelper.setObjectFieldValue(object, field, collectionTarget);
				}

				for (int i = 0; i < collectionSource.size(); i++) {
					Object tempObject = collectionTarget[i];
					if (isLastIteration) {
						GGObjectReflectionHelper.setObjectFieldValue(tempObject, nextField, collectionSource.get(i));
					} else {
						this.setValueRecursively(tempObject, collectionSource.get(i), fieldIndex + 1, fieldNameIndex + 1);
					}
				}
			}
		}
	}

	private void doIfIsCollection(Object object, Object value, int fieldIndex, int fieldNameIndex,
			boolean isLastIteration, Field field, Field nextField, Object temp)
			throws GGReflectionException {
		if (Collection.class.isAssignableFrom(field.getType())) {
			
			Collection<?> collectionTarget = (Collection<?>) temp;
			List<?> collectionSource = ((List<?>) value);

			if (collectionSource.size() != collectionTarget.size()) {
				int nbToCreate = collectionSource.size() - collectionTarget.size();
				if (nbToCreate > 0) {
					Class<?> listObjectType = GGFields.getGenericType(field, 0);
					for (int i = 0; i < nbToCreate; i++) {
						collectionTarget.add(GGObjectReflectionHelper.instanciateNewObject(listObjectType));
					}
				}
			}
			Iterator<?> it = collectionTarget.iterator();
			for (int i = 0; i < collectionSource.size(); i++) {
				Object tempObject = it.next();
				if (isLastIteration) {
					GGObjectReflectionHelper.setObjectFieldValue(tempObject, nextField, collectionSource.get(i));
				} else {
					this.setValueRecursively(tempObject, collectionSource.get(i), fieldIndex + 1, fieldNameIndex + 1);
				}
			}
		}
	}

	private void handleMultiDimensionalArray(Object object, Object value, int fieldIndex, int fieldNameIndex,
			boolean isLastIteration, Field field, Field nextField, Object fieldValue)
			throws GGReflectionException {
		Object[] array = (Object[]) fieldValue;
		Object[] sourceArray = (Object[]) value;

		if (array == null || array.length != sourceArray.length) {
			Class<?> componentType = field.getType().getComponentType();
			array = (Object[]) Array.newInstance(componentType, sourceArray.length);
			GGObjectReflectionHelper.setObjectFieldValue(object, field, array);
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
