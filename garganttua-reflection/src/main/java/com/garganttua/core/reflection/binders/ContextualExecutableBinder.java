package com.garganttua.core.reflection.binders;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.Supplier;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ContextualExecutableBinder<ReturnedType, Context>
        implements IContextualExecutableBinder<ReturnedType, Context> {

    protected final List<ISupplier<?>> parameterSuppliers;

    protected ContextualExecutableBinder(List<ISupplier<?>> parameterSuppliers) {
        log.atTrace().log("Creating ContextualExecutableBinder with {} parameter suppliers", parameterSuppliers.size());
        this.parameterSuppliers = Objects.requireNonNull(parameterSuppliers, "Parameter suppliers cannot be null");
    }

    @Override
    public Class<?>[] getParametersContextTypes() {
        if (parameterSuppliers.isEmpty()) {
            return new Class<?>[0];
        }

        return (Class<?>[]) this.parameterSuppliers.stream().map(supplier -> {
            if (supplier instanceof IContextualSupplier<?, ?> contextual) {
                return contextual.getOwnerContextType();
            }
            return null;
        }).collect(Collectors.toList()).toArray();
    }

    protected Object[] buildArguments(Object... contexts) throws ReflectionException {
        log.atTrace().log("Building arguments from {} suppliers", parameterSuppliers.size());
        if (parameterSuppliers.isEmpty()) {
            log.atDebug().log("No parameters to build");
            return new Object[0];
        }
        int i = 0;
        try {
            Object[] args = new Object[parameterSuppliers.size()];
            for (i = 0; i < parameterSuppliers.size(); i++) {
                args[i] = Supplier.contextualSupply(parameterSuppliers.get(i), contexts);
                log.atTrace().log("Built argument {}: {}", i, args[i]);
            }
            log.atDebug().log("Built {} arguments successfully", args.length);
            return args;
        } catch (SupplyException e) {
            log.atError().log("Error building parameter {} argument", i, e);
            throw new ReflectionException("Error on paramerer " + i, e);
        }
    }

    @Override
    public Set<Class<?>> getDependencies() {
        return new HashSet<>(this.parameterSuppliers.stream().map(supplier -> supplier.getSuppliedClass())
                .collect(Collectors.toSet()));
    }

}