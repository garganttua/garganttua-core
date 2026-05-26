package com.garganttua.core.runtime.dsl;

import java.util.Objects;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.runtime.IRuntimeStepCatch;
import com.garganttua.core.runtime.RuntimeStepCatch;
import com.garganttua.core.runtime.annotations.Catch;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected
public class RuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> extends
                AbstractAutomaticLinkedBuilder<IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepCatch>
                implements IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> {
    private static final IDiagnostic log = Diagnostics.of(RuntimeStepCatchBuilder.class);

        private IClass<? extends Throwable> exception;
        private Integer code;
        private Catch catchAnnotationForAutoDetection;

        public RuntimeStepCatchBuilder(Class<? extends Throwable> exception,
                        IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> link) {
                super(link);
                this.exception = IClass.getClass(Objects.requireNonNull(exception, "Exception cannot be null"));
                log.trace("Initialized RuntimeStepCatchBuilder");
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
                log.trace("Initialized RuntimeStepCatchBuilder for auto-detection");
        }

        @Override
        public IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> code(int i) {
                this.code = Objects.requireNonNull(i, "Code cannot be null");
                log.debug("Set exception code for RuntimeStepCatchBuilder");
                return this;
        }

        @Override
        protected IRuntimeStepCatch doBuild() throws DslException {
                log.trace("Building RuntimeStepCatch");
                IRuntimeStepCatch catchInstance = new RuntimeStepCatch(exception, code);
                log.debug("RuntimeStepCatch built successfully");
                return catchInstance;
        }

        @Override
        protected void doAutoDetection() throws DslException {
                log.trace("Starting auto-detection for RuntimeStepCatchBuilder");
                Objects.requireNonNull(this.catchAnnotationForAutoDetection, "Catch annotation cannot be null");
                this.code = this.catchAnnotationForAutoDetection.code();
                log.debug("Auto-detected catch code from annotation");
        }

}
