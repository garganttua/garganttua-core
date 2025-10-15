package com.garganttua.injection.spec.supplier.builder;

import com.garganttua.dsl.DslException;
import com.garganttua.dsl.IAutomaticBuilder;
import com.garganttua.injection.spec.supplier.builder.supplier.IObjectSupplierBuilder;

public interface IParametrizedBuilder<Builder, Built> extends IAutomaticBuilder<Builder, Built> {

    Builder withParam(int i, Object parameter) throws DslException;
    Builder withParam(int i, IObjectSupplierBuilder<?,?> supplier) throws DslException;
    Builder withParam(String paramName, Object parameter) throws DslException;
    Builder withParam(String paramName, IObjectSupplierBuilder<?,?> supplier) throws DslException;
    Builder withParam(Object parameter) throws DslException;
    Builder withParam(IObjectSupplierBuilder<?,?> supplier) throws DslException;

    Builder withParam(int i, Object parameter, boolean acceptNullable) throws DslException;
    Builder withParam(int i, IObjectSupplierBuilder<?,?> supplier, boolean acceptNullable) throws DslException;
    Builder withParam(String paramName, Object parameter, boolean acceptNullable) throws DslException;
    Builder withParam(String paramName, IObjectSupplierBuilder<?,?> supplier, boolean acceptNullable) throws DslException;
    Builder withParam(Object parameter, boolean acceptNullable) throws DslException;
    Builder withParam(IObjectSupplierBuilder<?,?> supplier, boolean acceptNullable) throws DslException;
}