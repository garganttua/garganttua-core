package com.garganttua.core.reflection.query;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.IObjectQuery;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.fields.Fields;
import com.garganttua.core.reflection.fields.ObjectFieldGetter;
import com.garganttua.core.reflection.fields.ObjectFieldSetter;
import com.garganttua.core.reflection.methods.MethodResolver;
import com.garganttua.core.reflection.methods.ObjectMethodInvoker;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ObjectQuery<T> implements IObjectQuery<T> {

    private Class<T> objectClass;
    private T object;

    protected ObjectQuery(Class<T> objectClass) throws ReflectionException {
        log.atTrace().log("Creating ObjectQuery with objectClass={}", objectClass);
        if (objectClass == null) {
            throw new ReflectionException("class is null");
        }
        this.objectClass = objectClass;
        this.object = ObjectReflectionHelper.instanciateNewObject(objectClass);
        log.atDebug().log("ObjectQuery initialized with objectClass={} and object={}", objectClass, object);
    }

    protected ObjectQuery(T object) throws ReflectionException {
        log.atTrace().log("Creating ObjectQuery with object instance={}", object);
        if (object == null) {
            throw new ReflectionException("object is null");
        }
        this.object = object;
        this.objectClass = (Class<T>) object.getClass();
        log.atDebug().log("ObjectQuery initialized with objectClass={} from object instance", objectClass);
    }

    protected ObjectQuery(Class<T> objectClass, T object) throws ReflectionException {
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
    public List<Object> find(String elementName) throws ReflectionException {
        log.atTrace().log("find(String) called with elementName='{}'", elementName);
        return this.find(new ObjectAddress(elementName, true));
    }

    @Override
    public List<Object> find(ObjectAddress elementName) throws ReflectionException {
        log.atDebug().log("find(ObjectAddress) called with elementName={} in class={}", elementName, objectClass);
        List<Object> list = new ArrayList<>();
        List<Object> result = findRecursively(this.objectClass, elementName, 0, list, false);
        log.atTrace().log("find result for {} : {}", elementName, result);
        return result;
    }

    @Override
    public List<List<Object>> findAll(String elementName) throws ReflectionException {
        log.atTrace().log("findAll(String) called with elementName='{}'", elementName);
        return this.findAll(new ObjectAddress(elementName, true));
    }

    @Override
    public List<List<Object>> findAll(ObjectAddress elementName) throws ReflectionException {
        log.atDebug().log("findAll(ObjectAddress) called with elementName={} in class={}", elementName, objectClass);
        return findAllRecursively(this.objectClass, elementName, 0, new ArrayList<>());
    }

    /**
     * Finds all matching paths when searching for an address element.
     * Returns a list of lists, where each inner list represents a path
     * from the root to a Field or Method.
     *
     * @param clazz current class being searched
     * @param address the target address
     * @param index current index in the address
     * @param currentPath the current path being built
     * @return list of all matching paths (list of lists)
     * @throws ReflectionException if resolution fails
     */
    private List<List<Object>> findAllRecursively(Class<?> clazz, ObjectAddress address, int index, List<Object> currentPath)
            throws ReflectionException {
        String element = address.getElement(index);
        log.atTrace().log("findAllRecursively: element='{}', index={}, class={}", element, index, clazz);

        if (clazz == null || index >= address.length()) {
            log.atError().log("Element '{}' not found in class {}", element, clazz);
            throw new ReflectionException("Object element " + element + " not found in class " + clazz);
        }

        Field field = ObjectReflectionHelper.getField(clazz, element);

        // For methods: get all methods with this name
        List<Method> methods = null;
        if (index == address.length() - 1 && field == null) {
            methods = ObjectReflectionHelper.getMethods(clazz, element);
        }

        boolean hasMethods = (methods != null && !methods.isEmpty());

        if (field == null && !hasMethods) {
            // Try superclass
            if (clazz.getSuperclass() != null && !Object.class.equals(clazz.getSuperclass())
                    && !Fields.BlackList.isBlackListed(clazz.getSuperclass())) {
                log.atTrace().log("Element '{}' not found in {}, checking superclass {}", element, clazz,
                        clazz.getSuperclass());
                return findAllRecursively(clazz.getSuperclass(), address, index, currentPath);
            }
        } else if (field != null && !hasMethods) {
            log.atDebug().log("Field '{}' found in {}", field.getName(), clazz.getName());
            List<Object> newPath = new ArrayList<>(currentPath);
            newPath.add(field);

            if (index == address.length() - 1) {
                log.atInfo().log("Resolved field '{}' fully in {}", element, clazz);
                List<List<Object>> result = new ArrayList<>();
                result.add(newPath);
                return result;
            }

            Class<?> fieldType = field.getType();
            if (Collection.class.isAssignableFrom(fieldType)) {
                Class<?> genericType = Fields.getGenericType(field, 0);
                log.atTrace().log("Field '{}' is Collection, recursing into type={}", field.getName(), genericType);
                return findAllRecursively(genericType, address, index + 1, newPath);
            } else if (Map.class.isAssignableFrom(fieldType)) {
                String nextElement = address.getElement(index + 1);
                log.atTrace().log("Field '{}' is Map, next address element='{}'", field.getName(), nextElement);
                if (ObjectAddress.MAP_VALUE_INDICATOR.equals(nextElement)) {
                    Class<?> valueType = Fields.getGenericType(field, 1);
                    return findAllRecursively(valueType, address, index + 2, newPath);
                } else if (ObjectAddress.MAP_KEY_INDICATOR.equals(nextElement)) {
                    Class<?> keyType = Fields.getGenericType(field, 0);
                    return findAllRecursively(keyType, address, index + 2, newPath);
                } else {
                    log.atError().log("Map field '{}' address element must indicate key or value, got '{}'",
                            field.getName(), nextElement);
                    throw new ReflectionException(
                            "Field " + element + " is a map, so address must indicate key or value");
                }
            } else {
                return findAllRecursively(fieldType, address, index + 1, newPath);
            }
        } else if (field == null && hasMethods) {
            log.atDebug().log("Found {} method(s) named '{}' in {}", methods.size(), element, clazz.getName());

            // Create one path per method found
            List<List<Object>> result = new ArrayList<>();
            for (Method method : methods) {
                List<Object> pathForMethod = new ArrayList<>(currentPath);
                pathForMethod.add(method);
                result.add(pathForMethod);
            }
            return result;
        } else {
            log.atError().log("Element '{}' is both a field and method in {}", element, clazz);
            throw new ReflectionException("Object element " + element + " is both a field and a method in " + clazz);
        }

        log.atError().log("Element '{}' could not be resolved in {}", element, clazz);
        throw new ReflectionException("Object element " + element + " not found in class " + clazz);
    }

    private List<Object> findRecursively(Class<?> clazz, ObjectAddress address, int index, List<Object> list, boolean findAll)
            throws ReflectionException {
        String element = address.getElement(index);
        log.atTrace().log("findRecursively: element='{}', index={}, class={}, findAll={}", element, index, clazz, findAll);

        if (clazz == null || index >= address.length()) {
            log.atError().log("Element '{}' not found in class {}", element, clazz);
            throw new ReflectionException("Object element " + element + " not found in class " + clazz);
        }

        Field field = ObjectReflectionHelper.getField(clazz, element);

        // For methods: get single method when findAll is false
        Method method = null;
        if (index == address.length() - 1 && field == null && !findAll) {
            method = ObjectReflectionHelper.getMethod(clazz, element);
        }

        boolean hasMethod = (method != null);

        if (field == null && !hasMethod) {
            if (clazz.getSuperclass() != null && !Object.class.equals(clazz.getSuperclass())
                    && !Fields.BlackList.isBlackListed(clazz.getSuperclass())) {
                log.atTrace().log("Element '{}' not found in {}, checking superclass {}", element, clazz,
                        clazz.getSuperclass());
                return findRecursively(clazz.getSuperclass(), address, index, list, findAll);
            }
        } else if (field != null && !hasMethod) {
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
                return findRecursively(genericType, address, index + 1, list, findAll);
            } else if (Map.class.isAssignableFrom(fieldType)) {
                String nextElement = address.getElement(index + 1);
                log.atTrace().log("Field '{}' is Map, next address element='{}'", field.getName(), nextElement);
                if (ObjectAddress.MAP_VALUE_INDICATOR.equals(nextElement)) {
                    Class<?> valueType = Fields.getGenericType(field, 1);
                    return findRecursively(valueType, address, index + 2, list, findAll);
                } else if (ObjectAddress.MAP_KEY_INDICATOR.equals(nextElement)) {
                    Class<?> keyType = Fields.getGenericType(field, 0);
                    return findRecursively(keyType, address, index + 2, list, findAll);
                } else {
                    log.atError().log("Map field '{}' address element must indicate key or value, got '{}'",
                            field.getName(), nextElement);
                    throw new ReflectionException(
                            "Field " + element + " is a map, so address must indicate key or value");
                }
            } else {
                return findRecursively(fieldType, address, index + 1, list, findAll);
            }
        } else if (field == null && hasMethod) {
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

    @Override
    public List<ObjectAddress> addresses(String elementName) throws ReflectionException {
        log.atDebug().log("addresses(String) called for element='{}' in class={}", elementName, objectClass);
        return addresses(this.objectClass, elementName, null);
    }

    /**
     * Recursively resolves all ObjectAddress instances for elements matching the given name.
     *
     * <p>
     * This is the core implementation for {@link #addresses(String)}. It searches for:
     * </p>
     * <ul>
     *   <li>All methods with the given name (including overloaded variants)</li>
     *   <li>Fields with the given name</li>
     *   <li>Nested elements in superclasses</li>
     *   <li>Nested elements in complex field types (collections, maps, arrays, objects)</li>
     * </ul>
     *
     * <p>
     * Unlike {@link #address(Class, String, ObjectAddress)} which returns only the first match,
     * this method returns ALL matches, making it essential for handling overloaded methods.
     * </p>
     *
     * @param objectClass the class to search in
     * @param elementName the name of the element(s) to find
     * @param baseAddress the base address to prepend to found addresses (may be null for top-level search)
     * @return list of all matching ObjectAddress instances (may be empty, never null)
     * @throws ReflectionException if address resolution fails
     */
    private List<ObjectAddress> addresses(Class<?> objectClass, String elementName, ObjectAddress baseAddress)
            throws ReflectionException {
        log.atTrace().log("Resolving all addresses for element='{}', class={}, baseAddress={}", elementName, objectClass,
                baseAddress);
        List<ObjectAddress> result = new ArrayList<>();

        Field field = null;
        try {
            field = objectClass.getDeclaredField(elementName);
        } catch (NoSuchFieldException | SecurityException ignored) {
        }

        // Get all methods with this name (including overloads)
        List<Method> methods = ObjectReflectionHelper.getMethods(objectClass, elementName);

        if (!methods.isEmpty()) {
            log.atDebug().log("Found {} method(s) named '{}' in {}", methods.size(), elementName, objectClass.getName());
            for (Method method : methods) {
                result.add(new ObjectAddress(baseAddress == null ? elementName : baseAddress + "." + elementName, true));
            }
        }

        if (field != null) {
            log.atDebug().log("Found field '{}' in {}", elementName, objectClass.getName());
            result.add(new ObjectAddress(baseAddress == null ? elementName : baseAddress + "." + elementName, true));
        }

        // If we found something in this class, return it
        if (!result.isEmpty()) {
            log.atInfo().log("Resolved {} address(es) for element '{}' in {}", result.size(), elementName, objectClass);
            return result;
        }

        // Search in superclass if nothing found
        if (objectClass.getSuperclass() != null && !Object.class.equals(objectClass.getSuperclass())
                && !Fields.BlackList.isBlackListed(objectClass.getSuperclass())) {
            List<ObjectAddress> superResult = addresses(objectClass.getSuperclass(), elementName, baseAddress);
            if (!superResult.isEmpty()) {
                return superResult;
            }
        }

        // Search in nested fields (same logic as address method)
        for (Field f : objectClass.getDeclaredFields()) {
            if (Fields.isNotPrimitive(f.getType())) {
                List<ObjectAddress> a;
                if ((a = doIfIsCollectionForAddresses(f, elementName, baseAddress)) != null && !a.isEmpty())
                    return a;
                if ((a = doIfIsMapForAddresses(f, elementName, baseAddress)) != null && !a.isEmpty())
                    return a;
                if ((a = doIfIsArrayForAddresses(f, elementName, baseAddress)) != null && !a.isEmpty())
                    return a;
                if ((a = doIfNotEnumForAddresses(f, elementName, baseAddress)) != null && !a.isEmpty())
                    return a;
            }
        }

        log.atWarn().log("No addresses found for element '{}' in {}", elementName, objectClass.getName());
        return result; // Return empty list if nothing found
    }

    private ObjectAddress address(Class<?> objectClass, String elementName, ObjectAddress address)
            throws ReflectionException {
        log.atTrace().log("Resolving address element='{}', class={}, baseAddress={}", elementName, objectClass,
                address);
        Field field = null;
        try {
            field = objectClass.getDeclaredField(elementName);
        } catch (NoSuchFieldException | SecurityException ignored) {
        }

        Method method = ObjectReflectionHelper.getMethod(objectClass, elementName);
        if (method != null) {
            log.atDebug().log("Found method '{}' in {}", elementName, objectClass.getName());
            return new ObjectAddress(address == null ? elementName : address + "." + elementName, true);
        }
        if (field != null) {
            log.atDebug().log("Found field '{}' in {}", elementName, objectClass.getName());
            return new ObjectAddress(address == null ? elementName : address + "." + elementName, true);
        }

        if (objectClass.getSuperclass() != null && !Object.class.equals(objectClass.getSuperclass())
                && !Fields.BlackList.isBlackListed(objectClass.getSuperclass())) {
            address = address(objectClass.getSuperclass(), elementName, address);
            if (address != null) {
                return address;
            }
        }

        for (Field f : objectClass.getDeclaredFields()) {
            if (Fields.isNotPrimitive(f.getType()) ) {
                ObjectAddress a;
                if ((a = doIfIsCollection(f, elementName, address)) != null)
                    return a;
                if ((a = doIfIsMap(f, elementName, address)) != null)
                    return a;
                if ((a = doIfIsArray(f, elementName, address)) != null)
                    return a;
                if ((a = doIfNotEnum(f, elementName, address)) != null)
                    return a;
            }
        }

        log.atWarn().log("Element '{}' could not be resolved in {}", elementName, objectClass.getName());
        return null;
    }

    // --- setValue / getValue ---
    @Override
    public Object setValue(Object object, String fieldAddress, Object fieldValue) throws ReflectionException {
        log.atDebug().log("setValue(String) called for field='{}', value={}, on object={}", fieldAddress, fieldValue,
                object);
        return setValue(object, new ObjectAddress(fieldAddress, true), fieldValue);
    }

    @Override
    public Object setValue(Object object, ObjectAddress fieldAddress, Object fieldValue) throws ReflectionException {
        log.atDebug().log("setValue(ObjectAddress) called for fieldAddress={}, value={}, object={}", fieldAddress,
                fieldValue, object);
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
    public <R> IMethodReturn<R> invoke(String methodAddress, Class<R> returnType, Object... args) throws ReflectionException {
        return invoke(this.object, new ObjectAddress(methodAddress, true), returnType, args);
    }

    @Override
    public <R> IMethodReturn<R> invoke(ObjectAddress methodAddress, Class<R> returnType, Object... args) throws ReflectionException {
        return invoke(this.object, methodAddress, returnType, args);
    }

    @Override
    public <R> IMethodReturn<R> invokeStatic(String methodAddress, Class<R> returnType, Object... args) throws ReflectionException {
        return this.invokeStatic(new ObjectAddress(methodAddress, true), returnType, args);
    }

    @Override
    public <R> IMethodReturn<R> invokeStatic(ObjectAddress methodAddress, Class<R> returnType, Object... args) throws ReflectionException {
        List<Object> methodPath = MethodResolver.selectBestMatch(findAll(methodAddress), returnType, ObjectReflectionHelper.parameterTypes(args), objectClass);
        return new ObjectMethodInvoker<>(this.objectClass, methodPath, methodAddress, returnType).invokeStatic(args);
    }

    @Override
    public <R> IMethodReturn<R> invoke(T object, ObjectAddress methodAddress, Class<R> returnType, Object... args) throws ReflectionException {
        log.atDebug().log("invoke called on object={} for methodAddress={} with args={}", object, methodAddress,
                Arrays.toString(args));
        List<Object> methodPath = MethodResolver.selectBestMatch(findAll(methodAddress), returnType, ObjectReflectionHelper.parameterTypes(args), objectClass);
        return new ObjectMethodInvoker<>((Class<T>) object.getClass(), methodPath, methodAddress, returnType).invoke(object, args);
    }

    private ObjectAddress doIfIsMap(Field f, String elementName, ObjectAddress address) throws ReflectionException {
        if (Map.class.isAssignableFrom(f.getType())) {
            log.atTrace().log("doIfIsMap checking field '{}' for element '{}'", f.getName(), elementName);
            Class<?> keyClass = Fields.getGenericType(f, 0);
            Class<?> valueClass = Fields.getGenericType(f, 1);
            if (Fields.isNotPrimitive(keyClass) && !Fields.BlackList.isBlackListed(keyClass)) {
                ObjectAddress keyAddress = address == null ? new ObjectAddress(f.getName(), true)
                        : address.clone().addElement(f.getName());
                keyAddress.addElement(ObjectAddress.MAP_KEY_INDICATOR);
                ObjectAddress a = address(keyClass, elementName, keyAddress);
                if (a != null)
                    return a;
            }
            if (Fields.isNotPrimitive(valueClass) && !Fields.BlackList.isBlackListed(valueClass)) {
                ObjectAddress valueAddress = address == null ? new ObjectAddress(f.getName(), true)
                        : address.clone().addElement(f.getName());
                valueAddress.addElement(ObjectAddress.MAP_VALUE_INDICATOR);
                ObjectAddress a = address(valueClass, elementName, valueAddress);
                if (a != null)
                    return a;
            }
        }
        return null;
    }

    private ObjectAddress doIfIsArray(Field f, String elementName, ObjectAddress address) throws ReflectionException {
        if (f.getType().isArray()) {
            log.atTrace().log("doIfIsArray checking array field '{}' for element '{}'", f.getName(), elementName);
            Class<?> componentType = f.getType().getComponentType();
            ObjectAddress newAddress = address == null ? new ObjectAddress(f.getName(), true)
                    : address.clone().addElement(f.getName());
            return address(componentType, elementName, newAddress);
        }
        return null;
    }

    private ObjectAddress doIfIsCollection(Field f, String elementName, ObjectAddress address)
            throws ReflectionException {
        if (Collection.class.isAssignableFrom(f.getType())) {
            log.atTrace().log("doIfIsCollection checking field '{}' for element '{}'", f.getName(), elementName);
            Class<?> t = Fields.getGenericType(f, 0);
            ObjectAddress newAddress = address == null ? new ObjectAddress(f.getName(), true)
                    : address.clone().addElement(f.getName());
            return address(t, elementName, newAddress);
        }
        return null;
    }

    private ObjectAddress doIfNotEnum(Field f, String elementName, ObjectAddress address) throws ReflectionException {
        if (!f.getType().isEnum()) {
            log.atTrace().log("doIfNotEnum checking field '{}' for element '{}'", f.getName(), elementName);
            ObjectAddress newAddress = address == null ? new ObjectAddress(f.getName(), true)
                    : address.clone().addElement(f.getName());
            return address(f.getType(), elementName, newAddress);
        }
        return null;
    }

    /**
     * Helper methods for {@link #addresses(Class, String, ObjectAddress)} that return Lists
     * instead of single ObjectAddress instances. These methods handle nested object traversal
     * for collections, maps, arrays, and complex objects when searching for all matching elements.
     */

    /**
     * Searches for addresses in Map field keys and values.
     * Returns all ObjectAddress instances found in either map keys or values.
     *
     * @param f the Map field to search
     * @param elementName the element name to find
     * @param address the base address (may be null)
     * @return list of ObjectAddress instances found, or null if field is not a Map
     * @throws ReflectionException if address resolution fails
     */
    private List<ObjectAddress> doIfIsMapForAddresses(Field f, String elementName, ObjectAddress address) throws ReflectionException {
        if (Map.class.isAssignableFrom(f.getType())) {
            log.atTrace().log("doIfIsMapForAddresses checking field '{}' for element '{}'", f.getName(), elementName);
            Class<?> keyClass = Fields.getGenericType(f, 0);
            Class<?> valueClass = Fields.getGenericType(f, 1);
            if (Fields.isNotPrimitive(keyClass) && !Fields.BlackList.isBlackListed(keyClass)) {
                ObjectAddress keyAddress = address == null ? new ObjectAddress(f.getName(), true)
                        : address.clone().addElement(f.getName());
                keyAddress.addElement(ObjectAddress.MAP_KEY_INDICATOR);
                List<ObjectAddress> a = addresses(keyClass, elementName, keyAddress);
                if (!a.isEmpty())
                    return a;
            }
            if (Fields.isNotPrimitive(valueClass) && !Fields.BlackList.isBlackListed(valueClass)) {
                ObjectAddress valueAddress = address == null ? new ObjectAddress(f.getName(), true)
                        : address.clone().addElement(f.getName());
                valueAddress.addElement(ObjectAddress.MAP_VALUE_INDICATOR);
                List<ObjectAddress> a = addresses(valueClass, elementName, valueAddress);
                if (!a.isEmpty())
                    return a;
            }
        }
        return null;
    }

    /**
     * Searches for addresses in array component types.
     *
     * @param f the array field to search
     * @param elementName the element name to find
     * @param address the base address (may be null)
     * @return list of ObjectAddress instances found in array component type, or null if field is not an array
     * @throws ReflectionException if address resolution fails
     */
    private List<ObjectAddress> doIfIsArrayForAddresses(Field f, String elementName, ObjectAddress address) throws ReflectionException {
        if (f.getType().isArray()) {
            log.atTrace().log("doIfIsArrayForAddresses checking array field '{}' for element '{}'", f.getName(), elementName);
            Class<?> componentType = f.getType().getComponentType();
            ObjectAddress newAddress = address == null ? new ObjectAddress(f.getName(), true)
                    : address.clone().addElement(f.getName());
            return addresses(componentType, elementName, newAddress);
        }
        return null;
    }

    /**
     * Searches for addresses in Collection generic type.
     *
     * @param f the Collection field to search
     * @param elementName the element name to find
     * @param address the base address (may be null)
     * @return list of ObjectAddress instances found in collection element type, or null if field is not a Collection
     * @throws ReflectionException if address resolution fails
     */
    private List<ObjectAddress> doIfIsCollectionForAddresses(Field f, String elementName, ObjectAddress address)
            throws ReflectionException {
        if (Collection.class.isAssignableFrom(f.getType())) {
            log.atTrace().log("doIfIsCollectionForAddresses checking field '{}' for element '{}'", f.getName(), elementName);
            Class<?> t = Fields.getGenericType(f, 0);
            ObjectAddress newAddress = address == null ? new ObjectAddress(f.getName(), true)
                    : address.clone().addElement(f.getName());
            return addresses(t, elementName, newAddress);
        }
        return null;
    }

    /**
     * Searches for addresses in non-enum complex field types.
     *
     * @param f the field to search
     * @param elementName the element name to find
     * @param address the base address (may be null)
     * @return list of ObjectAddress instances found in the field's type, or null if field is an enum
     * @throws ReflectionException if address resolution fails
     */
    private List<ObjectAddress> doIfNotEnumForAddresses(Field f, String elementName, ObjectAddress address) throws ReflectionException {
        if (!f.getType().isEnum()) {
            log.atTrace().log("doIfNotEnumForAddresses checking field '{}' for element '{}'", f.getName(), elementName);
            ObjectAddress newAddress = address == null ? new ObjectAddress(f.getName(), true)
                    : address.clone().addElement(f.getName());
            return addresses(f.getType(), elementName, newAddress);
        }
        return null;
    }
}
