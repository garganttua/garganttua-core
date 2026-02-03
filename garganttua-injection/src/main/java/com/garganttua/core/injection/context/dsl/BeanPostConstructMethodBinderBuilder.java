package com.garganttua.core.injection.context.dsl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IBeanFactory;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanPostConstructMethodBinderBuilder<Bean> extends
                AbstractMethodArgInjectBinderBuilder<Void, IBeanPostConstructMethodBinderBuilder<Bean>, IBeanFactoryBuilder<Bean>, IMethodBinder<Void>>
                implements IBeanPostConstructMethodBinderBuilder<Bean> {

        protected BeanPostConstructMethodBinderBuilder(IBeanFactoryBuilder<Bean> up,
                        ISupplierBuilder<Bean, IBeanFactory<Bean>> supplier) throws DslException {
                super(up, supplier);
                log.atTrace().log(
                                "Entering BeanPostConstructMethodBinderBuilder constructor with up: {}, supplier: {}, no resolver",
                                up, supplier);
                log.atDebug().log("BeanPostConstructMethodBinderBuilder initialized without resolver for beanClass: {}",
                                up.getSuppliedClass());
                log.atTrace().log("Exiting BeanPostConstructMethodBinderBuilder constructor");
        }

        @Override
        public IMethodBinder<Void> build(ISupplierBuilder<Bean, ISupplier<Bean>> supplierBuilder)
                        throws DslException {
                log.atTrace().log("Entering build() for beanClass: {}", supplierBuilder.getSuppliedClass());
                log.atDebug().log("Creating method binder");
                this.setSupplier(supplierBuilder);
                IMethodBinder<Void> binder = this.createBinder();
                log.atDebug().log("Method binder successfully built for beanClass: {}",
                                supplierBuilder.getSuppliedClass());
                log.atTrace().log("Exiting build()");
                return binder;
        }

        @Override
        public Set<Class<?>> dependencies() {
                log.atTrace().log("Entering getDependencies()");
                log.atDebug().log("Getting parameter types for post-construct method");
                Set<Class<?>> dependencies = new HashSet<>(Arrays.asList(this.getParameterTypes()));
                log.atDebug().log("Dependencies for BeanPostConstructMethodBinderBuilder: {}", dependencies);
                log.atTrace().log("Exiting getDependencies()");
                return dependencies;
        }
}