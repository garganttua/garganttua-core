package com.garganttua.injection.beans;

import java.lang.annotation.Annotation;
import java.util.Set;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.spec.supplier.builder.supplier.IObjectSupplierBuilder;

public interface IBeanFactoryBuilder<Bean> extends IObjectSupplierBuilder<Bean, IBeanFactory<Bean>> {

    IBeanFactoryBuilder<Bean> strategy(BeanStrategy singleton);

    IBeanConstructorBinderBuilder<Bean> constructor() throws DslException;

    IBeanPostConstructMethodBinderBuilder<Bean> postConstruction() throws DslException;

    IBeanFactoryBuilder<Bean> name(String name);

    IBeanFactoryBuilder<Bean> qualifier(Class<? extends Annotation> qualifier) throws DslException;

    IBeanFactoryBuilder<Bean> qualifiers(Set<Class<? extends Annotation>> qualifiers) throws DslException;

}
