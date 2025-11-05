package com.garganttua.core.reflection.binders.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.ILinkedBuilder;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public interface IValuableBuilder<Link, Built> extends ILinkedBuilder<Link, Built> {

    IValuableBuilder<Link, Built> withValue(Object value) throws DslException;

    IValuableBuilder<Link, Built> withValue(IObjectSupplierBuilder<?, ?> supplier) throws DslException;

    IValuableBuilder<Link, Built> allowNull(boolean allowNull) throws DslException;

}
