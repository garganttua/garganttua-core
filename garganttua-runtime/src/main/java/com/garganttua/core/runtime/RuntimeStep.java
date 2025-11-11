package com.garganttua.core.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.garganttua.core.reflection.binders.IMethodBinder;

public class RuntimeStep implements IRuntimeStep {

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

    @SuppressWarnings("unchecked")
    @Override
    public <T> IMethodBinder<T> getBinder(Class<T> clazz) {
        return (IMethodBinder<T>) binders.get(clazz);
    }

    @Override
    public Map<Class<?>, IMethodBinder<?>> getBinders() {
        return Collections.unmodifiableMap(binders);
    }
}
