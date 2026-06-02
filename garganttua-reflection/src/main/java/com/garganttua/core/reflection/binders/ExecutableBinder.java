package com.garganttua.core.reflection.binders;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Base class for non-contextual executable binders (constructors and methods). Holds the parameter
 * suppliers and resolves them into an argument array via {@link #buildArguments()}.
 *
 * @param <ReturnedType> the type produced by executing the bound member
 */
public abstract class ExecutableBinder<ReturnedType> implements IExecutableBinder<ReturnedType> {
    private static final Logger log = Logger.getLogger(ExecutableBinder.class);

    protected final List<ISupplier<?>> parameterSuppliers;

    /**
     * @param parameterSuppliers suppliers producing the member arguments, in declaration order
     */
    protected ExecutableBinder(List<ISupplier<?>> parameterSuppliers) {
        log.trace("Creating ExecutableBinder with {} parameter suppliers", parameterSuppliers.size());
        this.parameterSuppliers = Objects.requireNonNull(parameterSuppliers, "Parameter suppliers cannot be null");
    }

    /**
     * Resolves every parameter supplier to produce the argument array.
     *
     * @return the resolved arguments, in declaration order
     * @throws ReflectionException if a supplier fails to supply its value
     */
    protected Object[] buildArguments() throws ReflectionException {
        log.trace("Building arguments from {} suppliers", parameterSuppliers.size());
        if (parameterSuppliers.isEmpty()) {
            log.debug("No parameters to build");
            return new Object[0];
        }
        int i = 0;
        try {
            Object[] args = new Object[parameterSuppliers.size()];
            for (i = 0; i < parameterSuppliers.size(); i++) {
                args[i] = parameterSuppliers.get(i).supply().orElse(null);
                log.trace("Built argument {}: {}", i, args[i]);
            }
            log.debug("Built {} arguments successfully", args.length);
            return args;
        } catch (SupplyException e) {
            log.error("Error building parameter {} argument", i, e);
            throw new ReflectionException("Error on parameter "+i, e);
        }
    }

    /** {@return the set of supplied classes of all parameter suppliers, used for dependency ordering} */
    @Override
    public Set<IClass<?>> dependencies() {
        log.trace("Getting dependencies from parameter suppliers");
        Set<IClass<?>> dependencies = new HashSet<>(this.parameterSuppliers.stream().map(supplier -> supplier.getSuppliedClass())
                .collect(Collectors.toSet()));
        log.debug("Found {} dependencies", dependencies.size());
        return dependencies;
    }
}