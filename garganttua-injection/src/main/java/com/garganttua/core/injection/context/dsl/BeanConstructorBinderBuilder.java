package com.garganttua.core.injection.context.dsl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected
public class BeanConstructorBinderBuilder<Bean> extends
        AbstractConstructorArgInjectBinderBuilder<Bean, IBeanConstructorBinderBuilder<Bean>, IBeanFactoryBuilder<Bean>>
        implements IBeanConstructorBinderBuilder<Bean> {
    private static final IDiagnostic log = Diagnostics.of(BeanConstructorBinderBuilder.class);

    private IClass<Bean> beanType;

    protected BeanConstructorBinderBuilder(BeanFactoryBuilder<Bean> link, IClass<Bean> beanType) {
        super(link, beanType);
        this.beanType = beanType;
        log.trace("Entering BeanConstructorBinderBuilder constructor with link: {}, beanType: {}", link,
                beanType);
        log.debug("BeanConstructorBinderBuilder initialized without resolver for beanType: {}", beanType);
        log.trace("Exiting constructor");
    }

    @Override
    public Set<IClass<?>> dependencies() {
        log.trace("Entering getDependencies for beanType: {}", this.beanType.getSimpleName());
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
        log.debug("Dependencies for beanType {}: {}", this.beanType.getSimpleName(), dependencies);
        log.trace("Exiting getDependencies");
        return dependencies;
    }
}
