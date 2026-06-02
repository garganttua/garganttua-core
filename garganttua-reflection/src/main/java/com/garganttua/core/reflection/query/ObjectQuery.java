package com.garganttua.core.reflection.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IObjectQuery;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.fields.Fields;

/**
 * Default {@link IObjectQuery} implementation that resolves dotted member paths
 * (fields and methods) within a class hierarchy.
 *
 * <p>Resolution recurses through fields, superclasses, collection/array element types
 * and map key/value types, producing one or more {@link ObjectAddress}es or member paths
 * for a requested element name.
 *
 * @param <T> the queried class type
 */
public class ObjectQuery<T> implements IObjectQuery<T> {
    private static final Logger log = Logger.getLogger(ObjectQuery.class);

    private IClass<T> objectClass;
    private IReflectionProvider provider;

    // Well-known IClass instances cached for performance
    private IClass<?> objectIClass;
    private IClass<?> collectionIClass;
    private IClass<?> mapIClass;

    /**
     * Creates a query bound to a class and reflection provider.
     *
     * @param objectClass the class whose members are queried
     * @param provider    the reflection provider used to resolve well-known and generic types
     * @throws ReflectionException if {@code objectClass} is null
     */
    protected ObjectQuery(IClass<T> objectClass, IReflectionProvider provider) throws ReflectionException {
        log.trace("Creating ObjectQuery with objectClass={}", objectClass);
        if (objectClass == null) {
            throw new ReflectionException("class is null");
        }
        this.objectClass = objectClass;
        this.provider = provider;
        initWellKnownClasses();
        log.debug("ObjectQuery initialized with objectClass={}", objectClass);
    }

    private void initWellKnownClasses() {
        this.objectIClass = provider.getClass(Object.class);
        this.collectionIClass = provider.getClass(Collection.class);
        this.mapIClass = provider.getClass(Map.class);
    }

    // --- IObjectQuery implementation ---

    @Override
    public List<Object> find(String elementName) throws ReflectionException {
        log.trace("find(String) called with elementName='{}'", elementName);
        return this.find(new ObjectAddress(elementName, true));
    }

    @Override
    public List<Object> find(ObjectAddress elementName) throws ReflectionException {
        log.debug("find(ObjectAddress) called with elementName={} in class={}", elementName, objectClass);
        List<Object> list = new ArrayList<>();
        List<Object> result = findRecursively(this.objectClass, elementName, 0, list, false);
        log.trace("find result for {} : {}", elementName, result);
        return result;
    }

    @Override
    public List<List<Object>> findAll(String elementName) throws ReflectionException {
        log.trace("findAll(String) called with elementName='{}'", elementName);
        return this.findAll(new ObjectAddress(elementName, true));
    }

    @Override
    public List<List<Object>> findAll(ObjectAddress elementName) throws ReflectionException {
        log.debug("findAll(ObjectAddress) called with elementName={} in class={}", elementName, objectClass);
        return findAllRecursively(this.objectClass, elementName, 0, new ArrayList<>());
    }

    /** A map field's traversed generic type (key or value) and the address index past the indicator. */
    private record MapEntry(IClass<?> type, int nextIndex) {
    }

    /**
     * Resolve the generic type to recurse into for a map field, using the address
     * element following the field (which must indicate key or value).
     */
    private MapEntry resolveMapEntry(IField field, ObjectAddress address, int index, String element)
            throws ReflectionException {
        String nextElement = address.getElement(index + 1);
        log.trace("Field '{}' is Map, next address element='{}'", field.getName(), nextElement);
        if (ObjectAddress.MAP_VALUE_INDICATOR.equals(nextElement)) {
            return new MapEntry(Fields.getGenericType(field, 1, provider), index + 2);
        }
        if (ObjectAddress.MAP_KEY_INDICATOR.equals(nextElement)) {
            return new MapEntry(Fields.getGenericType(field, 0, provider), index + 2);
        }
        log.error("Map field '{}' address element must indicate key or value, got '{}'", field.getName(), nextElement);
        throw new ReflectionException("Field " + element + " is a map, so address must indicate key or value");
    }

