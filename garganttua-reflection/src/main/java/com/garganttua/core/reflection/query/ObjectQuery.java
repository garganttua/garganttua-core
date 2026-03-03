package com.garganttua.core.reflection.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IObjectQuery;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.fields.Fields;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ObjectQuery<T> implements IObjectQuery<T> {

    private IClass<T> objectClass;
    private IReflectionProvider provider;

    // Well-known IClass instances cached for performance
    private IClass<?> objectIClass;
    private IClass<?> collectionIClass;
    private IClass<?> mapIClass;

    protected ObjectQuery(IClass<T> objectClass, IReflectionProvider provider) throws ReflectionException {
        log.atTrace().log("Creating ObjectQuery with objectClass={}", objectClass);
        if (objectClass == null) {
            throw new ReflectionException("class is null");
        }
        this.objectClass = objectClass;
        this.provider = provider;
        initWellKnownClasses();
        log.atDebug().log("ObjectQuery initialized with objectClass={}", objectClass);
    }

    private void initWellKnownClasses() {
        this.objectIClass = provider.getClass(Object.class);
        this.collectionIClass = provider.getClass(Collection.class);
        this.mapIClass = provider.getClass(Map.class);
    }

    // --- Local helpers for IClass-based field/method lookup ---

    private static IField getField(IClass<?> clazz, String name) {
        for (IField f : clazz.getDeclaredFields()) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        IClass<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            return getField(superclass, name);
        }
        return null;
    }

    private static IMethod getMethod(IClass<?> clazz, String name) {
        for (IMethod m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        IClass<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            return getMethod(superclass, name);
        }
        return null;
    }

    private static List<IMethod> getMethods(IClass<?> clazz, String name) {
        List<IMethod> methods = new ArrayList<>();
        HashSet<String> seenSignatures = new HashSet<>();

        for (IMethod m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                String signature = buildMethodSignature(m);
                if (!seenSignatures.contains(signature)) {
                    methods.add(m);
                    seenSignatures.add(signature);
                }
            }
        }

        IClass<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            for (IMethod m : getMethods(superclass, name)) {
                String signature = buildMethodSignature(m);
                if (!seenSignatures.contains(signature)) {
                    methods.add(m);
                    seenSignatures.add(signature);
                }
            }
        }

        return methods;
    }

    private static String buildMethodSignature(IMethod method) {
        StringBuilder signature = new StringBuilder(method.getName());
        signature.append("(");
        IClass<?>[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) {
                signature.append(",");
            }
            signature.append(paramTypes[i].getName());
        }
        signature.append(")");
        return signature.toString();
    }

    // --- IObjectQuery implementation ---

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

    private List<List<Object>> findAllRecursively(IClass<?> clazz, ObjectAddress address, int index, List<Object> currentPath)
            throws ReflectionException {
        String element = address.getElement(index);
        log.atTrace().log("findAllRecursively: element='{}', index={}, class={}", element, index, clazz);

        if (clazz == null || index >= address.length()) {
            log.atError().log("Element '{}' not found in class {}", element, clazz);
            throw new ReflectionException("Object element " + element + " not found in class " + clazz);
        }

        IField field = getField(clazz, element);

        List<IMethod> methods = null;
        if (index == address.length() - 1 && field == null) {
            methods = getMethods(clazz, element);
        }

        boolean hasMethods = (methods != null && !methods.isEmpty());

        if (field == null && !hasMethods) {
            IClass<?> superclass = clazz.getSuperclass();
            if (superclass != null && !objectIClass.equals(superclass)
                    && !Fields.BlackList.isBlackListed(superclass)) {
                log.atTrace().log("Element '{}' not found in {}, checking superclass {}", element, clazz, superclass);
                return findAllRecursively(superclass, address, index, currentPath);
            }
        } else if (field != null && !hasMethods) {
            log.atDebug().log("Field '{}' found in {}", field.getName(), clazz.getName());
            List<Object> newPath = new ArrayList<>(currentPath);
            newPath.add(field);

            if (index == address.length() - 1) {
                log.atDebug().log("Resolved field '{}' fully in {}", element, clazz);
                List<List<Object>> result = new ArrayList<>();
                result.add(newPath);
                return result;
            }

            IClass<?> fieldType = field.getType();
            if (collectionIClass.isAssignableFrom(fieldType)) {
                IClass<?> genericType = Fields.getGenericType(field, 0, provider);
                log.atTrace().log("Field '{}' is Collection, recursing into type={}", field.getName(), genericType);
                return findAllRecursively(genericType, address, index + 1, newPath);
            } else if (mapIClass.isAssignableFrom(fieldType)) {
                String nextElement = address.getElement(index + 1);
                log.atTrace().log("Field '{}' is Map, next address element='{}'", field.getName(), nextElement);
                if (ObjectAddress.MAP_VALUE_INDICATOR.equals(nextElement)) {
                    IClass<?> valueType = Fields.getGenericType(field, 1, provider);
                    return findAllRecursively(valueType, address, index + 2, newPath);
                } else if (ObjectAddress.MAP_KEY_INDICATOR.equals(nextElement)) {
                    IClass<?> keyType = Fields.getGenericType(field, 0, provider);
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

            List<List<Object>> result = new ArrayList<>();
            for (IMethod method : methods) {
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

    private List<Object> findRecursively(IClass<?> clazz, ObjectAddress address, int index, List<Object> list, boolean findAll)
            throws ReflectionException {
        String element = address.getElement(index);
        log.atTrace().log("findRecursively: element='{}', index={}, class={}, findAll={}", element, index, clazz, findAll);

        if (clazz == null || index >= address.length()) {
            log.atError().log("Element '{}' not found in class {}", element, clazz);
            throw new ReflectionException("Object element " + element + " not found in class " + clazz);
        }

        IField field = getField(clazz, element);

        IMethod method = null;
        if (index == address.length() - 1 && field == null && !findAll) {
            method = getMethod(clazz, element);
        }

        boolean hasMethod = (method != null);

        if (field == null && !hasMethod) {
            IClass<?> superclass = clazz.getSuperclass();
            if (superclass != null && !objectIClass.equals(superclass)
                    && !Fields.BlackList.isBlackListed(superclass)) {
                log.atTrace().log("Element '{}' not found in {}, checking superclass {}", element, clazz, superclass);
                return findRecursively(superclass, address, index, list, findAll);
            }
        } else if (field != null && !hasMethod) {
            log.atDebug().log("Field '{}' found in {}", field.getName(), clazz.getName());
            list.add(field);
            if (index == address.length() - 1) {
                log.atDebug().log("Resolved field '{}' fully in {}", element, clazz);
                return list;
            }
            IClass<?> fieldType = field.getType();
            if (collectionIClass.isAssignableFrom(fieldType)) {
                IClass<?> genericType = Fields.getGenericType(field, 0, provider);
                log.atTrace().log("Field '{}' is Collection, recursing into type={}", field.getName(), genericType);
                return findRecursively(genericType, address, index + 1, list, findAll);
            } else if (mapIClass.isAssignableFrom(fieldType)) {
                String nextElement = address.getElement(index + 1);
                log.atTrace().log("Field '{}' is Map, next address element='{}'", field.getName(), nextElement);
                if (ObjectAddress.MAP_VALUE_INDICATOR.equals(nextElement)) {
                    IClass<?> valueType = Fields.getGenericType(field, 1, provider);
                    return findRecursively(valueType, address, index + 2, list, findAll);
                } else if (ObjectAddress.MAP_KEY_INDICATOR.equals(nextElement)) {
                    IClass<?> keyType = Fields.getGenericType(field, 0, provider);
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

    private List<ObjectAddress> addresses(IClass<?> objectClass, String elementName, ObjectAddress baseAddress)
            throws ReflectionException {
        log.atTrace().log("Resolving all addresses for element='{}', class={}, baseAddress={}", elementName, objectClass,
                baseAddress);
        List<ObjectAddress> result = new ArrayList<>();

        IField field = null;
        try {
            field = objectClass.getDeclaredField(elementName);
        } catch (NoSuchFieldException | SecurityException ignored) {
        }

        List<IMethod> methods = getMethods(objectClass, elementName);

        if (!methods.isEmpty()) {
            log.atDebug().log("Found {} method(s) named '{}' in {}", methods.size(), elementName, objectClass.getName());
            for (IMethod method : methods) {
                result.add(new ObjectAddress(baseAddress == null ? elementName : baseAddress + "." + elementName, true));
            }
        }

        if (field != null) {
            log.atDebug().log("Found field '{}' in {}", elementName, objectClass.getName());
            result.add(new ObjectAddress(baseAddress == null ? elementName : baseAddress + "." + elementName, true));
        }

        if (!result.isEmpty()) {
            log.atDebug().log("Resolved {} address(es) for element '{}' in {}", result.size(), elementName, objectClass);
            return result;
        }

        IClass<?> superclass = objectClass.getSuperclass();
        if (superclass != null && !objectIClass.equals(superclass)
                && !Fields.BlackList.isBlackListed(superclass)) {
            List<ObjectAddress> superResult = addresses(superclass, elementName, baseAddress);
            if (!superResult.isEmpty()) {
                return superResult;
            }
        }

        for (IField f : objectClass.getDeclaredFields()) {
            if (Fields.isNotPrimitiveOrInternal(f.getType())) {
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
        return result;
    }

    private ObjectAddress address(IClass<?> objectClass, String elementName, ObjectAddress address)
            throws ReflectionException {
        log.atTrace().log("Resolving address element='{}', class={}, baseAddress={}", elementName, objectClass,
                address);
        IField field = null;
        try {
            field = objectClass.getDeclaredField(elementName);
        } catch (NoSuchFieldException | SecurityException ignored) {
        }

        IMethod method = getMethod(objectClass, elementName);
        if (method != null) {
            log.atDebug().log("Found method '{}' in {}", elementName, objectClass.getName());
            return new ObjectAddress(address == null ? elementName : address + "." + elementName, true);
        }
        if (field != null) {
            log.atDebug().log("Found field '{}' in {}", elementName, objectClass.getName());
            return new ObjectAddress(address == null ? elementName : address + "." + elementName, true);
        }

        IClass<?> superclass = objectClass.getSuperclass();
        if (superclass != null && !objectIClass.equals(superclass)
                && !Fields.BlackList.isBlackListed(superclass)) {
            address = address(superclass, elementName, address);
            if (address != null) {
                return address;
            }
        }

        for (IField f : objectClass.getDeclaredFields()) {
            if (Fields.isNotPrimitiveOrInternal(f.getType())) {
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

    private ObjectAddress doIfIsMap(IField f, String elementName, ObjectAddress address) throws ReflectionException {
        if (mapIClass.isAssignableFrom(f.getType())) {
            log.atTrace().log("doIfIsMap checking field '{}' for element '{}'", f.getName(), elementName);
            IClass<?> keyClass = Fields.getGenericType(f, 0, provider);
            IClass<?> valueClass = Fields.getGenericType(f, 1, provider);
            if (keyClass != null && Fields.isNotPrimitive(keyClass) && !Fields.BlackList.isBlackListed(keyClass)) {
                ObjectAddress keyAddress = address == null ? new ObjectAddress(f.getName(), true)
                        : address.addElement(f.getName());
                keyAddress = keyAddress.addElement(ObjectAddress.MAP_KEY_INDICATOR);
                ObjectAddress a = address(keyClass, elementName, keyAddress);
                if (a != null)
                    return a;
            }
            if (valueClass != null && Fields.isNotPrimitive(valueClass) && !Fields.BlackList.isBlackListed(valueClass)) {
                ObjectAddress valueAddress = address == null ? new ObjectAddress(f.getName(), true)
                        : address.addElement(f.getName());
                valueAddress = valueAddress.addElement(ObjectAddress.MAP_VALUE_INDICATOR);
                ObjectAddress a = address(valueClass, elementName, valueAddress);
                if (a != null)
                    return a;
            }
        }
        return null;
    }

    private ObjectAddress doIfIsArray(IField f, String elementName, ObjectAddress address) throws ReflectionException {
        if (f.getType().isArray()) {
            log.atTrace().log("doIfIsArray checking array field '{}' for element '{}'", f.getName(), elementName);
            IClass<?> componentType = f.getType().getComponentType();
            ObjectAddress newAddress = address == null ? new ObjectAddress(f.getName(), true)
                    : address.addElement(f.getName());
            return address(componentType, elementName, newAddress);
        }
        return null;
    }

    private ObjectAddress doIfIsCollection(IField f, String elementName, ObjectAddress address)
            throws ReflectionException {
        if (collectionIClass.isAssignableFrom(f.getType())) {
            log.atTrace().log("doIfIsCollection checking field '{}' for element '{}'", f.getName(), elementName);
            IClass<?> t = Fields.getGenericType(f, 0, provider);
            ObjectAddress newAddress = address == null ? new ObjectAddress(f.getName(), true)
                    : address.addElement(f.getName());
            return address(t, elementName, newAddress);
        }
        return null;
    }

    private ObjectAddress doIfNotEnum(IField f, String elementName, ObjectAddress address) throws ReflectionException {
        if (!f.getType().isEnum() && Fields.isNotPrimitiveOrInternal(f.getType())) {
            log.atTrace().log("doIfNotEnum checking field '{}' for element '{}'", f.getName(), elementName);
            ObjectAddress newAddress = address == null ? new ObjectAddress(f.getName(), true)
                    : address.addElement(f.getName());
            return address(f.getType(), elementName, newAddress);
        }
        return null;
    }

    private List<ObjectAddress> doIfIsMapForAddresses(IField f, String elementName, ObjectAddress address) throws ReflectionException {
        if (mapIClass.isAssignableFrom(f.getType())) {
            log.atTrace().log("doIfIsMapForAddresses checking field '{}' for element '{}'", f.getName(), elementName);
            IClass<?> keyClass = Fields.getGenericType(f, 0, provider);
            IClass<?> valueClass = Fields.getGenericType(f, 1, provider);
            if (keyClass != null && Fields.isNotPrimitive(keyClass) && !Fields.BlackList.isBlackListed(keyClass)) {
                ObjectAddress keyAddress = address == null ? new ObjectAddress(f.getName(), true)
                        : address.addElement(f.getName());
                keyAddress = keyAddress.addElement(ObjectAddress.MAP_KEY_INDICATOR);
                List<ObjectAddress> a = addresses(keyClass, elementName, keyAddress);
                if (!a.isEmpty())
                    return a;
            }
            if (valueClass != null && Fields.isNotPrimitive(valueClass) && !Fields.BlackList.isBlackListed(valueClass)) {
                ObjectAddress valueAddress = address == null ? new ObjectAddress(f.getName(), true)
                        : address.addElement(f.getName());
                valueAddress = valueAddress.addElement(ObjectAddress.MAP_VALUE_INDICATOR);
                List<ObjectAddress> a = addresses(valueClass, elementName, valueAddress);
                if (!a.isEmpty())
                    return a;
            }
        }
        return null;
    }

    private List<ObjectAddress> doIfIsArrayForAddresses(IField f, String elementName, ObjectAddress address) throws ReflectionException {
        if (f.getType().isArray()) {
            log.atTrace().log("doIfIsArrayForAddresses checking array field '{}' for element '{}'", f.getName(), elementName);
            IClass<?> componentType = f.getType().getComponentType();
            ObjectAddress newAddress = address == null ? new ObjectAddress(f.getName(), true)
                    : address.addElement(f.getName());
            return addresses(componentType, elementName, newAddress);
        }
        return null;
    }

    private List<ObjectAddress> doIfIsCollectionForAddresses(IField f, String elementName, ObjectAddress address)
            throws ReflectionException {
        if (collectionIClass.isAssignableFrom(f.getType())) {
            log.atTrace().log("doIfIsCollectionForAddresses checking field '{}' for element '{}'", f.getName(), elementName);
            IClass<?> t = Fields.getGenericType(f, 0, provider);
            ObjectAddress newAddress = address == null ? new ObjectAddress(f.getName(), true)
                    : address.addElement(f.getName());
            return addresses(t, elementName, newAddress);
        }
        return null;
    }

    private List<ObjectAddress> doIfNotEnumForAddresses(IField f, String elementName, ObjectAddress address) throws ReflectionException {
        if (!f.getType().isEnum() && Fields.isNotPrimitiveOrInternal(f.getType())) {
            log.atTrace().log("doIfNotEnumForAddresses checking field '{}' for element '{}'", f.getName(), elementName);
            ObjectAddress newAddress = address == null ? new ObjectAddress(f.getName(), true)
                    : address.addElement(f.getName());
            return addresses(f.getType(), elementName, newAddress);
        }
        return null;
    }
}
