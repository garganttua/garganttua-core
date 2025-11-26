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
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanPostConstructMethodBinderBuilder<Bean> extends
        AbstractMethodArgInjectBinderBuilder<Void, IBeanPostConstructMethodBinderBuilder<Bean>, IBeanFactoryBuilder<Bean>, IMethodBinder<Void>>
        implements IBeanPostConstructMethodBinderBuilder<Bean> {

    protected BeanPostConstructMethodBinderBuilder(IBeanFactoryBuilder<Bean> up,
            IObjectSupplierBuilder<Bean, IBeanFactory<Bean>> supplier,
            IInjectableElementResolver resolver) throws DslException {
        super(resolver, up, supplier);
        log.atTrace().log("Initialized BeanPostConstructMethodBinderBuilder for beanClass: {}", up.getSuppliedType());
    }

    protected BeanPostConstructMethodBinderBuilder(IBeanFactoryBuilder<Bean> up,
            IObjectSupplierBuilder<Bean, IBeanFactory<Bean>> supplier,
            Optional<IInjectableElementResolver> resolver) throws DslException {
        super(resolver, up, supplier);
        log.atTrace().log("Initialized BeanPostConstructMethodBinderBuilder with optional resolver for beanClass: {}",
                up.getSuppliedType());
    }

    protected BeanPostConstructMethodBinderBuilder(IBeanFactoryBuilder<Bean> up,
            IObjectSupplierBuilder<Bean, IBeanFactory<Bean>> supplier) throws DslException {
        super(Optional.empty(), up, supplier);
        log.atTrace().log("Initialized BeanPostConstructMethodBinderBuilder without resolver for beanClass: {}",
                up.getSuppliedType());
    }

    @Override
    public IMethodBinder<Void> build(IObjectSupplierBuilder<Bean, IObjectSupplier<Bean>> supplierBuilder)
            throws DslException {
        log.atTrace().log("Building method binder for beanClass: {}", supplierBuilder.getSuppliedType());
        List<IObjectSupplier<?>> builtParameterSuppliers = this.getBuiltParameterSuppliers();
        log.atDebug().log("Built parameter suppliers count: {}", builtParameterSuppliers.size());
        IMethodBinder<Void> binder = this.createBinder(builtParameterSuppliers, supplierBuilder);
        log.atInfo().log("Method binder successfully built for beanClass: {}", supplierBuilder.getSuppliedType());
        return binder;
    }

    @Override
    public Set<Class<?>> getDependencies() {
        Set<Class<?>> dependencies = new HashSet<>(Arrays.asList(this.getParameterTypes()));
        log.atTrace().log("Dependencies for BeanPostConstructMethodBinderBuilder: {}", dependencies);
        return dependencies;
    }
}