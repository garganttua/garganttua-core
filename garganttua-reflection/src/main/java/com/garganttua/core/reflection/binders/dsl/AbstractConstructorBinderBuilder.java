package com.garganttua.core.reflection.binders.dsl;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.binders.ConstructorBinder;
import com.garganttua.core.reflection.binders.ContextualConstructorBinder;
import com.garganttua.core.reflection.binders.IConstructorBinder;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.NullableContextualSupplier;
import com.garganttua.core.supply.NullableSupplier;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractConstructorBinderBuilder<Constructed, Builder extends IConstructorBinderBuilder<Constructed, Builder, Link, IConstructorBinder<Constructed>>, Link>
        extends AbstractAutomaticLinkedBuilder<Builder, Link, IConstructorBinder<Constructed>>
        implements IConstructorBinderBuilder<Constructed, Builder, Link, IConstructorBinder<Constructed>> {

    private final Class<Constructed> objectClass;
    private final List<ISupplierBuilder<?, ? extends ISupplier<?>>> parameters;
    private final List<Boolean> parameterNullableAllowed;

    protected AbstractConstructorBinderBuilder(Link Link, Class<Constructed> objectClass) {
        super(Link);
        this.objectClass = Objects.requireNonNull(objectClass, "Object class cannot be null");
        this.parameters = new ArrayList<>();
        this.parameterNullableAllowed = new ArrayList<>();
    }

    private boolean buildContextual() {
        return this.parameters.stream().filter(param -> param.isContextual()).findFirst().isPresent();
    }

    @Override
    public Builder withParam(int i, Object parameter) throws DslException {
        ensureCapacity(i);
        this.parameters.set(i, createFixedObjectSupplierBuilder(parameter));
        this.parameterNullableAllowed.set(i, false);
        return (Builder) this;
    }

    @Override
    public Builder withParam(int i, ISupplierBuilder<?, ? extends ISupplier<?>> supplier) throws DslException {
        ensureCapacity(i);
        this.parameters.set(i, supplier);
        this.parameterNullableAllowed.set(i, false);
        return (Builder) this;
    }

    @Override
    public Builder withParam(int i, Object parameter, boolean acceptNullable) throws DslException {
        ensureCapacity(i);
        this.parameters.set(i, createFixedObjectSupplierBuilder(parameter));
        this.parameterNullableAllowed.set(i, acceptNullable);
        return (Builder) this;
    }

    @Override
    public Builder withParam(int i, ISupplierBuilder<?, ? extends ISupplier<?>> supplier, boolean acceptNullable) throws DslException {
        ensureCapacity(i);
        this.parameters.set(i, supplier);
        this.parameterNullableAllowed.set(i, acceptNullable);
        return (Builder) this;
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
    public Builder withParam(ISupplierBuilder<?, ? extends ISupplier<?>> supplier) throws DslException {
        return withParam(supplier, false);
    }

    @Override
    public Builder withParam(ISupplierBuilder<?, ? extends ISupplier<?>> supplier, boolean acceptNullable) throws DslException {
        int idx = parameters.size();
        return withParam(idx, supplier, acceptNullable);
    }

    @Override
    public Builder withParam(String paramName, Object parameter) throws DslException {
        throw new DslException("Parameter name-based binding is not supported for constructors");
    }

    @Override
    public Builder withParam(String paramName, ISupplierBuilder<?, ? extends ISupplier<?>> supplier) throws DslException {
        throw new DslException("Parameter name-based binding is not supported for constructors");
    }

    @Override
    public Builder withParam(String paramName, Object parameter, boolean acceptNullable) throws DslException {
        throw new DslException("Parameter name-based binding is not supported for constructors");
    }

    @Override
    public Builder withParam(String paramName, ISupplierBuilder<?, ? extends ISupplier<?>> supplier, boolean acceptNullable)
            throws DslException {
        throw new DslException("Parameter name-based binding is not supported for constructors");
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
                return new ConstructorBinder<Constructed>(objectClass, defaultCtor, Collections.emptyList());
            } catch (NoSuchMethodException e) {
                throw new DslException(
                        "No parameters provided and no default constructor found for " + objectClass.getName(), e);
            }
        }

        List<ISupplier<?>> builtsuppliers = new ArrayList<>();
        for (int i = 0; i < parameters.size(); i++) {
            ISupplierBuilder<?, ?> builder = parameters.get(i);
            if (builder == null) {
                log.atWarn().log("[ConstructorBinderBuilder] Parameter {} not configured", i);
                throw new DslException("Parameter " + i + " not configured");
            }

            builtsuppliers.add(createNullableObjectSupplier(builder,
                    Boolean.TRUE.equals(parameterNullableAllowed.get(i))));
        }

        Constructor<Constructed> matchedConstructor = findConstructor();
        if (matchedConstructor == null) {
            String msg = String.format("No matching constructor found for class %s with parameter types %s",
                    objectClass.getName(), formatTypes(this.getParameterTypes()));
            log.atError().log("[ConstructorBinderBuilder] {}", msg);
            throw new DslException(msg);
        }

        log.atDebug().log("[ConstructorBinderBuilder] Matched constructor {}({})",
                objectClass.getSimpleName(), formatTypes(this.getParameterTypes()));

        if (this.buildContextual())
            return new ContextualConstructorBinder<Constructed>(objectClass, matchedConstructor, builtsuppliers);
        return new ConstructorBinder<Constructed>(objectClass, matchedConstructor, builtsuppliers);
    }

    protected Class<?>[] getParameterTypes() {
        return this.parameters.stream().map(ISupplierBuilder::getSuppliedType).toArray(Class<?>[]::new);
    }

    protected Constructor<Constructed> findConstructor() {
        Class<?>[] paramTypes = this.getParameterTypes();
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

    protected static ISupplier<?> createNullableObjectSupplier(ISupplierBuilder<?,?> builder,
            boolean allowNullable) throws DslException {
        if (builder.isContextual()) {
            IContextualSupplier<?, ?> contextualSupplier = (IContextualSupplier<?, ?>) builder
                    .build();
            return new NullableContextualSupplier<>(contextualSupplier, allowNullable);
        }
        return new NullableSupplier<>(builder.build(), allowNullable);
    }

    protected static ISupplierBuilder<?, ?> createFixedObjectSupplierBuilder(Object objectToSupply) {
        return new FixedSupplierBuilder<>(objectToSupply);
    }
}
