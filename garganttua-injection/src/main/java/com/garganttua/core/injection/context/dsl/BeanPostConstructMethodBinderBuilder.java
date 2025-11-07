package com.garganttua.core.injection.context.dsl;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanFactory;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.reflection.binders.dsl.AbstractMethodBinderBuilder;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class BeanPostConstructMethodBinderBuilder<Bean> extends
        AbstractMethodBinderBuilder<Void, IBeanPostConstructMethodBinderBuilder<Bean>, IBeanFactoryBuilder<Bean>>
        implements IBeanPostConstructMethodBinderBuilder<Bean> {

    private IInjectableElementResolver resolver;

    protected BeanPostConstructMethodBinderBuilder(IBeanFactoryBuilder<Bean> up,
            IObjectSupplierBuilder<Bean, IBeanFactory<Bean>> supplier,
            IInjectableElementResolver resolver)
            throws DslException {
        super(up, supplier);
        this.resolver = Objects.requireNonNull(resolver,
                "Injectable builder Registry cannot be null");
    }

    protected BeanPostConstructMethodBinderBuilder(IBeanFactoryBuilder<Bean> up,
            IObjectSupplierBuilder<Bean, IBeanFactory<Bean>> supplier,
            Optional<IInjectableElementResolver> resolver)
            throws DslException {
        super(up, supplier);
        Objects.requireNonNull(resolver,
                "Injectable builder Registry cannot be null");
        this.resolver = resolver.orElse(null);
    }

    protected BeanPostConstructMethodBinderBuilder(IBeanFactoryBuilder<Bean> up,
            IObjectSupplierBuilder<Bean, IBeanFactory<Bean>> supplier)
            throws DslException {
        super(up, supplier);
    }

    @Override
    protected IBeanPostConstructMethodBinderBuilder<Bean> getBuilder() {
        return this;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        if (this.resolver == null) {
            throw new DslException("Cannot do auto detection without registry");
        }
        Method method;
        try {
            method = this.findMethod();

            Parameter[] params = method.getParameters();

            for (int i = 0; i < params.length; i++) {
                Class<?> elementType = params[i].getType();
                Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> builder = this.resolver
                        .resolve(elementType, params[i]);
                if (builder.isPresent())
                    this.withParam(i, builder.get());
            }
        } catch (DiException e) {
            throw new DslException(e.getMessage(), e);
        }
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
