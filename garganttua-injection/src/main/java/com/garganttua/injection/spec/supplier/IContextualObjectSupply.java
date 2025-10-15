package com.garganttua.injection.spec.supplier;

import com.garganttua.injection.spec.IDiContext;

@FunctionalInterface
public interface IContextualObjectSupply<Supplied> extends ICustomContextualObjectSupply<Supplied, IDiContext> {

}
