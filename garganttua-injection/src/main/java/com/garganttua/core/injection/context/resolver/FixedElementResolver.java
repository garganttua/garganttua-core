package com.garganttua.core.injection.context.resolver;

import java.lang.reflect.AnnotatedElement;
import java.util.Objects;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Fixed;
import com.garganttua.core.injection.annotations.Resolver;
import com.garganttua.core.reflection.fields.Fields;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Resolver(annotations={Fixed.class})
public class FixedElementResolver implements IElementResolver {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Resolved resolve(Class<?> elementType, AnnotatedElement element) throws DiException {
        log.atTrace().log("Entering resolve with elementType: {} and element: {}", elementType, element);

        Objects.requireNonNull(element, "Element cannot be null");
        log.atDebug().log("Element is not null: {}", element);

        Objects.requireNonNull(elementType, "ElementType cannot be null");
        log.atDebug().log("ElementType is not null: {}", elementType);

        if (Fields.isNotPrimitive(elementType)) {
            log.atWarn().log("Cannot use @Fixed annotation on non-primitive element: {}", elementType.getSimpleName());
            Resolved notResolved = Resolved.notResolved(elementType, element);
            log.atTrace().log("Exiting resolve with Resolved: {}", notResolved);
            return notResolved;
        }

        Fixed fixedAnnotation = element.getAnnotation(Fixed.class);
        log.atDebug().log("Retrieved @Fixed annotation: {}", fixedAnnotation);

        Object fixedValue = getFixedValue(fixedAnnotation, elementType);
        log.atInfo().log("Computed fixed value {} for elementType: {}", fixedValue, elementType.getSimpleName());

        ISupplierBuilder<?, ISupplier<?>> builder = new FixedSupplierBuilder(fixedValue);
        log.atInfo().log("Created FixedSupplierBuilder for elementType: {}", elementType.getSimpleName());

        Resolved resolved = new Resolved(true, elementType, builder, IInjectableElementResolver.isNullable(element));
        log.atTrace().log("Exiting resolve with Resolved: {}", resolved);

        return resolved;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getFixedValue(Fixed annotation, Class<T> targetType) throws DiException {
        log.atTrace().log("Entering getFixedValue with annotation: {} and targetType: {}", annotation, targetType);

        if (annotation == null || targetType == null) {
            log.atDebug().log("Annotation or targetType is null, returning null");
            return null;
        }

        Object value;

        if (targetType == int.class || targetType == Integer.class) {
            value = annotation.valueInt();
        } else if (targetType == double.class || targetType == Double.class) {
            value = annotation.valueDouble();
        } else if (targetType == float.class || targetType == Float.class) {
            value = annotation.valueFloat();
        } else if (targetType == long.class || targetType == Long.class) {
            value = annotation.valueLong();
        } else if (targetType == String.class) {
            value = annotation.valueString();
        } else if (targetType == byte.class || targetType == Byte.class) {
            value = annotation.valueByte();
        } else if (targetType == short.class || targetType == Short.class) {
            value = annotation.valueShort();
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            value = annotation.valueBoolean();
        } else if (targetType == char.class || targetType == Character.class) {
            value = annotation.valueChar();
        } else {
            log.atError().log("Unsupported type for @Fixed: {}", targetType.getName());
            throw new DiException("Unsupported type for @Fixed: " + targetType.getName());
        }

        log.atTrace().log("Exiting getFixedValue with value: {}", value);
        return (T) value;
    }
}
