package com.garganttua.core.injection.context.resolver;

import java.util.Objects;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Fixed;
import com.garganttua.core.injection.annotations.Resolver;
import com.garganttua.core.reflection.IAnnotatedElement;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.fields.Fields;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * {@link IElementResolver} for elements annotated with {@link Fixed}, supplying a constant
 * primitive (or boxed primitive) value declared on the annotation.
 */
@Resolver(annotations={Fixed.class})
public class FixedElementResolver implements IElementResolver {
    /** Creates a resolver for {@link Fixed}-annotated elements. */
    public FixedElementResolver() {
    }

    private static final Logger log = Logger.getLogger(FixedElementResolver.class);

    /**
     * Resolves the element to a supplier yielding the constant value carried by its {@link Fixed}
     * annotation. Non-primitive element types are rejected and reported as not resolved.
     *
     * @return a resolved supplier wrapping the fixed value, or a not-resolved marker for
     *         non-primitive types
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Resolved resolve(IClass<?> elementType, IAnnotatedElement element) throws DiException {
        log.trace("Entering resolve with elementType: {} and element: {}", elementType, element);

        Objects.requireNonNull(element, "Element cannot be null");
        log.debug("Element is not null: {}", element);

        Objects.requireNonNull(elementType, "ElementType cannot be null");
        log.debug("ElementType is not null: {}", elementType);

        if (Fields.isNotPrimitive(elementType)) {
            log.warn("Cannot use @Fixed annotation on non-primitive element: {}", elementType.getSimpleName());
            Resolved notResolved = Resolved.notResolved(elementType, element);
            log.trace("Exiting resolve with Resolved: {}", notResolved);
            return notResolved;
        }

        Fixed fixedAnnotation = element.getAnnotation(IClass.getClass(Fixed.class));

        log.debug("Retrieved @Fixed annotation: {}", fixedAnnotation);

        Object fixedValue = getFixedValue(fixedAnnotation, elementType);
        log.debug("Computed fixed value {} for elementType: {}", fixedValue, elementType.getSimpleName());

        ISupplierBuilder<?, ISupplier<?>> builder = new FixedSupplierBuilder(fixedValue, elementType);
        log.debug("Created FixedSupplierBuilder for elementType: {}", elementType.getSimpleName());

        Resolved resolved = new Resolved(true, elementType, builder, IInjectableElementResolver.isNullable(element));
        log.trace("Exiting resolve with Resolved: {}", resolved);

        return resolved;
    }

    /**
     * Extracts the constant value held by a {@link Fixed} annotation, coerced to the given target type.
     *
     * @param annotation the {@link Fixed} annotation, may be {@code null}
     * @param targetType the primitive or boxed target type, may be {@code null}
     * @return the fixed value, or {@code null} if either argument is {@code null}
     * @throws DiException if {@code targetType} is not a supported primitive or boxed type
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFixedValue(Fixed annotation, IClass<T> targetType) throws DiException {
        log.trace("Entering getFixedValue with annotation: {} and targetType: {}", annotation, targetType);

        if (annotation == null || targetType == null) {
            log.debug("Annotation or targetType is null, returning null");
            return null;
        }
        Object value;
        String typeName = targetType.getName();

        if (typeName.equals("int") || typeName.equals(Integer.class.getName())) {
            value = annotation.valueInt();
        } else if (typeName.equals("double") || typeName.equals(Double.class.getName())) {
            value = annotation.valueDouble();
        } else if (typeName.equals("float") || typeName.equals(Float.class.getName())) {
            value = annotation.valueFloat();
        } else if (typeName.equals("long") || typeName.equals(Long.class.getName())) {
            value = annotation.valueLong();
        } else if (typeName.equals(String.class.getName())) {
            value = annotation.valueString();
        } else if (typeName.equals("byte") || typeName.equals(Byte.class.getName())) {
            value = annotation.valueByte();
        } else if (typeName.equals("short") || typeName.equals(Short.class.getName())) {
            value = annotation.valueShort();
        } else if (typeName.equals("boolean") || typeName.equals(Boolean.class.getName())) {
            value = annotation.valueBoolean();
        } else if (typeName.equals("char") || typeName.equals(Character.class.getName())) {
            value = annotation.valueChar();
        } else {
            log.error("Unsupported type for @Fixed: {}", targetType.getName());
            throw new DiException("Unsupported type for @Fixed: " + targetType.getName());
        }

        log.trace("Exiting getFixedValue with value: {}", value);
        return (T) value;
    }
}
