package com.garganttua.core.injection.context.dsl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanConstructorBinderBuilder<Bean> extends
        AbstractConstructorArgInjectBinderBuilder<Bean, IBeanConstructorBinderBuilder<Bean>, IBeanFactoryBuilder<Bean>>
        implements IBeanConstructorBinderBuilder<Bean> {

    private Class<Bean> beanType;

    protected BeanConstructorBinderBuilder(BeanFactoryBuilder<Bean> link, Class<Bean> beanType) {
        super(link, beanType);
        this.beanType = beanType;
        log.atTrace().log("Entering BeanConstructorBinderBuilder constructor with link: {}, beanType: {}", link,
                beanType);
        log.atDebug().log("BeanConstructorBinderBuilder initialized without resolver for beanType: {}", beanType);
        log.atTrace().log("Exiting constructor");
    }

    @Override
    public Set<Class<?>> dependencies() {
        log.atTrace().log("Entering getDependencies for beanType: {}", this.beanType.getSimpleName());
        Set<Class<?>> dependencies = new HashSet<>(Arrays.asList(this.getParameterTypes()));
        log.atDebug().log("Dependencies for beanType {}: {}", this.beanType.getSimpleName(), dependencies);
        log.atTrace().log("Exiting getDependencies");
        return dependencies;
    }
}