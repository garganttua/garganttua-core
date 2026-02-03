package com.garganttua.core.runtime.dsl;

import java.util.Objects;

import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.runtime.IRuntimeStepCatch;
import com.garganttua.core.runtime.RuntimeStepCatch;
import com.garganttua.core.runtime.annotations.Catch;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> extends
                AbstractAutomaticLinkedBuilder<IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepCatch>
                implements IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> {

        private Class<? extends Throwable> exception;
        private Integer code;
        private Catch catchAnnotationForAutoDetection;

        public RuntimeStepCatchBuilder(Class<? extends Throwable> exception,
                        IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> link) {
                super(link);
                this.exception = Objects.requireNonNull(exception, "Exception cannot be null");
                log.atTrace()
                                .log("Initialized RuntimeStepCatchBuilder");
        }

        /**
         * Secondary ctor used only for auto detection
         *
         * @param exception
         * @param link
         * @param catchAnnotation
         */
        public RuntimeStepCatchBuilder(Class<? extends Throwable> exception,
                        IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> link,
                        Catch catchAnnotation) {
                this(exception, link);
                this.catchAnnotationForAutoDetection = Objects.requireNonNull(catchAnnotation,
                                "Catch annotation cannot be null");
                log.atTrace()
                                .log("Initialized RuntimeStepCatchBuilder for auto-detection");
        }

        @Override
        public IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> code(int i) {
                this.code = Objects.requireNonNull(i, "Code cannot be null");
                log.atDebug().log("Set exception code for RuntimeStepCatchBuilder");
                return this;
        }

        @Override
        protected IRuntimeStepCatch doBuild() throws DslException {
                log.atTrace()
                                .log("Building RuntimeStepCatch");
                IRuntimeStepCatch catchInstance = new RuntimeStepCatch(exception, code);
                log.atDebug()
                                .log("RuntimeStepCatch built successfully");
                return catchInstance;
        }

        @Override
        protected void doAutoDetection() throws DslException {
                log.atTrace().log("Starting auto-detection for RuntimeStepCatchBuilder");
                Objects.requireNonNull(this.catchAnnotationForAutoDetection, "Catch annotation cannot be null");
                this.code = this.catchAnnotationForAutoDetection.code();
                log.atDebug()
                                .log("Auto-detected catch code from annotation");
        }

}