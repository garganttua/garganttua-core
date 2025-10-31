package com.garganttua.injection.beans;

import com.garganttua.injection.spec.supplier.binder.Dependent;
import com.garganttua.injection.spec.supplier.builder.binder.IInjectableFieldBuilder;

public interface IBeanInjectableFieldBuilder<FieldType, BeanType> extends IInjectableFieldBuilder<FieldType, BeanType, IBeanFactoryBuilder<BeanType>>, Dependent {

}
