package com.garganttua.core.reflection.binders;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ExecutableBinder<ReturnedType> implements IExecutableBinder<ReturnedType> {

    protected final List<ISupplier<?>> parameterSuppliers;

    protected ExecutableBinder(List<ISupplier<?>> parameterSuppliers) {
        log.atTrace().log("Creating ExecutableBinder with {} parameter suppliers", parameterSuppliers.size());
        this.parameterSuppliers = Objects.requireNonNull(parameterSuppliers, "Parameter suppliers cannot be null");
    }

    protected Object[] buildArguments() throws ReflectionException {
        log.atTrace().log("Building arguments from {} suppliers", parameterSuppliers.size());
        if (parameterSuppliers.isEmpty()) {
            log.atDebug().log("No parameters to build");
            return new Object[0];
        }
        int i = 0;
        try {
            Object[] args = new Object[parameterSuppliers.size()];
            for (i = 0; i < parameterSuppliers.size(); i++) {
                args[i] = parameterSuppliers.get(i).supply().orElse(null);
                log.atTrace().log("Built argument {}: {}", i, args[i]);
            }
            log.atDebug().log("Built {} arguments successfully", args.length);
            return args;
        } catch (SupplyException e) {
            log.atError().log("Error building parameter {} argument", i, e);
            throw new ReflectionException("Error on parameter "+i, e);
        }
    }

    @Override
    public Set<Class<?>> getDependencies() {
        log.atTrace().log("Getting dependencies from parameter suppliers");
        Set<Class<?>> dependencies = new HashSet<>(this.parameterSuppliers.stream().map(supplier -> supplier.getSuppliedClass())
                .collect(Collectors.toSet()));
        log.atDebug().log("Found {} dependencies", dependencies.size());
        return dependencies;
    }
}