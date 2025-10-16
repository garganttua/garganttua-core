package com.garganttua.injection;

import com.garganttua.injection.spec.supplier.builder.supplier.IObjectSupplierBuilder;

public interface IBeanSupplierBuilder<Bean> extends IObjectSupplierBuilder<Bean, IBeanSupplier<Bean>> {

    IBeanSupplierBuilder<Bean> name(String name);

    IBeanSupplierBuilder<Bean> scope(String scope);

}
