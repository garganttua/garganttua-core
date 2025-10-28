package com.garganttua.injection.supplier;

import java.util.Optional;

import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.supplier.IObjectSupplier;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NullObjectSupplier<T> implements IObjectSupplier<T>{

    private Class<T> objectClass;

    @Override
    public Optional<T> getObject() throws DiException {
        return Optional.empty();
    }

    @Override
    public Class<T> getObjectClass() {
        return this.objectClass;
    }

}
