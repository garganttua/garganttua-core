package com.garganttua.core.reflection.binders;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.Supplier;
import com.garganttua.core.supply.SupplyException;

public abstract class ContextualExecutableBinder<ReturnedType, Context>
        implements IContextualExecutableBinder<ReturnedType, Context> {
    private static final Logger log = Logger.getLogger(ContextualExecutableBinder.class);

    protected final List<ISupplier<?>> parameterSuppliers;

    protected ContextualExecutableBinder(List<ISupplier<?>> parameterSuppliers) {
        log.trace("Creating ContextualExecutableBinder with {} parameter suppliers", parameterSuppliers.size());
        this.parameterSuppliers = Objects.requireNonNull(parameterSuppliers, "Parameter suppliers cannot be null");
    }

    @Override
    public IClass<?>[] getParametersContextTypes() {
        if (parameterSuppliers.isEmpty()) {
            return new IClass<?>[0];
        }

        return this.parameterSuppliers.stream().map(supplier -> {
            if (supplier instanceof IContextualSupplier<?, ?> contextual) {
                return contextual.getOwnerContextType();
            }
            return (IClass<?>) null;
        }).toArray(IClass<?>[]::new);
    }

    protected Object[] buildArguments(Object... contexts) throws ReflectionException {
        log.trace("Building arguments from {} suppliers", parameterSuppliers.size());
        if (parameterSuppliers.isEmpty()) {
            log.debug("No parameters to build");
            return new Object[0];
        }
        int i = 0;
        try {
            Object[] args = new Object[parameterSuppliers.size()];
            for (i = 0; i < parameterSuppliers.size(); i++) {
                args[i] = Supplier.contextualSupply(parameterSuppliers.get(i), contexts);
                log.trace("Built argument {}: {}", i, args[i]);
            }
            log.debug("Built {} arguments successfully", args.length);
            return args;
        } catch (SupplyException e) {
            log.error("Error building parameter {} argument", i, e);
            throw new ReflectionException("Error on paramerer " + i, e);
        }
    }

    @Override
    public Set<IClass<?>> dependencies() {
        return new HashSet<>(this.parameterSuppliers.stream().map(supplier -> supplier.getSuppliedClass())
                .collect(Collectors.toSet()));
    }

}