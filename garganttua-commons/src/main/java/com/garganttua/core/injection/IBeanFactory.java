package com.garganttua.core.injection;

import java.util.Set;

public interface IBeanFactory<Bean> extends IBeanSupplier<Bean> {

    boolean matches(BeanDefinition<?> example);

    BeanDefinition<Bean> getDefinition();

    Set<Class<?>> getDependencies();

}
