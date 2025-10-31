package com.garganttua.injection.spec.supplier.builder;

import com.garganttua.dsl.DslException;
import com.garganttua.dsl.ILinkedBuilder;
import com.garganttua.injection.spec.supplier.builder.supplier.IObjectSupplierBuilder;

public interface IFieldBuilder<Link, Built> extends ILinkedBuilder<Link, Built> {

    IFieldBuilder<Link, Built> withValue(Object value) throws DslException;

    IFieldBuilder<Link, Built> withValue(IObjectSupplierBuilder<?, ?> supplier) throws DslException;

}
