package com.garganttua.injection.supplier.binder;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.reflection.binders.IExecutableBinder;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.injection.supplier.Supplier;

public abstract class ExecutableBinder<Constructed, Context> implements IExecutableBinder<Constructed> {

    protected final List<IObjectSupplier<?>> parameterSuppliers;

    protected ExecutableBinder(List<IObjectSupplier<?>> parameterSuppliers) {
        this.parameterSuppliers = Objects.requireNonNull(parameterSuppliers, "Parameter suppliers cannot be null");
    }

    protected Object[] buildArguments(Context context) throws DiException {
        if (parameterSuppliers.isEmpty()) {
            return new Object[0];
        }

        Object[] args = new Object[parameterSuppliers.size()];
        for (int i = 0; i < parameterSuppliers.size(); i++) {
            args[i] = Supplier.getObject(parameterSuppliers.get(i), context);
        }
        return args;
    }

    @Override
    public Optional<Constructed> execute() throws DiException {
        return this.execute(null);
    }

    @Override
    public Set<Class<?>> getDependencies() {
        return new HashSet<>(this.parameterSuppliers.stream().map(supplier -> supplier.getObjectClass()).collect(Collectors.toSet()));
    }
}