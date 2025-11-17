package com.garganttua.core.injection;

public interface IDiChildContextFactory<ChildContext extends IDiContext> {

    /**
     * 
     * @param clonedParent => initialized and started cloned context
     * @param args
     * @return
     * @throws DiException
     */
    ChildContext createChildContext(IDiContext clonedParent, Object ...args) throws DiException;
}
