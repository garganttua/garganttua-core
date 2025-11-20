package com.garganttua.core.runtime.dsl;

import java.util.Objects;

import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.runtime.IRuntimeStepCatch;
import com.garganttua.core.runtime.RuntimeStepCatch;
import com.garganttua.core.runtime.annotations.Catch;

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
        
    }

    /**
     * Secondary ctor used only for auto detection
     * 
     * @param exception2
     * @param methodBuilder
     * @param runtimeStepBuilder
     * @param catchAnnotation
     */
    public RuntimeStepCatchBuilder(Class<? extends Throwable> exception,
            IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> link, Catch catchAnnotation) {
        this(exception, link);
        this.catchAnnotationForAutoDetection = Objects.requireNonNull(catchAnnotation,
                "Catch annotation cannot be null");
    }

    @Override
    public IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> code(int i) {
        this.code = Objects.requireNonNull(i, "Code cannot be null");
        return this;
    }

    @Override
    protected IRuntimeStepCatch doBuild() throws DslException {
        return new RuntimeStepCatch(exception, code);
    }

    @Override
    protected void doAutoDetection() throws DslException {
        Objects.requireNonNull(this.catchAnnotationForAutoDetection, "Catch annotation cannot be null");
        this.code = this.catchAnnotationForAutoDetection.code();
    }

}
