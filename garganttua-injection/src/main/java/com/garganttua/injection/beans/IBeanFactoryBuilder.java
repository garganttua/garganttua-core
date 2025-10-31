package com.garganttua.injection.beans;

import java.lang.annotation.Annotation;
import java.util.Set;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.spec.supplier.binder.Dependent;
import com.garganttua.injection.spec.supplier.builder.supplier.IObjectSupplierBuilder;

public interface IBeanFactoryBuilder<Bean> extends IObjectSupplierBuilder<Bean, IBeanFactory<Bean>>, Dependent {

    IBeanFactoryBuilder<Bean> strategy(BeanStrategy singleton);

    IBeanConstructorBinderBuilder<Bean> constructor() throws DslException;

    IBeanPostConstructMethodBinderBuilder<Bean> postConstruction() throws DslException;

    <FieldType> IBeanInjectableFieldBuilder<FieldType, Bean> field(Class<FieldType> fieldType)
            throws DslException;

    IBeanFactoryBuilder<Bean> name(String name);

    IBeanFactoryBuilder<Bean> qualifier(Class<? extends Annotation> qualifier) throws DslException;

    IBeanFactoryBuilder<Bean> qualifiers(Set<Class<? extends Annotation>> qualifiers) throws DslException;

}
