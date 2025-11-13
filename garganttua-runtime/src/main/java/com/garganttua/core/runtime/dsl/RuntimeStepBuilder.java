package com.garganttua.core.runtime.dsl;

import java.util.Objects;

import com.garganttua.core.condition.dsl.IConditionBuilder;
import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.runtime.IRuntimeStep;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeStepBuilder<ExecutionReturn, StepObjectType> extends 
        AbstractAutomaticLinkedBuilder<IRuntimeStepBuilder<ExecutionReturn, StepObjectType>, IRuntimeStageBuilder<?,?>, IRuntimeStep<ExecutionReturn>>
        implements IRuntimeStepBuilder<ExecutionReturn, StepObjectType> {

    private String stepName;
    private IObjectSupplierBuilder<StepObjectType, ? extends IObjectSupplier<StepObjectType>> supplier;
    private IConditionBuilder conditionBuilder;

    public RuntimeStepBuilder(RuntimeStageBuilder<StepObjectType, ? extends IObjectSupplier<StepObjectType>> runtimeStageBuilder, String stepName) {
        super(runtimeStageBuilder);
        this.stepName = Objects.requireNonNull(stepName, "Step name cannot be null");
    }

    @Override
    public IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType> method() throws DslException {
        return new RuntimeStepMethodBuilder<>(this, supplier);
    }

    @Override
    public IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType> fallBack() throws DslException {
        return new RuntimeStepFallbackBuilder<>(this, supplier);
    }

    @Override
    public IRuntimeStepBuilder<ExecutionReturn, StepObjectType> variable(String variableName) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'variable'");
    }

    @Override
    public IRuntimeStepBuilder<ExecutionReturn, StepObjectType> output(boolean output) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'output'");
    }

    @Override
    protected IRuntimeStep<ExecutionReturn> doBuild() throws DslException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'doBuild'");
    }

    @Override
    protected void doAutoDetection() throws DslException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'doAutoDetection'");
    }

    @Override
    public IRuntimeStepCatchBuilder katch(Class<? extends Throwable> exception) throws DslException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'katch'");
    }

    @Override
    public IRuntimeStepBuilder<ExecutionReturn, StepObjectType> condition(IConditionBuilder conditionBuilder) {
        this.conditionBuilder = Objects.requireNonNull(conditionBuilder, "Condition builder cannot be null");
        return this;
    }


    /* public IRuntimeStep build() throws DslException {
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
    } */

  /*   @Override
    public <T, ExecutionReturn> IRuntimeStepOperationBuilder<ExecutionReturn> object(
            IObjectSupplierBuilder<T, IObjectSupplier<T>> objectSupplierBuilder, Class<ExecutionReturn> returnType)
            throws DslException {
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
            IObjectSupplierBuilder<T, IObjectSupplier<T>> objectSupplier, Class<ExecutionReturn> returnType,
            RuntimeStepOperationPosition position)
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
    } */


}
