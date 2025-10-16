package com.garganttua.di.impl.supplier;

import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.IDiChildContextFactory;
import com.garganttua.injection.spec.IDiContext;

public class DummyChildContextFactory implements IDiChildContextFactory<DummyChildContext> {

    @Override
    public DummyChildContext createChildContext(IDiContext parent, Object... args) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createChildContext'");
    }

}
