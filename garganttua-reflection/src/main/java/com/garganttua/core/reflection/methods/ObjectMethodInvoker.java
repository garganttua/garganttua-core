package com.garganttua.core.reflection.methods;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.fields.Fields;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ObjectMethodInvoker {

	private Class<?> clazz;
	private List<Object> fields;
	private ObjectAddress address;

	public ObjectMethodInvoker(Class<?> clazz, List<Object> fields,
			ObjectAddress address) throws ReflectionException {
		log.atTrace().log("Creating ObjectMethodInvoker for class={}, address={}, fields count={}", clazz, address, fields != null ? fields.size() : 0);
		if (clazz == null) {
			log.atError().log("class parameter is null");
			throw new ReflectionException("class is null");
		}
		if (fields == null) {
			log.atError().log("fields parameter is null");
			throw new ReflectionException("fields is null");
		}
		if (address == null) {
			log.atError().log("address parameter is null");
			throw new ReflectionException("address is null");
		}

		this.clazz = clazz;
		this.fields = fields;
		this.address = address;
		log.atDebug().log("ObjectMethodInvoker initialized for class={}, address={}", clazz.getName(), address);
	}

	public Object invoke(Object object, Object... args) throws ReflectionException {
		log.atTrace().log("invoke entry: object={}, class={}, address={}, args count={}", object, this.clazz, this.address, args != null ? args.length : 0);
		log.atDebug().log("Invoking method: class={}, address={}, parameters count={}", this.clazz.getName(), this.address, args != null ? args.length : 0);

		if (object == null) {
			log.atError().log("object parameter is null");
			throw new ReflectionException("object is null");
		}
		if (!object.getClass().isAssignableFrom(this.clazz)) {
			log.atError().log("object type {} is not assignable from {}", object.getClass(), this.clazz);
			throw new ReflectionException("object is not of type " + this.clazz);
		}

		if (this.fields.size() == 1) {
			Method method = (Method) fields.get(0);
			String methodName = this.address.getElement(0);
			log.atDebug().log("Direct method invocation: methodName={}", methodName);

			if (!method.getName().equals(methodName)) {
				log.atError().log("method names mismatch: address={}, method={}", methodName, method.getName());
				throw new ReflectionException("method names of address " + methodName + " and fields list "
						+ method.getName() + " do not match");
			}

			Object result = ObjectReflectionHelper.invokeMethod(object, methodName, method, args);
			log.atInfo().log("Successfully invoked method {} on class {}", methodName, this.clazz.getName());
			return result;

		} else {
			log.atDebug().log("Recursive method invocation with {} fields", this.fields.size());
			Object result = this.invokeMethodRecursively(object, 0, 0, args);
			log.atInfo().log("Successfully invoked method recursively for address={}", this.address);
			return result;
		}
	}

	private Object invokeMethodRecursively(Object object, int fieldIndex, int fieldNameIndex, Object... args)
			throws ReflectionException {
		log.atTrace().log("invokeMethodRecursively: fieldIndex={}, fieldNameIndex={}", fieldIndex, fieldNameIndex);
		log.atDebug().log("Invoking method recursively: class={}, address={}, fieldIndex={}, fieldNameIndex={}", this.clazz.getName(), this.address, fieldIndex, fieldNameIndex);
		boolean isLastIteration = (fieldIndex + 2 == this.fields.size());
		Field field = (Field) this.fields.get(fieldIndex);
		String fieldName = this.address.getElement(fieldNameIndex);

		if (!field.getName().equals(fieldName)) {
			log.atError().log("field names mismatch: address={}, field={}", fieldName, field.getName());
			throw new ReflectionException(
					"field names of address " + fieldName + " and fields list " + field.getName() + " do not match");
		}

		Object temp = ObjectReflectionHelper.getObjectFieldValue(object, field);
		if (temp == null) {
			log.atError().log("Field {} is null, cannot invoke method with address {}", fieldName, this.address);
			throw new ReflectionException("cannot invoke method with address " + this.address + ". The field "
					+ fieldName + " of object " + object + " is null");
		}

		if (Fields.isArrayOrMapOrCollectionField(field)) {
			log.atDebug().log("Field {} is array/map/collection type", fieldName);
			List<Object> returned = new ArrayList<Object>();
			this.doIfIsCollection(fieldIndex, fieldNameIndex, isLastIteration, field, temp, returned, args);
			this.doIfIsMap(fieldIndex, fieldNameIndex, isLastIteration, field, temp, returned, args);
			this.doIfIsArray(fieldIndex, fieldNameIndex, isLastIteration, field, temp, returned, args);
			return returned;
		} else {
			if (isLastIteration) {
				log.atDebug().log("Last iteration, invoking final method");
				Method leafMethod = (Method) this.fields.get(fieldIndex + 1);
				String methodName = this.address.getElement(fieldNameIndex + 1);
				return ObjectReflectionHelper.invokeMethod(temp, methodName, leafMethod, args);
			} else {
				log.atTrace().log("Continuing recursive method invocation");
				return this.invokeMethodRecursively(temp, fieldIndex + 1, fieldNameIndex + 1, args);
			}
		}
	}

	private void doIfIsArray(int fieldIndex, int fieldNameIndex, boolean isLastIteration, Field field, Object temp,
			List<Object> returned, Object[] args) throws ReflectionException {
		if (field.getType().isArray()) {
			log.atDebug().log("Processing array field: {}", field.getName());
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
			log.atDebug().log("Processing map field: {}", field.getName());
			Map<Object, Object> sub = (Map<Object, Object>) temp;
			String mapElement = this.address.getElement(fieldNameIndex + 1);
			Iterator<?> it = null;
			if (mapElement.equals(ObjectAddress.MAP_KEY_INDICATOR)) {
				log.atDebug().log("Processing map keys");
				it = sub.keySet().iterator();
			}
			if (mapElement.equals(ObjectAddress.MAP_VALUE_INDICATOR)) {
				log.atDebug().log("Processing map values");
				it = sub.values().iterator();
			}
			if (it == null) {
				log.atError().log("Invalid map address element: {}, expected #key or #value", mapElement);
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
			log.atDebug().log("Processing collection field: {}", field.getName());
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
