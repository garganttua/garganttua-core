package com.garganttua.core.injection.context.resolver;

import java.lang.reflect.AnnotatedElement;
import java.util.Objects;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Fixed;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.FixedObjectSupplierBuilder;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FixedElementResolver implements IElementResolver {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Resolved resolve(Class<?> elementType,
            AnnotatedElement element) throws DiException {
        Objects.requireNonNull(element, "Element cannot be null");
        Objects.requireNonNull(elementType, "ElementType cannot be null");

        if (!elementType.isPrimitive()){
            log.atWarn().log(
                    "Cannot use @Fixed annotation on not primitive element " + elementType.getSimpleName());
            return Resolved.notResolved(elementType, element);
        }

        Fixed fixedAnnotation = element.getAnnotation(Fixed.class);

        IObjectSupplierBuilder<?, IObjectSupplier<?>> builder = new FixedObjectSupplierBuilder(getFixedValue(fixedAnnotation, elementType));

        return new Resolved(true, elementType, builder, IInjectableElementResolver.isNullable(element));
    }

    @SuppressWarnings("unchecked")
    public static <T> T getFixedValue(Fixed annotation, Class<T> targetType) throws DiException {
        if (annotation == null || targetType == null)
            return null;

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
            throw new DiException("Unsupported type for @Fixed: " + targetType.getName());
        }

        return (T) value;
    }

}
