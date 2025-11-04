package com.garganttua.core.supplying.dsl;

import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.supplying.IObjectSupplier;

public interface IObjectSupplierBuilder<Supplied, Built extends IObjectSupplier<Supplied>> extends IBuilder<Built> {

    Class<Supplied> getSuppliedType();

    boolean isContextual();

}
