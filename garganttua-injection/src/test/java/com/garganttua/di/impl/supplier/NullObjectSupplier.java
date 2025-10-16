package com.garganttua.di.impl.supplier;

import java.util.Optional;

import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.supplier.IObjectSupplier;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NullObjectSupplier<T> implements IObjectSupplier<T>{

    private Class<T> class1;

    @Override
    public Optional<T> getObject() throws DiException {
        return Optional.empty();
    }

    @Override
    public Class<T> getObjectClass() {
        return this.class1;
    }

}
