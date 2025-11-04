package com.garganttua.core.reflection.binders;

public interface IConstructorBinder<Constructed> extends IExecutableBinder<Constructed> {

    Class<Constructed> getConstructedType();

}
