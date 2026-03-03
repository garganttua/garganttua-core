package com.garganttua.core.reflection.binders.dsl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.dependency.AbstractAutomaticLinkedDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencyPhase;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.ConstructorBinder;
import com.garganttua.core.reflection.binders.ContextualConstructorBinder;
import com.garganttua.core.reflection.binders.IConstructorBinder;
import com.garganttua.core.reflection.constructors.ConstructorResolver;
import com.garganttua.core.reflection.constructors.ResolvedConstructor;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.NullableContextualSupplier;
import com.garganttua.core.supply.NullableSupplier;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractConstructorBinderBuilder<Constructed, Builder extends IConstructorBinderBuilder<Constructed, Builder, Link, IConstructorBinder<Constructed>>, Link>
        extends AbstractAutomaticLinkedDependentBuilder<Builder, Link, IConstructorBinder<Constructed>>
        implements IConstructorBinderBuilder<Constructed, Builder, Link, IConstructorBinder<Constructed>> {

    private final IClass<Constructed> objectClass;

    // Parameter entries: either a raw Object or an ISupplierBuilder
    private final List<Object> parameterEntries;
    private final List<Boolean> parameterNullableAllowed;
    private final Set<Integer> rawParamIndices = new HashSet<>();

    private IReflection reflection;

    protected AbstractConstructorBinderBuilder(Link link, IClass<Constructed> objectClass,
            Set<DependencySpec> dependencies) {
        super(
                link,
                Stream.concat(
                        dependencies.stream(),
                        Stream.of(DependencySpec.use(IReflectionBuilder.class, DependencyPhase.BUILD)))
                        .collect(Collectors.toUnmodifiableSet()));
        this.objectClass = Objects.requireNonNull(objectClass, "Object class cannot be null");
        this.parameterEntries = new ArrayList<>();
        this.parameterNullableAllowed = new ArrayList<>();
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        if (dependency instanceof IReflection ref) {
            this.reflection = ref;
        }
        this.doPreBuildWithDependency_(dependency);
    }

    protected abstract void doPreBuildWithDependency_(Object dependency);

    private IReflection effectiveReflection() {
        return this.reflection != null ? this.reflection : IClass.getReflection();
    }

    @Override
    public Builder withParam(int i, Object parameter) throws DslException {
        ensureCapacity(i);
        this.parameterEntries.set(i, parameter);
        this.rawParamIndices.add(i);
        this.parameterNullableAllowed.set(i, false);
        return (Builder) this;
    }

    @Override
    public Builder withParam(int i, ISupplierBuilder<?, ? extends ISupplier<?>> supplier) throws DslException {
        ensureCapacity(i);
        this.parameterEntries.set(i, supplier);
        this.rawParamIndices.remove(i);
        this.parameterNullableAllowed.set(i, false);
        return (Builder) this;
    }

    @Override
    public Builder withParam(int i, Object parameter, boolean acceptNullable) throws DslException {
        ensureCapacity(i);
        this.parameterEntries.set(i, parameter);
        this.rawParamIndices.add(i);
        this.parameterNullableAllowed.set(i, acceptNullable);
        return (Builder) this;
    }
    
    @Override
    public Builder withParam(int i, ISupplierBuilder<?, ? extends ISupplier<?>> supplier, boolean acceptNullable)
            throws DslException {
        ensureCapacity(i);
        this.parameterEntries.set(i, supplier);
        this.rawParamIndices.remove(i);
        this.parameterNullableAllowed.set(i, acceptNullable);
        return (Builder) this;
    }

    @Override
    public Builder withParam(Object parameter) throws DslException {
        return withParam(parameter, false);
    }

    @Override
    public Builder withParam(Object parameter, boolean acceptNullable) throws DslException {
        int idx = parameterEntries.size();
        return withParam(idx, parameter, acceptNullable);
    }

    @Override
    public Builder withParam(ISupplierBuilder<?, ? extends ISupplier<?>> supplier) throws DslException {
        return withParam(supplier, false);
    }

    @Override
    public Builder withParam(ISupplierBuilder<?, ? extends ISupplier<?>> supplier, boolean acceptNullable)
            throws DslException {
        int idx = parameterEntries.size();
        return withParam(idx, supplier, acceptNullable);
    }

    @Override
    public Builder withParam(String paramName, Object parameter) throws DslException {
        throw new DslException("Parameter name-based binding is not supported for constructors");
    }

    @Override
    public Builder withParam(String paramName, ISupplierBuilder<?, ? extends ISupplier<?>> supplier)
            throws DslException {
        throw new DslException("Parameter name-based binding is not supported for constructors");
    }

    @Override
    public Builder withParam(String paramName, Object parameter, boolean acceptNullable) throws DslException {
        throw new DslException("Parameter name-based binding is not supported for constructors");
    }

    @Override
    public Builder withParam(String paramName, ISupplierBuilder<?, ? extends ISupplier<?>> supplier,
            boolean acceptNullable)
            throws DslException {
        throw new DslException("Parameter name-based binding is not supported for constructors");
    }

    private void ensureCapacity(int index) {
        while (this.parameterEntries.size() <= index) {
            this.parameterEntries.add(null);
            this.parameterNullableAllowed.add(false);
        }
    }

    @Override
    public IConstructorBinder<Constructed> doBuild() throws DslException {
        IReflection reflection = effectiveReflection();
        log.atDebug().log("[ConstructorBinderBuilder] Building constructor binder for {}", objectClass.getName());

        try {
            if (parameterEntries.isEmpty()) {
                log.atTrace().log("[ConstructorBinderBuilder] No parameters provided, searching for default constructor");
                ResolvedConstructor<Constructed> resolved = ConstructorResolver.defaultConstructor(
                        objectClass, reflection);
                return new ConstructorBinder<>(objectClass, resolved.constructor(), Collections.emptyList());
            }

            // Resolve raw parameters to suppliers using reflection
            List<ISupplierBuilder<?, ?>> resolvedParams = resolveParameters();

            // Validate all params are configured
            for (int i = 0; i < resolvedParams.size(); i++) {
                if (resolvedParams.get(i) == null) {
                    log.atWarn().log("[ConstructorBinderBuilder] Parameter {} not configured", i);
                    throw new DslException("Parameter " + i + " not configured");
                }
            }

            // Build suppliers
            List<ISupplier<?>> builtSuppliers = new ArrayList<>();
            for (int i = 0; i < resolvedParams.size(); i++) {
                builtSuppliers.add(createNullableObjectSupplier(resolvedParams.get(i),
                        Boolean.TRUE.equals(parameterNullableAllowed.get(i))));
            }

            // Get parameter types
            IClass<?>[] paramTypes = resolvedParams.stream()
                    .map(ISupplierBuilder::getSuppliedClass)
                    .toArray(IClass<?>[]::new);

            // Find constructor via ConstructorResolver
            ResolvedConstructor<Constructed> resolved = ConstructorResolver.constructorByParameterTypes(
                    objectClass, reflection, paramTypes);

            log.atDebug().log("[ConstructorBinderBuilder] Matched constructor {}({})",
                    objectClass.getSimpleName(), formatTypes(paramTypes));

            IConstructor<Constructed> matchedConstructor = resolved.constructor();
            boolean contextual = resolvedParams.stream().anyMatch(ISupplierBuilder::isContextual);
            if (contextual)
                return new ContextualConstructorBinder<>(objectClass, matchedConstructor, builtSuppliers);

            return new ConstructorBinder<>(objectClass, matchedConstructor, builtSuppliers);

        } catch (ReflectionException e) {
            throw new DslException(e.getMessage(), e);
        }
    }

    private List<ISupplierBuilder<?, ?>> resolveParameters() {
        List<ISupplierBuilder<?, ?>> resolved = new ArrayList<>(parameterEntries.size());
        for (int i = 0; i < parameterEntries.size(); i++) {
            Object entry = parameterEntries.get(i);
            if (entry == null) {
                resolved.add(null);
            } else if (rawParamIndices.contains(i)) {
                IClass<?> clz = effectiveReflection().getClass(entry.getClass());
                resolved.add(new FixedSupplierBuilder(entry, clz));
            } else {
                resolved.add((ISupplierBuilder<?, ?>) entry);
            }
        }
        return resolved;
    }

    private String formatTypes(IClass<?>[] types) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            sb.append(types[i] == null ? "null" : types[i].getSimpleName());
            if (i < types.length - 1)
                sb.append(", ");
        }
        return sb.toString();
    }

    protected static ISupplier<?> createNullableObjectSupplier(ISupplierBuilder<?, ?> builder,
            boolean allowNullable) throws DslException {
        if (builder.isContextual()) {
            IContextualSupplier<?, ?> contextualSupplier = (IContextualSupplier<?, ?>) builder
                    .build();
            return new NullableContextualSupplier<>(contextualSupplier, allowNullable);
        }
        return new NullableSupplier<>(builder.build(), allowNullable);
    }

}
