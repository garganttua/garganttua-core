package com.garganttua.core.runtime.dsl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.condition.dsl.IConditionBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.context.dsl.AbstractMethodArgInjectBinderBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.binders.IContextualMethodBinder;
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
import com.garganttua.core.reflection.annotations.Reflected;

import jakarta.annotation.Nullable;

@Reflected
public class RuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> extends
        AbstractMethodArgInjectBinderBuilder<ExecutionReturn, IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType>>
        implements
        IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> {
    private static final Logger log = Logger.getLogger(RuntimeStepMethodBuilder.class);

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
        log.trace(
                "Entering RuntimeStepMethodBuilder constructor with runtimeName={}, stepName={}",
                runtimeName, stepName);
        this.stepName = Objects.requireNonNull(stepName, "Step name cannot be null");
        this.runtimeName = Objects.requireNonNull(runtimeName, "Runtime name cannot be null");
        this.supplier = supplier;
        log.debug("RuntimeStepMethodBuilder constructed successfully for step '{}'", stepName);
    }

    @Override
    public IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> condition(
            IConditionBuilder conditionBuilder) {
        log.trace("Entering condition method");
        this.conditionBuilder = Objects.requireNonNull(conditionBuilder, "Condition builder cannot be null");
        log.debug("Condition builder set: {}", conditionBuilder);
        return this;
    }

    @Override
    public RuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> variable(
            String variableName) {
        log.trace("Entering variable method with variableName={}", variableName);
        this.storeReturnInVariable = Objects.requireNonNull(variableName, "Variable name cannot be null");
        log.debug("Return variable set to '{}'", variableName);
        return this;
    }

    private IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> katch(
            Class<? extends Throwable> exception, Catch catchAnnotation) throws DslException {
        log.trace("Entering private katch method with exception={}", exception);
        Objects.requireNonNull(exception, "Exception cannot be null");
        IClass<? extends Throwable> iException = IClass.getClass(exception);
        if (!this.isThrown(iException)) {
            log.error("Exception {} is not thrown by method", exception.getSimpleName());
            throw new DslException("Exception " + exception.getSimpleName() + " is not thrown by method");
        }
        RuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> katch = new RuntimeStepCatchBuilder<>(
                exception, this, catchAnnotation);
        this.katches.put(exception, katch);
        log.debug("Katch added for exception {}", exception.getSimpleName());
        return katch;
    }

    @Override
    public IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> katch(
            IClass<? extends Throwable> exception) throws DslException {
        log.trace("Entering public katch method with exception={}", exception);
        Objects.requireNonNull(exception, "Exception cannot be null");
        if (!this.isThrown(exception)) {
            log.error("Exception {} is not thrown by method", exception.getSimpleName());
            throw new DslException("Exception " + exception.getSimpleName() + " is not thrown by method");
        }
        Class<? extends Throwable> rawException = (Class<? extends Throwable>) exception.getType();
        RuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> katch = new RuntimeStepCatchBuilder<>(
                rawException, this);
        this.katches.put(rawException, katch);
        log.debug("Katch added for exception {}", exception.getSimpleName());
        return katch;
    }

    @Override
    public IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> output(boolean output) {
        log.trace("Entering output method with value={}", output);
        this.output = Objects.requireNonNull(output, "Output cannot be null");
        log.debug("Output set to {}", output);
        return this;
    }

    @Override
    public boolean isThrown(IClass<? extends Throwable> exception) {
        log.trace("Checking if exception {} is thrown", exception);
        Objects.requireNonNull(exception, "Exception cannot be null");
        boolean thrown = Arrays.stream(this.method().getExceptionTypes())
                .anyMatch(e -> e.isAssignableFrom(exception));
        log.debug("isThrown result for {}: {}", exception.getSimpleName(), thrown);
        return thrown;
    }

    @Override
    public IRuntimeStepMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType> build()
            throws DslException {
        log.trace("Entering build method");
        IContextualMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>> binder = (IContextualMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>>) super.build();
        MethodBinderExpression<ExecutionReturn, IRuntimeContext<InputType, OutputType>> expression = new MethodBinderExpression<>(binder);
        ICondition condition = null;
        if (this.conditionBuilder != null) {
            condition = this.conditionBuilder.build();
        }
        log.debug("Building RuntimeStepMethodBinder for step '{}'", this.stepName);
        return new RuntimeStepMethodBinder<ExecutionReturn, InputType, OutputType>(this.runtimeName,
                this.stepName, expression,
                Optional.ofNullable(this.storeReturnInVariable), this.output, this.successCode, this.katches.entrySet().stream().map(b -> b.getValue().build())
                .collect(Collectors.toSet()),
                Optional.ofNullable(condition), this.abortOnUncatchedException, this.nullable,
                binder.getExecutableReference());
    }

    @Override
    public IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> code(Integer code) {
        log.trace("Entering code method with value={}", code);
        this.successCode = Objects.requireNonNull(code, "Code cannot be null");
        log.debug("Success code set to {}", code);
        return this;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.trace("Entering doAutoDetection method");
        super.doAutoDetection();

        IMethod method = this.method();
        detectAbortOnUncatchedException(method);
        detectCatches(method);
        detectCondition();
        detectOutput(method);
        detectVariable(method);
        detectCode(method);
        detectNullable(method);
        log.debug("Auto-detection completed for method {}", method.getName());
    }

    private void detectNullable(IMethod operationMethod) {
        log.trace("Detecting nullable annotation on method {}", operationMethod.getName());
        Nullable nullable = operationMethod.getAnnotation(IClass.getClass(Nullable.class));
        if (nullable != null) {
            this.nullable = true;
            log.debug("Method {} marked as nullable", operationMethod.getName());
        }
    }

    private void detectAbortOnUncatchedException(IMethod method) {
        log.trace("Detecting abortOnUncatchedException on method {}", method.getName());
        Operation operation = method.getAnnotation(IClass.getClass(Operation.class));
        this.abortOnUncatchedException = operation.abortOnUncatchedException();
        log.debug("abortOnUncatchedException set to {}", this.abortOnUncatchedException);
    }

    private void detectCatches(IMethod method) {
        log.trace("Detecting catches on method {}", method.getName());
        Catch[] catchAnnotations = method.getAnnotationsByType(IClass.getClass(Catch.class));
        for (Catch catchAnnotation : catchAnnotations) {
            katch(catchAnnotation.exception(), catchAnnotation).autoDetect(true);
            log.debug("Detected catch annotation for exception {}",
                    catchAnnotation.exception().getSimpleName());
        }
    }

    @SuppressWarnings("unchecked")
    private void detectCondition() {
        log.trace("Detecting condition");
        Optional<StepObjectType> owner = supplier.build().supply();
        if (owner.isEmpty()) {
            log.error("Owner supplier supplied empty value");
            throw new DslException("Owner supplier supplied empty value");
        }
        IClass<?> suppliedClass = supplier.getSuppliedClass();
        IClass<Condition> iConditionAnnotation = (IClass<Condition>) (IClass<?>) IClass.getClass(Condition.class);
        IField conditionField = null;
        for (IField field : suppliedClass.getDeclaredFields()) {
            if (field.getAnnotation(iConditionAnnotation) != null) {
                if (IClass.getClass(IConditionBuilder.class).isAssignableFrom(field.getType())) {
                    conditionField = field;
                    break;
                }
            }
        }

        if (conditionField != null) {
            try {
                conditionField.setAccessible(true);
                IConditionBuilder condition = (IConditionBuilder) conditionField.get(owner.get());
                this.condition(condition);
                log.debug("Condition detected and applied");
            } catch (IllegalAccessException e) {
                throw new DslException("Failed to access condition field", e);
            }
        }
    }

    private void detectCode(IMethod operationMethod) {
        log.trace("Detecting code annotation on method {}", operationMethod.getName());
        Code code = operationMethod.getAnnotation(IClass.getClass(Code.class));
        if (code != null) {
            this.code(code.value());
            log.debug("Code annotation detected with value {}", code.value());
        }
    }

    private void detectVariable(IMethod operationMethod) {
        log.trace("Detecting variable annotation on method {}", operationMethod.getName());
        Variable variable = operationMethod.getAnnotation(IClass.getClass(Variable.class));
        if (variable != null) {
            this.variable(variable.name());
            log.debug("Variable annotation detected with name {}", variable.name());
        }
    }

    private void detectOutput(IMethod operationMethod) {
        log.trace("Detecting output annotation on method {}", operationMethod.getName());
        if (operationMethod.getAnnotation(IClass.getClass(Output.class)) != null) {
            this.output(true);
            log.debug("Output annotation detected and set to true");
        }
    }

    @Override
    public IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> abortOnUncatchedException(
            boolean abort) {
        log.trace("Entering abortOnUncatchedException method with value={}", abort);
        this.abortOnUncatchedException = Objects.requireNonNull(abort, "Abort cannot be null");
        log.debug("abortOnUncatchedException set to {}", abort);
        return this;
    }

    @Override
    public IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> nullable(
            boolean nullable) {
        log.trace("Entering nullable method with value={}", nullable);
        this.nullable = Objects.requireNonNull(nullable, "Nullable cannot be null");
        log.debug("Nullable set to {}", nullable);
        return this;
    }
}
