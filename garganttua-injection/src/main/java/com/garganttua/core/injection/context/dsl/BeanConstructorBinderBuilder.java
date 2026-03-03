package com.garganttua.core.injection.context.dsl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanConstructorBinderBuilder<Bean> extends
        AbstractConstructorArgInjectBinderBuilder<Bean, IBeanConstructorBinderBuilder<Bean>, IBeanFactoryBuilder<Bean>>
        implements IBeanConstructorBinderBuilder<Bean> {

    private IClass<Bean> beanType;

    protected BeanConstructorBinderBuilder(BeanFactoryBuilder<Bean> link, IClass<Bean> beanType) {
        super(link, beanType);
        this.beanType = beanType;
        log.atTrace().log("Entering BeanConstructorBinderBuilder constructor with link: {}, beanType: {}", link,
                beanType);
        log.atDebug().log("BeanConstructorBinderBuilder initialized without resolver for beanType: {}", beanType);
        log.atTrace().log("Exiting constructor");
    }

    @Override
    public Set<IClass<?>> dependencies() {
        log.atTrace().log("Entering getDependencies for beanType: {}", this.beanType.getSimpleName());
        Set<IClass<?>> dependencies = new HashSet<>();
        IClass<Inject> injectClass = IClass.getClass(Inject.class);
        for (IConstructor<?> c : this.beanType.getDeclaredConstructors()) {
            if (c.isAnnotationPresent(injectClass)) {
                for (IClass<?> paramType : c.getParameterTypes()) {
                    dependencies.add(paramType);
                }
                break;
            }
        }
        log.atDebug().log("Dependencies for beanType {}: {}", this.beanType.getSimpleName(), dependencies);
        log.atTrace().log("Exiting getDependencies");
        return dependencies;
    }
}