    /** Recurse into a non-terminal field's type (collection element, map key/value, or the type itself) — all-paths variant. */
    private List<List<Object>> recurseIntoFieldTypeAll(IField field, ObjectAddress address, int index, String element,
            List<Object> newPath) throws ReflectionException {
        IClass<?> fieldType = field.getType();
        if (collectionIClass.isAssignableFrom(fieldType)) {
            IClass<?> genericType = Fields.getGenericType(field, 0, provider);
            log.trace("Field '{}' is Collection, recursing into type={}", field.getName(), genericType);
            return findAllRecursively(genericType, address, index + 1, newPath);
        }
        if (mapIClass.isAssignableFrom(fieldType)) {
            MapEntry entry = resolveMapEntry(field, address, index, element);
            return findAllRecursively(entry.type(), address, entry.nextIndex(), newPath);
        }
        return findAllRecursively(fieldType, address, index + 1, newPath);
    }

    /** Recurse into a non-terminal field's type (collection element, map key/value, or the type itself) — single-path variant. */
    private List<Object> recurseIntoFieldType(IField field, ObjectAddress address, int index, String element,
            List<Object> list, boolean findAll) throws ReflectionException {
        IClass<?> fieldType = field.getType();
        if (collectionIClass.isAssignableFrom(fieldType)) {
            IClass<?> genericType = Fields.getGenericType(field, 0, provider);
            log.trace("Field '{}' is Collection, recursing into type={}", field.getName(), genericType);
            return findRecursively(genericType, address, index + 1, list, findAll);
        }
        if (mapIClass.isAssignableFrom(fieldType)) {
            MapEntry entry = resolveMapEntry(field, address, index, element);
            return findRecursively(entry.type(), address, entry.nextIndex(), list, findAll);
        }
        return findRecursively(fieldType, address, index + 1, list, findAll);
    }

    private List<List<Object>> findAllRecursively(IClass<?> clazz, ObjectAddress address, int index, List<Object> currentPath)
            throws ReflectionException {
        String element = address.getElement(index);
        log.trace("findAllRecursively: element='{}', index={}, class={}", element, index, clazz);

        if (clazz == null || index >= address.length()) {
            log.error("Element '{}' not found in class {}", element, clazz);
            throw new ReflectionException("Object element " + element + " not found in class " + clazz);
        }

        IField field = MemberLookup.getField(clazz, element);

        List<IMethod> methods = null;
        if (index == address.length() - 1 && field == null) {
            methods = MemberLookup.getMethods(clazz, element);
        }

        boolean hasMethods = (methods != null && !methods.isEmpty());

        if (field == null && !hasMethods) {
            IClass<?> superclass = clazz.getSuperclass();
            if (superclass != null && !objectIClass.equals(superclass)
                    && !Fields.BlackList.isBlackListed(superclass)) {
                log.trace("Element '{}' not found in {}, checking superclass {}", element, clazz, superclass);
                return findAllRecursively(superclass, address, index, currentPath);
            }
        } else if (field != null && !hasMethods) {
            log.debug("Field '{}' found in {}", field.getName(), clazz.getName());
            List<Object> newPath = new ArrayList<>(currentPath);
            newPath.add(field);

            if (index == address.length() - 1) {
                log.debug("Resolved field '{}' fully in {}", element, clazz);
                List<List<Object>> result = new ArrayList<>();
                result.add(newPath);
                return result;
            }
            return recurseIntoFieldTypeAll(field, address, index, element, newPath);
        } else if (field == null && hasMethods) {
            log.debug("Found {} method(s) named '{}' in {}", methods.size(), element, clazz.getName());

            List<List<Object>> result = new ArrayList<>();
            for (IMethod method : methods) {
                List<Object> pathForMethod = new ArrayList<>(currentPath);
                pathForMethod.add(method);
                result.add(pathForMethod);
            }
            return result;
        } else {
            log.error("Element '{}' is both a field and method in {}", element, clazz);
            throw new ReflectionException("Object element " + element + " is both a field and a method in " + clazz);
        }

        log.error("Element '{}' could not be resolved in {}", element, clazz);
        throw new ReflectionException("Object element " + element + " not found in class " + clazz);
    }

