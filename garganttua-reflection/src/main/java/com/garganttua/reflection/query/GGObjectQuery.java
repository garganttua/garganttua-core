package com.garganttua.reflection.query;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.garganttua.reflection.GGObjectAddress;
import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.fields.GGFields;
import com.garganttua.reflection.fields.GGObjectFieldGetter;
import com.garganttua.reflection.fields.GGObjectFieldSetter;
import com.garganttua.reflection.methods.GGObjectMethodInvoker;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GGObjectQuery implements IGGObjectQuery {

	private Class<?> objectClass;
	private Object object;

	protected GGObjectQuery(Class<?> objectClass) throws GGReflectionException {
		if (objectClass == null) {
			throw new GGReflectionException("class is null");
		}
		this.objectClass = objectClass;
		this.object = GGObjectReflectionHelper.instanciateNewObject(objectClass);
	}

	protected GGObjectQuery(Object object) throws GGReflectionException {
		if (object == null) {
			throw new GGReflectionException("object is null");
		}
		this.object = object;
		this.objectClass = object.getClass();
	}

	protected GGObjectQuery(Class<?> objectClass, Object object) throws GGReflectionException {
		if (object == null) {
			throw new GGReflectionException("object is null");
		}
		if (objectClass == null) {
			throw new GGReflectionException("class is null");
		}
		if (!object.getClass().isAssignableFrom(objectClass)) {
			throw new GGReflectionException(
					"Provided class " + objectClass + " and object " + object.getClass() + " do not match");
		}
		this.object = object;
		this.objectClass = objectClass;
	}

	@Override
	public List<Object> find(String fieldAddress) throws GGReflectionException {
		return this.find(new GGObjectAddress(fieldAddress, true));
	}

	@Override
	public List<Object> find(GGObjectAddress fieldAddress)
			throws GGReflectionException {
		if (log.isDebugEnabled()) {
			log.debug("Looking for field " + fieldAddress + " in " + objectClass);
		}
		List<Object> list = new ArrayList<Object>();
		return this.findRecursively(this.objectClass, fieldAddress, 0, list);
	}

	private List<Object> findRecursively(Class<?> clazz, GGObjectAddress address, int index,
			List<Object> list) throws GGReflectionException {
		if (log.isDebugEnabled()) {
			log.debug("Looking for object element " + address.getElement(index) + " in " + clazz);
		}

		if (clazz == null || index >= address.length()) {
			throw new GGReflectionException(
					"Object element " + address.getElement(index) + " not found in class " + clazz);
		}

		Field field = GGObjectReflectionHelper.getField(clazz, address.getElement(index));

		Method method = null;
		if (index == address.length() - 1 && field == null) {
			method = GGObjectReflectionHelper.getMethod(clazz, address.getElement(index));
		}

		if (field == null && method == null) {
			if (clazz.getSuperclass() != null) {
				return this.findRecursively(clazz.getSuperclass(), address, index, list);
			}
		} else if (field != null && method == null) {
			list.add(field);
			if (index == address.length() - 1) {
				return list;
			} else {
				Class<?> fieldType = field.getType();
				if (Collection.class.isAssignableFrom(fieldType)) {
					Class<?> genericType = GGFields.getGenericType(field, 0);
					return this.findRecursively(genericType, address, index + 1, list);
				} else if (Map.class.isAssignableFrom(fieldType)) {

					if (address.getElement(index + 1).equals(GGObjectAddress.MAP_VALUE_INDICATOR)) {
						Class<?> genericType = GGFields.getGenericType(field, 1);
						return this.findRecursively(genericType, address, index + 2, list);
					} else if (address.getElement(index + 1).equals(GGObjectAddress.MAP_KEY_INDICATOR)) {
						Class<?> genericType = GGFields.getGenericType(field, 0);
						return this.findRecursively(genericType, address, index + 2, list);
					} else {
						throw new GGReflectionException("Field " + address.getElement(index)
								+ " is a map, so address must indicate key or value");
					}

				} else {
					return this.findRecursively(field.getType(), address, index + 1, list);
				}
			}
		} else if (field == null && method != null) {
			list.add(method);
			return list;
		} else if (field != null && method != null) {
			throw new GGReflectionException(
					"Object element " + address.getElement(index) + " is also a field and a method in " + clazz);
		}

		throw new GGReflectionException(
				"Object element " + address.getElement(index) + " not found in class " + clazz);
	}

	@Override
	public GGObjectAddress address(String elementName) throws GGReflectionException {
		if (log.isDebugEnabled()) {
			log.debug("Looking for object element " + elementName + " in " + objectClass);
		}
		return this.address(this.objectClass, elementName, null);
	}

	private GGObjectAddress address(Class<?> objectClass, String elementName, GGObjectAddress address)
			throws GGReflectionException {
		if (log.isDebugEnabled()) {
			log.debug("Looking for object element " + elementName + " in " + objectClass + " address " + address);
		}
		Field field = null;
		try {
			field = objectClass.getDeclaredField(elementName);
		} catch (NoSuchFieldException | SecurityException e) {

		}

		Method method = GGObjectReflectionHelper.getMethod(objectClass, elementName);

		if (method != null) {
			return new GGObjectAddress(address == null ? elementName : address + "." + elementName, true);
		}
		if (field != null) {
			return new GGObjectAddress(address == null ? elementName : address + "." + elementName, true);
		}

		if (objectClass.getSuperclass() != null && !Object.class.equals(objectClass.getSuperclass())) {
			address = this.address(objectClass.getSuperclass(), elementName, address);
			if (address != null) {
				return address;
			}
		}
		if (field == null && method == null) {

			for (Field f : objectClass.getDeclaredFields()) {
				if (GGFields.isNotPrimitive(f.getType())) {
					GGObjectAddress a = null;
					a = this.doIfIsCollection(f, elementName, address);
					if (a != null)
						return a;
					a = this.doIfIsMap(f, elementName, address);
					if (a != null)
						return a;
					a = this.doIfIsArray(f, elementName, address);
					if (a != null)
						return a;
					a = this.doIfNotEnum(f, elementName, address);
					if (a != null)
						return a;
				}
			}
		}
		return null;
	}

	private GGObjectAddress doIfIsMap(Field f, String elementName, GGObjectAddress address)
			throws GGReflectionException {
		if (Map.class.isAssignableFrom(f.getType())) {
			Class<?> keyClass = GGFields.getGenericType(f, 0);
			Class<?> valueClass = GGFields.getGenericType(f, 1);
			if (GGFields.isNotPrimitive(keyClass)) {
				GGObjectAddress keyAddress = null;
				if (address == null) {
					keyAddress = new GGObjectAddress(f.getName(), true);
					keyAddress.addElement(GGObjectAddress.MAP_KEY_INDICATOR);
				} else {
					keyAddress = address.clone();
					keyAddress.addElement(f.getName());
					keyAddress.addElement(GGObjectAddress.MAP_KEY_INDICATOR);
				}
				GGObjectAddress a = this.address(keyClass, elementName, keyAddress);
				if (a != null) {
					return a;
				}
			}
			if (GGFields.isNotPrimitive(valueClass)) {
				GGObjectAddress valueAddress = null;
				if (address == null) {
					valueAddress = new GGObjectAddress(f.getName(), true);
					valueAddress.addElement(GGObjectAddress.MAP_VALUE_INDICATOR);
				} else {
					valueAddress = address.clone();
					valueAddress.addElement(f.getName());
					valueAddress.addElement(GGObjectAddress.MAP_VALUE_INDICATOR);
				}
				GGObjectAddress a = this.address(valueClass, elementName, valueAddress);
				if (a != null) {
					return a;
				}
			}
		}
		return null;
	}

	private GGObjectAddress doIfIsArray(Field f, String elementName, GGObjectAddress address)
			throws GGReflectionException {
		if (f.getType().isArray()) {
			final Class<?> componentType = f.getType().getComponentType();
			final GGObjectAddress newAddress = address == null ? new GGObjectAddress(f.getName(), true)
					: address.addElement(f.getName());
			return this.address(componentType, elementName, newAddress);
		}
		return null;
	}

	private GGObjectAddress doIfIsCollection(Field f, String elementName, GGObjectAddress address)
			throws GGReflectionException {
		if (Collection.class.isAssignableFrom(f.getType())) {
			final Class<?> t = GGFields.getGenericType(f, 0);
			final GGObjectAddress a = this.address(t, elementName,
					address == null ? new GGObjectAddress(f.getName(), true)
							: address.addElement(f.getName()));
			if (a != null) {
				return a;
			}
		}
		return null;
	}

	private GGObjectAddress doIfNotEnum(Field f, String elementName, GGObjectAddress address)
			throws GGReflectionException {
		if (!f.getType().isEnum()) {
			GGObjectAddress a = this.address(f.getType(), elementName,
					address == null ? new GGObjectAddress(f.getName(), true)
							: address.addElement(f.getName()));
			if (a != null) {
				return a;
			}
		}
		return null;
	}

	@Override
	public Object setValue(Object object, String fieldAddress, Object fieldValue) throws GGReflectionException {
		if (object == null) {
			throw new GGReflectionException("Object is null");
		}
		return this.setValue(object, new GGObjectAddress(fieldAddress, true), fieldValue);
	}

	@Override
	public Object setValue(Object object, GGObjectAddress fieldAddress, Object fieldValue)
			throws GGReflectionException {
		if (object == null) {
			throw new GGReflectionException("Object is null");
		}
		List<Object> field = this.find(fieldAddress);
		return new GGObjectFieldSetter(object.getClass(), field, fieldAddress).setValue(object, fieldValue);
	}

	@Override
	public Object getValue(Object object, String fieldAddress) throws GGReflectionException {
		if (object == null) {
			throw new GGReflectionException("Object is null");
		}
		return this.getValue(object, new GGObjectAddress(fieldAddress, true));
	}

	@Override
	public Object getValue(Object object, GGObjectAddress fieldAddress) throws GGReflectionException {
		if (object == null) {
			throw new GGReflectionException("Object is null");
		}
		List<Object> field = this.find(fieldAddress);
		return new GGObjectFieldGetter(object.getClass(), field, fieldAddress).getValue(object);
	}

	@Override
	public Object setValue(String fieldAddress, Object fieldValue) throws GGReflectionException {
		return this.setValue(new GGObjectAddress(fieldAddress, true), fieldValue);
	}

	@Override
	public Object getValue(String fieldAddress) throws GGReflectionException {
		return this.getValue(new GGObjectAddress(fieldAddress, true));
	}

	@Override
	public Object setValue(GGObjectAddress fieldAddress, Object fieldValue) throws GGReflectionException {
		List<Object> field = this.find(fieldAddress);
		return new GGObjectFieldSetter(this.objectClass, field, fieldAddress).setValue(this.object, fieldValue);
	}

	@Override
	public Object getValue(GGObjectAddress fieldAddress) throws GGReflectionException {
		List<Object> field = this.find(fieldAddress);
		return new GGObjectFieldGetter(this.objectClass, field, fieldAddress).getValue(this.object);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object fieldValueStructure(GGObjectAddress address) throws GGReflectionException {
		List<Object> fields = this.find(address);
		Object structure = null;

		for (int i = fields.size() - 1; i >= 0; i--) {
			if (i == fields.size() - 1) {
				structure = GGFields.instanciate((Field) fields.get(i));
			} else if (GGFields.isArrayOrMapOrCollectionField((Field) fields.get(i))) {
				ArrayList<Object> list = (ArrayList<Object>) GGObjectReflectionHelper
						.newArrayListOf(((Field) fields.get(i)).getType());
				list.add(structure);
				structure = list;
			}
		}

		return structure;
	}

	@Override
	public Object fieldValueStructure(String address) throws GGReflectionException {
		return this.fieldValueStructure(new GGObjectAddress(address, true));
	}

	@Override
	public Object invoke(String methodAddress, Object... args) throws GGReflectionException {
		return this.invoke(this.object, new GGObjectAddress(methodAddress, true), args);
	}

	@Override
	public Object invoke(GGObjectAddress methodAddress, Object... args) throws GGReflectionException {
		return this.invoke(this.object, methodAddress, args);
	}

	@Override
	public Object invoke(Object object, GGObjectAddress methodAddress, Object... args) throws GGReflectionException {
		if (object == null) {
			throw new GGReflectionException("Object is null");
		}

		List<Object> field = this.find(methodAddress);
		return new GGObjectMethodInvoker(object.getClass(), field, methodAddress).invoke(object, args);
	}
}
