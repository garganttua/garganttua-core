package com.garganttua.injection.spec.supplier.builder.supplier;

import com.garganttua.core.reflection.binders.IConstructorBinder;
import com.garganttua.core.reflection.binders.dsl.IConstructorBinderBuilder;

public interface INewObjectSupplierConstructorBinderBuilder<Constructed, Link> extends IConstructorBinderBuilder<Constructed, INewObjectSupplierConstructorBinderBuilder<?,?>, Link, IConstructorBinder<Constructed>> {

}