    private List<Object> findRecursively(IClass<?> clazz, ObjectAddress address, int index, List<Object> list, boolean findAll)
            throws ReflectionException {
        String element = address.getElement(index);
        log.trace("findRecursively: element='{}', index={}, class={}, findAll={}", element, index, clazz, findAll);

        if (clazz == null || index >= address.length()) {
            log.error("Element '{}' not found in class {}", element, clazz);
            throw new ReflectionException("Object element " + element + " not found in class " + clazz);
        }

        IField field = MemberLookup.getField(clazz, element);

        IMethod method = null;
        if (index == address.length() - 1 && field == null && !findAll) {
            method = MemberLookup.getMethod(clazz, element);
        }

        boolean hasMethod = (method != null);

        if (field == null && !hasMethod) {
            IClass<?> superclass = clazz.getSuperclass();
            if (superclass != null && !objectIClass.equals(superclass)
                    && !Fields.BlackList.isBlackListed(superclass)) {
                log.trace("Element '{}' not found in {}, checking superclass {}", element, clazz, superclass);
                return findRecursively(superclass, address, index, list, findAll);
            }
        } else if (field != null && !hasMethod) {
            log.debug("Field '{}' found in {}", field.getName(), clazz.getName());
            list.add(field);
            if (index == address.length() - 1) {
                log.debug("Resolved field '{}' fully in {}", element, clazz);
                return list;
            }
            return recurseIntoFieldType(field, address, index, element, list, findAll);
        } else if (field == null && hasMethod) {
            log.debug("Method '{}' found in {}", method.getName(), clazz.getName());
            list.add(method);
            return list;
        } else {
            log.error("Element '{}' is both a field and method in {}", element, clazz);
            throw new ReflectionException("Object element " + element + " is both a field and a method in " + clazz);
        }

        log.error("Element '{}' could not be resolved in {}", element, clazz);
        throw new ReflectionException("Object element " + element + " not found in class " + clazz);
    }

    @Override
    public ObjectAddress address(String elementName) throws ReflectionException {
        log.debug("address(String) called for element='{}' in class={}", elementName, objectClass);
        return address(this.objectClass, elementName, null);
    }

    @Override
    public List<ObjectAddress> addresses(String elementName) throws ReflectionException {
        log.debug("addresses(String) called for element='{}' in class={}", elementName, objectClass);
        return addresses(this.objectClass, elementName, null);
    }

