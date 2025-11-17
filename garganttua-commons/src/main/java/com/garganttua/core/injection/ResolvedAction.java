package com.garganttua.core.injection;

import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

@FunctionalInterface
public interface ResolvedAction {

    void ifResolved(IObjectSupplierBuilder<?, ? extends IObjectSupplier<?>> elementSupplier, boolean nullable);

}
