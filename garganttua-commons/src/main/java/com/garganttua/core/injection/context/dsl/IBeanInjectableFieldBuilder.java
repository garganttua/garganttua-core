package com.garganttua.core.injection.context.dsl;

import com.garganttua.core.reflection.binders.Dependent;
import com.garganttua.core.reflection.binders.dsl.IFieldBinderBuilder;

public interface IBeanInjectableFieldBuilder<FieldType, BeanType> extends IFieldBinderBuilder<FieldType, BeanType, IBeanFactoryBuilder<BeanType>>, Dependent {

}
