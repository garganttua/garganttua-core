package com.garganttua.core.runtime.dsl;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.dependency.AbstractAutomaticLinkedDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencySpecBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.injection.IInjectableElementResolverBuilder;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.IRuntimeStep;
import com.garganttua.core.runtime.IRuntimeStepFallbackBinder;
import com.garganttua.core.runtime.RuntimeStep;
import com.garganttua.core.runtime.annotations.FallBack;
import com.garganttua.core.runtime.annotations.Operation;
import com.garganttua.core.runtime.annotations.Output;
import com.garganttua.core.runtime.annotations.Variable;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Fluent builder for a single runtime step, configuring its operation method and
 * optional fallback.
 *
 * <p>Supports annotation-driven auto-detection of the {@code @Operation} and
 * {@code @FallBack} methods on the step's object class, and builds an
 * {@link IRuntimeStep} bound to the resolved injection context.</p>
 *
 * @param <ExecutionReturn> the operation method return type
 * @param <StepObjectType>  the type of the object holding the step methods
 * @param <InputType>       the runtime input type
 * @param <OutputType>      the runtime output type
 */
@Reflected
public class RuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>
        extends
        AbstractAutomaticLinkedDependentBuilder<IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeBuilder<InputType, OutputType>, IRuntimeStep<?, InputType, OutputType>>
        implements IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> {
    private static final Logger log = Logger.getLogger(RuntimeStepBuilder.class);

    private String stepName;
    private String runtimeName;
    private ISupplierBuilder<StepObjectType, ? extends ISupplier<StepObjectType>> supplier;
    private RuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> methodBuilder;
    private Class<ExecutionReturn> executionReturn;
    private RuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> fallbackBuilder;
    private IInjectableElementResolverBuilder resolverBuilder;
    private IInjectionContextBuilder injectionContextBuilder;
    private IObservableBuilder<?, ?> reflectionBuilderRef;

    /**
     * Creates a step builder.
     *
     * @param runtimeBuilder  the parent runtime builder
     * @param runtimeName     the owning runtime name
     * @param stepName        the unique step name
     * @param executionReturn the step's execution return type
     * @param supplier        supplier of the object whose operation/fallback methods the step invokes
     */
    public RuntimeStepBuilder(RuntimeBuilder<InputType, OutputType> runtimeBuilder, String runtimeName,
            String stepName,
            Class<ExecutionReturn> executionReturn,
            ISupplierBuilder<StepObjectType, ? extends ISupplier<StepObjectType>> supplier) {
        super(runtimeBuilder, Set.of(
                new DependencySpecBuilder(IClass.getClass(IInjectionContextBuilder.class)).useForAutoDetect().build()));
        this.stepName = Objects.requireNonNull(stepName, "Step name cannot be null");
        this.runtimeName = Objects.requireNonNull(runtimeName, "Runtime name cannot be null");
        this.executionReturn = Objects.requireNonNull(executionReturn, "Execution return type cannot be null");
        this.supplier = Objects.requireNonNull(supplier, "Supplier builder cannot be null");

        log.trace("{} Initialized RuntimeStepBuilder", logLineHeader());
        log.debug("{} Supplier type: {}", logLineHeader(), supplier.getSuppliedClass());
    }

    /**
     * Returns the operation method builder, creating it lazily on first call.
     *
     * @return the method builder for the step's operation
     * @throws DslException if the method builder cannot be created
     */
    @Override
    public IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> method()
            throws DslException {
        log.trace("{} Entering method() method", logLineHeader());
        if (this.methodBuilder == null) {
            this.methodBuilder = new RuntimeStepMethodBuilder<>(runtimeName, stepName, this, supplier);
            log.debug("{} Method builder created", logLineHeader());
        } else {
            log.debug("{} Reusing existing method builder", logLineHeader());
        }
        log.trace("{} Exiting method() method", logLineHeader());
        return this.methodBuilder;
    }

    /**
     * Returns the fallback method builder, creating it lazily on first call.
     *
     * @return the fallback builder for the step
     * @throws DslException if the fallback builder cannot be created
     */
    @Override
    public IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> fallBack()
            throws DslException {
        log.trace("{} Entering fallBack() method", logLineHeader());
        if (this.fallbackBuilder == null) {
            this.fallbackBuilder = new RuntimeStepFallbackBuilder<>(runtimeName, stepName, this, supplier);
            log.debug("{} Fallback builder created", logLineHeader());
        } else {
            log.debug("{} Reusing existing fallback builder", logLineHeader());
        }
        log.trace("{} Exiting fallBack() method", logLineHeader());
        return this.fallbackBuilder;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.trace("{} Starting auto-detection", logLineHeader());
        detectOperationMethod();
        detectFallback();
        log.trace("{} Finished auto-detection", logLineHeader());
    }

    private void detectFallback() {
        log.trace("{} Detecting fallback method", logLineHeader());
        IMethod fallbackMethod = findMethodAnnotatedWith(supplier.getSuppliedClass(), FallBack.class);
        if (fallbackMethod != null) {
            try {
                fallBack();
                this.fallbackBuilder.provide(this.resolverBuilder);
                this.fallbackBuilder.autoDetect(true);
                this.fallbackBuilder.method(fallbackMethod);

                if (fallbackMethod.getAnnotation(IClass.getClass(Output.class)) != null) {
                    this.fallbackBuilder.output(true);
                }

                Variable variable = fallbackMethod.getAnnotation(IClass.getClass(Variable.class));
                if (variable != null) {
                    this.fallbackBuilder.variable(variable.name());
                }

                log.debug("{} Detected fallback method [{}]", logLineHeader(), fallbackMethod.getName());
            } catch (DslException e) {
                log.warn("{} Exception while handling fallback method [{}]", logLineHeader(),
                        fallbackMethod.getName());
            }
        } else {
            log.debug("{} No fallback method detected", logLineHeader());
        }
    }

    @SuppressWarnings("unchecked")
    private IMethod detectOperationMethod() throws DslException {
        log.trace("{} Detecting operation method", logLineHeader());
        IMethod method = findMethodAnnotatedWith(supplier.getSuppliedClass(), Operation.class);
        if (method == null) {
            log.error("{} No @Operation method found in class {}", logLineHeader(),
                    supplier.getSuppliedClass().getSimpleName());
            throw new DslException("Class " + supplier.getSuppliedClass().getSimpleName() +
                    " does not declare any @Operation method");
        }
        this.executionReturn = (Class<ExecutionReturn>) method.getReturnType().getType();
        method();
        this.methodBuilder.provide(this.resolverBuilder);
        this.methodBuilder.autoDetect(true);
        this.methodBuilder.method(method);

        log.debug("{} Detected operation method [{}] returning [{}]", logLineHeader(), method.getName(),
                executionReturn.getSimpleName());
        return method;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected IRuntimeStep<ExecutionReturn, InputType, OutputType> doBuild() throws DslException {
        log.trace("{} Entering doBuild() method", logLineHeader());

        // Propagate IReflectionBuilder to method and fallback builders before building
        if (this.reflectionBuilderRef != null) {
            if (this.methodBuilder != null) {
                this.methodBuilder.provide(this.reflectionBuilderRef);
            }
            if (this.fallbackBuilder != null) {
                this.fallbackBuilder.provide(this.reflectionBuilderRef);
            }
        }

        IRuntimeStepFallbackBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType> fallback = null;
        if (this.fallbackBuilder != null) {
            fallback = this.fallbackBuilder.build();
            log.debug("{} Built fallback method", logLineHeader());
        } else {
            log.debug("{} No fallback to build", logLineHeader());
        }

        log.debug("{} Building RuntimeStep", logLineHeader());
        IRuntimeStep<ExecutionReturn, InputType, OutputType> step = new RuntimeStep(runtimeName, stepName,
                executionReturn, this.methodBuilder.build(),
                Optional.ofNullable(fallback));

        log.trace("{} Exiting doBuild() method", logLineHeader());
        return step;
    }

    private String logLineHeader() {
        return "[Runtime " + runtimeName + "][Step " + stepName + "] ";
    }

    /**
     * Receives a build dependency, capturing the injection context builder and its
     * resolvers when present, then delegates to the superclass.
     *
     * @param dependency the dependency builder being provided
     * @return this builder for chaining
     * @throws DslException if the superclass rejects the dependency
     */
    @Override
    public IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> provide(
            IObservableBuilder<?, ?> dependency) throws DslException {
        if (dependency instanceof IInjectionContextBuilder icb) {
            this.injectionContextBuilder = icb;
            this.resolverBuilder = icb.resolvers();
        }
        return super.provide(dependency);
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
    }

    @SuppressWarnings("unchecked")
    private static <A extends java.lang.annotation.Annotation> IMethod findMethodAnnotatedWith(
            IClass<?> ownerClass, Class<A> annotationClass) {
        IClass<A> iAnnotation = (IClass<A>) IClass.getClass(annotationClass);
        return Arrays.stream(ownerClass.getDeclaredMethods())
                .filter(m -> m.getAnnotation(iAnnotation) != null)
                .findFirst()
                .orElse(null);
    }

    void provideReflectionBuilder(IObservableBuilder<?, ?> reflectionBuilder) {
        this.reflectionBuilderRef = reflectionBuilder;
    }
}
