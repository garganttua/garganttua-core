package com.garganttua.core.injection.context.dsl;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Concrete constructor-binder builder for beans, linked back to its owning
 * {@link BeanFactoryBuilder}. Reports the {@code @Inject} constructor's parameter types
 * as DI dependencies so the container can order bean creation correctly.
 *
 * @param <Bean> the bean type whose constructor is bound
 */
@Reflected
public class BeanConstructorBinderBuilder<Bean> extends
        AbstractConstructorArgInjectBinderBuilder<Bean, IBeanConstructorBinderBuilder<Bean>, IBeanFactoryBuilder<Bean>>
        implements IBeanConstructorBinderBuilder<Bean> {
    private static final Logger log = Logger.getLogger(BeanConstructorBinderBuilder.class);

    private IClass<Bean> beanType;

    /**
     * Builds a constructor binder builder for the given bean type, linked to its factory builder.
     */
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
