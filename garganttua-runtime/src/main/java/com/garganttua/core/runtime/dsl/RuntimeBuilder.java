package com.garganttua.core.runtime.dsl;

import static com.garganttua.core.supply.dsl.FixedObjectSupplierBuilder.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.OrderedMapBuilder;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.reflection.query.ObjectQueryFactory;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflection.utils.ParameterizedTypeImpl;
import com.garganttua.core.reflection.utils.WildcardTypeImpl;
import com.garganttua.core.runtime.IMutex;
import com.garganttua.core.runtime.IRuntime;
import com.garganttua.core.runtime.IRuntimeStage;
import com.garganttua.core.runtime.Runtime;
import com.garganttua.core.runtime.annotations.Stages;
import com.garganttua.core.runtime.annotations.Variables;
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;
import com.garganttua.core.utils.OrderedMapPosition;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeBuilder<InputType, OutputType>
                extends
                AbstractAutomaticLinkedBuilder<IRuntimeBuilder<InputType, OutputType>, IRuntimesBuilder, IRuntime<InputType, OutputType>>
                implements IRuntimeBuilder<InputType, OutputType> {

        private String name;
        private final OrderedMapBuilder<String, IRuntimeStageBuilder<InputType, OutputType>, IRuntimeStage<InputType, OutputType>> stages = new OrderedMapBuilder<>();
        private IDiContext context;
        private Class<InputType> inputType;
        private Class<OutputType> outputType;
        private Object objectForAutoDetection;
        private Map<String, IObjectSupplierBuilder<?, ? extends IObjectSupplier<?>>> presetVariables = new HashMap<>();

        public RuntimeBuilder(RuntimesBuilder runtimesBuilder, String name, Class<InputType> inputType,
                        Class<OutputType> outputType) {
                super(Objects.requireNonNull(runtimesBuilder, "RuntimesBuilder cannot be null"));
                this.name = Objects.requireNonNull(name, "Name cannot be null");
                this.inputType = Objects.requireNonNull(inputType, "Input type cannot be null");
                this.outputType = Objects.requireNonNull(outputType, "Output Type cannot be null");

                log.atTrace().log("{} Initialized RuntimeBuilder constructor", logLineHeader());
                log.atDebug().log("{} Input type: {}, Output type: {}", logLineHeader(), inputType, outputType);
                log.atInfo().log("{} RuntimeBuilder initialized", logLineHeader());
        }

        protected RuntimeBuilder(RuntimesBuilder runtimesBuilder, String name, Class<InputType> inputType,
                        Class<OutputType> outputType, Object objectForAutoDetection) {
                this(runtimesBuilder, name, inputType, outputType);
                this.objectForAutoDetection = Objects.requireNonNull(objectForAutoDetection,
                                "objectForAutoDetection cannot be null");

                log.atInfo().log("{} RuntimeBuilder initialized for auto-detection", logLineHeader());
                log.atTrace().log("{} Object for auto-detection class: {}", logLineHeader(),
                                objectForAutoDetection.getClass().getName());
        }

        @Override
        public IRuntimeStageBuilder<InputType, OutputType> stage(String stageName) {
                Objects.requireNonNull(stageName, "Stage name cannot be null");
                String key = stageName.trim();

                log.atTrace().log("{} Entering stage method with name: {}", logLineHeader(), stageName);
                IRuntimeStageBuilder<InputType, OutputType> stageBuilder = new RuntimeStageBuilder<>(this, name, key);
                stages.put(key, stageBuilder);

                log.atDebug().log("{} Added stage [{}]", logLineHeader(), key);
                log.atInfo().log("{} Stage [{}] registered", logLineHeader(), key);
                log.atTrace().log("{} Exiting stage method for name: {}", logLineHeader(), stageName);
                return stageBuilder;
        }

        @Override
        public IRuntimeStageBuilder<InputType, OutputType> stage(String stageName,
                        OrderedMapPosition<String> position) {
                Objects.requireNonNull(stageName, "Stage name cannot be null");
                Objects.requireNonNull(position, "Position cannot be null");

                log.atTrace().log("{} Entering stage method with name: {} at position: {}", logLineHeader(), stageName,
                                position);
                String key = stageName.trim();
                IRuntimeStageBuilder<InputType, OutputType> stageBuilder = new RuntimeStageBuilder<>(this, name, key);
                stages.putAt(key, stageBuilder, position);

                log.atDebug().log("{} Added stage [{}] at position {}", logLineHeader(), key, position);
                log.atInfo().log("{} Stage [{}] registered at position {}", logLineHeader(), key, position);
                log.atTrace().log("{} Exiting stage method for name: {} at position: {}", logLineHeader(), stageName,
                                position);
                return stages.get(key);
        }

        @Override
        protected IRuntime<InputType, OutputType> doBuild() throws DslException {
                Objects.requireNonNull(this.context, "Context cannot be null");

                log.atTrace().log("{} Entering doBuild method", logLineHeader());
                log.atInfo().log("{} Building Runtime with {} stage(s)", logLineHeader(), stages.size());
                log.atTrace().log("{} Stages to build: {}", logLineHeader(), stages.keySet());

                Map<String, IRuntimeStage<InputType, OutputType>> builtStages = this.stages.build();
                Map<String, IObjectSupplier<?>> variables = this.presetVariables.entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().build()));

                log.atDebug().log("{} Preset variables: {}", logLineHeader(), variables.keySet());
                log.atTrace().log("{} Exiting doBuild method", logLineHeader());

                return new Runtime<>(name, builtStages, this.context, this.inputType, this.outputType, variables);
        }

        @Override
        protected void doAutoDetection() {
                Objects.requireNonNull(this.objectForAutoDetection, "objectForAutoDetection cannot be null");
                Objects.requireNonNull(this.context, "Context cannot be null");

                log.atTrace().log("{} Entering doAutoDetection method", logLineHeader());
                log.atInfo().log("{} Performing auto-detection of stages and variables", logLineHeader());
                log.atDebug().log("{} Object for auto-detection class: {}", logLineHeader(),
                                objectForAutoDetection.getClass().getName());

                this.collectStages();
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
                        log.atTrace().log("{} Exiting collectPresetVariables method with no variables",
                                        logLineHeader());
                        return;
                }

                Map<String, IObjectSupplierBuilder<?, ? extends IObjectSupplier<?>>> variables = (Map<String, IObjectSupplierBuilder<?, ? extends IObjectSupplier<?>>>) ObjectQueryFactory
                                .objectQuery(this.objectForAutoDetection).getValue(address);

                variables.entrySet().forEach(e -> this.variable(e.getKey(), e.getValue()));

                log.atDebug().log("{} Collected preset variables: {}", logLineHeader(), variables.keySet());
                log.atInfo().log("{} Collected {} preset variable(s)", logLineHeader(), variables.size());
                log.atTrace().log("{} Exiting collectPresetVariables method", logLineHeader());
        }

        @SuppressWarnings("unchecked")
        private void collectStages() {
                log.atTrace().log("{} Entering collectStages method", logLineHeader());
                ParameterizedType mapType = getStagesMapType();

                String address = ObjectReflectionHelper.getFieldAddressAnnotatedWithAndCheckType(
                                this.objectForAutoDetection.getClass(), Stages.class, mapType.getRawType());

                if (address == null) {
                        log.atError().log("{} No field annotated with @Stages found", logLineHeader());
                        throw new DslException(logLineHeader() + "No field annotated with @Stages found");
                }

                Map<String, List<Class<Object>>> stages = (Map<String, List<Class<Object>>>) ObjectQueryFactory
                                .objectQuery(this.objectForAutoDetection).getValue(address);

                stages.entrySet().forEach(e -> {
                        String stageName = e.getKey();
                        List<Class<Object>> steps = e.getValue();
                        IRuntimeStageBuilder<InputType, OutputType> stageBuilder = new RuntimeStageBuilder<>(this, name,
                                        stageName, steps).autoDetect(true);
                        stageBuilder.handle(this.context);
                        this.stages.put(stageName, stageBuilder);

                        log.atDebug().log("{} Auto-detected stage [{}] with steps: {}", logLineHeader(), stageName,
                                        steps);
                        log.atInfo().log("{} Auto-detected stage [{}] with {} step(s)", logLineHeader(), stageName,
                                        steps.size());
                });
                log.atTrace().log("{} Exiting collectStages method", logLineHeader());
        }

        private ParameterizedType getVariablesMapType() {
                WildcardType wildcardIObjectSupplier = WildcardTypeImpl.extends_(new ParameterizedTypeImpl(
                                IObjectSupplier.class,
                                new Type[] { WildcardTypeImpl.unbounded() }));

                ParameterizedType supplierBuilderType = new ParameterizedTypeImpl(
                                IObjectSupplierBuilder.class,
                                new Type[] { WildcardTypeImpl.unbounded(), wildcardIObjectSupplier });

                return new ParameterizedTypeImpl(
                                Map.class,
                                new Type[] { String.class, supplierBuilderType });
        }

        private ParameterizedType getStagesMapType() {
                ParameterizedType listOfClass = new ParameterizedTypeImpl(List.class, new Type[] { Class.class });
                return new ParameterizedTypeImpl(Map.class, new Type[] { String.class, listOfClass });
        }

        @Override
        public void handle(IDiContext context) {
                log.atTrace().log("{} Entering handle method", logLineHeader());
                this.context = Objects.requireNonNull(context, "Context cannot be null");
                this.stages.values().forEach(s -> s.handle(context));
                log.atTrace().log("{} Context handled for all stages", logLineHeader());
                log.atTrace().log("{} Exiting handle method", logLineHeader());
        }

        @Override
        public IRuntimeBuilder<InputType, OutputType> variable(String name,
                        IObjectSupplierBuilder<?, ? extends IObjectSupplier<?>> value) {
                log.atTrace().log("{} Entering variable registration for [{}]", logLineHeader(), name);
                this.presetVariables.put(Objects.requireNonNull(name, "Variable name cannot be null"),
                                Objects.requireNonNull(value, "Value supplier builder cannot be null"));
                log.atDebug().log("{} Variable [{}] registered", logLineHeader(), name);
                log.atTrace().log("{} Exiting variable registration for [{}]", logLineHeader(), name);
                return this;
        }

        @Override
        public IRuntimeBuilder<InputType, OutputType> variable(String name, Object value) {
                log.atTrace().log("{} Entering variable registration for [{}]", logLineHeader(), name);
                this.presetVariables.put(Objects.requireNonNull(name, "Variable name cannot be null"),
                                of(Objects.requireNonNull(value, "Value  cannot be null")));
                log.atDebug().log("{} Variable [{}] registered", logLineHeader(), name);
                log.atTrace().log("{} Exiting variable registration for [{}]", logLineHeader(), name);
                return this;
        }

        public IRuntimeBuilder<InputType, OutputType> setObjectForAutoDetection(
                        Object runtimeDefinitionObject) {
                this.objectForAutoDetection = Objects.requireNonNull(runtimeDefinitionObject,
                                "runtimeDefinitionObject cannot be null");
                return this;
        }

        @Override
        public IRuntimeBuilder<InputType, OutputType> mutex(
                        IObjectSupplierBuilder<? extends IMutex, ? extends IObjectSupplier<? extends IMutex>> mutex) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'mutex'");
        }
}