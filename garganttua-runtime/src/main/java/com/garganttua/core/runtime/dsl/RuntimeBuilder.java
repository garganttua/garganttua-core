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

import com.garganttua.core.dsl.dependency.AbstractAutomaticLinkedDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencySpecBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.OrderedMapBuilder;
import com.garganttua.core.injection.IBeanSupplier;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.query.ObjectQueryFactory;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeBuilder<InputType, OutputType>
                extends
                AbstractAutomaticLinkedDependentBuilder<IRuntimeBuilder<InputType, OutputType>, IRuntimesBuilder, IRuntime<InputType, OutputType>>
                implements IRuntimeBuilder<InputType, OutputType> {

        private String name;
        private final OrderedMapBuilder<String, IRuntimeStepBuilder<?, ?, InputType, OutputType>, IRuntimeStep<?, InputType, OutputType>> steps = new OrderedMapBuilder<>();
        private IInjectionContextBuilder injectionContextBuilder;
        private Class<InputType> inputType;
        private Class<OutputType> outputType;
        private Object objectForAutoDetection;
        private Map<String, ISupplierBuilder<?, ? extends ISupplier<?>>> presetVariables = new HashMap<>();

        /*
         * This object is set only during prebuild
         */
        private IInjectionContext injectionContext;

        public RuntimeBuilder(RuntimesBuilder runtimesBuilder, String name, Class<InputType> inputType,
                        Class<OutputType> outputType) {
                super(Objects.requireNonNull(runtimesBuilder, "RuntimesBuilder cannot be null"),
                                Set.of(
                                                new DependencySpecBuilder(IInjectionContextBuilder.class)
                                                                .requireForBuild().build()));
                this.name = Objects.requireNonNull(name, "Name cannot be null");
                this.inputType = Objects.requireNonNull(inputType, "Input type cannot be null");
                this.outputType = Objects.requireNonNull(outputType, "Output Type cannot be null");

                log.atTrace().log("{} Initialized RuntimeBuilder constructor with phase-aware dependencies",
                                logLineHeader());
                log.atDebug().log("{} Input type: {}, Output type: {}", logLineHeader(), inputType, outputType);
                log.atDebug().log("{} RuntimeBuilder initialized", logLineHeader());
        }

        protected RuntimeBuilder(RuntimesBuilder runtimesBuilder, String name, Class<InputType> inputType,
                        Class<OutputType> outputType, Object objectForAutoDetection) {
                this(runtimesBuilder, name, inputType, outputType);
                this.objectForAutoDetection = Objects.requireNonNull(objectForAutoDetection,
                                "objectForAutoDetection cannot be null");

                log.atDebug().log("{} RuntimeBuilder initialized for auto-detection", logLineHeader());
                log.atTrace().log("{} Object for auto-detection class: {}", logLineHeader(),
                                objectForAutoDetection.getClass().getName());
        }

        @Override
        public <StepObjectType, ExecutionReturn> IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> step(
                        String stepName,
                        ISupplierBuilder<StepObjectType, ISupplier<StepObjectType>> objectSupplier,
                        Class<ExecutionReturn> returnType) {

                Objects.requireNonNull(stepName, "Step name cannot be null");
                Objects.requireNonNull(returnType, "Return type cannot be null");
                Objects.requireNonNull(objectSupplier, "Object supplier builder cannot be null");

                IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> stepBuilder = new RuntimeStepBuilder<>(
                                this, name, stepName, returnType, objectSupplier);

                this.steps.put(stepName, stepBuilder);
                log.atDebug().log("{} Added step [{}]", logLineHeader(), stepName);
                return stepBuilder;
        }

        @Override
        public <StepObjectType, ExecutionReturn> IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> step(
                        String stepName,
                        OrderedMapPosition<String> position,
                        ISupplierBuilder<StepObjectType, ISupplier<StepObjectType>> objectSupplier,
                        Class<ExecutionReturn> returnType) {

                Objects.requireNonNull(stepName, "Step name cannot be null");
                Objects.requireNonNull(returnType, "Return type cannot be null");
                Objects.requireNonNull(objectSupplier, "Object supplier builder cannot be null");
                Objects.requireNonNull(position, "Position cannot be null");

                IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> stepBuilder = new RuntimeStepBuilder<>(
                                this, name, stepName, returnType, objectSupplier);

                this.steps.putAt(stepName, stepBuilder, position);
                log.atDebug().log("{} Added step [{}] at position {}", logLineHeader(), stepName, position);
                return stepBuilder;
        }

        @Override
        protected IRuntime<InputType, OutputType> doBuild() throws DslException {

                log.atTrace().log("{} Entering doBuild method", logLineHeader());
                log.atDebug().log("{} Building Runtime with {} step(s)", logLineHeader(), steps.size());

                Map<String, IRuntimeStep<?, InputType, OutputType>> builtSteps = this.steps.build();
                Map<String, ISupplier<?>> variables = this.presetVariables.entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().build()));

                log.atDebug().log("{} Preset variables: {}", logLineHeader(), variables.keySet());

                return new Runtime<>(name, builtSteps, this.injectionContext, this.inputType, this.outputType, variables);
        }

        @Override
        protected void doAutoDetection() {
                Objects.requireNonNull(this.objectForAutoDetection, "objectForAutoDetection cannot be null");

                log.atTrace().log("{} Entering doAutoDetection method", logLineHeader());
                log.atDebug().log("{} Performing auto-detection of steps and variables", logLineHeader());

                this.collectSteps();
                this.collectPresetVariables();
                log.atTrace().log("{} Exiting doAutoDetection method", logLineHeader());
        }

        private String logLineHeader() {
                return "[RuntimeBuilder " + name + "] ";
        }

        @SuppressWarnings("unchecked")
        private void collectPresetVariables() {
                log.atTrace().log("{} Entering collectPresetVariables method", logLineHeader());
                ParameterizedType mapType = getVariablesMapType();
                String address = ObjectReflectionHelper.getFieldAddressAnnotatedWithAndCheckType(
                                this.objectForAutoDetection.getClass(), Variables.class, mapType.getRawType());

                if (address == null) {
                        log.atWarn().log("{} No preset variables found", logLineHeader());
                        return;
                }

                Map<String, ISupplierBuilder<?, ? extends ISupplier<?>>> variables = (Map<String, ISupplierBuilder<?, ? extends ISupplier<?>>>) ObjectQueryFactory
                                .objectQuery(this.objectForAutoDetection).getValue(address);

                variables.entrySet().forEach(e -> this.variable(e.getKey(), e.getValue()));

                log.atDebug().log("{} Collected preset variables: {}", logLineHeader(), variables.keySet());
                log.atDebug().log("{} Collected {} preset variable(s)", logLineHeader(), variables.size());
        }

        @SuppressWarnings("unchecked")
        private void collectSteps() {
                log.atTrace().log("{} Entering collectSteps method", logLineHeader());
                ParameterizedType listType = getStepsListType();

                String address = ObjectReflectionHelper.getFieldAddressAnnotatedWithAndCheckType(
                                this.objectForAutoDetection.getClass(), Steps.class, listType.getRawType());

                if (address == null) {
                        log.atError().log("{} No field annotated with @Steps found", logLineHeader());
                        throw new DslException(logLineHeader() + "No field annotated with @Steps found");
                }

                List<Class<Object>> stepsList = (List<Class<Object>>) ObjectQueryFactory
                                .objectQuery(this.objectForAutoDetection).getValue(address);

                stepsList.forEach(c -> {
                        String stepName = UUID.randomUUID().toString();
                        Named stepNamedAnnotation = c.getAnnotation(Named.class);
                        if (stepNamedAnnotation != null) {
                                stepName = stepNamedAnnotation.value();
                        }

                        log.atDebug().log("{} Creating auto-detected step [{}]", logLineHeader(), stepName);

                        ISupplierBuilder<Object, IBeanSupplier<Object>> supplierBuilder = bean(c);
                        IRuntimeStepBuilder<?, ?, InputType, OutputType> stepBuilder = new RuntimeStepBuilder<>(this, name,
                                        stepName, Void.class, supplierBuilder)
                                        .autoDetect(true);

                        if (this.injectionContextBuilder != null) {
                                stepBuilder.provide(this.injectionContextBuilder);
                        }
                        this.steps.put(stepName, stepBuilder);

                        log.atDebug().log("{} Auto-detected step [{}] registered", logLineHeader(), stepName);
                });
                log.atTrace().log("{} Exiting collectSteps method", logLineHeader());
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

        @Override
        public IRuntimeBuilder<InputType, OutputType> provide(IObservableBuilder<?, ?> dependency) throws DslException {
                if (dependency instanceof IInjectionContextBuilder injCtxBuilder) {
                        this.injectionContextBuilder = injCtxBuilder;
                }
                return super.provide(dependency);
        }

        @Override
        public IRuntimeBuilder<InputType, OutputType> variable(String name,
                        ISupplierBuilder<?, ? extends ISupplier<?>> value) {
                log.atTrace().log("{} Entering variable registration for [{}]", logLineHeader(), name);
                this.presetVariables.put(Objects.requireNonNull(name, "Variable name cannot be null"),
                                Objects.requireNonNull(value, "Value supplier builder cannot be null"));
                log.atDebug().log("{} Variable [{}] registered", logLineHeader(), name);
                return this;
        }

        @Override
        public IRuntimeBuilder<InputType, OutputType> variable(String name, Object value) {
                log.atTrace().log("{} Entering variable registration for [{}]", logLineHeader(), name);
                this.presetVariables.put(Objects.requireNonNull(name, "Variable name cannot be null"),
                                of(Objects.requireNonNull(value, "Value  cannot be null")));
                log.atDebug().log("{} Variable [{}] registered", logLineHeader(), name);
                return this;
        }

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
}
