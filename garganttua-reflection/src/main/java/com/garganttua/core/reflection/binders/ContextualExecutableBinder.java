package com.garganttua.core.reflection.binders;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.supplying.IContextualObjectSupplier;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.Supplier;
import com.garganttua.core.supplying.SupplyException;

public abstract class ContextualExecutableBinder<ReturnedType, Context>
        implements IContextualExecutableBinder<ReturnedType, Context> {

    protected final List<IObjectSupplier<?>> parameterSuppliers;

    protected ContextualExecutableBinder(List<IObjectSupplier<?>> parameterSuppliers) {
        this.parameterSuppliers = Objects.requireNonNull(parameterSuppliers, "Parameter suppliers cannot be null");
    }

    @Override
    public Class<?>[] getParametersContextTypes() {
        if (parameterSuppliers.isEmpty()) {
            return new Class<?>[0];
        }

        return (Class<?>[]) this.parameterSuppliers.stream().map(supplier -> {
            if (supplier instanceof IContextualObjectSupplier<?, ?> contextual) {
                return contextual.getOwnerContextType();
            }
            return null;
        }).collect(Collectors.toList()).toArray();
    }

    protected Object[] buildArguments(Object... contexts) throws ReflectionException {
        if (parameterSuppliers.isEmpty()) {
            return new Object[0];
        }
        int i = 0;
        try {
            Object[] args = new Object[parameterSuppliers.size()];
            for (i = 0; i < parameterSuppliers.size(); i++) {
                args[i] = Supplier.contextualSupply(parameterSuppliers.get(i), contexts);
            }
            return args;
        } catch (SupplyException e) {
            throw new ReflectionException("Error on paramerer " + i, e);
        }
    }

    @Override
    public Set<Class<?>> getDependencies() {
        return new HashSet<>(this.parameterSuppliers.stream().map(supplier -> supplier.getSuppliedType())
                .collect(Collectors.toSet()));
    }

}