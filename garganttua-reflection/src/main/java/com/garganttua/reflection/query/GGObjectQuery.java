package com.garganttua.reflection.query;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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
        log.atTrace().log("Creating GGObjectQuery with objectClass={}", objectClass);
        if (objectClass == null) {
            throw new GGReflectionException("class is null");
        }
        this.objectClass = objectClass;
        this.object = GGObjectReflectionHelper.instanciateNewObject(objectClass);
        log.atDebug().log("GGObjectQuery initialized with objectClass={} and object={}", objectClass, object);
    }

    protected GGObjectQuery(Object object) throws GGReflectionException {
        log.atTrace().log("Creating GGObjectQuery with object instance={}", object);
        if (object == null) {
            throw new GGReflectionException("object is null");
        }
        this.object = object;
        this.objectClass = object.getClass();
        log.atDebug().log("GGObjectQuery initialized with objectClass={} from object instance", objectClass);
    }

    protected GGObjectQuery(Class<?> objectClass, Object object) throws GGReflectionException {
        log.atTrace().log("Creating GGObjectQuery with objectClass={} and object={}", objectClass, object);
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
        log.atDebug().log("GGObjectQuery fully initialized with objectClass={} and object={}", objectClass, object);
    }

    @Override
    public List<Object> find(String fieldAddress) throws GGReflectionException {
        log.atTrace().log("find(String) called with fieldAddress='{}'", fieldAddress);
        return this.find(new GGObjectAddress(fieldAddress, true));
    }

    @Override
    public List<Object> find(GGObjectAddress fieldAddress) throws GGReflectionException {
        log.atDebug().log("find(GGObjectAddress) called with fieldAddress={} in class={}", fieldAddress, objectClass);
        List<Object> list = new ArrayList<>();
        List<Object> result = findRecursively(this.objectClass, fieldAddress, 0, list);
        log.atTrace().log("find result for {} : {}", fieldAddress, result);
        return result;
    }

    private List<Object> findRecursively(Class<?> clazz, GGObjectAddress address, int index, List<Object> list)
            throws GGReflectionException {
        String element = address.getElement(index);
        log.atTrace().log("findRecursively: element='{}', index={}, class={}", element, index, clazz);

        if (clazz == null || index >= address.length()) {
            log.atError().log("Element '{}' not found in class {}", element, clazz);
            throw new GGReflectionException("Object element " + element + " not found in class " + clazz);
        }

        Field field = GGObjectReflectionHelper.getField(clazz, element);
        Method method = (index == address.length() - 1 && field == null) ? GGObjectReflectionHelper.getMethod(clazz, element) : null;

        if (field == null && method == null) {
            if (clazz.getSuperclass() != null && !Object.class.equals(clazz.getSuperclass()) && !GGFields.BlackList.isBlackListed(clazz.getSuperclass())) {
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
                Class<?> genericType = GGFields.getGenericType(field, 0);
                log.atTrace().log("Field '{}' is Collection, recursing into type={}", field.getName(), genericType);
                return findRecursively(genericType, address, index + 1, list);
            } else if (Map.class.isAssignableFrom(fieldType)) {
                String nextElement = address.getElement(index + 1);
                log.atTrace().log("Field '{}' is Map, next address element='{}'", field.getName(), nextElement);
                if (GGObjectAddress.MAP_VALUE_INDICATOR.equals(nextElement)) {
                    Class<?> valueType = GGFields.getGenericType(field, 1);
                    return findRecursively(valueType, address, index + 2, list);
                } else if (GGObjectAddress.MAP_KEY_INDICATOR.equals(nextElement)) {
                    Class<?> keyType = GGFields.getGenericType(field, 0);
                    return findRecursively(keyType, address, index + 2, list);
                } else {
                    log.atError().log("Map field '{}' address element must indicate key or value, got '{}'", field.getName(), nextElement);
                    throw new GGReflectionException("Field " + element + " is a map, so address must indicate key or value");
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
            throw new GGReflectionException("Object element " + element + " is both a field and a method in " + clazz);
        }

        log.atError().log("Element '{}' could not be resolved in {}", element, clazz);
        throw new GGReflectionException("Object element " + element + " not found in class " + clazz);
    }

    @Override
    public GGObjectAddress address(String elementName) throws GGReflectionException {
        log.atDebug().log("address(String) called for element='{}' in class={}", elementName, objectClass);
        return address(this.objectClass, elementName, null);
    }

    private GGObjectAddress address(Class<?> objectClass, String elementName, GGObjectAddress address)
            throws GGReflectionException {
        log.atTrace().log("Resolving address element='{}', class={}, baseAddress={}", elementName, objectClass, address);
        Field field = null;
        try {
            field = objectClass.getDeclaredField(elementName);
        } catch (NoSuchFieldException | SecurityException ignored) {}

        Method method = GGObjectReflectionHelper.getMethod(objectClass, elementName);
        if (method != null) {
            log.atDebug().log("Found method '{}' in {}", elementName, objectClass.getName());
            return new GGObjectAddress(address == null ? elementName : address + "." + elementName, true);
        }
        if (field != null) {
            log.atDebug().log("Found field '{}' in {}", elementName, objectClass.getName());
            return new GGObjectAddress(address == null ? elementName : address + "." + elementName, true);
        }

        if (objectClass.getSuperclass() != null && !Object.class.equals(objectClass.getSuperclass()) && !GGFields.BlackList.isBlackListed(objectClass.getSuperclass())) {
            address = address(objectClass.getSuperclass(), elementName, address);
            if( address != null ) {
				return address;
			}
        }

        for (Field f : objectClass.getDeclaredFields()) {
            if (GGFields.isNotPrimitive(f.getType()) /* && !GGFields.BlackList.isBlackListed(f.getType()) && !Object.class.equals(f.getType()) && !"serialPersistentFields".equalsIgnoreCase(f.getName()) */) {
                GGObjectAddress a;
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
    public Object setValue(Object object, String fieldAddress, Object fieldValue) throws GGReflectionException {
        log.atDebug().log("setValue(String) called for field='{}', value={}, on object={}", fieldAddress, fieldValue, object);
        return setValue(object, new GGObjectAddress(fieldAddress, true), fieldValue);
    }

    @Override
    public Object setValue(Object object, GGObjectAddress fieldAddress, Object fieldValue) throws GGReflectionException {
        log.atDebug().log("setValue(GGObjectAddress) called for fieldAddress={}, value={}, object={}", fieldAddress, fieldValue, object);
        List<Object> fieldStructure = find(fieldAddress);
        return new GGObjectFieldSetter(object.getClass(), fieldStructure, fieldAddress).setValue(object, fieldValue);
    }

    @Override
    public Object getValue(Object object, String fieldAddress) throws GGReflectionException {
        log.atDebug().log("getValue(String) called for field='{}' on object={}", fieldAddress, object);
        return getValue(object, new GGObjectAddress(fieldAddress, true));
    }

    @Override
    public Object getValue(Object object, GGObjectAddress fieldAddress) throws GGReflectionException {
        log.atDebug().log("getValue(GGObjectAddress) called for fieldAddress={} on object={}", fieldAddress, object);
        List<Object> fieldStructure = find(fieldAddress);
        return new GGObjectFieldGetter(object.getClass(), fieldStructure, fieldAddress).getValue(object);
    }

    @Override
    public Object setValue(String fieldAddress, Object fieldValue) throws GGReflectionException {
        return setValue(new GGObjectAddress(fieldAddress, true), fieldValue);
    }

    @Override
    public Object getValue(String fieldAddress) throws GGReflectionException {
        return getValue(new GGObjectAddress(fieldAddress, true));
    }

    @Override
    public Object setValue(GGObjectAddress fieldAddress, Object fieldValue) throws GGReflectionException {
        List<Object> fieldStructure = find(fieldAddress);
        return new GGObjectFieldSetter(this.objectClass, fieldStructure, fieldAddress).setValue(this.object, fieldValue);
    }

    @Override
    public Object getValue(GGObjectAddress fieldAddress) throws GGReflectionException {
        List<Object> fieldStructure = find(fieldAddress);
        return new GGObjectFieldGetter(this.objectClass, fieldStructure, fieldAddress).getValue(this.object);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object fieldValueStructure(GGObjectAddress address) throws GGReflectionException {
        log.atTrace().log("fieldValueStructure called for address={}", address);
        List<Object> fields = find(address);
        Object structure = null;

        for (int i = fields.size() - 1; i >= 0; i--) {
            Field f = (Field) fields.get(i);
            if (i == fields.size() - 1) {
                structure = GGFields.instanciate(f);
            } else if (GGFields.isArrayOrMapOrCollectionField(f)) {
                ArrayList<Object> list = (ArrayList<Object>) GGObjectReflectionHelper.newArrayListOf(f.getType());
                list.add(structure);
                structure = list;
            }
        }
        log.atTrace().log("fieldValueStructure resolved: {}", structure);
        return structure;
    }

    @Override
    public Object fieldValueStructure(String address) throws GGReflectionException {
        return fieldValueStructure(new GGObjectAddress(address, true));
    }

    @Override
    public Object invoke(String methodAddress, Object... args) throws GGReflectionException {
        return invoke(this.object, new GGObjectAddress(methodAddress, true), args);
    }

    @Override
    public Object invoke(GGObjectAddress methodAddress, Object... args) throws GGReflectionException {
        return invoke(this.object, methodAddress, args);
    }

    @Override
    public Object invoke(Object object, GGObjectAddress methodAddress, Object... args) throws GGReflectionException {
        log.atDebug().log("invoke called on object={} for methodAddress={} with args={}", object, methodAddress, Arrays.toString(args));
        List<Object> fieldStructure = find(methodAddress);
        return new GGObjectMethodInvoker(object.getClass(), fieldStructure, methodAddress).invoke(object, args);
    }

    private GGObjectAddress doIfIsMap(Field f, String elementName, GGObjectAddress address) throws GGReflectionException {
        if (Map.class.isAssignableFrom(f.getType())) {
            log.atTrace().log("doIfIsMap checking field '{}' for element '{}'", f.getName(), elementName);
            Class<?> keyClass = GGFields.getGenericType(f, 0);
            Class<?> valueClass = GGFields.getGenericType(f, 1);
            if (GGFields.isNotPrimitive(keyClass) && !GGFields.BlackList.isBlackListed(keyClass)) {
                GGObjectAddress keyAddress = address == null ? new GGObjectAddress(f.getName(), true) : address.clone().addElement(f.getName());
                keyAddress.addElement(GGObjectAddress.MAP_KEY_INDICATOR);
                GGObjectAddress a = address(keyClass, elementName, keyAddress);
                if (a != null) return a;
            }
            if (GGFields.isNotPrimitive(valueClass) && !GGFields.BlackList.isBlackListed(valueClass)) {
                GGObjectAddress valueAddress = address == null ? new GGObjectAddress(f.getName(), true) : address.clone().addElement(f.getName());
                valueAddress.addElement(GGObjectAddress.MAP_VALUE_INDICATOR);
                GGObjectAddress a = address(valueClass, elementName, valueAddress);
                if (a != null) return a;
            }
        }
        return null;
    }

    private GGObjectAddress doIfIsArray(Field f, String elementName, GGObjectAddress address) throws GGReflectionException {
        if (f.getType().isArray()) {
            log.atTrace().log("doIfIsArray checking array field '{}' for element '{}'", f.getName(), elementName);
            Class<?> componentType = f.getType().getComponentType();
            GGObjectAddress newAddress = address == null ? new GGObjectAddress(f.getName(), true) : address.clone().addElement(f.getName());
            return address(componentType, elementName, newAddress);
        }
        return null;
    }

    private GGObjectAddress doIfIsCollection(Field f, String elementName, GGObjectAddress address) throws GGReflectionException {
        if (Collection.class.isAssignableFrom(f.getType())) {
            log.atTrace().log("doIfIsCollection checking field '{}' for element '{}'", f.getName(), elementName);
            Class<?> t = GGFields.getGenericType(f, 0);
            GGObjectAddress newAddress = address == null ? new GGObjectAddress(f.getName(), true) : address.clone().addElement(f.getName());
            return address(t, elementName, newAddress);
        }
        return null;
    }

    private GGObjectAddress doIfNotEnum(Field f, String elementName, GGObjectAddress address) throws GGReflectionException {
        if (!f.getType().isEnum()) {
            log.atTrace().log("doIfNotEnum checking field '{}' for element '{}'", f.getName(), elementName);
            GGObjectAddress newAddress = address == null ? new GGObjectAddress(f.getName(), true) : address.clone().addElement(f.getName());
            return address(f.getType(), elementName, newAddress);
        }
        return null;
    }
}
