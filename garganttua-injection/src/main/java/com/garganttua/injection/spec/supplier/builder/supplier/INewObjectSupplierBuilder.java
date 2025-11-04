package com.garganttua.injection.spec.supplier.builder.supplier;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.supplying.IContextualObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public interface INewObjectSupplierBuilder<Constructed, Context> extends IAutomaticBuilder<INewObjectSupplierBuilder<Constructed, Context>, IContextualObjectSupplier<Constructed, Context>>, IObjectSupplierBuilder<Constructed, IContextualObjectSupplier<Constructed, Context>> {

    INewObjectSupplierConstructorBinderBuilder<Constructed, INewObjectSupplierBuilder<Constructed, Context>> withConstructor();


}
