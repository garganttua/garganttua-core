package com.garganttua.core.reflection.binders.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public interface IParametrableBuilder<Builder, Built> extends IAutomaticBuilder<Builder, Built> {

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