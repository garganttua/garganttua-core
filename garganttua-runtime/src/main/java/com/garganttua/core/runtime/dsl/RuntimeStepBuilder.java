package com.garganttua.core.runtime.dsl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import com.garganttua.core.dsl.AbstractLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.runtime.IRuntimeStep;
import com.garganttua.core.runtime.Position;
import com.garganttua.core.runtime.RuntimeStep;
import com.garganttua.core.runtime.RuntimeStepOperationPosition;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeStepBuilder extends AbstractLinkedBuilder<IRuntimeStageBuilder, IRuntimeStep>
        implements IRuntimeStepBuilder {

    private String stepName;
    private final Map<Class<?>, IRuntimeStepOperationBuilder<?>> operations = new LinkedHashMap<>();

    public RuntimeStepBuilder(RuntimeStageBuilder runtimeStageBuilder, String stepName) {
        super(runtimeStageBuilder);
        this.stepName = Objects.requireNonNull(stepName, "Step name cannot be null");
    }

    public IRuntimeStep build() throws DslException {
        log.info("Building step [{}] with {} operation(s)", stepName, operations.size());

        Map<Class<?>, IMethodBinder<?>> builtBinders = new LinkedHashMap<>();

        for (Map.Entry<Class<?>, IRuntimeStepOperationBuilder<?>> entry : operations.entrySet()) {
            Class<?> key = entry.getKey();
            IRuntimeStepOperationBuilder<?> builder = entry.getValue();
            Objects.requireNonNull(builder, "Binder builder for operation " + key + " cannot be null");

            IMethodBinder<?> binder = builder.build();
            builtBinders.put(key, binder);
        }

        return new RuntimeStep(stepName, builtBinders);
    }

    @Override
    public <T, ExecutionReturn> IRuntimeStepOperationBuilder<ExecutionReturn> object(
            IObjectSupplierBuilder<T, IObjectSupplier<T>> objectSupplierBuilder, Class<ExecutionReturn> returnType) throws DslException {
        Objects.requireNonNull(objectSupplierBuilder, "Object supplier builder cannot be null");

        Class<T> key = objectSupplierBuilder.getSuppliedType();
        if (operations.containsKey(key)) {
            throw new IllegalArgumentException("Operation already exists in step [" + stepName + "]: " + key);
        }

        IRuntimeStepOperationBuilder<ExecutionReturn> operationStepBuilder = new RuntimeOperationStepBuilder<>(this,
                objectSupplierBuilder);
                operationStepBuilder.withReturn(returnType);
        operations.put(key, operationStepBuilder);

        log.info("Added operation [{}] to step [{}]", key, stepName);
        return operationStepBuilder;
    }

    @Override
    public <T, ExecutionReturn> IRuntimeStepOperationBuilder<ExecutionReturn> object(
            IObjectSupplierBuilder<T, IObjectSupplier<T>> objectSupplier, Class<ExecutionReturn> returnType, RuntimeStepOperationPosition position)
            throws DslException {
        Objects.requireNonNull(objectSupplier, "Object supplier builder cannot be null");
        Objects.requireNonNull(position, "RuntimeStepOperationPosition cannot be null");

        Class<T> key = objectSupplier.getSuppliedType();

        if (operations.containsKey(key)) {
            throw new IllegalArgumentException("Operation already exists in step [" + stepName + "]: " + key);
        }
        IRuntimeStepOperationBuilder<ExecutionReturn> operationStepBuilder = new RuntimeOperationStepBuilder<>(this,
                objectSupplier);
                operationStepBuilder.withReturn(returnType);

        Map<Class<?>, IRuntimeStepOperationBuilder<?>> reordered = new LinkedHashMap<>();
        boolean inserted = false;

        for (Entry<Class<?>, IRuntimeStepOperationBuilder<?>> entry : operations.entrySet()) {
            Class<?> existingKey = entry.getKey();

            if (position.position() == Position.BEFORE && existingKey.equals(position.element())) {
                reordered.put(key, operationStepBuilder);
                inserted = true;
            }

            reordered.put(existingKey, entry.getValue());

            if (position.position() == Position.AFTER && existingKey.equals(position.element())) {
                reordered.put(key, operationStepBuilder);
                inserted = true;
            }
        }

        if (!inserted) {
            reordered.put(key, operationStepBuilder);
            log.warn("Reference operation [{}] not found — inserted [{}] at the end of step [{}]",
                    position.element(), key, stepName);
        }

        operations.clear();
        operations.putAll(reordered);

        log.info("Added operation [{}] {} [{}] in step [{}]",
                key, position.position(), position.element(), stepName);

        return operationStepBuilder;
    }

}
