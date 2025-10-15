package com.garganttua.injection.spec.supplier.builder.supplier;

import com.garganttua.dsl.IAutomaticBuilder;
import com.garganttua.injection.spec.supplier.IContextualObjectSupplier;

public interface INewObjectSupplierBuilder<Constructed, Context> extends IAutomaticBuilder<INewObjectSupplierBuilder<Constructed, Context>, IContextualObjectSupplier<Constructed, Context>>, IObjectSupplierBuilder<Constructed, IContextualObjectSupplier<Constructed, Context>> {

    INewObjectSupplierConstructorBinderBuilder<Constructed, INewObjectSupplierBuilder<Constructed, Context>> withConstructor();


}
