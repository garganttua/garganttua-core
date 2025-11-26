package com.garganttua.core.injection;

import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;

@FunctionalInterface
public interface ResolvedAction {

    void ifResolved(IObjectSupplierBuilder<?, ? extends IObjectSupplier<?>> elementSupplier, boolean nullable);

}
