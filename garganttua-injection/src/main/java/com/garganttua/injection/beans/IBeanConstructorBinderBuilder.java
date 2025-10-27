package com.garganttua.injection.beans;

import com.garganttua.injection.spec.supplier.binder.IConstructorBinder;
import com.garganttua.injection.spec.supplier.builder.binder.IConstructorBinderBuilder;

public interface IBeanConstructorBinderBuilder<Bean> extends IConstructorBinderBuilder<Bean, IBeanConstructorBinderBuilder<Bean>, IBeanFactoryBuilder<Bean>, IConstructorBinder<Bean>> {

}
