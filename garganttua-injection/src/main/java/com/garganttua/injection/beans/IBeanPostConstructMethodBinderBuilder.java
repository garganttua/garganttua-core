package com.garganttua.injection.beans;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.spec.supplier.IObjectSupplier;
import com.garganttua.injection.spec.supplier.binder.IMethodBinder;
import com.garganttua.injection.spec.supplier.builder.binder.IMethodBinderBuilder;
import com.garganttua.injection.spec.supplier.builder.supplier.IObjectSupplierBuilder;

public interface IBeanPostConstructMethodBinderBuilder<Bean> extends IMethodBinderBuilder<Void, IBeanPostConstructMethodBinderBuilder<Bean>, IBeanFactoryBuilder<Bean>, IMethodBinder<Void>> {

    IMethodBinder<Void> build(IObjectSupplierBuilder<Bean, IObjectSupplier<Bean>> bean) throws DslException;

}
