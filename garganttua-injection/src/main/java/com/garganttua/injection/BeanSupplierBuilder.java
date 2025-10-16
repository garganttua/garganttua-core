package com.garganttua.injection;

import java.util.Objects;

import com.garganttua.dsl.DslException;

public class BeanSupplierBuilder<Bean> implements IBeanSupplierBuilder<Bean> {

    private String name;
    private String scope;
    private Class<Bean> type;

    public BeanSupplierBuilder(Class<Bean> type) {
        this.type = Objects.requireNonNull(type, "Type cannot be null");
    }

    @Override
    public Class<Bean> getObjectClass() {
        return type;
    }

    @Override
    public IBeanSupplier<Bean> build() throws DslException {
        if (type == null) {
            throw new DslException("Bean type must be provided");
        }
        return new BeanSupplier<Bean>(name, type, scope);
    }

    @Override
    public IBeanSupplierBuilder<Bean> name(String name) {
        this.name = Objects.requireNonNull(name, "Bean name cannot be null");
        return this;
    }

    @Override
    public IBeanSupplierBuilder<Bean> scope(String scope) {
        this.scope = Objects.requireNonNull(scope, "Bean scope cannot be null");
        return this;
    }

}
