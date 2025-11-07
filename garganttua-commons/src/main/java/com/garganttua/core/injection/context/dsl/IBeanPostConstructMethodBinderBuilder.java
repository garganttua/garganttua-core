package com.garganttua.core.injection.context.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.binders.Dependent;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.reflection.binders.dsl.IMethodBinderBuilder;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public interface IBeanPostConstructMethodBinderBuilder<Bean> extends
        IMethodBinderBuilder<Void, IBeanPostConstructMethodBinderBuilder<Bean>, IBeanFactoryBuilder<Bean>, IMethodBinder<Void>>, Dependent {

    IMethodBinder<Void> build(IObjectSupplierBuilder<Bean, IObjectSupplier<Bean>> bean) throws DslException;

}
