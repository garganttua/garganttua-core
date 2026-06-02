package com.garganttua.core.runtime.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.context.dsl.AbstractMethodArgInjectBinderBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.binders.IContextualMethodBinder;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.IRuntimeStepFallbackBinder;
import com.garganttua.core.runtime.MethodBinderExpression;
import com.garganttua.core.runtime.RuntimeStepFallbackBinder;
import com.garganttua.core.runtime.annotations.OnException;
import com.garganttua.core.runtime.annotations.Output;
import com.garganttua.core.runtime.annotations.Variable;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.reflection.annotations.Reflected;

import jakarta.annotation.Nullable;

/**
 * Fluent builder for a step's {@code @FallBack} method, invoked when the operation
 * raises a matching exception.
 *
 * <p>Configures the fallback's output flag, result variable, nullability and the
 * set of {@code @OnException} handlers, and binds the fallback method via the
 * injection context.</p>
 *
 * @param <ExecutionReturn> the fallback method return type
 * @param <StepObjectType>  the type of the object holding the step methods
 * @param <InputType>       the runtime input type
 * @param <OutputType>      the runtime output type
 */
@Reflected
public class RuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> extends
        AbstractMethodArgInjectBinderBuilder<ExecutionReturn, IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepFallbackBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType>>
        implements IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> {
    private static final Logger log = Logger.getLogger(RuntimeStepFallbackBuilder.class);

    private String storeReturnInVariable = null;
    private Boolean output = false;
    private List<IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>> onExceptions = new ArrayList<>();
    private String stepName;
    private String runtimeName;
    private Boolean nullable = false;

    /**
     * Creates a fallback builder.
     *
     * @param runtimeName the owning runtime name
     * @param stepName    the owning step name
     * @param up          the parent step builder
     * @param supplier    supplier of the object whose fallback method is invoked
     * @throws DslException if the underlying binder builder cannot be initialized
     */
    protected RuntimeStepFallbackBuilder(String runtimeName,
            String stepName,
            IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> up,
            ISupplierBuilder<StepObjectType, ? extends ISupplier<StepObjectType>> supplier)
            throws DslException {
        super(up, supplier);
        this.stepName = Objects.requireNonNull(stepName, "Step name cannot be null");
        this.runtimeName = Objects.requireNonNull(runtimeName, "Runtime name cannot be null");
        log.trace("{} Initialized RuntimeStepFallbackBuilder", logLineHeader());
    }

    /**
     * Stores the fallback's return value in the named runtime variable.
     *
     * @param variableName the variable name
     * @return this builder for chaining
     */
    @Override
    public IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> variable(
            String variableName) {
        this.storeReturnInVariable = Objects.requireNonNull(variableName, "Variable name cannot be null");
        log.debug("{} Variable set for fallback", logLineHeader());
        return this;
    }

    /**
     * Marks whether the fallback's return value is the runtime output.
     *
     * @param output {@code true} to treat the return value as the runtime output
     * @return this builder for chaining
     */
    @Override
    public IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> output(boolean output) {
        this.output = Objects.requireNonNull(output, "Output cannot be null");
        log.debug("{} Output flag set for fallback", logLineHeader());
        return this;
    }

    /**
     * Builds the fallback binder from the configured method, handlers and flags.
     *
     * @return the built {@link IRuntimeStepFallbackBinder}
     * @throws DslException if the underlying method binding cannot be built
     */
    @Override
    public IRuntimeStepFallbackBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType> build()
            throws DslException {
        log.trace("{} Entering build() method", logLineHeader());
        IContextualMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>> binder = (IContextualMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>>) super.build();
        MethodBinderExpression<ExecutionReturn, IRuntimeContext<InputType, OutputType>> expression = new MethodBinderExpression<>(binder);
        RuntimeStepFallbackBinder<ExecutionReturn, InputType, OutputType> fallbackBinder = new RuntimeStepFallbackBinder<ExecutionReturn, InputType, OutputType>(
                this.runtimeName,
                this.stepName, expression, Optional.ofNullable(this.storeReturnInVariable), this.output,
                this.onExceptions.stream().map(b -> b.build()).collect(Collectors.toList()), this.nullable,
                binder.getExecutableReference());
        log.debug("{} RuntimeStepFallbackBinder built successfully", logLineHeader());
        log.trace("{} Exiting build() method", logLineHeader());
        return fallbackBinder;
    }

    /**
     * Declares an exception this fallback handles.
     *
     * @param exception the exception type to handle
     * @return the on-exception builder for further configuration
     * @throws DslException if the handler cannot be created
     */
    @Override
    public IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> onException(
            IClass<? extends Throwable> exception) throws DslException {
        Objects.requireNonNull(exception, "Exception cannot be null");
        Class<? extends Throwable> rawException = (Class<? extends Throwable>) exception.getType();
        IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> onException = new RuntimeStepOnExceptionBuilder<>(
                this, this.runtimeName, rawException);
        this.onExceptions.add(onException);
        log.debug("{} Added onException handler", logLineHeader());
        return onException;
    }

    /**
     * Declares an exception this fallback handles, seeded with an
     * {@link OnException} annotation for auto-detection.
     *
     * @param exception    the exception type to handle
     * @param oneException the source {@link OnException} annotation
     * @return the on-exception builder for further configuration
     * @throws DslException if the handler cannot be created
     */
    public IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> onException(
            Class<? extends Throwable> exception, OnException oneException) throws DslException {
        IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> onException = new RuntimeStepOnExceptionBuilder<>(
                this, this.runtimeName, Objects.requireNonNull(exception, "Exception cannot be null"),
                Objects.requireNonNull(oneException, "On exception annotation cannot be null"));
        this.onExceptions.add(onException);
        log.debug("{} Added onException handler with annotation", logLineHeader());
        return onException;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.trace("{} Starting auto-detection for fallback builder", logLineHeader());
        super.doAutoDetection();

        IMethod method = this.method();
        detectOutput(method);
        detectVariable(method);
        detectOnExceptions(method);
        detectNullable(method);
        log.trace("{} Finished auto-detection for fallback builder", logLineHeader());
    }

    private void detectNullable(IMethod method) {
        Nullable nullable = method.getAnnotation(IClass.getClass(Nullable.class));
        if (nullable != null) {
            this.nullable = true;
            log.debug("{} Nullable detected for fallback", logLineHeader());
        }
    }

    private void detectOnExceptions(IMethod method) {
        OnException[] onExceptionAnnotations = method.getAnnotationsByType(IClass.getClass(OnException.class));
        for (OnException onExceptionAnnotation : onExceptionAnnotations) {
            onException(onExceptionAnnotation.exception(), onExceptionAnnotation).autoDetect(true);
            log.debug("{} Auto-detected onException", logLineHeader());
        }
    }

    private void detectVariable(IMethod operationMethod) {
        Variable variable = operationMethod.getAnnotation(IClass.getClass(Variable.class));
        if (variable != null) {
            this.variable(variable.name());
            log.debug("{} Auto-detected variable for fallback", logLineHeader());
        }
    }

    private void detectOutput(IMethod operationMethod) {
        if (operationMethod.getAnnotation(IClass.getClass(Output.class)) != null) {
            this.output(true);
            log.debug("{} Auto-detected output for fallback", logLineHeader());
        }
    }

    /**
     * Marks whether the fallback may return {@code null}.
     *
     * @param nullable {@code true} if a {@code null} return is permitted
     * @return this builder for chaining
     */
    @Override
    public IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> nullable(
            boolean nullable) {
        this.nullable = Objects.requireNonNull(nullable, "Nullable cannot be null");
        log.debug("{} Nullable flag set manually", logLineHeader());
        return this;
    }

    private String logLineHeader() {
        return "[Runtime " + runtimeName + "][Step " + stepName + "][Fallback] ";
    }
}
