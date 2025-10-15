package com.garganttua.reflection.methods;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.garganttua.reflection.GGObjectAddress;
import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.fields.GGFields;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GGObjectMethodInvoker {

	private Class<?> clazz;
	private List<Object> fields;
	private GGObjectAddress address;

	public GGObjectMethodInvoker(Class<?> clazz, List<Object> fields,
			GGObjectAddress address) throws GGReflectionException {
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

	public Object invoke(Object object, Object... args) throws GGReflectionException {
		if (log.isDebugEnabled()) {
			log.debug("Invoking method of object : class {}, address {}, parameters {}", this.clazz, this.address,
					args);
		}

		if (object == null) {
			throw new GGReflectionException("object is null");
		}
		if (!object.getClass().isAssignableFrom(this.clazz)) {
			throw new GGReflectionException("object is not of type " + this.clazz);
		}

		if (this.fields.size() == 1) {
			Method method = (Method) fields.get(0);
			String methodName = this.address.getElement(0);

			if (!method.getName().equals(methodName)) {
				throw new GGReflectionException("method names of address " + methodName + " and fields list "
						+ method.getName() + " do not match");
			}

			return GGObjectReflectionHelper.invokeMethod(object, methodName, method, args);

		} else {
			return this.invokeMethodRecursively(object, 0, 0, args);
		}
	}

	private Object invokeMethodRecursively(Object object, int fieldIndex, int fieldNameIndex, Object... args)
			throws GGReflectionException {
		if (log.isDebugEnabled()) {
			log.debug(
					"Invoking method of object : class {}, address {}, parameters {}, address {}, fieldIndex {}, fieldNameIndex {}",
					this.clazz, this.address, args, fieldIndex, fieldNameIndex);
		}
		boolean isLastIteration = (fieldIndex + 2 == this.fields.size());
		Field field = (Field) this.fields.get(fieldIndex);
		String fieldName = this.address.getElement(fieldNameIndex);

		if (!field.getName().equals(fieldName)) {
			throw new GGReflectionException(
					"field names of address " + fieldName + " and fields list " + field.getName() + " do not match");
		}

		Object temp = GGObjectReflectionHelper.getObjectFieldValue(object, field);
		if (temp == null) {
			throw new GGReflectionException("cannot invoke method with address " + this.address + ". The field "
					+ fieldName + " of object " + object + " is null");
		}

		if (GGFields.isArrayOrMapOrCollectionField(field)) {
			List<Object> returned = new ArrayList<Object>();
			this.doIfIsCollection(fieldIndex, fieldNameIndex, isLastIteration, field, temp, returned, args);
			this.doIfIsMap(fieldIndex, fieldNameIndex, isLastIteration, field, temp, returned, args);
			this.doIfIsArray(fieldIndex, fieldNameIndex, isLastIteration, field, temp, returned, args);
			return returned;
		} else {
			if (isLastIteration) {
				Method leafMethod = (Method) this.fields.get(fieldIndex + 1);
				String methodName = this.address.getElement(fieldNameIndex + 1);
				return GGObjectReflectionHelper.invokeMethod(temp, methodName, leafMethod, args);
			} else {
				return this.invokeMethodRecursively(temp, fieldIndex + 1, fieldNameIndex + 1, args);
			}
		}
	}

	private void doIfIsArray(int fieldIndex, int fieldNameIndex, boolean isLastIteration, Field field, Object temp,
			List<Object> returned, Object[] args) throws GGReflectionException {
		if (field.getType().isArray()) {
			Object[] array = (Object[]) temp;

			for (Object obj : array) {
				if (isLastIteration) {
					Method leafMethod = (Method) this.fields.get(fieldIndex + 1);
					String methodName = this.address.getElement(fieldNameIndex + 1);
					returned.add(GGObjectReflectionHelper.invokeMethod(obj, methodName, leafMethod, args));
				} else {
					returned.add(this.invokeMethodRecursively(obj, fieldIndex + 1, fieldNameIndex + 1, args));
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void doIfIsMap(int fieldIndex, int fieldNameIndex, boolean isLastIteration, Field field, Object temp,
			List<Object> returned, Object[] args) throws GGReflectionException {
		if (Map.class.isAssignableFrom(field.getType())) {
			Map<Object, Object> sub = (Map<Object, Object>) temp;
			String mapElement = this.address.getElement(fieldNameIndex + 1);
			Iterator<?> it = null;
			if (mapElement.equals(GGObjectAddress.MAP_KEY_INDICATOR)) {
				it = sub.keySet().iterator();
			}
			if (mapElement.equals(GGObjectAddress.MAP_VALUE_INDICATOR)) {
				it = sub.values().iterator();
			}
			if (it == null) {
				throw new GGReflectionException("Invalid address, " + mapElement + " should be either #key or #value");
			}
			for (int i = 0; i < sub.size(); i++) {
				Object tempObject = it.next();
				if (isLastIteration) {
					Method leafMethod = (Method) this.fields.get(fieldIndex + 2);
					String methodName = this.address.getElement(fieldNameIndex + 2);
					returned.add(GGObjectReflectionHelper.invokeMethod(tempObject, methodName, leafMethod, args));
				} else {
					returned.add(this.invokeMethodRecursively(tempObject, fieldIndex + 2, fieldNameIndex + 2, args));
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void doIfIsCollection(int fieldIndex, int fieldNameIndex, boolean isLastIteration, Field field, Object temp,
			List<Object> returned, Object... args)
			throws GGReflectionException {
		if (Collection.class.isAssignableFrom(field.getType())) {
			Collection<Object> sub = (Collection<Object>) temp;
			for (Object obj : sub) {
				if (isLastIteration) {
					Method leafMethod = (Method) this.fields.get(fieldIndex + 1);
					String methodName = this.address.getElement(fieldNameIndex + 1);
					returned.add(GGObjectReflectionHelper.invokeMethod(obj, methodName, leafMethod, args));
				} else {
					returned.add(this.invokeMethodRecursively(obj, fieldIndex + 1, fieldNameIndex + 1, args));
				}
			}
		}
	}
}
