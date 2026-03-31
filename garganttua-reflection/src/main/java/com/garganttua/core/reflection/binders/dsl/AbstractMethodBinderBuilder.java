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
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IParameter;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.binders.ContextualMethodBinder;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.reflection.binders.MethodBinder;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.methods.MethodResolver;
import com.garganttua.core.reflection.methods.ResolvedMethod;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractMethodBinderBuilder<ExecutionReturn, Builder extends IMethodBinderBuilder<ExecutionReturn, Builder, Link, Built>, Link, Built extends IMethodBinder<ExecutionReturn>>
        extends AbstractAutomaticLinkedDependentBuilder<Builder, Link, Built>
        implements IMethodBinderBuilder<ExecutionReturn, Builder, Link, Built> {

    @Setter
    private ISupplierBuilder<?, ?> supplier;

    // Parameter entries: either a raw Object or an ISupplierBuilder
    private List<Object> parameterEntries;
    private List<Boolean> parameterNullableAllowed;
    private final Set<Integer> rawParamIndices = new HashSet<>();

    private boolean collection = false;

    private ResolvedMethod resolvedMethod;

    private IReflection reflection;

    protected AbstractMethodBinderBuilder(Link up, ISupplierBuilder<?, ?> supplier, Set<DependencySpec> dependencies)
            throws DslException {
        this(up, supplier, false, dependencies);
    }

    protected AbstractMethodBinderBuilder(Link up, ISupplierBuilder<?, ?> supplier, boolean collection,
            Set<DependencySpec> dependencies)
            throws DslException {
        super(
                up,
                Stream.concat(
                        dependencies.stream(),
                        Stream.of(DependencySpec.use(IClass.getClass(IReflectionBuilder.class), DependencyPhase.BUILD)))
                        .collect(Collectors.toUnmodifiableSet()));
        log.atTrace().log("[MethodBinderBuilder] Creating with up={} and supplier={}", up, supplier);
        this.supplier = Objects.requireNonNull(supplier, "Supplier cannot be null");
        this.collection = collection;
    }

    protected List<Boolean> nullableParameters() {
        return this.parameterNullableAllowed;
    }

    protected void nullableParameter(int i, boolean acceptNullable) {
        this.parameterNullableAllowed.set(i, acceptNullable);
    }

    private boolean buildContextual(List<ISupplierBuilder<?, ?>> resolvedParams) {
        if (this.supplier.isContextual())
            return true;
        return resolvedParams.stream().anyMatch(param -> param.isContextual());
    }

    public String getMethodName() {
        if (this.resolvedMethod != null) {
            return this.resolvedMethod.getName();
        }
        return null;
    }

    @Override
    public ObjectAddress methodAddress() throws DslException {
        return this.resolvedMethod.address();
    }

    @Override
    public IMethod method() throws DslException {
        return this.resolvedMethod;
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        if (dependency instanceof IReflection ref) {
            this.reflection = ref;
        }
        this.doPreBuildWithDependency_(dependency);
    }

    protected abstract void doPreBuildWithDependency_(Object dependency);

    /**
     * Returns the IReflection to use. Prefers the explicitly provided one (set during build
     * phase via doPreBuildWithDependency), falls back to the global/thread-local IReflection.
     */
    private IReflection effectiveReflection() {
        return this.reflection != null ? this.reflection : IClass.getReflection();
    }

    @Override
    public Builder method(IMethod method) throws DslException {
        log.atDebug().log("[MethodBinderBuilder] Storing method {} for deferred resolution", method.getName());

        this.resolvedMethod = MethodResolver.methodByMethod(this.supplier.getSuppliedClass(), effectiveReflection(), method);

        this.initParameters();

        return (Builder) this;
    }

    @Override
    public Builder method(ObjectAddress methodAddress, IClass<ExecutionReturn> returnType, IClass<?>... parameterTypes)
            throws DslException {
        log.atDebug().log("[MethodBinderBuilder] Storing method address={} with returnType={} for deferred resolution",
                methodAddress, returnType);

        Objects.requireNonNull(methodAddress, "Method address cannot be null");

        this.resolvedMethod = MethodResolver.methodByAddress(this.supplier.getSuppliedClass(), effectiveReflection(), methodAddress,
                returnType,
                parameterTypes);

        this.initParameters();

        return (Builder) this;
    }

    @Override
    public Builder method(String methodName, IClass<ExecutionReturn> returnType, IClass<?>... parameterTypes)
            throws DslException {
        log.atDebug().log("[MethodBinderBuilder] Storing method name={} with returnType={} for deferred resolution",
                methodName, returnType);

        this.resolvedMethod = MethodResolver.methodByName(this.supplier.getSuppliedClass(), effectiveReflection(), methodName, returnType,
                parameterTypes);

        this.initParameters();

        return (Builder) this;
    }

    // --------------------------
    // withParam implementations
    // --------------------------

    @Override
    public Builder withParam(int i, Object object) throws DslException {
        return withParam(i, object, false);
    }

    @Override
    public Builder withParam(int i, ISupplierBuilder<?, ? extends ISupplier<?>> object)
            throws DslException {
        return withParam(i, object, false);
    }

    @Override
    public Builder withParam(int i, Object object, boolean acceptNullable) throws DslException {
        log.atTrace().log("[MethodBinderBuilder] Binding parameter {} with value={} (acceptNullable={})", i, object,
                acceptNullable);

        // Ensure method is set
        if (this.resolvedMethod == null) {
            throw new DslException("[MethodBinderBuilder] Method must be set before setting parameters");
        }

        // Lazy initialize parameters list if needed
        if (this.parameterEntries == null) {
            this.parameterEntries = new ArrayList<>();
            this.parameterNullableAllowed = new ArrayList<>();
        }

        // Ensure the lists are large enough
        while (this.parameterEntries.size() <= i) {
            this.parameterEntries.add(null);
            this.parameterNullableAllowed.add(Boolean.FALSE);
        }

        // Store raw object — will be resolved in doBuild() using reflection
        this.parameterEntries.set(i, object);
        this.rawParamIndices.add(i);
        this.parameterNullableAllowed.set(i, acceptNullable);

        log.atDebug().log("[MethodBinderBuilder] Parameter {} bound successfully with type {} (acceptNullable={})", i,
                object.getClass(), acceptNullable);
        return (Builder) this;
    }

    @Override
    public Builder withParam(int i, ISupplierBuilder<?, ? extends ISupplier<?>> object,
            boolean acceptNullable) throws DslException {
        log.atTrace().log("[MethodBinderBuilder] Binding parameter {} with supplier of type {} (acceptNullable={})", i,
                object == null ? "null" : object.getSuppliedClass(), acceptNullable);

        // Ensure method is set
        if (this.resolvedMethod == null) {
            throw new DslException("[MethodBinderBuilder] Method must be set before setting parameters");
        }
        Objects.requireNonNull(object, "Supplier cannot be null");

        // Lazy initialize parameters list if needed
        if (this.parameterEntries == null) {
            this.parameterEntries = new ArrayList<>();
            this.parameterNullableAllowed = new ArrayList<>();
        }

        // Ensure the lists are large enough
        while (this.parameterEntries.size() <= i) {
            this.parameterEntries.add(null);
            this.parameterNullableAllowed.add(Boolean.FALSE);
        }

        this.parameterEntries.set(i, object);
        this.rawParamIndices.remove(i);
        this.parameterNullableAllowed.set(i, acceptNullable);

        log.atDebug().log(
                "[MethodBinderBuilder] Parameter {} bound successfully with supplier type {} (acceptNullable={})", i,
                object.getSuppliedClass(), acceptNullable);
        return (Builder) this;
    }

    @Override
    public Builder withParam(String paramName, Object parameter) throws DslException {
        return withParam(paramName, parameter, false);
    }

    @Override
    public Builder withParam(String paramName, ISupplierBuilder<?, ? extends ISupplier<?>> supplier)
            throws DslException {
        return withParam(paramName, supplier, false);
    }

    @Override
    public Builder withParam(String paramName, Object parameter, boolean acceptNullable) throws DslException {
        Objects.requireNonNull(paramName, "paramName cannot be null");

        // Ensure method is set
        if (this.resolvedMethod == null) {
            throw new DslException("[MethodBinderBuilder] Method must be set before setting parameters");
        }

        IParameter[] params = this.resolvedMethod.getParameters();
        Integer foundIdx = null;
        for (int i = 0; i < params.length; i++) {
            if (paramName.equals(params[i].getName())) {
                foundIdx = i;
                break;
            }
        }
        if (foundIdx == null) {
            log.atWarn().log("[MethodBinderBuilder] Parameter name '{}' not found for method {}", paramName,
                    getMethodName());
            throw new DslException(
                    "Parameter name " + paramName + " not found for method " + getMethodName());
        }
        return withParam(foundIdx, parameter, acceptNullable);
    }

    @Override
    public Builder withParam(String paramName, ISupplierBuilder<?, ? extends ISupplier<?>> supplier,
            boolean acceptNullable)
            throws DslException {
        Objects.requireNonNull(paramName, "paramName cannot be null");
        Objects.requireNonNull(supplier, "supplier cannot be null");

        // Ensure method is set
        if (this.resolvedMethod == null) {
            throw new DslException("[MethodBinderBuilder] Method must be set before setting parameters");
        }

        IParameter[] params = this.resolvedMethod.getParameters();
        Integer foundIdx = null;
        for (int i = 0; i < params.length; i++) {
            if (paramName.equals(params[i].getName())) {
                foundIdx = i;
                break;
            }
        }
        if (foundIdx == null) {
            log.atWarn().log("[MethodBinderBuilder] Parameter name '{}' not found for method {}", paramName,
                    getMethodName());
            throw new DslException(
                    "Parameter name " + paramName + " not found for method " + getMethodName());
        }
        return withParam(foundIdx, supplier, acceptNullable);
    }

    @Override
    public Builder withParam(Object parameter) throws DslException {
        return withParam(parameter, false);
    }

    @Override
    public Builder withParam(ISupplierBuilder<?, ? extends ISupplier<?>> supplier) throws DslException {
        return withParam(supplier, false);
    }

    @Override
    public Builder withParam(Object parameter, boolean acceptNullable) throws DslException {
        // Ensure method is set
        if (this.resolvedMethod == null) {
            throw new DslException("[MethodBinderBuilder] Method must be set before setting parameters");
        }

        int idx = findNextFreeParameterIndex();
        if (idx < 0) {
            log.atWarn().log("[MethodBinderBuilder] No free parameter slot available for method {}", getMethodName());
            throw new DslException("No free parameter slot available");
        }
        return withParam(idx, parameter, acceptNullable);
    }

    @Override
    public Builder withParam(ISupplierBuilder<?, ? extends ISupplier<?>> supplier, boolean acceptNullable)
            throws DslException {
        // Ensure method is set
        if (this.resolvedMethod == null) {
            throw new DslException("[MethodBinderBuilder] Method must be set before setting parameters");
        }
        Objects.requireNonNull(supplier, "supplier cannot be null");

        int idx = findNextFreeParameterIndex();
        if (idx < 0) {
            log.atWarn().log("[MethodBinderBuilder] No free parameter slot available for method {}", getMethodName());
            throw new DslException("No free parameter slot available");
        }
        return withParam(idx, supplier, acceptNullable);
    }

    private int findNextFreeParameterIndex() {
        if (this.parameterEntries == null) {
            return 0;
        }
        for (int i = 0; i < this.parameterEntries.size(); i++) {
            if (this.parameterEntries.get(i) == null) {
                return i;
            }
        }
        // Return the next index (will be added to the list)
        return this.parameterEntries.size();
    }

    private List<ISupplierBuilder<?, ?>> resolveParameters() {
        List<ISupplierBuilder<?, ?>> resolved = new ArrayList<>(this.parameterEntries.size());
        for (int i = 0; i < this.parameterEntries.size(); i++) {
            Object entry = this.parameterEntries.get(i);
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

    protected List<ISupplier<?>> getBuiltParameterSuppliers(List<ISupplierBuilder<?, ?>> resolvedParams)
            throws DslException {
        List<ISupplier<?>> builtParameterSuppliers = new ArrayList<>(resolvedParams.size());
        for (int i = 0; i < resolvedParams.size(); i++) {
            ISupplierBuilder<?, ?> builder = resolvedParams.get(i);
            if (builder == null) {
                log.atWarn().log("Parameter {} has no supplier configured for method {}", i,
                        getMethodName());
                throw new DslException(
                        "Parameter " + i + " not configured for method " + getMethodName());
            }
            boolean allowNull = Boolean.TRUE.equals(this.parameterNullableAllowed.get(i));
            builtParameterSuppliers
                    .add(AbstractConstructorBinderBuilder.createNullableObjectSupplier(builder, allowNull));
        }
        return builtParameterSuppliers;
    }

    protected void initParameters() throws DslException {
        // If parameters list wasn't initialized yet, create it
        if (this.parameterEntries == null) {
            this.parameterEntries = new ArrayList<>(Collections.nCopies(this.getParameterTypes().length, null));
        }

        // Initialize nullability if not already done
        if (this.parameterNullableAllowed == null) {
            this.parameterNullableAllowed = new ArrayList<>(
                    Collections.nCopies(this.getParameterTypes().length, Boolean.FALSE));
        }

        log.atDebug().log("[MethodBinderBuilder] Successfully resolved method {} with {} parameters",
                getMethodName(), this.getParameterTypes().length);
    }

    @Override
    protected Built doBuild() throws DslException {
        // Ensure reflection is available (either explicitly provided or via IClass global/thread-local)
        effectiveReflection();
        log.atTrace().log("[MethodBinderBuilder] Building MethodBinder - resolving method and parameters");

        // Ensure method is set
        if (this.resolvedMethod == null) {
            throw new DslException("Method is not set");
        }

        // Validate that all required data is now available
        Objects.requireNonNull(this.parameterEntries, "Parameters are not set");
        Objects.requireNonNull(this.parameterNullableAllowed, "Parameter nullability metadata not initialized");

        // Resolve raw parameters to suppliers
        List<ISupplierBuilder<?, ?>> resolvedParams = resolveParameters();

        // Validate parameter count matches
        if (resolvedParams.size() != this.getParameterTypes().length) {
            throw new DslException("Method " + getMethodName() + " expects " + this.getParameterTypes().length
                    + " parameters but " + resolvedParams.size() + " were configured");
        }

        if (this.buildContextual(resolvedParams))
            return createContextualBinder(resolvedParams);
        return createBinder(resolvedParams);
    }

    private Built createContextualBinder(List<ISupplierBuilder<?, ?>> resolvedParams) {
        return (Built) new ContextualMethodBinder<>(this.supplier.build(), this.resolvedMethod,
                this.getBuiltParameterSuppliers(resolvedParams), this.collection);
    }

    protected Built createBinder(List<ISupplierBuilder<?, ?>> resolvedParams) {
        return (Built) new MethodBinder<>(this.supplier.build(), this.resolvedMethod,
                this.getBuiltParameterSuppliers(resolvedParams),
                this.collection);
    }

    protected IClass<?>[] getParameterTypes() {
        return this.resolvedMethod.getParameterTypes();
    }
}
