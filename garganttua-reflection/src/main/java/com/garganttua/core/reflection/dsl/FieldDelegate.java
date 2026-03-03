package com.garganttua.core.reflection.dsl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.fields.FieldResolver;
import com.garganttua.core.reflection.fields.Fields;
import com.garganttua.core.reflection.fields.ResolvedField;

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
        IClass<?> objectClass = provider.getClass(object.getClass());
        Optional<IField> optField = findField(objectClass, fieldName);
        if (optField.isEmpty()) {
            throw new ReflectionException("Cannot get field " + fieldName + " of object " + object.getClass().getName());
        }
        return getFieldValue(object, optField.get());
    }

    Object getFieldValue(Object object, IField field) throws ReflectionException {
        field.setAccessible(true);
        try {
            return field.get(object);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new ReflectionException(
                    "Cannot get field " + field.getName() + " of object " + object.getClass().getName(), e);
        }
    }

    void setFieldValue(Object object, String fieldName, Object value) throws ReflectionException {
        IClass<?> objectClass = provider.getClass(object.getClass());
        Optional<IField> optField = findField(objectClass, fieldName);
        if (optField.isEmpty()) {
            throw new ReflectionException(
                    "Cannot set field " + fieldName + " of object " + object.getClass().getName());
        }
        setFieldValue(object, optField.get(), value);
    }

    void setFieldValue(Object object, IField field, Object value) throws ReflectionException {
        if (field == null) {
            throw new ReflectionException("Cannot set null field of object " + object.getClass().getName());
        }
        field.setAccessible(true);
        try {
            field.set(object, value);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new ReflectionException(
                    "Cannot set field " + field.getName() + " of object " + object.getClass().getName()
                            + " with value " + value, e);
        }
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
