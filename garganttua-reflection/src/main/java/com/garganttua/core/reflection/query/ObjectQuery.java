package com.garganttua.core.reflection.query;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.fields.Fields;
import com.garganttua.core.reflection.fields.ObjectFieldGetter;
import com.garganttua.core.reflection.fields.ObjectFieldSetter;
import com.garganttua.core.reflection.methods.ObjectMethodInvoker;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflection.IObjectQuery;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ObjectQuery implements IObjectQuery {

    private Class<?> objectClass;
    private Object object;

    protected ObjectQuery(Class<?> objectClass) throws ReflectionException {
        log.atTrace().log("Creating ObjectQuery with objectClass={}", objectClass);
        if (objectClass == null) {
            throw new ReflectionException("class is null");
        }
        this.objectClass = objectClass;
        this.object = ObjectReflectionHelper.instanciateNewObject(objectClass);
        log.atDebug().log("ObjectQuery initialized with objectClass={} and object={}", objectClass, object);
    }

    protected ObjectQuery(Object object) throws ReflectionException {
        log.atTrace().log("Creating ObjectQuery with object instance={}", object);
        if (object == null) {
            throw new ReflectionException("object is null");
        }
        this.object = object;
        this.objectClass = object.getClass();
        log.atDebug().log("ObjectQuery initialized with objectClass={} from object instance", objectClass);
    }

    protected ObjectQuery(Class<?> objectClass, Object object) throws ReflectionException {
        log.atTrace().log("Creating ObjectQuery with objectClass={} and object={}", objectClass, object);
        if (object == null) {
            throw new ReflectionException("object is null");
        }
        if (objectClass == null) {
            throw new ReflectionException("class is null");
        }
        if (!object.getClass().isAssignableFrom(objectClass)) {
            throw new ReflectionException(
                    "Provided class " + objectClass + " and object " + object.getClass() + " do not match");
        }
        this.object = object;
        this.objectClass = objectClass;
        log.atDebug().log("ObjectQuery fully initialized with objectClass={} and object={}", objectClass, object);
    }

    @Override
    public List<Object> find(String fieldAddress) throws ReflectionException {
        log.atTrace().log("find(String) called with fieldAddress='{}'", fieldAddress);
        return this.find(new ObjectAddress(fieldAddress, true));
    }

    @Override
    public List<Object> find(ObjectAddress fieldAddress) throws ReflectionException {
        log.atDebug().log("find(ObjectAddress) called with fieldAddress={} in class={}", fieldAddress, objectClass);
        List<Object> list = new ArrayList<>();
        List<Object> result = findRecursively(this.objectClass, fieldAddress, 0, list);
        log.atTrace().log("find result for {} : {}", fieldAddress, result);
        return result;
    }

    private List<Object> findRecursively(Class<?> clazz, ObjectAddress address, int index, List<Object> list)
            throws ReflectionException {
        String element = address.getElement(index);
        log.atTrace().log("findRecursively: element='{}', index={}, class={}", element, index, clazz);

        if (clazz == null || index >= address.length()) {
            log.atError().log("Element '{}' not found in class {}", element, clazz);
            throw new ReflectionException("Object element " + element + " not found in class " + clazz);
        }

        Field field = ObjectReflectionHelper.getField(clazz, element);
        Method method = (index == address.length() - 1 && field == null) ? ObjectReflectionHelper.getMethod(clazz, element) : null;

        if (field == null && method == null) {
            if (clazz.getSuperclass() != null && !Object.class.equals(clazz.getSuperclass()) && !Fields.BlackList.isBlackListed(clazz.getSuperclass())) {
                log.atTrace().log("Element '{}' not found in {}, checking superclass {}", element, clazz, clazz.getSuperclass());
                return findRecursively(clazz.getSuperclass(), address, index, list);
            }
        } else if (field != null && method == null) {
            log.atDebug().log("Field '{}' found in {}", field.getName(), clazz.getName());
            list.add(field);
            if (index == address.length() - 1) {
                log.atInfo().log("Resolved field '{}' fully in {}", element, clazz);
                return list;
            }
            Class<?> fieldType = field.getType();
            if (Collection.class.isAssignableFrom(fieldType)) {
                Class<?> genericType = Fields.getGenericType(field, 0);
                log.atTrace().log("Field '{}' is Collection, recursing into type={}", field.getName(), genericType);
                return findRecursively(genericType, address, index + 1, list);
            } else if (Map.class.isAssignableFrom(fieldType)) {
                String nextElement = address.getElement(index + 1);
                log.atTrace().log("Field '{}' is Map, next address element='{}'", field.getName(), nextElement);
                if (ObjectAddress.MAP_VALUE_INDICATOR.equals(nextElement)) {
                    Class<?> valueType = Fields.getGenericType(field, 1);
                    return findRecursively(valueType, address, index + 2, list);
                } else if (ObjectAddress.MAP_KEY_INDICATOR.equals(nextElement)) {
                    Class<?> keyType = Fields.getGenericType(field, 0);
                    return findRecursively(keyType, address, index + 2, list);
                } else {
                    log.atError().log("Map field '{}' address element must indicate key or value, got '{}'", field.getName(), nextElement);
                    throw new ReflectionException("Field " + element + " is a map, so address must indicate key or value");
                }
            } else {
                return findRecursively(fieldType, address, index + 1, list);
            }
        } else if (field == null && method != null) {
            log.atDebug().log("Method '{}' found in {}", method.getName(), clazz.getName());
            list.add(method);
            return list;
        } else {
            log.atError().log("Element '{}' is both a field and method in {}", element, clazz);
            throw new ReflectionException("Object element " + element + " is both a field and a method in " + clazz);
        }

        log.atError().log("Element '{}' could not be resolved in {}", element, clazz);
        throw new ReflectionException("Object element " + element + " not found in class " + clazz);
    }

    @Override
    public ObjectAddress address(String elementName) throws ReflectionException {
        log.atDebug().log("address(String) called for element='{}' in class={}", elementName, objectClass);
        return address(this.objectClass, elementName, null);
    }

    private ObjectAddress address(Class<?> objectClass, String elementName, ObjectAddress address)
            throws ReflectionException {
        log.atTrace().log("Resolving address element='{}', class={}, baseAddress={}", elementName, objectClass, address);
        Field field = null;
        try {
            field = objectClass.getDeclaredField(elementName);
        } catch (NoSuchFieldException | SecurityException ignored) {}

        Method method = ObjectReflectionHelper.getMethod(objectClass, elementName);
        if (method != null) {
            log.atDebug().log("Found method '{}' in {}", elementName, objectClass.getName());
            return new ObjectAddress(address == null ? elementName : address + "." + elementName, true);
        }
        if (field != null) {
            log.atDebug().log("Found field '{}' in {}", elementName, objectClass.getName());
            return new ObjectAddress(address == null ? elementName : address + "." + elementName, true);
        }

        if (objectClass.getSuperclass() != null && !Object.class.equals(objectClass.getSuperclass()) && !Fields.BlackList.isBlackListed(objectClass.getSuperclass())) {
            address = address(objectClass.getSuperclass(), elementName, address);
            if( address != null ) {
				return address;
			}
        }

        for (Field f : objectClass.getDeclaredFields()) {
            if (Fields.isNotPrimitive(f.getType()) /* && !Fields.BlackList.isBlackListed(f.getType()) && !Object.class.equals(f.getType()) && !"serialPersistentFields".equalsIgnoreCase(f.getName()) */) {
                ObjectAddress a;
                if ((a = doIfIsCollection(f, elementName, address)) != null) return a;
                if ((a = doIfIsMap(f, elementName, address)) != null) return a;
                if ((a = doIfIsArray(f, elementName, address)) != null) return a;
                if ((a = doIfNotEnum(f, elementName, address)) != null) return a;
            }
        }

        log.atWarn().log("Element '{}' could not be resolved in {}", elementName, objectClass.getName());
        return null;
    }

    // --- setValue / getValue ---
    @Override
    public Object setValue(Object object, String fieldAddress, Object fieldValue) throws ReflectionException {
        log.atDebug().log("setValue(String) called for field='{}', value={}, on object={}", fieldAddress, fieldValue, object);
        return setValue(object, new ObjectAddress(fieldAddress, true), fieldValue);
    }

    @Override
    public Object setValue(Object object, ObjectAddress fieldAddress, Object fieldValue) throws ReflectionException {
        log.atDebug().log("setValue(ObjectAddress) called for fieldAddress={}, value={}, object={}", fieldAddress, fieldValue, object);
        List<Object> fieldStructure = find(fieldAddress);
        return new ObjectFieldSetter(object.getClass(), fieldStructure, fieldAddress).setValue(object, fieldValue);
    }

    @Override
    public Object getValue(Object object, String fieldAddress) throws ReflectionException {
        log.atDebug().log("getValue(String) called for field='{}' on object={}", fieldAddress, object);
        return getValue(object, new ObjectAddress(fieldAddress, true));
    }

    @Override
    public Object getValue(Object object, ObjectAddress fieldAddress) throws ReflectionException {
        log.atDebug().log("getValue(ObjectAddress) called for fieldAddress={} on object={}", fieldAddress, object);
        List<Object> fieldStructure = find(fieldAddress);
        return new ObjectFieldGetter(object.getClass(), fieldStructure, fieldAddress).getValue(object);
    }

    @Override
    public Object setValue(String fieldAddress, Object fieldValue) throws ReflectionException {
        return setValue(new ObjectAddress(fieldAddress, true), fieldValue);
    }

    @Override
    public Object getValue(String fieldAddress) throws ReflectionException {
        return getValue(new ObjectAddress(fieldAddress, true));
    }

    @Override
    public Object setValue(ObjectAddress fieldAddress, Object fieldValue) throws ReflectionException {
        List<Object> fieldStructure = find(fieldAddress);
        return new ObjectFieldSetter(this.objectClass, fieldStructure, fieldAddress).setValue(this.object, fieldValue);
    }

    @Override
    public Object getValue(ObjectAddress fieldAddress) throws ReflectionException {
        List<Object> fieldStructure = find(fieldAddress);
        return new ObjectFieldGetter(this.objectClass, fieldStructure, fieldAddress).getValue(this.object);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object fieldValueStructure(ObjectAddress address) throws ReflectionException {
        log.atTrace().log("fieldValueStructure called for address={}", address);
        List<Object> fields = find(address);
        Object structure = null;

        for (int i = fields.size() - 1; i >= 0; i--) {
            Field f = (Field) fields.get(i);
            if (i == fields.size() - 1) {
                structure = Fields.instanciate(f);
            } else if (Fields.isArrayOrMapOrCollectionField(f)) {
                ArrayList<Object> list = (ArrayList<Object>) ObjectReflectionHelper.newArrayListOf(f.getType());
                list.add(structure);
                structure = list;
            }
        }
        log.atTrace().log("fieldValueStructure resolved: {}", structure);
        return structure;
    }

    @Override
    public Object fieldValueStructure(String address) throws ReflectionException {
        return fieldValueStructure(new ObjectAddress(address, true));
    }

    @Override
    public Object invoke(String methodAddress, Object... args) throws ReflectionException {
        return invoke(this.object, new ObjectAddress(methodAddress, true), args);
    }

    @Override
    public Object invoke(ObjectAddress methodAddress, Object... args) throws ReflectionException {
        return invoke(this.object, methodAddress, args);
    }

    @Override
    public Object invoke(Object object, ObjectAddress methodAddress, Object... args) throws ReflectionException {
        log.atDebug().log("invoke called on object={} for methodAddress={} with args={}", object, methodAddress, Arrays.toString(args));
        List<Object> fieldStructure = find(methodAddress);
        return new ObjectMethodInvoker(object.getClass(), fieldStructure, methodAddress).invoke(object, args);
    }

    private ObjectAddress doIfIsMap(Field f, String elementName, ObjectAddress address) throws ReflectionException {
        if (Map.class.isAssignableFrom(f.getType())) {
            log.atTrace().log("doIfIsMap checking field '{}' for element '{}'", f.getName(), elementName);
            Class<?> keyClass = Fields.getGenericType(f, 0);
            Class<?> valueClass = Fields.getGenericType(f, 1);
            if (Fields.isNotPrimitive(keyClass) && !Fields.BlackList.isBlackListed(keyClass)) {
                ObjectAddress keyAddress = address == null ? new ObjectAddress(f.getName(), true) : address.clone().addElement(f.getName());
                keyAddress.addElement(ObjectAddress.MAP_KEY_INDICATOR);
                ObjectAddress a = address(keyClass, elementName, keyAddress);
                if (a != null) return a;
            }
            if (Fields.isNotPrimitive(valueClass) && !Fields.BlackList.isBlackListed(valueClass)) {
                ObjectAddress valueAddress = address == null ? new ObjectAddress(f.getName(), true) : address.clone().addElement(f.getName());
                valueAddress.addElement(ObjectAddress.MAP_VALUE_INDICATOR);
                ObjectAddress a = address(valueClass, elementName, valueAddress);
                if (a != null) return a;
            }
        }
        return null;
    }

    private ObjectAddress doIfIsArray(Field f, String elementName, ObjectAddress address) throws ReflectionException {
        if (f.getType().isArray()) {
            log.atTrace().log("doIfIsArray checking array field '{}' for element '{}'", f.getName(), elementName);
            Class<?> componentType = f.getType().getComponentType();
            ObjectAddress newAddress = address == null ? new ObjectAddress(f.getName(), true) : address.clone().addElement(f.getName());
            return address(componentType, elementName, newAddress);
        }
        return null;
    }

    private ObjectAddress doIfIsCollection(Field f, String elementName, ObjectAddress address) throws ReflectionException {
        if (Collection.class.isAssignableFrom(f.getType())) {
            log.atTrace().log("doIfIsCollection checking field '{}' for element '{}'", f.getName(), elementName);
            Class<?> t = Fields.getGenericType(f, 0);
            ObjectAddress newAddress = address == null ? new ObjectAddress(f.getName(), true) : address.clone().addElement(f.getName());
            return address(t, elementName, newAddress);
        }
        return null;
    }

    private ObjectAddress doIfNotEnum(Field f, String elementName, ObjectAddress address) throws ReflectionException {
        if (!f.getType().isEnum()) {
            log.atTrace().log("doIfNotEnum checking field '{}' for element '{}'", f.getName(), elementName);
            ObjectAddress newAddress = address == null ? new ObjectAddress(f.getName(), true) : address.clone().addElement(f.getName());
            return address(f.getType(), elementName, newAddress);
        }
        return null;
    }
}
