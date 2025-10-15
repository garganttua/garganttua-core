package com.garganttua.injection.spec.supplier.builder.binder;

import com.garganttua.injection.spec.supplier.binder.IExecutableBinder;

public interface IConstructorBinderBuilder<Constructed, Builder extends IConstructorBinderBuilder<?, ?, ?, ?>, Link, Built extends IExecutableBinder<Constructed>>
                extends IExecutableBinderBuilder<Constructed, Builder, Link, Built> {
}