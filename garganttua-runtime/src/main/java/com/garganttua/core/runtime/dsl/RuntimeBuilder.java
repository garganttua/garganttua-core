package com.garganttua.core.runtime.dsl;

import static com.garganttua.core.injection.context.beans.Beans.*;
import static com.garganttua.core.supply.dsl.FixedSupplierBuilder.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Named;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.dependency.AbstractAutomaticLinkedDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencyPhase;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.dsl.dependency.DependencySpecBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.OrderedMapBuilder;
import com.garganttua.core.injection.IBeanSupplier;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.observability.IObservable;
import com.garganttua.core.observability.ObservabilityBinding;
import com.garganttua.core.observability.dsl.IObservabilityBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.utils.ParameterizedTypeImpl;
import com.garganttua.core.reflection.utils.WildcardTypeImpl;
import com.garganttua.core.runtime.IRuntime;
import com.garganttua.core.runtime.IRuntimeStep;
import com.garganttua.core.runtime.Runtime;
import com.garganttua.core.runtime.annotations.Steps;
import com.garganttua.core.runtime.annotations.Variables;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.utils.OrderedMapPosition;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Fluent builder that assembles a single {@link IRuntime} from declared steps and
 * preset variables.
 *
 * <p>Supports both programmatic construction (via {@link #step} and
 * {@link #variable}) and annotation-driven auto-detection from a runtime
 * definition object. Resolves an {@link IInjectionContextBuilder} at build time
 * and optionally attaches the built runtime to an observability binding.</p>
 *
 * @param <InputType>  the runtime input type
 * @param <OutputType> the runtime output type
 */
@Reflected
public class RuntimeBuilder<InputType, OutputType>
                extends
                AbstractAutomaticLinkedDependentBuilder<IRuntimeBuilder<InputType, OutputType>, IRuntimesBuilder, IRuntime<InputType, OutputType>>
                implements IRuntimeBuilder<InputType, OutputType> {
    private static final Logger log = Logger.getLogger(RuntimeBuilder.class);

        private String name;
        private final OrderedMapBuilder<String, IRuntimeStepBuilder<?, ?, InputType, OutputType>, IRuntimeStep<?, InputType, OutputType>> steps = new OrderedMapBuilder<>();
        private final java.util.LinkedHashMap<String, IRuntimeStep<?, InputType, OutputType>> prebuiltSteps = new java.util.LinkedHashMap<>();
        private IInjectionContextBuilder injectionContextBuilder;
        private Class<InputType> inputType;
        private Class<OutputType> outputType;
        private Object objectForAutoDetection;
        private Map<String, ISupplierBuilder<?, ? extends ISupplier<?>>> presetVariables = new HashMap<>();
        private IObservableBuilder<?, ?> reflectionBuilderRef;
        private IObservabilityBuilder observabilityBuilder;

        /*
         * This object is set only during prebuild
         */
        private IInjectionContext injectionContext;

        /**
         * Creates a runtime builder for programmatic step/variable definition.
         *
         * @param runtimesBuilder the parent builder this runtime belongs to
         * @param name            the runtime name
         * @param inputType       the runtime input type
         * @param outputType      the runtime output type
         */
        public RuntimeBuilder(RuntimesBuilder runtimesBuilder, String name, Class<InputType> inputType,
                        Class<OutputType> outputType) {
                super(Objects.requireNonNull(runtimesBuilder, "RuntimesBuilder cannot be null"),
                                Set.of(
                                                new DependencySpecBuilder(IClass.getClass(IInjectionContextBuilder.class))
                                                                .requireForBuild().build(),
                                                DependencySpec.use(IClass.getClass(IReflectionBuilder.class), DependencyPhase.BUILD),
                                                DependencySpec.use(IClass.getClass(IObservabilityBuilder.class))));
                this.name = Objects.requireNonNull(name, "Name cannot be null");
                this.inputType = Objects.requireNonNull(inputType, "Input type cannot be null");
                this.outputType = Objects.requireNonNull(outputType, "Output Type cannot be null");

                log.trace("{} Initialized RuntimeBuilder constructor with phase-aware dependencies",
                                logLineHeader());
                log.debug("{} Input type: {}, Output type: {}", logLineHeader(), inputType, outputType);
                log.debug("{} RuntimeBuilder initialized", logLineHeader());
        }

        /**
         * Creates a runtime builder seeded with a runtime definition object for
         * annotation-driven auto-detection of its steps and variables.
         *
         * @param runtimesBuilder         the parent builder this runtime belongs to
         * @param name                    the runtime name
         * @param inputType               the runtime input type
         * @param outputType              the runtime output type
         * @param objectForAutoDetection  the annotated runtime definition instance to scan
         */
        protected RuntimeBuilder(RuntimesBuilder runtimesBuilder, String name, Class<InputType> inputType,
                        Class<OutputType> outputType, Object objectForAutoDetection) {
                this(runtimesBuilder, name, inputType, outputType);
                this.objectForAutoDetection = Objects.requireNonNull(objectForAutoDetection,
                                "objectForAutoDetection cannot be null");

                log.debug("{} RuntimeBuilder initialized for auto-detection", logLineHeader());
                log.trace("{} Object for auto-detection class: {}", logLineHeader(),
                                objectForAutoDetection.getClass().getName());
        }

        /**
         * Declares a step appended after the existing steps.
         *
         * @param stepName       the unique step name
         * @param objectSupplier supplier of the object whose operation method the step invokes
         * @param returnType     the step's execution return type
         * @return the step builder for further configuration
         */
        @Override
        public <StepObjectType, ExecutionReturn> IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> step(
                        String stepName,
                        ISupplierBuilder<StepObjectType, ISupplier<StepObjectType>> objectSupplier,
                        IClass<ExecutionReturn> returnType) {

                Objects.requireNonNull(stepName, "Step name cannot be null");
                Objects.requireNonNull(returnType, "Return type cannot be null");
                Objects.requireNonNull(objectSupplier, "Object supplier builder cannot be null");

                IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> stepBuilder = new RuntimeStepBuilder<>(
                                this, name, stepName, (Class<ExecutionReturn>) returnType.getType(), objectSupplier);

                this.steps.put(stepName, stepBuilder);
                log.debug("{} Added step [{}]", logLineHeader(), stepName);
                return stepBuilder;
        }

        /**
         * Declares a step inserted at the given position relative to existing steps.
         *
         * @param stepName       the unique step name
         * @param position       where to insert the step in the ordered step map
         * @param objectSupplier supplier of the object whose operation method the step invokes
         * @param returnType     the step's execution return type
         * @return the step builder for further configuration
         */
        @Override
        public <StepObjectType, ExecutionReturn> IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> step(
                        String stepName,
                        OrderedMapPosition<String> position,
                        ISupplierBuilder<StepObjectType, ISupplier<StepObjectType>> objectSupplier,
                        IClass<ExecutionReturn> returnType) {

                Objects.requireNonNull(stepName, "Step name cannot be null");
                Objects.requireNonNull(returnType, "Return type cannot be null");
                Objects.requireNonNull(objectSupplier, "Object supplier builder cannot be null");
                Objects.requireNonNull(position, "Position cannot be null");

                IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> stepBuilder = new RuntimeStepBuilder<>(
                                this, name, stepName, (Class<ExecutionReturn>) returnType.getType(), objectSupplier);

                this.steps.putAt(stepName, stepBuilder, position);
                log.debug("{} Added step [{}] at position {}", logLineHeader(), stepName, position);
                return stepBuilder;
        }

        /**
         * Registers an already-built step under the given name.
         *
         * @param name the unique step name
         * @param step the pre-built step instance
         * @return this builder for chaining
         */
        @Override
        public IRuntimeBuilder<InputType, OutputType> step(String name, IRuntimeStep<?, InputType, OutputType> step) {
                Objects.requireNonNull(name, "Step name cannot be null");
                Objects.requireNonNull(step, "Step cannot be null");
                this.prebuiltSteps.put(name, step);
                log.debug("{} Added pre-built step [{}]", logLineHeader(), name);
                return this;
        }

        @Override
        protected IRuntime<InputType, OutputType> doBuild() throws DslException {

                log.trace("{} Entering doBuild method", logLineHeader());
                log.debug("{} Building Runtime with {} step(s)", logLineHeader(), steps.size());

                // Propagate IReflectionBuilder to step builders before building
                if (this.reflectionBuilderRef != null) {
                        this.steps.values().forEach(stepBuilder -> {
                                if (stepBuilder instanceof RuntimeStepBuilder<?, ?, ?, ?> rsb) {
                                        rsb.provideReflectionBuilder(this.reflectionBuilderRef);
                                }
                        });
                }

                Map<String, IRuntimeStep<?, InputType, OutputType>> builtSteps = new java.util.LinkedHashMap<>(this.steps.build());
                builtSteps.putAll(this.prebuiltSteps);

                Map<String, ISupplier<?>> variables = this.presetVariables.entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().build()));

                log.debug("{} Preset variables: {}", logLineHeader(), variables.keySet());

                Runtime<InputType, OutputType> runtime = new Runtime<>(
                                name, builtSteps, this.injectionContext, this.inputType, this.outputType, variables);

                if (this.observabilityBuilder != null) {
                        ObservabilityBinding binding = this.observabilityBuilder.getBinding();
                        if (binding != null) {
                                binding.attachSource((IObservable) runtime);
                                log.trace("{} Runtime '{}' attached to ObservabilityBinding",
                                                logLineHeader(), name);
                        } else {
                                log.warn("{} Runtime '{}' has an IObservabilityBuilder dependency but its binding is null",
                                                logLineHeader(), name);
                        }
                }

                return runtime;
        }

        @Override
        protected void doAutoDetection() {
                Objects.requireNonNull(this.objectForAutoDetection, "objectForAutoDetection cannot be null");

                log.trace("{} Entering doAutoDetection method", logLineHeader());
                log.debug("{} Performing auto-detection of steps and variables", logLineHeader());

                this.collectSteps();
                this.collectPresetVariables();
                log.trace("{} Exiting doAutoDetection method", logLineHeader());
        }

        private String logLineHeader() {
                return "[RuntimeBuilder " + name + "] ";
        }

        @SuppressWarnings("unchecked")
        private void collectPresetVariables() {
                log.trace("{} Entering collectPresetVariables method", logLineHeader());
                ParameterizedType mapType = getVariablesMapType();
                String address = findFieldAddressAnnotatedWithAndCheckType(
                                IClass.getClass(this.objectForAutoDetection.getClass()), Variables.class, (Class<?>) mapType.getRawType());

                if (address == null) {
                        log.debug("{} No preset variables found", logLineHeader());
                        return;
                }

                IClass<?> ownerClass = IClass.getClass(this.objectForAutoDetection.getClass());
                IField variablesField = findFieldByName(ownerClass, address);
                Map<String, ISupplierBuilder<?, ? extends ISupplier<?>>> variables;
                try {
                        variablesField.setAccessible(true);
                        variables = (Map<String, ISupplierBuilder<?, ? extends ISupplier<?>>>) variablesField.get(this.objectForAutoDetection);
                } catch (IllegalAccessException e) {
                        throw new DslException("Failed to access variables field", e);
                }

                variables.entrySet().forEach(e -> this.variable(e.getKey(), e.getValue()));

                log.debug("{} Collected preset variables: {}", logLineHeader(), variables.keySet());
                log.debug("{} Collected {} preset variable(s)", logLineHeader(), variables.size());
        }

        @SuppressWarnings("unchecked")
        private void collectSteps() {
                log.trace("{} Entering collectSteps method", logLineHeader());
                ParameterizedType listType = getStepsListType();

                String address = findFieldAddressAnnotatedWithAndCheckType(
                                IClass.getClass(this.objectForAutoDetection.getClass()), Steps.class, (Class<?>) listType.getRawType());

                if (address == null) {
                        log.error("{} No field annotated with @Steps found", logLineHeader());
                        throw new DslException(logLineHeader() + "No field annotated with @Steps found");
                }

                IClass<?> stepsOwnerClass = IClass.getClass(this.objectForAutoDetection.getClass());
                IField stepsField = findFieldByName(stepsOwnerClass, address);
                List<Class<Object>> stepsList;
                try {
                        stepsField.setAccessible(true);
                        stepsList = (List<Class<Object>>) stepsField.get(this.objectForAutoDetection);
                } catch (IllegalAccessException e) {
                        throw new DslException("Failed to access steps field", e);
                }

                stepsList.forEach(c -> {
                        String stepName = UUID.randomUUID().toString();
                        Named stepNamedAnnotation = c.getAnnotation(Named.class);
                        if (stepNamedAnnotation != null) {
                                stepName = stepNamedAnnotation.value();
                        }

                        log.debug("{} Creating auto-detected step [{}]", logLineHeader(), stepName);

                        ISupplierBuilder<Object, IBeanSupplier<Object>> supplierBuilder = bean(IClass.getClass(c));
                        RuntimeStepBuilder<?, ?, InputType, OutputType> stepBuilder = new RuntimeStepBuilder<>(this, name,
                                        stepName, Void.class, supplierBuilder);
                        if (this.reflectionBuilderRef != null) {
                                stepBuilder.provideReflectionBuilder(this.reflectionBuilderRef);
                        }
                        stepBuilder.autoDetect(true);

                        if (this.injectionContextBuilder != null) {
                                stepBuilder.provide(this.injectionContextBuilder);
                        }
                        this.steps.put(stepName, stepBuilder);

                        log.debug("{} Auto-detected step [{}] registered", logLineHeader(), stepName);
                });
                log.trace("{} Exiting collectSteps method", logLineHeader());
        }

        private ParameterizedType getVariablesMapType() {
                WildcardType wildcardISupplier = WildcardTypeImpl.extends_(new ParameterizedTypeImpl(
                                ISupplier.class,
                                new Type[] { WildcardTypeImpl.unbounded() }));

                ParameterizedType supplierBuilderType = new ParameterizedTypeImpl(
                                ISupplierBuilder.class,
                                new Type[] { WildcardTypeImpl.unbounded(), wildcardISupplier });

                return new ParameterizedTypeImpl(
                                Map.class,
                                new Type[] { String.class, supplierBuilderType });
        }

        private ParameterizedType getStepsListType() {
                return new ParameterizedTypeImpl(List.class, new Type[] { Class.class });
        }

        /**
         * Receives a build dependency, capturing the injection context, reflection
         * and observability builders when present, then delegates to the superclass.
         *
         * @param dependency the dependency builder being provided
         * @return this builder for chaining
         * @throws DslException if the superclass rejects the dependency
         */
        @Override
        public IRuntimeBuilder<InputType, OutputType> provide(IObservableBuilder<?, ?> dependency) throws DslException {
                if (dependency instanceof IInjectionContextBuilder injCtxBuilder) {
                        this.injectionContextBuilder = injCtxBuilder;
                }
                if (dependency instanceof IReflectionBuilder) {
                        this.reflectionBuilderRef = dependency;
                }
                if (dependency instanceof IObservabilityBuilder obs) {
                        this.observabilityBuilder = obs;
                }
                return super.provide(dependency);
        }

        /**
         * Registers a preset runtime variable backed by a supplier builder.
         *
         * @param name  the variable name
         * @param value the supplier builder producing the variable value
         * @return this builder for chaining
         */
        @Override
        public IRuntimeBuilder<InputType, OutputType> variable(String name,
                        ISupplierBuilder<?, ? extends ISupplier<?>> value) {
                log.trace("{} Entering variable registration for [{}]", logLineHeader(), name);
                this.presetVariables.put(Objects.requireNonNull(name, "Variable name cannot be null"),
                                Objects.requireNonNull(value, "Value supplier builder cannot be null"));
                log.debug("{} Variable [{}] registered", logLineHeader(), name);
                return this;
        }

        /**
         * Registers a preset runtime variable with a fixed value.
         *
         * @param name  the variable name
         * @param value the fixed variable value
         * @return this builder for chaining
         */
        @Override
        public IRuntimeBuilder<InputType, OutputType> variable(String name, Object value) {
                log.trace("{} Entering variable registration for [{}]", logLineHeader(), name);
                this.presetVariables.put(Objects.requireNonNull(name, "Variable name cannot be null"),
                                of(Objects.requireNonNull(value, "Value  cannot be null")));
                log.debug("{} Variable [{}] registered", logLineHeader(), name);
                return this;
        }

        /**
         * Sets the runtime definition object used as the source for auto-detection.
         *
         * @param runtimeDefinitionObject the annotated runtime definition instance to scan
         * @return this builder for chaining
         */
        public IRuntimeBuilder<InputType, OutputType> setObjectForAutoDetection(
                        Object runtimeDefinitionObject) {
                this.objectForAutoDetection = Objects.requireNonNull(runtimeDefinitionObject,
                                "runtimeDefinitionObject cannot be null");
                return this;
        }

        @Override
        protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
        }

        @Override
        protected void doPreBuildWithDependency(Object dependency) {
                if (dependency instanceof IInjectionContext ic)
                        this.injectionContext = ic;
        }

        @Override
        protected void doPostBuildWithDependency(Object dependency) {
        }

        @SuppressWarnings("unchecked")
        private static <A extends java.lang.annotation.Annotation> String findFieldAddressAnnotatedWithAndCheckType(
                        IClass<?> ownerClass, Class<A> annotationClass, Class<?> expectedType) {
                IClass<A> iAnnotation = (IClass<A>) IClass.getClass(annotationClass);
                for (IField field : ownerClass.getDeclaredFields()) {
                        if (field.getAnnotation(iAnnotation) != null) {
                                IClass<?> fieldType = field.getType();
                                if (fieldType.isAssignableFrom(expectedType)
                                                || IClass.getClass(expectedType).isAssignableFrom(fieldType)) {
                                        return field.getName();
                                }
                        }
                }
                return null;
        }

        private static IField findFieldByName(IClass<?> ownerClass, String fieldName) {
                for (IField field : ownerClass.getDeclaredFields()) {
                        if (field.getName().equals(fieldName)) {
                                return field;
                        }
                }
                return null;
        }
}
