package com.garganttua.core.reflection.binders.dsl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.mutex.IMutex;
import com.garganttua.core.reflection.IObjectQuery;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.ContextualMethodBinder;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.reflection.binders.MethodBinder;
import com.garganttua.core.reflection.methods.MethodResolver;
import com.garganttua.core.reflection.methods.ResolvedMethod;
import com.garganttua.core.reflection.query.ObjectQueryFactory;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractMethodBinderBuilder<ExecutionReturn, Builder extends IMethodBinderBuilder<ExecutionReturn, Builder, Link, Built>, Link, Built extends IMethodBinder<ExecutionReturn>>
        extends AbstractAutomaticLinkedBuilder<Builder, Link, Built>
        implements IMethodBinderBuilder<ExecutionReturn, Builder, Link, Built> {

    @Setter
    private ISupplierBuilder<?, ?> supplier;

    // Parameter configuration (set during builder phase)
    private List<ISupplierBuilder<?, ?>> parameters;
    private List<Boolean> parameterNullableAllowed;

    private IObjectQuery<?> objectQuery;
    private boolean collection = false;

    private ISupplierBuilder<? extends IMutex, ? extends ISupplier<? extends IMutex>> mutex;

    private ResolvedMethod resolvedMethod;

    protected AbstractMethodBinderBuilder(Link up, ISupplierBuilder<?, ?> supplier) throws DslException {
        this(up, supplier, false);
    }

    protected AbstractMethodBinderBuilder(Link up, ISupplierBuilder<?, ?> supplier, boolean collection)
            throws DslException {
        super(up);
        log.atTrace().log("[MethodBinderBuilder] Creating with up={} and supplier={}", up, supplier);
        this.supplier = Objects.requireNonNull(supplier, "Supplier cannot be null");
        this.collection = collection;
        try {
            this.objectQuery = ObjectQueryFactory.objectQuery(this.supplier.getSuppliedClass());
        } catch (ReflectionException e) {
            log.atError().log("[MethodBinderBuilder] Error creating objectQuery for class {}",
                    this.supplier.getSuppliedClass(), e);
            throw new DslException(e.getMessage(), e);
        }
    }

    protected List<Boolean> nullableParameters() {
        return this.parameterNullableAllowed;
    }

    protected void nullableParameter(int i, boolean acceptNullable) {
        this.parameterNullableAllowed.set(i, acceptNullable);
    }

    private boolean buildContextual() {
        if (this.supplier.isContextual())
            return true;
        return this.parameters.stream().anyMatch(param -> param.isContextual());
    }

    public String getMethodName() {
        if (this.resolvedMethod != null) {
            return this.resolvedMethod.name();
        }
        return null;
    }

    @Override
    public ObjectAddress methodAddress() throws DslException {
        return this.resolvedMethod.address();
    }

    @Override
    public Method method() throws DslException {
        return this.resolvedMethod.method();
    }

    /**
     * Binds a specific Method object to this builder.
     *
     * <p>
     * This method is particularly important for handling overloaded methods
     * correctly.
     * By passing the actual Method object, the builder stores it directly and
     * avoids
     * re-lookup issues that could return the wrong overload variant.
     * </p>
     *
     * <p>
     * The method resolution and parameter initialization are deferred until build
     * time.
     * </p>
     *
     * @param method the Method object to bind
     * @return this builder instance for method chaining
     * @throws DslException if method validation fails
     */
    @Override
    public Builder method(Method method) throws DslException {
        log.atDebug().log("[MethodBinderBuilder] Storing method {} for deferred resolution", method.getName());

        this.resolvedMethod = MethodResolver.methodByMethod(this.supplier.getSuppliedClass(), method);

        this.initParameters();

        return (Builder) this;
    }

    @Override
    public Builder method(ObjectAddress methodAddress, Class<ExecutionReturn> returnType, Class<?>... parameterTypes)
            throws DslException {
        log.atDebug().log("[MethodBinderBuilder] Storing method address={} with returnType={} for deferred resolution",
                methodAddress, returnType);

        Objects.requireNonNull(methodAddress, "Method address cannot be null");

        this.resolvedMethod = MethodResolver.methodByAddress(this.supplier.getSuppliedClass(), methodAddress,
                returnType,
                parameterTypes);

        this.initParameters();

        return (Builder) this;
    }

    @Override
    public Builder method(String methodName, Class<ExecutionReturn> returnType, Class<?>... parameterTypes)
            throws DslException {
        log.atDebug().log("[MethodBinderBuilder] Storing method name={} with returnType={} for deferred resolution",
                methodName, returnType);

        this.resolvedMethod = MethodResolver.methodByName(this.supplier.getSuppliedClass(), methodName, returnType,
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
        if (this.parameters == null) {
            // We don't know the size yet, will be checked during build
            this.parameters = new ArrayList<>();
            this.parameterNullableAllowed = new ArrayList<>();
        }

        // Ensure the lists are large enough
        while (this.parameters.size() <= i) {
            this.parameters.add(null);
            this.parameterNullableAllowed.add(Boolean.FALSE);
        }

        this.parameters.set(i, AbstractConstructorBinderBuilder.createFixedObjectSupplierBuilder(object));
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
        if (this.parameters == null) {
            this.parameters = new ArrayList<>();
            this.parameterNullableAllowed = new ArrayList<>();
        }

        // Ensure the lists are large enough
        while (this.parameters.size() <= i) {
            this.parameters.add(null);
            this.parameterNullableAllowed.add(Boolean.FALSE);
        }

        this.parameters.set(i, object);
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

        java.lang.reflect.Parameter[] params = this.resolvedMethod.parameters();
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

        java.lang.reflect.Parameter[] params = this.resolvedMethod.parameters();
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
        if (this.parameters == null) {
            return 0;
        }
        for (int i = 0; i < this.parameters.size(); i++) {
            if (this.parameters.get(i) == null) {
                return i;
            }
        }
        // Return the next index (will be added to the list)
        return this.parameters.size();
    }

    protected List<ISupplier<?>> getBuiltParameterSuppliers() throws DslException {
        List<ISupplier<?>> builtParameterSuppliers = new ArrayList<>(this.parameters.size());
        for (int i = 0; i < this.parameters.size(); i++) {
            ISupplierBuilder<?, ?> builder = this.parameters.get(i);
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
        if (this.parameters == null) {
            this.parameters = new ArrayList<>(Collections.nCopies(this.getParameterTypes().length, null));
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
        log.atTrace().log("[MethodBinderBuilder] Building MethodBinder - resolving method and parameters");

        // Ensure method is set
        if (this.resolvedMethod == null) {
            throw new DslException("Method is not set");
        }

        // Validate that all required data is now available
        Objects.requireNonNull(this.resolvedMethod, "Resolved method is null after resolution");
        Objects.requireNonNull(this.parameters, "Parameters are not set");
        Objects.requireNonNull(this.parameterNullableAllowed, "Parameter nullability metadata not initialized");

        // Validate parameter count matches
        if (this.parameters.size() != this.getParameterTypes().length) {
            throw new DslException("Method " + getMethodName() + " expects " + this.getParameterTypes().length
                    + " parameters but " + this.parameters.size() + " were configured");
        }

        if (this.buildContextual())
            return createContextualBinder();
        return createBinder();
    }

    private Built createContextualBinder() {
        return (Built) new ContextualMethodBinder<>(this.supplier.build(), this.resolvedMethod,
                this.getBuiltParameterSuppliers(), this.collection);
    }

    protected Built createBinder() {
        return (Built) new MethodBinder<>(this.supplier.build(), this.resolvedMethod, this.getBuiltParameterSuppliers(),
                this.collection);
    }

    protected Class<?>[] getParameterTypes() {
        return this.resolvedMethod.parameterTypes();
    }

    @Override
    public Builder mutex(ISupplierBuilder<? extends IMutex, ? extends ISupplier<? extends IMutex>> mutex)
            throws DslException {
        this.mutex = Objects.requireNonNull(mutex, "Mutex cannot be null");
        return (Builder) this;
    }
}
