package com.garganttua.core.injection.context.dsl;

import com.garganttua.core.injection.IPropertySupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public interface IPropertySupplierBuilder<Property> extends IObjectSupplierBuilder<Property, IPropertySupplier<Property>>  {

    IPropertySupplierBuilder<Property> key(String name);

    IPropertySupplierBuilder<Property> provider(String provider);

}
