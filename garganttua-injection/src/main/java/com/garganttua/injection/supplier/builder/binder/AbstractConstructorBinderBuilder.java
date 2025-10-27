package com.garganttua.injection.supplier.builder.binder;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.garganttua.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.dsl.DslException;
import com.garganttua.injection.spec.supplier.IObjectSupplier;
import com.garganttua.injection.spec.supplier.binder.IConstructorBinder;
import com.garganttua.injection.spec.supplier.builder.binder.IConstructorBinderBuilder;
import com.garganttua.injection.spec.supplier.builder.supplier.IObjectSupplierBuilder;
import com.garganttua.injection.supplier.binder.ConstructorBinder;
import com.garganttua.injection.supplier.builder.supplier.FixedObjectSupplierBuilder;
import com.garganttua.injection.supplier.builder.supplier.NullableEnforcingObjectSupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractConstructorBinderBuilder<Constructed, Builder extends IConstructorBinderBuilder<Constructed, Builder, Link, IConstructorBinder<Constructed>>, Link>
        extends AbstractAutomaticLinkedBuilder<Builder, Link, IConstructorBinder<Constructed>>
        implements IConstructorBinderBuilder<Constructed, Builder, Link, IConstructorBinder<Constructed>> {

    private final Class<Constructed> objectClass;
    private final List<IObjectSupplierBuilder<?, ?>> parameters;
    private final List<Boolean> parameterNullableAllowed;

    protected AbstractConstructorBinderBuilder(Link Link, Class<Constructed> objectClass) {
        super(Link);
        this.objectClass = Objects.requireNonNull(objectClass, "Object class cannot be null");
        this.parameters = new ArrayList<>();
        this.parameterNullableAllowed = new ArrayList<>();
    }

    protected abstract Builder getBuilder();

    @Override
    public Builder withParam(int i, Object parameter) throws DslException {
        ensureCapacity(i);
        this.parameters.set(i, new FixedObjectSupplierBuilder<>(parameter));
        this.parameterNullableAllowed.set(i, false);
        return this.getBuilder();
    }

    @Override
    public Builder withParam(int i, IObjectSupplierBuilder<?, ?> supplier) throws DslException {
        ensureCapacity(i);
        this.parameters.set(i, supplier);
        this.parameterNullableAllowed.set(i, false);
        return this.getBuilder();
    }

    @Override
    public Builder withParam(int i, Object parameter, boolean acceptNullable) throws DslException {
        ensureCapacity(i);
        this.parameters.set(i, new FixedObjectSupplierBuilder<>(parameter));
        this.parameterNullableAllowed.set(i, acceptNullable);
        return this.getBuilder();
    }

    @Override
    public Builder withParam(int i, IObjectSupplierBuilder<?, ?> supplier, boolean acceptNullable) throws DslException {
        ensureCapacity(i);
        this.parameters.set(i, supplier);
        this.parameterNullableAllowed.set(i, acceptNullable);
        return this.getBuilder();
    }

    @Override
    public Builder withParam(Object parameter) throws DslException {
        return withParam(parameter, false);
    }

    @Override
    public Builder withParam(Object parameter, boolean acceptNullable) throws DslException {
        int idx = parameters.size();
        return withParam(idx, parameter, acceptNullable);
    }

    @Override
    public Builder withParam(IObjectSupplierBuilder<?, ?> supplier) throws DslException {
        return withParam(supplier, false);
    }

    @Override
    public Builder withParam(IObjectSupplierBuilder<?, ?> supplier, boolean acceptNullable) throws DslException {
        int idx = parameters.size();
        return withParam(idx, supplier, acceptNullable);
    }

    @Override
    public Builder withParam(String paramName, Object parameter) throws DslException {
        throw new DslException("Parameter name-based binding is not sLinkported for constructors");
    }

    @Override
    public Builder withParam(String paramName, IObjectSupplierBuilder<?, ?> supplier) throws DslException {
        throw new DslException("Parameter name-based binding is not sLinkported for constructors");
    }

    @Override
    public Builder withParam(String paramName, Object parameter, boolean acceptNullable) throws DslException {
        throw new DslException("Parameter name-based binding is not sLinkported for constructors");
    }

    @Override
    public Builder withParam(String paramName, IObjectSupplierBuilder<?, ?> supplier, boolean acceptNullable)
            throws DslException {
        throw new DslException("Parameter name-based binding is not sLinkported for constructors");
    }

    private void ensureCapacity(int index) {
        while (this.parameters.size() <= index) {
            this.parameters.add(null);
            this.parameterNullableAllowed.add(false);
        }
    }

    @Override
    public IConstructorBinder<Constructed> doBuild() throws DslException {
        log.atDebug().log("[ConstructorBinderBuilder] Building constructor binder for {}", objectClass.getName());

        if (parameters == null || parameters.isEmpty()) {
            log.atTrace().log("[ConstructorBinderBuilder] No parameters provided, searching for default constructor");
            try {
                Constructor<Constructed> defaultCtor = objectClass.getDeclaredConstructor();
                defaultCtor.setAccessible(true);
                return new ConstructorBinder<Constructed>(objectClass, defaultCtor, Collections.emptyList());
            } catch (NoSuchMethodException e) {
                throw new DslException(
                        "No parameters provided and no default constructor found for " + objectClass.getName(), e);
            }
        }

        List<IObjectSupplier<?>> builtsuppliers = new ArrayList<>();
        for (int i = 0; i < parameters.size(); i++) {
            IObjectSupplierBuilder<?, ?> builder = parameters.get(i);
            if (builder == null) {
                log.atWarn().log("[ConstructorBinderBuilder] Parameter {} not configured", i);
                throw new DslException("Parameter " + i + " not configured");
            }
            IObjectSupplier<?> nullable = new NullableEnforcingObjectSupplier<>(
                    builder.build(),
                    Boolean.TRUE.equals(parameterNullableAllowed.get(i)),
                    i,
                    objectClass.getName());

            builtsuppliers.add(nullable);
        }

        Class<?>[] paramTypes = builtsuppliers.stream()
                .map(IObjectSupplier::getObjectClass)
                .toArray(Class<?>[]::new);

        Constructor<Constructed> matchedConstructor = findMatchingConstructor(paramTypes);
        if (matchedConstructor == null) {
            String msg = String.format("No matching constructor found for class %s with parameter types %s",
                    objectClass.getName(), formatTypes(paramTypes));
            log.atError().log("[ConstructorBinderBuilder] {}", msg);
            throw new DslException(msg);
        }

        log.atInfo().log("[ConstructorBinderBuilder] Matched constructor {}({})",
                objectClass.getSimpleName(), formatTypes(paramTypes));

        return new ConstructorBinder<Constructed>(objectClass, matchedConstructor, builtsuppliers);
    }

    @SuppressWarnings("unchecked")
    private Constructor<Constructed> findMatchingConstructor(Class<?>[] paramTypes) {
        Constructor<Constructed>[] constructors = (Constructor<Constructed>[]) objectClass.getDeclaredConstructors();

        for (Constructor<Constructed> ctor : constructors) {
            Class<?>[] ctorParams = ctor.getParameterTypes();
            if (ctorParams.length != paramTypes.length)
                continue;

            boolean match = true;
            for (int i = 0; i < ctorParams.length; i++) {
                if (!isCompatible(ctorParams[i], paramTypes[i])) {
                    match = false;
                    break;
                }
            }
            if (match)
                return ctor;
        }
        return null;
    }

    private boolean isCompatible(Class<?> expected, Class<?> actual) {
        if (actual == null)
            return true; // might be nullable, checked later
        return expected.isAssignableFrom(actual)
                || (expected.isPrimitive() && getWrapperType(expected).equals(actual))
                || (actual.isPrimitive() && getWrapperType(actual).equals(expected));
    }

    private String formatTypes(Class<?>[] types) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            sb.append(types[i] == null ? "null" : types[i].getSimpleName());
            if (i < types.length - 1)
                sb.append(", ");
        }
        return sb.toString();
    }

    private static Class<?> getWrapperType(Class<?> primitive) {
        if (primitive == int.class)
            return Integer.class;
        if (primitive == long.class)
            return Long.class;
        if (primitive == boolean.class)
            return Boolean.class;
        if (primitive == double.class)
            return Double.class;
        if (primitive == float.class)
            return Float.class;
        if (primitive == char.class)
            return Character.class;
        if (primitive == byte.class)
            return Byte.class;
        if (primitive == short.class)
            return Short.class;
        return primitive;
    }

}
