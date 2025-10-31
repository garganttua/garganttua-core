package com.garganttua.injection;

import com.garganttua.injection.spec.supplier.IObjectSupplier;
import com.garganttua.injection.spec.supplier.binder.Dependent;

public interface IBeanSupplier<Bean> extends IObjectSupplier<Bean>, Dependent {

}
