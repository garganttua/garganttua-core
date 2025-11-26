package com.garganttua.core.injection;

import com.garganttua.core.reflection.binders.Dependent;
import com.garganttua.core.supply.IObjectSupplier;

public interface IBeanSupplier<Bean> extends IObjectSupplier<Bean>, Dependent {

}
