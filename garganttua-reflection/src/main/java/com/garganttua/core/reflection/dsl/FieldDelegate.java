package com.garganttua.core.reflection.dsl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IFieldValue;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.fields.FieldAccessor;
import com.garganttua.core.reflection.fields.FieldResolver;
import com.garganttua.core.reflection.fields.Fields;
import com.garganttua.core.reflection.fields.ResolvedField;
import com.garganttua.core.reflection.fields.SingleFieldValue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class FieldDelegate {

    private final IReflectionProvider provider;

    FieldDelegate(IReflectionProvider provider) {
        this.provider = provider;
    }

    Optional<IField> findField(IClass<?> clazz, String fieldName) {
        log.atTrace().log("Finding field {} in class: {}", fieldName, clazz.getName());
        for (IField f : clazz.getDeclaredFields()) {
            if (f.getName().equals(fieldName)) {
                return Optional.of(f);
            }
        }
        if (clazz.getSuperclass() != null) {
            return findField(clazz.getSuperclass(), fieldName);
        }
        return Optional.empty();
    }

    Optional<IField> findFieldAnnotatedWith(IClass<?> clazz, IClass<? extends Annotation> annotation) {
        log.atTrace().log("Finding field annotated with {} in class: {}", annotation.getName(), clazz.getName());
        for (IField f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(annotation)) {
                return Optional.of(f);
            }
            if (Fields.isNotPrimitiveOrInternal(f.getType()) && !clazz.equals(f.getType())) {
                Optional<IField> found = findFieldAnnotatedWith(f.getType(), annotation);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        if (clazz.getSuperclass() != null) {
            return findFieldAnnotatedWith(clazz.getSuperclass(), annotation);
        }
        return Optional.empty();
    }

    List<String> findFieldAddressesWithAnnotation(IClass<?> clazz, IClass<? extends Annotation> annotation,
            boolean linked) {
        log.atTrace().log("Finding field addresses with annotation {} in class: {}", annotation.getName(), clazz.getName());
        List<String> addresses = new ArrayList<>();
        findFieldAddressesRecursively(addresses, clazz, annotation, linked);
        return addresses;
    }

    private void findFieldAddressesRecursively(List<String> addresses, IClass<?> clazz,
            IClass<? extends Annotation> annotation, boolean linked) {
        for (IField field : clazz.getDeclaredFields()) {
            boolean found = false;
            if (field.isAnnotationPresent(annotation)) {
                addresses.add(field.getName());
                found = true;
            }
            if (((found && linked) || !linked)
                    && Fields.isNotPrimitiveOrInternal(field.getType())
                    && !clazz.equals(field.getType())) {
                findFieldAddressesRecursively(addresses, field.getType(), annotation, linked);
            }
            if (((found && linked) || !linked)
                    && Fields.isArrayOrMapOrCollectionField(field, provider)) {
                int i = 0;
                IClass<?> genericType = Fields.getGenericType(field.getType(), i, provider);
                while (genericType != null) {
                    findFieldAddressesRecursively(addresses, genericType, annotation, linked);
                    i++;
                    genericType = Fields.getGenericType(field.getType(), i, provider);
                }
            }
        }
        if (clazz.getSuperclass() != null) {
            findFieldAddressesRecursively(addresses, clazz.getSuperclass(), annotation, linked);
        }
    }

    Object getFieldValue(Object object, String fieldName) throws ReflectionException {
        return getFieldValue(object, fieldName, false);
    }

    Object getFieldValue(Object object, String fieldName, boolean force) throws ReflectionException {
        IClass<?> objectClass = provider.getClass(object.getClass());
        ResolvedField resolved = FieldResolver.fieldByFieldName(objectClass, provider, fieldName);
        var accessor = new FieldAccessor<>(resolved, force);
        IFieldValue<?> result = accessor.getValue(object);
        if (result.hasException()) {
            throw new ReflectionException(
                    "Cannot get field " + fieldName + " of object " + object.getClass().getName(), result.getException());
        }
        return result.first();
    }

    Object getFieldValue(Object object, IField field) throws ReflectionException {
        return getFieldValue(object, field, false);
    }

    Object getFieldValue(Object object, IField field, boolean force) throws ReflectionException {
        IClass<?> objectClass = provider.getClass(object.getClass());
        ResolvedField resolved = FieldResolver.fieldByField(objectClass, provider, field);
        var accessor = new FieldAccessor<>(resolved, force);
        IFieldValue<?> result = accessor.getValue(object);
        if (result.hasException()) {
            throw new ReflectionException(
                    "Cannot get field " + field.getName() + " of object " + object.getClass().getName(), result.getException());
        }
        return result.first();
    }

    Object getFieldValue(Object object, ObjectAddress address) throws ReflectionException {
        return getFieldValue(object, address, false);
    }

    Object getFieldValue(Object object, ObjectAddress address, boolean force) throws ReflectionException {
        IClass<?> objectClass = provider.getClass(object.getClass());
        ResolvedField resolved = FieldResolver.fieldByAddress(objectClass, provider, address);
        var accessor = new FieldAccessor<>(resolved, force);
        IFieldValue<?> result = accessor.getValue(object);
        if (result.hasException()) {
            throw new ReflectionException(
                    "Cannot get field at address " + address + " of object " + object.getClass().getName(), result.getException());
        }
        return result.first();
    }

    void setFieldValue(Object object, ObjectAddress address, Object value) throws ReflectionException {
        setFieldValue(object, address, value, false);
    }

    void setFieldValue(Object object, ObjectAddress address, Object value, boolean force) throws ReflectionException {
        IClass<?> objectClass = provider.getClass(object.getClass());
        ResolvedField resolved = FieldResolver.fieldByAddress(objectClass, provider, address);
        var accessor = new FieldAccessor<>(resolved, force);
        accessor.setValue(object, singleValue(value, resolved));
    }

    void setFieldValue(Object object, String fieldName, Object value) throws ReflectionException {
        setFieldValue(object, fieldName, value, false);
    }

    void setFieldValue(Object object, String fieldName, Object value, boolean force) throws ReflectionException {
        IClass<?> objectClass = provider.getClass(object.getClass());
        ResolvedField resolved = FieldResolver.fieldByFieldName(objectClass, provider, fieldName);
        var accessor = new FieldAccessor<>(resolved, force);
        accessor.setValue(object, singleValue(value, resolved));
    }

    void setFieldValue(Object object, IField field, Object value) throws ReflectionException {
        setFieldValue(object, field, value, false);
    }

    void setFieldValue(Object object, IField field, Object value, boolean force) throws ReflectionException {
        if (field == null) {
            throw new ReflectionException("Cannot set null field of object " + object.getClass().getName());
        }
        IClass<?> objectClass = provider.getClass(object.getClass());
        ResolvedField resolved = FieldResolver.fieldByField(objectClass, provider, field);
        var accessor = new FieldAccessor<>(resolved, force);
        accessor.setValue(object, singleValue(value, resolved));
    }

    @SuppressWarnings("unchecked")
    private static <T> SingleFieldValue<T> singleValue(Object value, ResolvedField resolved) {
        return SingleFieldValue.of((T) value, (IClass<T>) resolved.fieldType());
    }

    Optional<ObjectAddress> resolveFieldAddress(String fieldName, IClass<?> entityClass) throws ReflectionException {
        try {
            ResolvedField resolved = FieldResolver.fieldByFieldName(entityClass, provider, fieldName);
            return Optional.ofNullable(resolved.address());
        } catch (ReflectionException e) {
            return Optional.empty();
        }
    }

    Optional<ObjectAddress> resolveFieldAddress(String fieldName, IClass<?> entityClass, IClass<?> fieldType)
            throws ReflectionException {
        try {
            ResolvedField resolved = FieldResolver.fieldByFieldName(entityClass, provider, fieldName, fieldType);
            return Optional.ofNullable(resolved.address());
        } catch (ReflectionException e) {
            return Optional.empty();
        }
    }

    Optional<ObjectAddress> resolveFieldAddress(ObjectAddress address, IClass<?> entityClass) throws ReflectionException {
        try {
            ResolvedField resolved = FieldResolver.fieldByAddress(entityClass, provider, address);
            return Optional.ofNullable(resolved.address());
        } catch (ReflectionException e) {
            return Optional.empty();
        }
    }
}