    private List<ObjectAddress> addresses(IClass<?> objectClass, String elementName, ObjectAddress baseAddress)
            throws ReflectionException {
        log.trace("Resolving all addresses for element='{}', class={}, baseAddress={}", elementName, objectClass,
                baseAddress);
        List<ObjectAddress> result = new ArrayList<>();

        IField field = null;
        try {
            field = objectClass.getDeclaredField(elementName);
        } catch (NoSuchFieldException | SecurityException ignored) {
        }

        List<IMethod> methods = MemberLookup.getMethods(objectClass, elementName);

        if (!methods.isEmpty()) {
            log.debug("Found {} method(s) named '{}' in {}", methods.size(), elementName, objectClass.getName());
            for (IMethod method : methods) {
                result.add(new ObjectAddress(baseAddress == null ? elementName : baseAddress + "." + elementName, true));
            }
        }

        if (field != null) {
            log.debug("Found field '{}' in {}", elementName, objectClass.getName());
            result.add(new ObjectAddress(baseAddress == null ? elementName : baseAddress + "." + elementName, true));
        }

        if (!result.isEmpty()) {
            log.debug("Resolved {} address(es) for element '{}' in {}", result.size(), elementName, objectClass);
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

        List<ObjectAddress> nested = scanFieldsForAddresses(objectClass, elementName, baseAddress);
        if (nested != null) {
            return nested;
        }

        log.warn("No addresses found for element '{}' in {}", elementName, objectClass.getName());
        return result;
    }

    private List<ObjectAddress> scanFieldsForAddresses(IClass<?> objectClass, String elementName, ObjectAddress baseAddress)
            throws ReflectionException {
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
        return null;
    }

    private ObjectAddress address(IClass<?> objectClass, String elementName, ObjectAddress address)
            throws ReflectionException {
        log.trace("Resolving address element='{}', class={}, baseAddress={}", elementName, objectClass,
                address);
        IField field = null;
        try {
            field = objectClass.getDeclaredField(elementName);
        } catch (NoSuchFieldException | SecurityException ignored) {
        }

        IMethod method = MemberLookup.getMethod(objectClass, elementName);
        if (method != null) {
            log.debug("Found method '{}' in {}", elementName, objectClass.getName());
            return new ObjectAddress(address == null ? elementName : address + "." + elementName, true);
        }
        if (field != null) {
            log.debug("Found field '{}' in {}", elementName, objectClass.getName());
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

        ObjectAddress nested = scanFieldsForAddress(objectClass, elementName, address);
        if (nested != null) {
            return nested;
        }

        log.warn("Element '{}' could not be resolved in {}", elementName, objectClass.getName());
        return null;
    }

    private ObjectAddress scanFieldsForAddress(IClass<?> objectClass, String elementName, ObjectAddress address)
            throws ReflectionException {
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
        return null;
    }

    private ObjectAddress doIfIsMap(IField f, String elementName, ObjectAddress address) throws ReflectionException {
        if (mapIClass.isAssignableFrom(f.getType())) {
            log.trace("doIfIsMap checking field '{}' for element '{}'", f.getName(), elementName);
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
            log.trace("doIfIsArray checking array field '{}' for element '{}'", f.getName(), elementName);
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
            log.trace("doIfIsCollection checking field '{}' for element '{}'", f.getName(), elementName);
            IClass<?> t = Fields.getGenericType(f, 0, provider);
            ObjectAddress newAddress = address == null ? new ObjectAddress(f.getName(), true)
                    : address.addElement(f.getName());
            return address(t, elementName, newAddress);
        }
        return null;
    }

    private ObjectAddress doIfNotEnum(IField f, String elementName, ObjectAddress address) throws ReflectionException {
        if (!f.getType().isEnum() && Fields.isNotPrimitiveOrInternal(f.getType())) {
            log.trace("doIfNotEnum checking field '{}' for element '{}'", f.getName(), elementName);
            ObjectAddress newAddress = address == null ? new ObjectAddress(f.getName(), true)
                    : address.addElement(f.getName());
            return address(f.getType(), elementName, newAddress);
        }
        return null;
    }

    private List<ObjectAddress> doIfIsMapForAddresses(IField f, String elementName, ObjectAddress address) throws ReflectionException {
        if (mapIClass.isAssignableFrom(f.getType())) {
            log.trace("doIfIsMapForAddresses checking field '{}' for element '{}'", f.getName(), elementName);
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
            log.trace("doIfIsArrayForAddresses checking array field '{}' for element '{}'", f.getName(), elementName);
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
            log.trace("doIfIsCollectionForAddresses checking field '{}' for element '{}'", f.getName(), elementName);
            IClass<?> t = Fields.getGenericType(f, 0, provider);
            ObjectAddress newAddress = address == null ? new ObjectAddress(f.getName(), true)
                    : address.addElement(f.getName());
            return addresses(t, elementName, newAddress);
        }
        return null;
    }

    private List<ObjectAddress> doIfNotEnumForAddresses(IField f, String elementName, ObjectAddress address) throws ReflectionException {
        if (!f.getType().isEnum() && Fields.isNotPrimitiveOrInternal(f.getType())) {
            log.trace("doIfNotEnumForAddresses checking field '{}' for element '{}'", f.getName(), elementName);
            ObjectAddress newAddress = address == null ? new ObjectAddress(f.getName(), true)
                    : address.addElement(f.getName());
            return addresses(f.getType(), elementName, newAddress);
        }
        return null;
    }
}
