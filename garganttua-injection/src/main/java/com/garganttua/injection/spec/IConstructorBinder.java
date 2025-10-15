package com.garganttua.injection.spec;

import com.garganttua.injection.spec.supplier.binder.IExecutableBinder;

public interface IConstructorBinder<Constructed> extends IExecutableBinder<Constructed> {

    Class<Constructed> getConstructedClass();

}
