package com.garganttua.core.injection.context.dsl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IBeanFactory;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected
public class BeanPostConstructMethodBinderBuilder<Bean> extends
                AbstractMethodArgInjectBinderBuilder<Void, IBeanPostConstructMethodBinderBuilder<Bean>, IBeanFactoryBuilder<Bean>, IMethodBinder<Void>>
                implements IBeanPostConstructMethodBinderBuilder<Bean> {
    private static final Logger log = Logger.getLogger(BeanPostConstructMethodBinderBuilder.class);

        protected BeanPostConstructMethodBinderBuilder(IBeanFactoryBuilder<Bean> up,
                        ISupplierBuilder<Bean, IBeanFactory<Bean>> supplier) throws DslException {
                super(up, supplier);
                log.trace(
                                "Entering BeanPostConstructMethodBinderBuilder constructor with up: {}, supplier: {}, no resolver",
                                up, supplier);
                log.debug("BeanPostConstructMethodBinderBuilder initialized without resolver for beanClass: {}",
                                up.getSuppliedClass());
                log.trace("Exiting BeanPostConstructMethodBinderBuilder constructor");
        }

        @Override
        public IMethodBinder<Void> build(ISupplierBuilder<Bean, ISupplier<Bean>> supplierBuilder)
                        throws DslException {
                log.trace("Entering build() for beanClass: {}", supplierBuilder.getSuppliedClass());
                log.debug("Creating method binder");
                this.setSupplier(supplierBuilder);
                IMethodBinder<Void> binder = this.doBuild();
                log.debug("Method binder successfully built for beanClass: {}",
                                supplierBuilder.getSuppliedClass());
                log.trace("Exiting build()");
                return binder;
        }

        @Override
        public Set<IClass<?>> dependencies() {
                log.trace("Entering getDependencies()");
                log.debug("Getting parameter types for post-construct method");
                Set<IClass<?>> dependencies = new HashSet<>(Arrays.asList(this.getParameterTypes()));
                log.debug("Dependencies for BeanPostConstructMethodBinderBuilder: {}", dependencies);
                log.trace("Exiting getDependencies()");
                return dependencies;
        }
}
