package com.garganttua.core.query.dsl;

import java.lang.reflect.Method;
import java.util.Objects;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.expression.dsl.IQueryBuilder;
import com.garganttua.core.expression.dsl.IQueryMethodBinder;
import com.garganttua.core.expression.dsl.IQueryMethodBinderBuilder;
import com.garganttua.core.reflection.binders.dsl.AbstractMethodBinderBuilder;
import com.garganttua.core.reflection.methods.Methods;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.supply.dsl.NullSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueryMethodBinderBuilder<S> extends
        AbstractMethodBinderBuilder<ISupplier<S>, IQueryMethodBinderBuilder<S>, IQueryBuilder, IQueryMethodBinder<S>>
        implements IQueryMethodBinderBuilder<S> {

    private Class<?> ownerType;
    private Class<S> supplied;

    public QueryMethodBinderBuilder(IQueryBuilder QueryBuilder, Class<?> methodOwner, Class<S> supplied) {
        super(QueryBuilder, new NullSupplierBuilder<>(methodOwner));
        this.supplied = Objects.requireNonNull(supplied, "Supplied type cannot be null");
        this.ownerType = Objects.requireNonNull(methodOwner, "Method owner type cannot be null");
    }

    @Override
    public IQueryMethodBinderBuilder<S> withParam(int i, Object parameter) throws DslException {
        log.atInfo().log("Adding parameter for IQueryMethodBinderBuilder is not available");
        return this;
    }

    @Override
    public IQueryMethodBinderBuilder<S> withParam(int i, ISupplierBuilder<?, ? extends ISupplier<?>> supplier)
            throws DslException {
        log.atInfo().log("Adding parameter for IQueryMethodBinderBuilder is not available");
        return this;
    }

    @Override
    public IQueryMethodBinderBuilder<S> withParam(String paramName, Object parameter) throws DslException {
        log.atInfo().log("Adding parameter for IQueryMethodBinderBuilder is not available");
        return this;
    }

    @Override
    public IQueryMethodBinderBuilder<S> withParam(String paramName,
            ISupplierBuilder<?, ? extends ISupplier<?>> supplier) throws DslException {
        log.atInfo().log("Adding parameter for IQueryMethodBinderBuilder is not available");
        return this;
    }

    @Override
    public IQueryMethodBinderBuilder<S> withParam(Object parameter) throws DslException {
        log.atInfo().log("Adding parameter for IQueryMethodBinderBuilder is not available");
        return this;
    }

    @Override
    public IQueryMethodBinderBuilder<S> withParam(ISupplierBuilder<?, ? extends ISupplier<?>> supplier)
            throws DslException {
        log.atInfo().log("Adding parameter for IQueryMethodBinderBuilder is not available");
        return this;
    }

    @Override
    public IQueryMethodBinderBuilder<S> withParam(int i, Object parameter, boolean acceptNullable) throws DslException {
        log.atInfo().log("Adding parameter for IQueryMethodBinderBuilder is not available");
        return this;
    }

    @Override
    public IQueryMethodBinderBuilder<S> withParam(int i, ISupplierBuilder<?, ? extends ISupplier<?>> supplier,
            boolean acceptNullable) throws DslException {
        log.atInfo().log("Adding parameter for IQueryMethodBinderBuilder is not available");
        return this;
    }

    @Override
    public IQueryMethodBinderBuilder<S> withParam(String paramName, Object parameter, boolean acceptNullable)
            throws DslException {
        log.atInfo().log("Adding parameter for IQueryMethodBinderBuilder is not available");
        return this;
    }

    @Override
    public IQueryMethodBinderBuilder<S> withParam(String paramName,
            ISupplierBuilder<?, ? extends ISupplier<?>> supplier, boolean acceptNullable)
            throws DslException {
        log.atInfo().log("Adding parameter for IQueryMethodBinderBuilder is not available");
        return this;
    }

    @Override
    public IQueryMethodBinderBuilder<S> withParam(Object parameter, boolean acceptNullable) throws DslException {
        log.atInfo().log("Adding parameter for IQueryMethodBinderBuilder is not available");
        return this;
    }

    @Override
    public IQueryMethodBinderBuilder<S> withParam(ISupplierBuilder<?, ? extends ISupplier<?>> supplier,
            boolean acceptNullable) throws DslException {
        log.atInfo().log("Adding parameter for IQueryMethodBinderBuilder is not available");
        return this;
    }

    @Override
    protected IQueryMethodBinder<S> doBuild() throws DslException {
        Method method = this.method();
        if( !Methods.isStatic(method) ){
            throw new DslException("Method " + method.getName() + " is not static in class " + this.ownerType.getName());
        }
        return new QueryMethodBinder<>(method, this.supplied);
    }

    @Override
    protected void doAutoDetection() throws DslException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'doAutoDetection'");
    }

}
