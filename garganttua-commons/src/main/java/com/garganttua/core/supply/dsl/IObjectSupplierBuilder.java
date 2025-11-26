package com.garganttua.core.supply.dsl;

import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.supply.IObjectSupplier;

public interface IObjectSupplierBuilder<Supplied, Built extends IObjectSupplier<Supplied>> extends IBuilder<Built> {

    Class<Supplied> getSuppliedType();

    boolean isContextual();

}
