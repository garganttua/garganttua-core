package com.garganttua.injection.spec;

import com.garganttua.injection.DiException;

public interface IDiChildContextFactory<ChildContext extends IDiContext> {

    ChildContext createChildContext(IDiContext parent, Object ...args) throws DiException;
}
