package com.garganttua.injection;

import com.garganttua.injection.spec.supplier.builder.supplier.IObjectSupplierBuilder;

public interface IPropertySupplierBuilder<Property> extends IObjectSupplierBuilder<Property, IPropertySupplier<Property>>  {

    IPropertySupplierBuilder<Property> key(String name);

    IPropertySupplierBuilder<Property> provider(String provider);

}
