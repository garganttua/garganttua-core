package com.garganttua.core.injection.context.dsl;

import com.garganttua.core.injection.IDiContext;

@FunctionalInterface
public interface IContextBuilderObserver {

    void handle(IDiContext context);
}
