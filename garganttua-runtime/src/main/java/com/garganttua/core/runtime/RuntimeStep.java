package com.garganttua.core.runtime;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.IMethodBinder;

public class RuntimeStep<ExecutionReturn> implements IRuntimeStep<ExecutionReturn> {

    private final String stepName;
    private final Map<Class<?>, IMethodBinder<?>> binders = new LinkedHashMap<>();

    public RuntimeStep(String stepName, Map<Class<?>, IMethodBinder<?>> binders) {
        this.stepName = Objects.requireNonNull(stepName, "Step name cannot be null");
        if (binders != null) {
            this.binders.putAll(binders);
        }
    }

    @Override
    public String getStepName() {
        return stepName;
    }

    @Override
    public Optional<ExecutionReturn> execute() throws ReflectionException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'execute'");
    }

    @Override
    public Set<Class<?>> getDependencies() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDependencies'");
    }

    /*     @SuppressWarnings("unchecked")
    @Override
    public <T> IMethodBinder<T> getBinder(Class<T> clazz) {
        return (IMethodBinder<T>) binders.get(clazz);
    }

    @Override
    public Map<Class<?>, IMethodBinder<?>> getBinders() {
        return Collections.unmodifiableMap(binders);
    } */
}
