package com.garganttua.core.runtime.dsl;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.condition.ICondition;
import com.garganttua.core.condition.dsl.IConditionBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.context.dsl.AbstractMethodArgInjectBinderBuilder;
import com.garganttua.core.reflection.binders.IContextualMethodBinder;
import com.garganttua.core.reflection.query.ObjectQueryFactory;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.runtime.IRuntime;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.IRuntimeStepCatch;
import com.garganttua.core.runtime.IRuntimeStepMethodBinder;
import com.garganttua.core.runtime.RuntimeStepMethodBinder;
import com.garganttua.core.runtime.annotations.Catch;
import com.garganttua.core.runtime.annotations.Code;
import com.garganttua.core.runtime.annotations.Condition;
import com.garganttua.core.runtime.annotations.Operation;
import com.garganttua.core.runtime.annotations.Output;
import com.garganttua.core.runtime.annotations.Variable;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class RuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> extends
        AbstractMethodArgInjectBinderBuilder<ExecutionReturn, IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType>>
        implements
        IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> {

    private String storeReturnInVariable = null;
    private Boolean output = false;
    private Integer successCode = IRuntime.GENERIC_RUNTIME_SUCCESS_CODE;
    private Map<Class<? extends Throwable>, IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>> katches = new HashMap<>();
    private IObjectSupplierBuilder<StepObjectType, ? extends IObjectSupplier<StepObjectType>> supplier;
    private String stepName;
    private String stageName;
    private String runtimeName;
    private IConditionBuilder conditionBuilder;
    private Boolean abortOnUncatchedException = false;

    protected RuntimeStepMethodBuilder(String runtimeName,
            String stageName, String stepName,
            IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> up,
            IObjectSupplierBuilder<StepObjectType, ? extends IObjectSupplier<StepObjectType>> supplier,
            IInjectableElementResolver resolver)
            throws DslException {
        super(Optional.ofNullable(resolver), up, supplier);
        this.stepName = Objects.requireNonNull(stepName, "Step name cannot be null");
        this.stageName = Objects.requireNonNull(stageName, "Stage name cannot be null");
        this.runtimeName = Objects.requireNonNull(runtimeName, "Runtime name cannot be null");
        this.supplier = supplier;
    }

    @Override
    public IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> condition(
            IConditionBuilder conditionBuilder) {
        this.conditionBuilder = Objects.requireNonNull(conditionBuilder, "Condition builder cannot be null");
        return this;
    }

    @Override
    public RuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> variable(
            String variableName) {
        this.storeReturnInVariable = Objects.requireNonNull(variableName, "Variable name cannot be null");
        return this;
    }

    private IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> katch(
            Class<? extends Throwable> exception, Catch catchAnnotation) throws DslException {
        Objects.requireNonNull(exception, "Exception cannot be null");
        if (!this.isThrown(exception)) {
            throw new DslException("Exception " + exception.getSimpleName() + " is not thrown by method");
        }
        RuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> katch = new RuntimeStepCatchBuilder<>(
                exception, this, catchAnnotation);
        this.katches.put(exception, katch);
        return katch;
    }

    @Override
    public IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> katch(
            Class<? extends Throwable> exception) throws DslException {
        Objects.requireNonNull(exception, "Exception cannot be null");
        if (!this.isThrown(exception)) {
            throw new DslException("Exception " + exception.getSimpleName() + " is not thrown by method");
        }
        RuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> katch = new RuntimeStepCatchBuilder<>(
                exception, this);
        this.katches.put(exception, katch);
        return katch;
    }

    @Override
    public RuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> output(boolean output) {
        this.output = Objects.requireNonNull(output, "Output cannot be null");
        return this;
    }

    @Override
    public boolean isThrown(Class<? extends Throwable> exception) {
        Objects.requireNonNull(exception, "Exception cannot be null");
        return Arrays.stream(this.findMethod().getExceptionTypes()).anyMatch(e -> e.isAssignableFrom(exception));
    }

    @Override
    public void handle(IDiContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        this.setResolver(context);
    }

    @Override
    public IRuntimeStepMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType> build()
            throws DslException {
        Set<IRuntimeStepCatch> builtCatches = this.katches.entrySet().stream().map(b -> b.getValue().build())
                .collect(Collectors.toSet());
        ICondition condition = null;
        if (this.conditionBuilder != null) {
            condition = this.conditionBuilder.build();
        }
        IContextualMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>> binder = (IContextualMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>>) super.build();
        return new RuntimeStepMethodBinder<ExecutionReturn, InputType, OutputType>(this.runtimeName, this.stageName,
                this.stepName, binder,
                Optional.ofNullable(this.storeReturnInVariable), this.output, this.successCode, builtCatches, Optional.ofNullable(condition), abortOnUncatchedException);
    }

    @Override
    public IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> code(Integer code) {
        this.successCode = Objects.requireNonNull(code, "Code cannot be null");
        return this;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        super.doAutoDetection();

        Method method = this.findMethod();
        detectAbortOnUncatchedException(method);
        detectCatches(method);
        detectCondition();
        detectOutput(method);
        detectVariable(method);
        detectCode(method);
    }

    private void detectAbortOnUncatchedException(Method method) {
        Operation operation = method.getAnnotation(Operation.class);
        this.abortOnUncatchedException = operation.abortOnUncatchedException();
    }

    private void detectCatches(Method method) {
        Catch[] catchAnnotations = method.getAnnotationsByType(Catch.class);
        for (Catch catchAnnotation: catchAnnotations) {
            katch(catchAnnotation.exception(), catchAnnotation).autoDetect(true);
        }
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

    private void detectCode(Method operationMethod) {
        Code code = operationMethod.getAnnotation(Code.class);

        if (code != null) {
            method().code(code.value());
        }
    }

    private void detectVariable(Method operationMethod) {
        Variable variable = operationMethod.getAnnotation(Variable.class);

        if (variable != null) {
            method().variable(variable.name());
        }
    }

    private void detectOutput(Method operationMethod) {
        if (operationMethod.getAnnotation(Output.class) != null) {
            method().output(true);
        }
    }

    @Override
    public IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> abortOnUncatchedException(
            boolean abort) {
        this.abortOnUncatchedException = Objects.requireNonNull(abort, "Abort cannot be null");
        return this;
    }
}
