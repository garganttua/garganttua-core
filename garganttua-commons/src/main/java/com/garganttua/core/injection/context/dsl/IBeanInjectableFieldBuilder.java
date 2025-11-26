package com.garganttua.core.injection.context.dsl;

import com.garganttua.core.reflection.binders.Dependent;
import com.garganttua.core.reflection.binders.dsl.IFieldBinderBuilder;
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;

public interface IBeanInjectableFieldBuilder<FieldType, BeanType> extends IFieldBinderBuilder<FieldType, BeanType, IBeanInjectableFieldBuilder<FieldType, BeanType>, IBeanFactoryBuilder<BeanType>>, Dependent {

    public IBeanInjectableFieldBuilder<FieldType, BeanType> setBean(IObjectSupplierBuilder<BeanType, ? extends IObjectSupplier<BeanType>> beanSupplier);

}
