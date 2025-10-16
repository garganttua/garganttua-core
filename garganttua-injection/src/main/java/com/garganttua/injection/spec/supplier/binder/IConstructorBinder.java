package com.garganttua.injection.spec.supplier.binder;

public interface IConstructorBinder<Constructed> extends IExecutableBinder<Constructed> {

    Class<Constructed> getConstructedClass();

}
