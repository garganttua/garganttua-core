package com.garganttua.core.injection.context.dsl;

import com.garganttua.core.reflection.binders.Dependent;
import com.garganttua.core.reflection.binders.dsl.IFieldBinderBuilder;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public interface IBeanInjectableFieldBuilder<FieldType, BeanType> extends IFieldBinderBuilder<FieldType, BeanType, IBeanInjectableFieldBuilder<FieldType, BeanType>, IBeanFactoryBuilder<BeanType>>, Dependent {

    public IBeanInjectableFieldBuilder<FieldType, BeanType> setBean(IObjectSupplierBuilder<BeanType, ? extends IObjectSupplier<BeanType>> beanSupplier);

}
