package com.garganttua.injection.beans;

import java.lang.annotation.Annotation;

import com.garganttua.dsl.IBuilder;

public interface IBeanQueryBuilder<Bean> extends IBuilder<IBeanQuery<Bean>>{

    IBeanQueryBuilder<Bean> type(Class<Bean> type);

    IBeanQueryBuilder<Bean> name(String name);

    IBeanQueryBuilder<Bean> qualifier(Class<? extends Annotation> qualifier);

    IBeanQueryBuilder<Bean> strategy(BeanStrategy strategy);

    IBeanQueryBuilder<Bean> scope(String scope);

}
