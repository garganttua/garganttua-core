package com.garganttua.core.reflection.binders;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.SupplyException;

public abstract class ExecutableBinder<ReturnedType> implements IExecutableBinder<ReturnedType> {

    protected final List<IObjectSupplier<?>> parameterSuppliers;

    protected ExecutableBinder(List<IObjectSupplier<?>> parameterSuppliers) {
        this.parameterSuppliers = Objects.requireNonNull(parameterSuppliers, "Parameter suppliers cannot be null");
    }

    protected Object[] buildArguments() throws ReflectionException {
        if (parameterSuppliers.isEmpty()) {
            return new Object[0];
        }

        try {
            Object[] args = new Object[parameterSuppliers.size()];
            for (int i = 0; i < parameterSuppliers.size(); i++) {
                args[i] = parameterSuppliers.get(i).supply().orElse(null);
            }
            return args;
        } catch (SupplyException e) {
            throw new ReflectionException(e);
        }
    }

    @Override
    public Set<Class<?>> getDependencies() {
        return new HashSet<>(this.parameterSuppliers.stream().map(supplier -> supplier.getSuppliedType())
                .collect(Collectors.toSet()));
    }
}