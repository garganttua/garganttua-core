package com.garganttua.core.reflection.binders;

public interface IContextualConstructorBinder<Constructed> extends IConstructorBinder<Constructed>, IContextualExecutableBinder<Constructed, Void> {

    @Override
    default Class<Void> getOwnerContextType(){
        return Void.class;
    }

}
