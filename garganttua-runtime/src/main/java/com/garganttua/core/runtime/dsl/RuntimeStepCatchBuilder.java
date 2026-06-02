package com.garganttua.core.runtime.dsl;

import java.util.Objects;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.runtime.IRuntimeStepCatch;
import com.garganttua.core.runtime.RuntimeStepCatch;
import com.garganttua.core.runtime.annotations.Catch;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Fluent builder for a step's {@code @Catch} clause, pairing a caught exception
 * type with an exit code.
 *
 * @param <ExecutionReturn> the operation method return type
 * @param <StepObjectType>  the type of the object holding the step methods
 * @param <InputType>       the runtime input type
 * @param <OutputType>      the runtime output type
 */
@Reflected
public class RuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> extends
                AbstractAutomaticLinkedBuilder<IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepCatch>
                implements IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> {
    private static final Logger log = Logger.getLogger(RuntimeStepCatchBuilder.class);

        private IClass<? extends Throwable> exception;
        private Integer code;
        private Catch catchAnnotationForAutoDetection;

        /**
         * Creates a catch builder for the given exception type.
         *
         * @param exception the exception type to catch
         * @param link      the parent method builder
         */
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

        /**
         * Sets the exit code associated with the caught exception.
         *
         * @param i the exit code
         * @return this builder for chaining
         */
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
