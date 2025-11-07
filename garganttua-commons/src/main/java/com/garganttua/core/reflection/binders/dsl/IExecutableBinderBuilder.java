package com.garganttua.core.reflection.binders.dsl;

import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.reflection.binders.IExecutableBinder;

public interface IExecutableBinderBuilder<ExecutionReturn, Builder extends IExecutableBinderBuilder<?, ?, ?, ?>, Link, Built extends IExecutableBinder<ExecutionReturn>>
        extends IAutomaticLinkedBuilder<Builder, Link, Built>, IParametrableBuilder<Builder, Built> { }
