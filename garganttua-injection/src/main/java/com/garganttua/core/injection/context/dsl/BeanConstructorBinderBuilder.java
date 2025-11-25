package com.garganttua.core.injection.context.dsl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.injection.IInjectableElementResolver;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanConstructorBinderBuilder<Bean> extends
        AbstractConstructorArgInjectBinderBuilder<Bean, IBeanConstructorBinderBuilder<Bean>, IBeanFactoryBuilder<Bean>>
        implements IBeanConstructorBinderBuilder<Bean> {

    private Class<Bean> beanType;

    protected BeanConstructorBinderBuilder(BeanFactoryBuilder<Bean> link, Class<Bean> beanType,
            IInjectableElementResolver resolver) {
        super(resolver, link, beanType);
        this.beanType = beanType;
        log.atTrace().log("Entering BeanConstructorBinderBuilder constructor with resolver: {}, link: {}, beanType: {}",
                resolver, link, beanType);
        log.atInfo().log("BeanConstructorBinderBuilder initialized with resolver: {}, beanType: {}", resolver,
                beanType);
        log.atTrace().log("Exiting constructor");
    }

    protected BeanConstructorBinderBuilder(BeanFactoryBuilder<Bean> link, Class<Bean> beanType,
            Optional<IInjectableElementResolver> resolver) {
        super(resolver, link, beanType);
        this.beanType = beanType;
        log.atTrace().log(
                "Entering BeanConstructorBinderBuilder constructor with optional resolver: {}, link: {}, beanType: {}",
                resolver, link, beanType);
        log.atInfo().log("BeanConstructorBinderBuilder initialized with resolver: {}", resolver.orElse(null));
        log.atTrace().log("Exiting constructor");
    }

    protected BeanConstructorBinderBuilder(BeanFactoryBuilder<Bean> link, Class<Bean> beanType) {
        super(Optional.empty(), link, beanType);
        this.beanType = beanType;
        log.atTrace().log("Entering BeanConstructorBinderBuilder constructor with link: {}, beanType: {}", link,
                beanType);
        log.atInfo().log("BeanConstructorBinderBuilder initialized without resolver for beanType: {}", beanType);
        log.atTrace().log("Exiting constructor");
    }

    @Override
    public Set<Class<?>> getDependencies() {
        log.atTrace().log("Entering getDependencies for beanType: {}", this.beanType.getSimpleName());
        Set<Class<?>> dependencies = new HashSet<>(Arrays.asList(this.getParameterTypes()));
        log.atInfo().log("Dependencies for beanType {}: {}", this.beanType.getSimpleName(), dependencies);
        log.atTrace().log("Exiting getDependencies");
        return dependencies;
    }
}