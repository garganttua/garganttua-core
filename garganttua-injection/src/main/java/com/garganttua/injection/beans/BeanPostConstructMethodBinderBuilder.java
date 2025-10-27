package com.garganttua.injection.beans;

import java.util.List;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.spec.supplier.IObjectSupplier;
import com.garganttua.injection.spec.supplier.binder.IMethodBinder;
import com.garganttua.injection.spec.supplier.builder.supplier.IObjectSupplierBuilder;
import com.garganttua.injection.supplier.builder.binder.AbstractMethodBinderBuilder;

public class BeanPostConstructMethodBinderBuilder<Bean> extends
        AbstractMethodBinderBuilder<Void, IBeanPostConstructMethodBinderBuilder<Bean>, IBeanFactoryBuilder<Bean>>
        implements IBeanPostConstructMethodBinderBuilder<Bean> {

    protected BeanPostConstructMethodBinderBuilder(IBeanFactoryBuilder<Bean> up,
            IObjectSupplierBuilder<Bean, IBeanFactory<Bean>> supplier)
            throws DslException {
        super(up, supplier);
    }

    @Override
    public IBeanPostConstructMethodBinderBuilder<Bean> autoDetect(boolean b) throws DslException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'autoDetect'");
    }

    @Override
    protected IBeanPostConstructMethodBinderBuilder<Bean> getReturned() {
        return this;
    }

    @Override
    public IMethodBinder<Void> build(IObjectSupplierBuilder<Bean, IObjectSupplier<Bean>> supplier) throws DslException {
        List<IObjectSupplier<?>> builtParameterSuppliers = this.getBuiltParameterSuppliers();
        return this.createBinder(builtParameterSuppliers, supplier);
    }

}
