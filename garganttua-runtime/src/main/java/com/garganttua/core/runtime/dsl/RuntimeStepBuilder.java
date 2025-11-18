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
import com.garganttua.core.reflection.query.ObjectQueryFactory;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.runtime.IRuntimeStep;
import com.garganttua.core.runtime.IRuntimeStepCatch;
import com.garganttua.core.runtime.RuntimeStep;
import com.garganttua.core.runtime.annotations.Catch;
import com.garganttua.core.runtime.annotations.Code;
import com.garganttua.core.runtime.annotations.Condition;
import com.garganttua.core.runtime.annotations.FallBack;
import com.garganttua.core.runtime.annotations.Operation;
import com.garganttua.core.runtime.annotations.Output;
import com.garganttua.core.runtime.annotations.Variable;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> extends
        AbstractAutomaticLinkedBuilder<IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStageBuilder<InputType, OutputType>, IRuntimeStep<?, InputType, OutputType>>
        implements IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> {

    private String stepName;
    private String stageName;
    private String runtimeName;
    private IObjectSupplierBuilder<StepObjectType, ? extends IObjectSupplier<StepObjectType>> supplier;
    private IConditionBuilder conditionBuilder;
    private IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> methodBuilder;
    private Class<ExecutionReturn> executionReturn;
    private Set<RuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>> katches = new HashSet<>();
    private IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> fallbackBuilder;
    private IDiContext context;

    public RuntimeStepBuilder(RuntimeStageBuilder<InputType, OutputType> runtimeStageBuilder, String runtimeName,
            String stageName, String stepName,
            Class<ExecutionReturn> executionReturn,
            IObjectSupplierBuilder<StepObjectType, ? extends IObjectSupplier<StepObjectType>> supplier) {
        super(runtimeStageBuilder);
        this.stepName = Objects.requireNonNull(stepName, "Step name cannot be null");
        this.stageName = Objects.requireNonNull(stageName, "Stage name cannot be null");
        this.runtimeName = Objects.requireNonNull(runtimeName, "Runtime name cannot be null");
        this.executionReturn = Objects.requireNonNull(executionReturn, "Execution return type cannot be null");
        this.supplier = Objects.requireNonNull(supplier, "supplier builder cannot be null");
    }

    @Override
    public IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> method()
            throws DslException {
        if (this.methodBuilder == null) {
            this.methodBuilder = new RuntimeStepMethodBuilder<>(this, this.supplier, this.context);
            this.methodBuilder.withReturn(this.executionReturn);
        }
        return this.methodBuilder;

    }

    @Override
    public IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> fallBack()
            throws DslException {
        if (this.katches.isEmpty())
            throw new DslException("No katch defined");
        if (this.fallbackBuilder == null) {
            this.fallbackBuilder = new RuntimeStepFallbackBuilder<>(this, this.supplier, this.context);
            this.fallbackBuilder.withReturn(this.executionReturn);
        }
        return this.fallbackBuilder;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doAutoDetection() throws DslException {
        Method operationMethod = detectOperationMethod(supplier.getSuppliedType());
        this.executionReturn = (Class<ExecutionReturn>) operationMethod.getReturnType();

        method().autoDetect(true).method(operationMethod).withReturn(executionReturn).handle(context);

        if (operationMethod.getAnnotation(Output.class) != null) {
            method().output(true);
        }

        Variable variable = operationMethod.getAnnotation(Variable.class);

        if (variable != null) {
            method().variable(variable.name());
        }

        Code code = operationMethod.getAnnotation(Code.class);

        if (code != null) {
            method().code(code.value());
        }

        Catch catchAnnotation = operationMethod.getAnnotation(Catch.class);
        if (catchAnnotation != null) {
            handleCatch(supplier.getSuppliedType(), catchAnnotation);
        }

        detectCondition();
    }

    private void detectCondition() {
        Optional<StepObjectType> owner = supplier.build().supply();

        if (owner.isEmpty())
            throw new DslException("Owner supplier supplied empty value");
        String conditionField = ObjectReflectionHelper.getFieldAddressAnnotatedWithAndCheckType(
                supplier.getSuppliedType(), Condition.class, IConditionBuilder.class);

        if (conditionField != null) {
            IConditionBuilder condition = (IConditionBuilder) ObjectQueryFactory.objectQuery(owner.get())
                    .getValue(conditionField);
            this.condition(condition);
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
            fallBack().autoDetect(true).method(fallbackMethod).withReturn(executionReturn).handle(context);

            if (fallbackMethod.getAnnotation(Output.class) != null) {
                fallBack().output(true);
            }

            Variable variable = fallbackMethod.getAnnotation(Variable.class);

            if (variable != null) {
                fallBack().variable(variable.name());
            }
        }
    }

    private IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> katch(
            Class<? extends Throwable> exception, Catch catchAnnotation) throws DslException {
        Objects.requireNonNull(exception, "Exception cannot be null");
        Objects.requireNonNull(methodBuilder, "Method is not yet set");
        RuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> katch = new RuntimeStepCatchBuilder<>(
                exception, methodBuilder, this, catchAnnotation);
        this.katches.add(katch);
        return katch;
    }

    @Override
    public IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> katch(
            Class<? extends Throwable> exception) throws DslException {
        Objects.requireNonNull(exception, "Exception cannot be null");
        Objects.requireNonNull(methodBuilder, "Method is not yet set");
        RuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> katch = new RuntimeStepCatchBuilder<>(
                exception, methodBuilder, this);
        this.katches.add(katch);
        return katch;
    }

    @Override
    public IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> condition(
            IConditionBuilder conditionBuilder) {
        this.conditionBuilder = Objects.requireNonNull(conditionBuilder, "Condition builder cannot be null");
        return this;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected IRuntimeStep<ExecutionReturn, InputType, OutputType> doBuild() throws DslException {
        IMethodBinder<ExecutionReturn> fallback = null;
        ICondition condition = null;
        if (this.fallbackBuilder != null) {
            fallback = this.fallbackBuilder.build();
        }
        if (this.conditionBuilder != null) {
            condition = this.conditionBuilder.build();
        }

        Set<IRuntimeStepCatch> builtCatches = this.katches.stream().map(b -> b.build()).collect(Collectors.toSet());

        return new RuntimeStep(runtimeName, stageName, stepName, this.executionReturn, this.methodBuilder.build(),
                Optional.ofNullable(fallback), builtCatches, Optional.ofNullable(condition));
    }

    @Override
    public void handle(IDiContext context) {
        this.context = Objects.requireNonNull(context, "Context cannot be null");
        if (methodBuilder != null) {
            methodBuilder.handle(context);
        }
        if (fallbackBuilder != null) {
            fallbackBuilder.handle(context);
        }
    }
}
