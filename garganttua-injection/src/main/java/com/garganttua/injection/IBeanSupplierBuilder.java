package com.garganttua.injection;

import java.lang.annotation.Annotation;

import com.garganttua.injection.beans.BeanStrategy;
import com.garganttua.injection.spec.supplier.builder.supplier.IObjectSupplierBuilder;

public interface IBeanSupplierBuilder<Bean> extends IObjectSupplierBuilder<Bean, IBeanSupplier<Bean>> {

    IBeanSupplierBuilder<Bean> name(String name);

    IBeanSupplierBuilder<Bean> provider(String provider);

    IBeanSupplierBuilder<Bean> strategy(BeanStrategy strategy);

    IBeanSupplierBuilder<Bean> qualifier(Class<? extends Annotation> qualifier);

}
