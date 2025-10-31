package com.garganttua.injection;

import java.lang.annotation.Annotation;

import com.garganttua.injection.beans.BeanStrategy;
import com.garganttua.injection.spec.supplier.binder.Dependent;
import com.garganttua.injection.spec.supplier.builder.supplier.IObjectSupplierBuilder;

public interface IBeanSupplierBuilder<Bean> extends IObjectSupplierBuilder<Bean, IBeanSupplier<Bean>>, Dependent {

    IBeanSupplierBuilder<Bean> name(String name);

    IBeanSupplierBuilder<Bean> provider(String provider);

    IBeanSupplierBuilder<Bean> strategy(BeanStrategy strategy);

    IBeanSupplierBuilder<Bean> qualifier(Class<? extends Annotation> qualifier);

}
