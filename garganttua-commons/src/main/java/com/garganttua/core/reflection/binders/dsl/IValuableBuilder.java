package com.garganttua.core.reflection.binders.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public interface IValuableBuilder<Builder, Link, Built> extends IAutomaticLinkedBuilder<Builder, Link, Built> {

    IValuableBuilder<Builder, Link, Built> withValue(Object value) throws DslException;

    IValuableBuilder<Builder, Link, Built> withValue(IObjectSupplierBuilder<?, ? extends IObjectSupplier<?>> supplier) throws DslException;

    IValuableBuilder<Builder, Link, Built> allowNull(boolean allowNull) throws DslException;

}
