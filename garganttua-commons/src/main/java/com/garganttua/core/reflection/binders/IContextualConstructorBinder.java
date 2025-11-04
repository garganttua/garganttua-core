package com.garganttua.core.reflection.binders;

public interface IContextualConstructorBinder<Constructed, OwnerContextType> extends IConstructorBinder<Constructed>, IContextualExecutableBinder<Constructed, OwnerContextType> {

}
