package com.garganttua.injection.supplier.builder.binder;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.garganttua.dsl.AbstractLinkedBuilder;
import com.garganttua.dsl.DslException;
import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.supplier.IObjectSupplier;
import com.garganttua.injection.spec.supplier.binder.IMethodBinder;
import com.garganttua.injection.spec.supplier.builder.binder.IMethodBinderBuilder;
import com.garganttua.injection.spec.supplier.builder.supplier.IObjectSupplierBuilder;
import com.garganttua.injection.supplier.binder.MethodBinder;
import com.garganttua.injection.supplier.builder.supplier.FixedObjectSupplierBuilder;
import com.garganttua.injection.supplier.builder.supplier.NullableEnforcingObjectSupplier;
import com.garganttua.reflection.GGObjectAddress;
import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.query.GGObjectQueryFactory;
import com.garganttua.reflection.query.IGGObjectQuery;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractMethodBinderBuilder<ExecutionReturn, Builder extends IMethodBinderBuilder<ExecutionReturn, Builder, Link, IMethodBinder<ExecutionReturn>>, Link>
        extends AbstractLinkedBuilder<Link, IMethodBinder<ExecutionReturn>>
        implements IMethodBinderBuilder<ExecutionReturn, Builder, Link, IMethodBinder<ExecutionReturn>> {

    private IObjectSupplierBuilder<?, ?> supplier;
    private GGObjectAddress method = null;
    private List<IObjectSupplierBuilder<?, ?>> parameters;
    private List<Boolean> parameterNullableAllowed;
    private Class<?>[] parameterTypes;

    protected abstract Builder getReturned();

    private IGGObjectQuery objectQuery;
    private boolean collection = false;
    private Class<ExecutionReturn> returnedType;

    protected AbstractMethodBinderBuilder(Link up, IObjectSupplierBuilder<?, ?> supplier) throws DslException {
        this(up, supplier, false);
    }

    protected AbstractMethodBinderBuilder(Link up, IObjectSupplierBuilder<?, ?> supplier, boolean collection)
            throws DslException {
        super(up);
        log.atTrace().log("[MethodBinderBuilder] Creating with up={} and supplier={}", up, supplier);
        this.supplier = Objects.requireNonNull(supplier, "Supplier cannot be null");
        this.collection = collection;
        try {
            this.objectQuery = GGObjectQueryFactory.objectQuery(this.supplier.getObjectClass());
        } catch (GGReflectionException e) {
            log.atError().log("[MethodBinderBuilder] Error creating objectQuery for class {}",
                    this.supplier.getObjectClass(), e);
            throw new DslException(e.getMessage(), e);
        }
    }

    public String getMethodName() {
        return this.method != null ? this.method.getElement(this.method.length() - 1) : null;
    }

    @Override
    public Builder withReturn(Class<ExecutionReturn> returnedType) throws DslException {
        this.returnedType = Objects.requireNonNull(returnedType, "Returned type cannot be null");

        return this.getReturned();
    }

    @Override
    public Builder method() throws DslException {
        if (this.method == null) {
            throw new DslException("Method must be set");
        }
        return this.getReturned();
    }

    @Override
    public Builder method(Method method) throws DslException {
        log.atDebug().log("[MethodBinderBuilder] Resolving method {} in class {}", method.getName(),
                this.supplier.getObjectClass());

        try {
            this.method = MethodResolver.methodByMethod(method, this.supplier.getObjectClass());
            this.initParameters();
            return this.getReturned();
        } catch (DiException e) {
            throw new DslException(e.getMessage(), e);
        }
    }

    @Override
    public Builder method(GGObjectAddress methodAddress) throws DslException {
        log.atDebug().log("[MethodBinderBuilder] Resolving method by address={} in class {}", methodAddress,
                this.supplier.getObjectClass());

        try {
            this.method = MethodResolver.methodByAddress(methodAddress, this.objectQuery,
                    this.supplier.getObjectClass());
            this.initParameters();
            return this.getReturned();
        } catch (DiException e) {
            throw new DslException(e.getMessage(), e);
        }
    }

    @Override
    public Builder method(String methodName) throws DslException {
        log.atDebug().log("[MethodBinderBuilder] Resolving method by name={} in class {}", methodName,
                this.supplier.getObjectClass());

        try {
            this.method = MethodResolver.methodByName(methodName, this.objectQuery, this.supplier.getObjectClass());
            this.initParameters();
            return this.getReturned();
        } catch (DiException e) {
            throw new DslException(e.getMessage(), e);
        }
    }

    @Override
    public Builder method(Method method, Class<ExecutionReturn> returnType, Class<?>... parameterTypes)
            throws DslException {
        log.atDebug().log("[MethodBinderBuilder] Resolving method {} with returnType={} in class {}",
                method.getName(), returnType, this.supplier.getObjectClass());

        try {
            this.method = MethodResolver.methodByMethod(method, this.supplier.getObjectClass(), returnType,
                    parameterTypes);
            this.initParameters();
            return this.getReturned();
        } catch (DiException e) {
            throw new DslException(e.getMessage(), e);
        }
    }

    @Override
    public Builder method(GGObjectAddress methodAddress, Class<ExecutionReturn> returnType, Class<?>... parameterTypes)
            throws DslException {
        log.atDebug().log("[MethodBinderBuilder] Resolving method by address={} with returnType={} in class {}",
                methodAddress, returnType, this.supplier.getObjectClass());

        try {
            this.method = MethodResolver.methodByAddress(methodAddress, this.objectQuery,
                    this.supplier.getObjectClass(),
                    returnType, parameterTypes);
            this.initParameters();
            return this.getReturned();
        } catch (DiException e) {
            throw new DslException(e.getMessage(), e);
        }
    }

    @Override
    public Builder method(String methodName, Class<ExecutionReturn> returnType, Class<?>... parameterTypes)
            throws DslException {
        log.atDebug().log("[MethodBinderBuilder] Resolving method by name={} with returnType={} in class {}",
                methodName, returnType, this.supplier.getObjectClass());

        try {
            this.method = MethodResolver.methodByName(methodName, this.objectQuery, this.supplier.getObjectClass(),
                    returnType, parameterTypes);
            this.initParameters();
            return this.getReturned();
        } catch (DiException e) {
            throw new DslException(e.getMessage(), e);
        }
    }

    private void initParameters() throws DslException {
        Objects.requireNonNull(this.method, "[MethodBinderBuilder] Method must be set before initializing parameters");
        Objects.requireNonNull(this.objectQuery, "[MethodBinderBuilder] Object query cannot be null");

        try {
            Method m = (Method) this.objectQuery.find(this.method).getLast();
            this.parameterTypes = m.getParameterTypes();
            this.parameters = new ArrayList<>(Collections.nCopies(this.parameterTypes.length, null));
            // default : parameters are NOT nullable unless specified
            this.parameterNullableAllowed = new ArrayList<>(
                    Collections.nCopies(this.parameterTypes.length, Boolean.FALSE));

            log.atInfo().log("[MethodBinderBuilder] Successfully bound method {} with {} parameters",
                    getMethodName(), this.parameterTypes.length);
        } catch (GGReflectionException e) {
            log.atError().log("[MethodBinderBuilder] Error initializing parameters for method {}", getMethodName(), e);
            throw new DslException(e.getMessage(), e);
        }
    }

    // -----------------------
    // withParam implementations
    // -----------------------

    @Override
    public Builder withParam(int i, Object object) throws DslException {
        return withParam(i, object, false);
    }

    @Override
    public Builder withParam(int i, IObjectSupplierBuilder<?, ?> object) throws DslException {
        return withParam(i, object, false);
    }

    @Override
    public Builder withParam(int i, Object object, boolean acceptNullable) throws DslException {
        log.atTrace().log("[MethodBinderBuilder] Binding parameter {} with value={} (acceptNullable={})", i, object,
                acceptNullable);
        Objects.requireNonNull(this.method, "[MethodBinderBuilder] Method must be set before setting parameters");

        if (!this.isValidParameterIndex(i)) {
            log.atWarn().log("[MethodBinderBuilder] Invalid parameter index {} for method {}", i, getMethodName());
            throw new DslException(
                    "Method " + getMethodName() + " has only " + this.parameterTypes.length + " parameters");
        }

        if (object == null) {
            if (!acceptNullable) {
                log.atWarn().log("[MethodBinderBuilder] Null value provided for parameter {} but acceptNullable=false",
                        i);
                throw new DslException(
                        "Parameter " + i + " of method " + getMethodName() + " cannot be null");
            }
            this.parameters.set(i, new FixedObjectSupplierBuilder<>(null));
            this.parameterNullableAllowed.set(i, Boolean.TRUE);
            log.atInfo().log("[MethodBinderBuilder] Parameter {} bound as nullable null", i);
            return this.getReturned();
        }

        if (!this.isValidParameterType(i, object)) {
            log.atWarn().log("[MethodBinderBuilder] Invalid parameter type {} for method {} expected {}",
                    object.getClass(), getMethodName(), this.parameterTypes[i]);
            throw new DslException(
                    "Parameter " + i + " of method " + getMethodName() + " is of type "
                            + this.parameterTypes[i].getName()
                            + " and cannot be assigned a value of type " + object.getClass().getName());
        }

        this.parameters.set(i, new FixedObjectSupplierBuilder<>(object));
        this.parameterNullableAllowed.set(i, acceptNullable);
        log.atInfo().log("[MethodBinderBuilder] Parameter {} bound successfully with type {} (acceptNullable={})", i,
                object.getClass(), acceptNullable);
        return this.getReturned();
    }

    @Override
    public Builder withParam(int i, IObjectSupplierBuilder<?, ?> object, boolean acceptNullable) throws DslException {
        log.atTrace().log("[MethodBinderBuilder] Binding parameter {} with supplier of type {} (acceptNullable={})", i,
                object == null ? "null" : object.getObjectClass(), acceptNullable);
        Objects.requireNonNull(this.method, "[MethodBinderBuilder] Method must be set before setting parameters");
        Objects.requireNonNull(object, "Supplier cannot be null");

        if (!this.isValidParameterIndex(i)) {
            log.atWarn().log("[MethodBinderBuilder] Invalid parameter index {} for method {}", i, getMethodName());
            throw new DslException(
                    "Method " + getMethodName() + " has only " + this.parameterTypes.length + " parameters");
        }
        // type check using supplier declared class
        Class<?> suppliedClass = object.getObjectClass();
        if (suppliedClass == null) {
            log.atWarn().log("[MethodBinderBuilder] Supplier.getObjectClass() returned null for parameter {}", i);
            throw new DslException(
                    "Supplier for parameter " + i + " does not declare object class");
        }
        if (!this.isValidParameterType(i, suppliedClass)) {
            log.atWarn().log("[MethodBinderBuilder] Invalid supplier type {} for parameter {} of method {} expected {}",
                    suppliedClass, i, getMethodName(), this.parameterTypes[i]);
            throw new DslException(
                    "Parameter " + i + " of method " + getMethodName() + " is of type "
                            + this.parameterTypes[i].getName()
                            + " and cannot be assigned a value of type " + suppliedClass.getName());
        }

        this.parameters.set(i, object);
        this.parameterNullableAllowed.set(i, acceptNullable);
        log.atInfo().log(
                "[MethodBinderBuilder] Parameter {} bound successfully with supplier type {} (acceptNullable={})", i,
                suppliedClass, acceptNullable);
        return this.getReturned();
    }

    @Override
    public Builder withParam(String paramName, Object parameter) throws DslException {
        return withParam(paramName, parameter, false);
    }

    @Override
    public Builder withParam(String paramName, IObjectSupplierBuilder<?, ?> supplier) throws DslException {
        return withParam(paramName, supplier, false);
    }

    @Override
    public Builder withParam(String paramName, Object parameter, boolean acceptNullable) throws DslException {
        Objects.requireNonNull(paramName, "paramName cannot be null");
        Objects.requireNonNull(this.method, "[MethodBinderBuilder] Method must be set before setting parameters");

        Method m;
        try {
            m = (Method) this.objectQuery.find(this.method).getLast();
        } catch (GGReflectionException e) {
            throw new DslException(e.getMessage(), e);
        }

        java.lang.reflect.Parameter[] params = m.getParameters();
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
    public Builder withParam(String paramName, IObjectSupplierBuilder<?, ?> supplier, boolean acceptNullable)
            throws DslException {
        Objects.requireNonNull(paramName, "paramName cannot be null");
        Objects.requireNonNull(supplier, "supplier cannot be null");
        Objects.requireNonNull(this.method, "[MethodBinderBuilder] Method must be set before setting parameters");

        Method m;
        try {
            m = (Method) this.objectQuery.find(this.method).getLast();
        } catch (GGReflectionException e) {
            throw new DslException(e.getMessage(), e);
        }

        java.lang.reflect.Parameter[] params = m.getParameters();
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
    public Builder withParam(IObjectSupplierBuilder<?, ?> supplier) throws DslException {
        return withParam(supplier, false);
    }

    @Override
    public Builder withParam(Object parameter, boolean acceptNullable) throws DslException {
        Objects.requireNonNull(this.method, "[MethodBinderBuilder] Method must be set before setting parameters");
        int idx = findNextFreeParameterIndex();
        if (idx < 0) {
            log.atWarn().log("[MethodBinderBuilder] No free parameter slot available for method {}", getMethodName());
            throw new DslException("No free parameter slot available");
        }
        return withParam(idx, parameter, acceptNullable);
    }

    @Override
    public Builder withParam(IObjectSupplierBuilder<?, ?> supplier, boolean acceptNullable) throws DslException {
        Objects.requireNonNull(this.method, "[MethodBinderBuilder] Method must be set before setting parameters");
        Objects.requireNonNull(supplier, "supplier cannot be null");
        int idx = findNextFreeParameterIndex();
        if (idx < 0) {
            log.atWarn().log("[MethodBinderBuilder] No free parameter slot available for method {}", getMethodName());
            throw new DslException("No free parameter slot available");
        }
        return withParam(idx, supplier, acceptNullable);
    }

    private int findNextFreeParameterIndex() {
        for (int i = 0; i < this.parameters.size(); i++) {
            if (this.parameters.get(i) == null) {
                return i;
            }
        }
        return -1;
    }

    private boolean isValidParameterIndex(int i) {
        boolean valid = i >= 0 && i < this.parameterTypes.length;
        log.atTrace().log("[MethodBinderBuilder] Parameter index {} validity: {}", i, valid);
        return valid;
    }

    private boolean isValidParameterType(int index, Object object) {
        return this.isValidParameterType(index, object.getClass());
    }

    private boolean isValidParameterType(int index, Class<?> objectType) {
        boolean valid = objectType.isAssignableFrom(this.parameterTypes[index]);
        log.atTrace().log("[MethodBinderBuilder] Parameter type check for index {} expected {} got {} validity={}",
                index, this.parameterTypes[index], objectType, valid);
        return valid;
    }

    @Override
    public IMethodBinder<ExecutionReturn> build() throws DslException {
        log.atTrace().log("[MethodBinderBuilder] Building MethodBinder");
        Objects.requireNonNull(this.method, "Method is not set");
        Objects.requireNonNull(this.parameters, "Parameters are not set");
        Objects.requireNonNull(this.parameterNullableAllowed, "Parameter nullability metadata not initialized");
        Objects.requireNonNull(this.returnedType, "Returned type cannot be null");

        List<IObjectSupplier<?>> builtParameterSuppliers = new ArrayList<>(this.parameters.size());
        for (int i = 0; i < this.parameters.size(); i++) {
            IObjectSupplierBuilder<?, ?> builder = this.parameters.get(i);
            if (builder == null) {
                log.atWarn().log("[MethodBinderBuilder] Parameter {} has no supplier configured for method {}", i,
                        getMethodName());
                throw new DslException(
                        "Parameter " + i + " not configured for method " + getMethodName());
            }
            IObjectSupplier<?> supplierInstance;
            try {
                supplierInstance = builder.build();
            } catch (DslException e) {
                log.atError().log("[MethodBinderBuilder] Error building supplier for parameter {} of method {}", i,
                        getMethodName(), e);
                throw e;
            }
            boolean allowNull = Boolean.TRUE.equals(this.parameterNullableAllowed.get(i));
            builtParameterSuppliers
                    .add(new NullableEnforcingObjectSupplier<>(supplierInstance, allowNull, i, getMethodName()));
        }

        IObjectSupplier<?> ownerSupplier = this.supplier.build();
        return new MethodBinder<ExecutionReturn>(ownerSupplier, this.method, builtParameterSuppliers,
                this.returnedType, this.collection);
    }
}
