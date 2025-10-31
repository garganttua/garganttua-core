package com.garganttua.injection.beans;

import java.util.Set;

import com.garganttua.injection.IBeanSupplier;

public interface IBeanFactory<Bean> extends IBeanSupplier<Bean> {

    boolean matches(BeanDefinition<?> example);

    BeanDefinition<Bean> getDefinition();

    Set<Class<?>> getDependencies();

}
