package com.garganttua.core.injection.context.dsl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IBeanFactory;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class BeanPostConstructMethodBinderBuilder<Bean> extends
        AbstractMethodArgInjectBinderBuilder<Void, IBeanPostConstructMethodBinderBuilder<Bean>, IBeanFactoryBuilder<Bean>, IMethodBinder<Void>>
        implements IBeanPostConstructMethodBinderBuilder<Bean> {

    protected BeanPostConstructMethodBinderBuilder(IBeanFactoryBuilder<Bean> up,
            IObjectSupplierBuilder<Bean, IBeanFactory<Bean>> supplier,
            IInjectableElementResolver resolver)
            throws DslException {
        super(resolver, up, supplier);
    }

    protected BeanPostConstructMethodBinderBuilder(IBeanFactoryBuilder<Bean> up,
            IObjectSupplierBuilder<Bean, IBeanFactory<Bean>> supplier,
            Optional<IInjectableElementResolver> resolver)
            throws DslException {
        super(resolver, up, supplier);

    }

    protected BeanPostConstructMethodBinderBuilder(IBeanFactoryBuilder<Bean> up,
            IObjectSupplierBuilder<Bean, IBeanFactory<Bean>> supplier)
            throws DslException {
        super(Optional.empty(), up, supplier);
    }

    @Override
    public IMethodBinder<Void> build(IObjectSupplierBuilder<Bean, IObjectSupplier<Bean>> supplierBuilder)
            throws DslException {
        List<IObjectSupplier<?>> builtParameterSuppliers = this.getBuiltParameterSuppliers();
        return this.createBinder(builtParameterSuppliers, supplierBuilder);
    }

    @Override
    public Set<Class<?>> getDependencies() {
        return new HashSet<>(Arrays.asList(this.getParameterTypes()));
    }

}
