package com.garganttua.injection.spec.supplier.binder;

import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.supplier.IObjectSupplier;

public interface IInjectableField<OnwerType> {

    void inject(IObjectSupplier<OnwerType> ownerSupplier) throws DiException;

}
