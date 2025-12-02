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
        log.atTrace().log("Entering BeanPostConstructMethodBinderBuilder constructor with up: {}, supplier: {}, resolver: {}",
                up, supplier, resolver);
        log.atInfo().log("BeanPostConstructMethodBinderBuilder initialized for beanClass: {}", up.getSuppliedType());
        log.atTrace().log("Exiting BeanPostConstructMethodBinderBuilder constructor");
    }

    protected BeanPostConstructMethodBinderBuilder(IBeanFactoryBuilder<Bean> up,
            IObjectSupplierBuilder<Bean, IBeanFactory<Bean>> supplier,
            Optional<IInjectableElementResolver> resolver) throws DslException {
        super(resolver, up, supplier);
        log.atTrace().log("Entering BeanPostConstructMethodBinderBuilder constructor with up: {}, supplier: {}, optional resolver: {}",
                up, supplier, resolver);
        log.atInfo().log("BeanPostConstructMethodBinderBuilder initialized with optional resolver for beanClass: {}",
                up.getSuppliedType());
        log.atTrace().log("Exiting BeanPostConstructMethodBinderBuilder constructor");
    }

    protected BeanPostConstructMethodBinderBuilder(IBeanFactoryBuilder<Bean> up,
            IObjectSupplierBuilder<Bean, IBeanFactory<Bean>> supplier) throws DslException {
        super(Optional.empty(), up, supplier);
        log.atTrace().log("Entering BeanPostConstructMethodBinderBuilder constructor with up: {}, supplier: {}, no resolver",
                up, supplier);
        log.atInfo().log("BeanPostConstructMethodBinderBuilder initialized without resolver for beanClass: {}",
                up.getSuppliedType());
        log.atTrace().log("Exiting BeanPostConstructMethodBinderBuilder constructor");
    }

    @Override
    public IMethodBinder<Void> build(IObjectSupplierBuilder<Bean, IObjectSupplier<Bean>> supplierBuilder)
            throws DslException {
        log.atTrace().log("Entering build() for beanClass: {}", supplierBuilder.getSuppliedType());
        log.atDebug().log("Getting built parameter suppliers");
        List<IObjectSupplier<?>> builtParameterSuppliers = this.getBuiltParameterSuppliers();
        log.atDebug().log("Built parameter suppliers count: {}", builtParameterSuppliers.size());
        log.atDebug().log("Creating method binder");
        IMethodBinder<Void> binder = this.createBinder(builtParameterSuppliers, supplierBuilder);
        log.atInfo().log("Method binder successfully built for beanClass: {}", supplierBuilder.getSuppliedType());
        log.atTrace().log("Exiting build()");
        return binder;
    }

    @Override
    public Set<Class<?>> getDependencies() {
        log.atTrace().log("Entering getDependencies()");
        log.atDebug().log("Getting parameter types for post-construct method");
        Set<Class<?>> dependencies = new HashSet<>(Arrays.asList(this.getParameterTypes()));
        log.atDebug().log("Dependencies for BeanPostConstructMethodBinderBuilder: {}", dependencies);
        log.atTrace().log("Exiting getDependencies()");
        return dependencies;
    }
}