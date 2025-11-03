package com.garganttua.injection.spec.supplier.builder.supplier;

import com.garganttua.dsl.IBuilder;
import com.garganttua.injection.spec.supplier.IObjectSupplier;

public interface IObjectSupplierBuilder<Supplied, Built extends IObjectSupplier<Supplied>> extends IBuilder<Built> {

    Class<Supplied> getObjectClass();

}
