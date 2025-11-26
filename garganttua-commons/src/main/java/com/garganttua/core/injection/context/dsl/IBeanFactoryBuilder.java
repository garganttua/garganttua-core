package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;
import java.util.Set;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IBeanFactory;
import com.garganttua.core.reflection.binders.Dependent;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;

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
