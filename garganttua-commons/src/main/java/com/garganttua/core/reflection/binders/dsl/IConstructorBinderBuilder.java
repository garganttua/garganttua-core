package com.garganttua.core.reflection.binders.dsl;

import com.garganttua.core.reflection.binders.IExecutableBinder;

public interface IConstructorBinderBuilder<Constructed, Builder extends IConstructorBinderBuilder<?, ?, ?, ?>, Link, Built extends IExecutableBinder<Constructed>>
                extends IExecutableBinderBuilder<Constructed, Builder, Link, Built> {
}