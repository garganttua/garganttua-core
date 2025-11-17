package com.garganttua.core.runtime.dsl;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.condition.ICondition;
import com.garganttua.core.condition.dsl.IConditionBuilder;
import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.runtime.IRuntimeStep;
import com.garganttua.core.runtime.IRuntimeStepCatch;
import com.garganttua.core.runtime.RuntimeStep;
import com.garganttua.core.runtime.annotations.Catch;
import com.garganttua.core.runtime.annotations.FallBack;
import com.garganttua.core.runtime.annotations.Operation;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeStepBuilder<ExecutionReturn, StepObjectType> extends
        AbstractAutomaticLinkedBuilder<IRuntimeStepBuilder<ExecutionReturn, StepObjectType>, IRuntimeStageBuilder<?, ?>, IRuntimeStep>
        implements IRuntimeStepBuilder<ExecutionReturn, StepObjectType> {

    private String stepName;
    private IObjectSupplierBuilder<StepObjectType, ? extends IObjectSupplier<StepObjectType>> supplier;
    private IConditionBuilder conditionBuilder;
    private RuntimeStepMethodBuilder<ExecutionReturn, StepObjectType> methodBuilder;
    private Class<ExecutionReturn> executionReturn;
    private Set<RuntimeStepCatchBuilder> katches = new HashSet<>();
    private RuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType> fallbackBuilder;
    private IDiContext context;

    public RuntimeStepBuilder(RuntimeStageBuilder<?, ?> runtimeStageBuilder, String stepName,
            Class<ExecutionReturn> executionReturn,
            IObjectSupplierBuilder<StepObjectType, ? extends IObjectSupplier<StepObjectType>> supplier) {
        super(runtimeStageBuilder);
        this.stepName = Objects.requireNonNull(stepName, "Step name cannot be null");
        this.executionReturn = Objects.requireNonNull(executionReturn, "Execution return type cannot be null");
        this.supplier = Objects.requireNonNull(supplier, "supplier builder cannot be null");
    }

    @Override
    public IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType> method() throws DslException {
        this.methodBuilder = new RuntimeStepMethodBuilder<>(this, this.supplier, this.context);
        this.methodBuilder.withReturn(this.executionReturn);
        return this.methodBuilder;
    }

    @Override
    public IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType> fallBack() throws DslException {
        if (this.katches.isEmpty())
            throw new DslException("No katch defined");
        this.fallbackBuilder = new RuntimeStepFallbackBuilder<>(this, this.supplier, this.context);
        this.fallbackBuilder.withReturn(this.executionReturn);
        return this.fallbackBuilder;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        Class<?> ownerType = supplier.getSuppliedType();

        Method operationMethod = detectOperationMethod(ownerType);
        method().autoDetect(true).method(operationMethod).handle(context);


        Catch catchAnnotation = operationMethod.getAnnotation(Catch.class);
        if (catchAnnotation != null) {
            handleCatch(ownerType, catchAnnotation);
        }
    }

    private Method detectOperationMethod(Class<?> ownerType) throws DslException {
        Method method = ObjectReflectionHelper.getMethodAnnotatedWith(ownerType, Operation.class);
        if (method == null) {
            throw new DslException("Class " + ownerType.getSimpleName() +
                    " does not declare any @Operation method");
        }
        return method;
    }

    private void handleCatch(Class<?> ownerType, Catch catchAnnotation) {
        katch(catchAnnotation.exception(), catchAnnotation).autoDetect(true);

        Method fallbackMethod = ObjectReflectionHelper.getMethodAnnotatedWith(ownerType, FallBack.class);

        if (fallbackMethod != null) {
            fallBack().autoDetect(true).method(fallbackMethod).handle(context);
        }
    }

    private IRuntimeStepCatchBuilder katch(
            Class<? extends Throwable> exception, Catch catchAnnotation) throws DslException {
        Objects.requireNonNull(exception, "Exception cannot be null");
        Objects.requireNonNull(methodBuilder, "Method is not yet set");
        RuntimeStepCatchBuilder katch = new RuntimeStepCatchBuilder(exception, methodBuilder, this, catchAnnotation);
        this.katches.add(katch);
        return katch;
    }

    @Override
    public IRuntimeStepCatchBuilder katch(Class<? extends Throwable> exception) throws DslException {
        Objects.requireNonNull(exception, "Exception cannot be null");
        Objects.requireNonNull(methodBuilder, "Method is not yet set");
        RuntimeStepCatchBuilder katch = new RuntimeStepCatchBuilder(exception, methodBuilder, this);
        this.katches.add(katch);
        return katch;
    }

    @Override
    public IRuntimeStepBuilder<ExecutionReturn, StepObjectType> condition(IConditionBuilder conditionBuilder) {
        this.conditionBuilder = Objects.requireNonNull(conditionBuilder, "Condition builder cannot be null");
        return this;
    }

    @Override
    protected IRuntimeStep doBuild() throws DslException {
        IMethodBinder<ExecutionReturn> fallback = null;
        ICondition condition = null;
        if (this.fallbackBuilder != null) {
            fallback = this.fallbackBuilder.build();
        }
        if (this.conditionBuilder != null) {
            condition = this.conditionBuilder.build();
        }

        Set<IRuntimeStepCatch> builtCatches = this.katches.stream().map(b -> b.build()).collect(Collectors.toSet());

        return new RuntimeStep<>(stepName, this.executionReturn, this.methodBuilder.build(),
                Optional.ofNullable(fallback), builtCatches, Optional.ofNullable(condition));
    }

    @Override
    public void handle(IDiContext context) {
        this.context = Objects.requireNonNull(context, "Context cannot be null");
        if( methodBuilder != null ){
            methodBuilder.handle(context);
        }
        if( fallbackBuilder != null ){
            fallbackBuilder.handle(context);
        }
    }
}
