package com.garganttua.core.injection;

public interface IDiChildContextFactory<ChildContext extends IDiContext> {

    ChildContext createChildContext(IDiContext parent, Object ...args) throws DiException;
}
