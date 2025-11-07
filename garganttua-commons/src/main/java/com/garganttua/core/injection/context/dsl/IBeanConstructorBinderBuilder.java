package com.garganttua.core.injection.context.dsl;

import com.garganttua.core.reflection.binders.Dependent;
import com.garganttua.core.reflection.binders.IConstructorBinder;
import com.garganttua.core.reflection.binders.dsl.IConstructorBinderBuilder;

public interface IBeanConstructorBinderBuilder<Bean> extends
        IConstructorBinderBuilder<Bean, IBeanConstructorBinderBuilder<Bean>, IBeanFactoryBuilder<Bean>, IConstructorBinder<Bean>>, Dependent {

}
