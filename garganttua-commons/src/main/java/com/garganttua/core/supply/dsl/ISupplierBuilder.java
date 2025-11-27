package com.garganttua.core.supply.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.binders.IConstructorBinder;
import com.garganttua.core.supply.IContextualObjectSupply;
import com.garganttua.core.supply.IObjectSupplier;

public interface ISupplierBuilder<Supplied> extends IObjectSupplierBuilder<Supplied, IObjectSupplier<Supplied>>{

    ISupplierBuilder<Supplied> nullable(boolean nullable);

    <ContextType> ISupplierBuilder<Supplied> withContext(Class<ContextType> contextType, IContextualObjectSupply<Supplied, ContextType> supply) throws DslException;

    ISupplierBuilder<Supplied> withValue(Supplied value) throws DslException;

    ISupplierBuilder<Supplied> withConstructor(IConstructorBinder<Supplied> constructorBinder) throws DslException;

}