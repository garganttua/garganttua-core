package com.garganttua.core.injection;

import java.lang.annotation.Annotation;

import com.garganttua.core.dsl.IBuilder;

public interface IBeanQueryBuilder<Bean> extends IBuilder<IBeanQuery<Bean>>{

    IBeanQueryBuilder<Bean> type(Class<Bean> type);

    IBeanQueryBuilder<Bean> name(String name);

    IBeanQueryBuilder<Bean> qualifier(Class<? extends Annotation> qualifier) throws DiException;

    IBeanQueryBuilder<Bean> strategy(BeanStrategy strategy);

    IBeanQueryBuilder<Bean> provider(String provider);

}
