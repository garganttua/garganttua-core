package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;

import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IBeanSupplier;
import com.garganttua.core.reflection.binders.Dependent;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public interface IBeanSupplierBuilder<Bean> extends IObjectSupplierBuilder<Bean, IBeanSupplier<Bean>>, Dependent {

    IBeanSupplierBuilder<Bean> name(String name);

    IBeanSupplierBuilder<Bean> provider(String provider);

    IBeanSupplierBuilder<Bean> strategy(BeanStrategy strategy);

    IBeanSupplierBuilder<Bean> qualifier(Class<? extends Annotation> qualifier);

}
