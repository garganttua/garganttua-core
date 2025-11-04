package com.garganttua.core.injection;

import com.garganttua.core.supplying.IContextualObjectSupply;

@FunctionalInterface
public interface IDiContextObjectSupply<Supplied> extends IContextualObjectSupply<Supplied, IDiContext> {

}
