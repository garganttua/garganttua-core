package com.garganttua.core.runtime.dsl;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.garganttua.core.condition.ICondition;
import com.garganttua.core.condition.dsl.IConditionBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.context.dsl.AbstractMethodArgInjectBinderBuilder;
import com.garganttua.core.reflection.binders.IContextualMethodBinder;
import com.garganttua.core.reflection.query.ObjectQueryFactory;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.runtime.IRuntime;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.IRuntimeStepMethodBinder;
import com.garganttua.core.runtime.MethodBinderExpression;
import com.garganttua.core.runtime.RuntimeStepMethodBinder;
import com.garganttua.core.runtime.annotations.Catch;
import com.garganttua.core.runtime.annotations.Code;
import com.garganttua.core.runtime.annotations.Condition;
import com.garganttua.core.runtime.annotations.Operation;
import com.garganttua.core.runtime.annotations.Output;
import com.garganttua.core.runtime.annotations.Variable;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> extends
        AbstractMethodArgInjectBinderBuilder<ExecutionReturn, IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType>>
        implements
        IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> {

    private String storeReturnInVariable = null;
    private Boolean output = false;
    private Integer successCode = IRuntime.GENERIC_RUNTIME_SUCCESS_CODE;
    private Map<Class<? extends Throwable>, IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>> katches = new HashMap<>();
    private ISupplierBuilder<StepObjectType, ? extends ISupplier<StepObjectType>> supplier;
    private String stepName;
    private String runtimeName;
    private IConditionBuilder conditionBuilder;
    private Boolean abortOnUncatchedException = false;
    private Boolean nullable = false;

    protected RuntimeStepMethodBuilder(String runtimeName,
            String stepName,
            IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> up,
            ISupplierBuilder<StepObjectType, ? extends ISupplier<StepObjectType>> supplier)
            throws DslException {
        super(up, supplier);
        log.atTrace().log(
                "Entering RuntimeStepMethodBuilder constructor with runtimeName={}, stepName={}",
                runtimeName, stepName);
        this.stepName = Objects.requireNonNull(stepName, "Step name cannot be null");
        this.runtimeName = Objects.requireNonNull(runtimeName, "Runtime name cannot be null");
        this.supplier = supplier;
        log.atDebug().log("RuntimeStepMethodBuilder constructed successfully for step '{}'", stepName);
    }

    @Override
    public IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> condition(
            IConditionBuilder conditionBuilder) {
        log.atTrace().log("Entering condition method");
        this.conditionBuilder = Objects.requireNonNull(conditionBuilder, "Condition builder cannot be null");
        log.atDebug().log("Condition builder set: {}", conditionBuilder);
        return this;
    }

    @Override
    public RuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> variable(
            String variableName) {
        log.atTrace().log("Entering variable method with variableName={}", variableName);
        this.storeReturnInVariable = Objects.requireNonNull(variableName, "Variable name cannot be null");
        log.atDebug().log("Return variable set to '{}'", variableName);
        return this;
    }

    private IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> katch(
            Class<? extends Throwable> exception, Catch catchAnnotation) throws DslException {
        log.atTrace().log("Entering private katch method with exception={}", exception);
        Objects.requireNonNull(exception, "Exception cannot be null");
        if (!this.isThrown(exception)) {
            log.atError().log("Exception {} is not thrown by method", exception.getSimpleName());
            throw new DslException("Exception " + exception.getSimpleName() + " is not thrown by method");
        }
        RuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> katch = new RuntimeStepCatchBuilder<>(
                exception, this, catchAnnotation);
        this.katches.put(exception, katch);
        log.atDebug().log("Katch added for exception {}", exception.getSimpleName());
        return katch;
    }

    @Override
    public IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> katch(
            Class<? extends Throwable> exception) throws DslException {
        log.atTrace().log("Entering public katch method with exception={}", exception);
        Objects.requireNonNull(exception, "Exception cannot be null");
        if (!this.isThrown(exception)) {
            log.atError().log("Exception {} is not thrown by method", exception.getSimpleName());
            throw new DslException("Exception " + exception.getSimpleName() + " is not thrown by method");
        }
        RuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> katch = new RuntimeStepCatchBuilder<>(
                exception, this);
        this.katches.put(exception, katch);
        log.atDebug().log("Katch added for exception {}", exception.getSimpleName());
        return katch;
    }

    @Override
    public IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> output(boolean output) {
        log.atTrace().log("Entering output method with value={}", output);
        this.output = Objects.requireNonNull(output, "Output cannot be null");
        log.atDebug().log("Output set to {}", output);
        return this;
    }

    @Override
    public boolean isThrown(Class<? extends Throwable> exception) {
        log.atTrace().log("Checking if exception {} is thrown", exception);
        Objects.requireNonNull(exception, "Exception cannot be null");
        boolean thrown = Arrays.stream(this.method().getExceptionTypes())
                .anyMatch(e -> e.isAssignableFrom(exception));
        log.atDebug().log("isThrown result for {}: {}", exception.getSimpleName(), thrown);
        return thrown;
    }

    @Override
    public IRuntimeStepMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType> build()
            throws DslException {
        log.atTrace().log("Entering build method");
        IContextualMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>> binder = (IContextualMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>>) super.build();
        MethodBinderExpression<ExecutionReturn, IRuntimeContext<InputType, OutputType>> expression = new MethodBinderExpression<>(binder);
        ICondition condition = null;
        if (this.conditionBuilder != null) {
            condition = this.conditionBuilder.build();
        }
        log.atDebug().log("Building RuntimeStepMethodBinder for step '{}'", this.stepName);
        return new RuntimeStepMethodBinder<ExecutionReturn, InputType, OutputType>(this.runtimeName,
                this.stepName, expression,
                Optional.ofNullable(this.storeReturnInVariable), this.output, this.successCode, this.katches.entrySet().stream().map(b -> b.getValue().build())
                .collect(Collectors.toSet()),
                Optional.ofNullable(condition), this.abortOnUncatchedException, this.nullable,
                binder.getExecutableReference());
    }

    @Override
    public IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> code(Integer code) {
        log.atTrace().log("Entering code method with value={}", code);
        this.successCode = Objects.requireNonNull(code, "Code cannot be null");
        log.atDebug().log("Success code set to {}", code);
        return this;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("Entering doAutoDetection method");
        super.doAutoDetection();

        Method method = this.method();
        detectAbortOnUncatchedException(method);
        detectCatches(method);
        detectCondition();
        detectOutput(method);
        detectVariable(method);
        detectCode(method);
        detectNullable(method);
        log.atDebug().log("Auto-detection completed for method {}", method.getName());
    }

    private void detectNullable(Method operationMethod) {
        log.atTrace().log("Detecting nullable annotation on method {}", operationMethod.getName());
        Nullable nullable = operationMethod.getAnnotation(Nullable.class);
        if (nullable != null) {
            this.nullable = true;
            log.atDebug().log("Method {} marked as nullable", operationMethod.getName());
        }
    }

    private void detectAbortOnUncatchedException(Method method) {
        log.atTrace().log("Detecting abortOnUncatchedException on method {}", method.getName());
        Operation operation = method.getAnnotation(Operation.class);
        this.abortOnUncatchedException = operation.abortOnUncatchedException();
        log.atDebug().log("abortOnUncatchedException set to {}", this.abortOnUncatchedException);
    }

    private void detectCatches(Method method) {
        log.atTrace().log("Detecting catches on method {}", method.getName());
        Catch[] catchAnnotations = method.getAnnotationsByType(Catch.class);
        for (Catch catchAnnotation : catchAnnotations) {
            katch(catchAnnotation.exception(), catchAnnotation).autoDetect(true);
            log.atDebug().log("Detected catch annotation for exception {}",
                    catchAnnotation.exception().getSimpleName());
        }
    }

    private void detectCondition() {
        log.atTrace().log("Detecting condition");
        Optional<StepObjectType> owner = supplier.build().supply();
        if (owner.isEmpty()) {
            log.atError().log("Owner supplier supplied empty value");
            throw new DslException("Owner supplier supplied empty value");
        }
        String conditionField = ObjectReflectionHelper.getFieldAddressAnnotatedWithAndCheckType(
                supplier.getSuppliedClass(), Condition.class, IConditionBuilder.class);

        if (conditionField != null) {
            IConditionBuilder condition = (IConditionBuilder) ObjectQueryFactory.objectQuery(owner.get())
                    .getValue(conditionField);
            this.condition(condition);
            log.atDebug().log("Condition detected and applied");
        }
    }

    private void detectCode(Method operationMethod) {
        log.atTrace().log("Detecting code annotation on method {}", operationMethod.getName());
        Code code = operationMethod.getAnnotation(Code.class);
        if (code != null) {
            this.code(code.value());
            log.atDebug().log("Code annotation detected with value {}", code.value());
        }
    }

    private void detectVariable(Method operationMethod) {
        log.atTrace().log("Detecting variable annotation on method {}", operationMethod.getName());
        Variable variable = operationMethod.getAnnotation(Variable.class);
        if (variable != null) {
            this.variable(variable.name());
            log.atDebug().log("Variable annotation detected with name {}", variable.name());
        }
    }

    private void detectOutput(Method operationMethod) {
        log.atTrace().log("Detecting output annotation on method {}", operationMethod.getName());
        if (operationMethod.getAnnotation(Output.class) != null) {
            this.output(true);
            log.atDebug().log("Output annotation detected and set to true");
        }
    }

    @Override
    public IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> abortOnUncatchedException(
            boolean abort) {
        log.atTrace().log("Entering abortOnUncatchedException method with value={}", abort);
        this.abortOnUncatchedException = Objects.requireNonNull(abort, "Abort cannot be null");
        log.atDebug().log("abortOnUncatchedException set to {}", abort);
        return this;
    }

    @Override
    public IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> nullable(
            boolean nullable) {
        log.atTrace().log("Entering nullable method with value={}", nullable);
        this.nullable = Objects.requireNonNull(nullable, "Nullable cannot be null");
        log.atDebug().log("Nullable set to {}", nullable);
        return this;
    }
}