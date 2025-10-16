package com.garganttua.injection.spec.supplier.builder.supplier;

import com.garganttua.injection.spec.supplier.binder.IConstructorBinder;
import com.garganttua.injection.spec.supplier.builder.binder.IConstructorBinderBuilder;

public interface INewObjectSupplierConstructorBinderBuilder<Constructed, Link> extends IConstructorBinderBuilder<Constructed, INewObjectSupplierConstructorBinderBuilder<?,?>, Link, IConstructorBinder<Constructed>> {

}
