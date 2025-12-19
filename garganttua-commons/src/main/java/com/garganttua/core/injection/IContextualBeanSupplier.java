package com.garganttua.core.injection;

import com.garganttua.core.supply.IContextualSupplier;

public interface IContextualBeanSupplier<Bean> extends IContextualSupplier<Bean, IDiContext>, IBeanSupplier<Bean> {

}
