package com.garganttua.reflection.methods;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.reflection.fields.Fields;
import com.garganttua.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ObjectMethodInvoker {

	private Class<?> clazz;
	private List<Object> fields;
	private ObjectAddress address;

	public ObjectMethodInvoker(Class<?> clazz, List<Object> fields,
			ObjectAddress address) throws ReflectionException {
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

	public Object invoke(Object object, Object... args) throws ReflectionException {
		if (log.isDebugEnabled()) {
			log.debug("Invoking method of object : class {}, address {}, parameters {}", this.clazz, this.address,
					args);
		}

		if (object == null) {
			throw new ReflectionException("object is null");
		}
		if (!object.getClass().isAssignableFrom(this.clazz)) {
			throw new ReflectionException("object is not of type " + this.clazz);
		}

		if (this.fields.size() == 1) {
			Method method = (Method) fields.get(0);
			String methodName = this.address.getElement(0);

			if (!method.getName().equals(methodName)) {
				throw new ReflectionException("method names of address " + methodName + " and fields list "
						+ method.getName() + " do not match");
			}

			return ObjectReflectionHelper.invokeMethod(object, methodName, method, args);

		} else {
			return this.invokeMethodRecursively(object, 0, 0, args);
		}
	}

	private Object invokeMethodRecursively(Object object, int fieldIndex, int fieldNameIndex, Object... args)
			throws ReflectionException {
		if (log.isDebugEnabled()) {
			log.debug(
					"Invoking method of object : class {}, address {}, parameters {}, address {}, fieldIndex {}, fieldNameIndex {}",
					this.clazz, this.address, args, fieldIndex, fieldNameIndex);
		}
		boolean isLastIteration = (fieldIndex + 2 == this.fields.size());
		Field field = (Field) this.fields.get(fieldIndex);
		String fieldName = this.address.getElement(fieldNameIndex);

		if (!field.getName().equals(fieldName)) {
			throw new ReflectionException(
					"field names of address " + fieldName + " and fields list " + field.getName() + " do not match");
		}

		Object temp = ObjectReflectionHelper.getObjectFieldValue(object, field);
		if (temp == null) {
			throw new ReflectionException("cannot invoke method with address " + this.address + ". The field "
					+ fieldName + " of object " + object + " is null");
		}

		if (Fields.isArrayOrMapOrCollectionField(field)) {
			List<Object> returned = new ArrayList<Object>();
			this.doIfIsCollection(fieldIndex, fieldNameIndex, isLastIteration, field, temp, returned, args);
			this.doIfIsMap(fieldIndex, fieldNameIndex, isLastIteration, field, temp, returned, args);
			this.doIfIsArray(fieldIndex, fieldNameIndex, isLastIteration, field, temp, returned, args);
			return returned;
		} else {
			if (isLastIteration) {
				Method leafMethod = (Method) this.fields.get(fieldIndex + 1);
				String methodName = this.address.getElement(fieldNameIndex + 1);
				return ObjectReflectionHelper.invokeMethod(temp, methodName, leafMethod, args);
			} else {
				return this.invokeMethodRecursively(temp, fieldIndex + 1, fieldNameIndex + 1, args);
			}
		}
	}

	private void doIfIsArray(int fieldIndex, int fieldNameIndex, boolean isLastIteration, Field field, Object temp,
			List<Object> returned, Object[] args) throws ReflectionException {
		if (field.getType().isArray()) {
			Object[] array = (Object[]) temp;

			for (Object obj : array) {
				if (isLastIteration) {
					Method leafMethod = (Method) this.fields.get(fieldIndex + 1);
					String methodName = this.address.getElement(fieldNameIndex + 1);
					returned.add(ObjectReflectionHelper.invokeMethod(obj, methodName, leafMethod, args));
				} else {
					returned.add(this.invokeMethodRecursively(obj, fieldIndex + 1, fieldNameIndex + 1, args));
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void doIfIsMap(int fieldIndex, int fieldNameIndex, boolean isLastIteration, Field field, Object temp,
			List<Object> returned, Object[] args) throws ReflectionException {
		if (Map.class.isAssignableFrom(field.getType())) {
			Map<Object, Object> sub = (Map<Object, Object>) temp;
			String mapElement = this.address.getElement(fieldNameIndex + 1);
			Iterator<?> it = null;
			if (mapElement.equals(ObjectAddress.MAP_KEY_INDICATOR)) {
				it = sub.keySet().iterator();
			}
			if (mapElement.equals(ObjectAddress.MAP_VALUE_INDICATOR)) {
				it = sub.values().iterator();
			}
			if (it == null) {
				throw new ReflectionException("Invalid address, " + mapElement + " should be either #key or #value");
			}
			for (int i = 0; i < sub.size(); i++) {
				Object tempObject = it.next();
				if (isLastIteration) {
					Method leafMethod = (Method) this.fields.get(fieldIndex + 2);
					String methodName = this.address.getElement(fieldNameIndex + 2);
					returned.add(ObjectReflectionHelper.invokeMethod(tempObject, methodName, leafMethod, args));
				} else {
					returned.add(this.invokeMethodRecursively(tempObject, fieldIndex + 2, fieldNameIndex + 2, args));
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void doIfIsCollection(int fieldIndex, int fieldNameIndex, boolean isLastIteration, Field field, Object temp,
			List<Object> returned, Object... args)
			throws ReflectionException {
		if (Collection.class.isAssignableFrom(field.getType())) {
			Collection<Object> sub = (Collection<Object>) temp;
			for (Object obj : sub) {
				if (isLastIteration) {
					Method leafMethod = (Method) this.fields.get(fieldIndex + 1);
					String methodName = this.address.getElement(fieldNameIndex + 1);
					returned.add(ObjectReflectionHelper.invokeMethod(obj, methodName, leafMethod, args));
				} else {
					returned.add(this.invokeMethodRecursively(obj, fieldIndex + 1, fieldNameIndex + 1, args));
				}
			}
		}
	}
}
