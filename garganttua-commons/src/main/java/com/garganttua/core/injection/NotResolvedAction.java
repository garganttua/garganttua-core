package com.garganttua.core.injection;

@FunctionalInterface
public interface NotResolvedAction {

    void ifNotResolved(boolean nullable);

}
