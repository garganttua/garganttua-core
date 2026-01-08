package com.garganttua.core.injection.dummies;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IInjectionChildContextFactory;
import com.garganttua.core.injection.IInjectionContext;

public class DummyChildContextFactory implements IInjectionChildContextFactory<DummyChildContext> {

    @Override
    public DummyChildContext createChildContext(IInjectionContext parent, Object... args) throws DiException {
        throw new UnsupportedOperationException("Unimplemented method 'createChildContext'");
    }

}
