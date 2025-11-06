package com.garganttua.core.injection.dummies;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IDiChildContextFactory;
import com.garganttua.core.injection.IDiContext;

public class DummyChildContextFactory implements IDiChildContextFactory<DummyChildContext> {

    @Override
    public DummyChildContext createChildContext(IDiContext parent, Object... args) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createChildContext'");
    }

}
