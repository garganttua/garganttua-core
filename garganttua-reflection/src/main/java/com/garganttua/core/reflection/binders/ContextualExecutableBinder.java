package com.garganttua.core.reflection.binders;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.supplying.IObjectSupplier;

public abstract class ContextualExecutableBinder<Constructed, Context> implements IContextualExecutableBinder<Constructed, Context> {

    protected final List<IObjectSupplier<?>> parameterSuppliers;

    protected ContextualExecutableBinder(List<IObjectSupplier<?>> parameterSuppliers) {
        this.parameterSuppliers = Objects.requireNonNull(parameterSuppliers, "Parameter suppliers cannot be null");
    }

    protected Object[] buildArguments(Context context) throws ReflectionException {
        if (parameterSuppliers.isEmpty()) {
            return new Object[0];
        }

        Object[] args = new Object[parameterSuppliers.size()];
        for (int i = 0; i < parameterSuppliers.size(); i++) {
            args[i] = Supplier.contextualSupply(parameterSuppliers.get(i), context);
        }
        return args;
    }

    @Override
    public Optional<Constructed> execute(Context context) {
        return this.execute(null);
    }

    @Override
    public Set<Class<?>> getDependencies() {
        return new HashSet<>(this.parameterSuppliers.stream().map(supplier -> supplier.getSuppliedType()).collect(Collectors.toSet()));
    }
}