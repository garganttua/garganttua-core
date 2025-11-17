package com.garganttua.core.injection.context.dsl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.injection.IInjectableElementResolver;

public class BeanConstructorBinderBuilder<Bean> extends
        AbstractConstructorArgInjectBinderBuilder<Bean, IBeanConstructorBinderBuilder<Bean>, IBeanFactoryBuilder<Bean>>
        implements IBeanConstructorBinderBuilder<Bean> {

    protected BeanConstructorBinderBuilder(BeanFactoryBuilder<Bean> link, Class<Bean> beanType,
            IInjectableElementResolver resolver) {
        super(resolver, link, beanType);
    }

    protected BeanConstructorBinderBuilder(BeanFactoryBuilder<Bean> link, Class<Bean> beanType,
            Optional<IInjectableElementResolver> resolver) {
        super(resolver, link, beanType);
    }

    protected BeanConstructorBinderBuilder(BeanFactoryBuilder<Bean> link, Class<Bean> beanType) {
        super(Optional.empty(), link, beanType);
    }

    @Override
    public Set<Class<?>> getDependencies() {
        return new HashSet<>(Arrays.asList(this.getParameterTypes()));
    }
}