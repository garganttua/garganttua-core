package com.garganttua.injection.spec.supplier.builder.binder;

import com.garganttua.dsl.IAutomaticLinkedBuilder;
import com.garganttua.injection.spec.supplier.binder.Dependent;
import com.garganttua.injection.spec.supplier.binder.IExecutableBinder;
import com.garganttua.injection.spec.supplier.builder.IParametrizedBuilder;

public interface IExecutableBinderBuilder<ExecutionReturn, Builder extends IExecutableBinderBuilder<?, ?, ?, ?>, Link, Built extends IExecutableBinder<ExecutionReturn>>
        extends IAutomaticLinkedBuilder<Builder, Link, Built>, IParametrizedBuilder<Builder, Built>, Dependent { }
