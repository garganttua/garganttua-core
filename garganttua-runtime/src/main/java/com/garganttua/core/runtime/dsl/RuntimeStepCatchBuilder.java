package com.garganttua.core.runtime.dsl;

import java.util.Objects;

import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.runtime.IRuntimeStepCatch;
import com.garganttua.core.runtime.RuntimeStepCatch;
import com.garganttua.core.runtime.annotations.Catch;

public class RuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> extends
        AbstractAutomaticLinkedBuilder<IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepCatch>
        implements IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> {

    private Class<? extends Throwable> exception;
    private Integer code;
    private Boolean fallback;
    private Boolean abort;
    private Catch catchAnnotationForAutoDetection;

    public RuntimeStepCatchBuilder(Class<? extends Throwable> exception, IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> method,
            IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> link) {
        super(link);
        Objects.requireNonNull(method, "Method step builder cannot be null");
        this.exception = Objects.requireNonNull(exception, "Exception cannot be null");
        if (!method.isThrown(exception)) {
            throw new DslException("Exception " + exception.getSimpleName() + " is not thrown by method");
        }
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
            IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> method,
            IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> link, Catch catchAnnotation) {
        this(exception, method, link);
        this.catchAnnotationForAutoDetection = Objects.requireNonNull(catchAnnotation,
                "Catch annotation cannot be null");
    }

    @Override
    public IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> code(int i) {
        this.code = Objects.requireNonNull(i, "Code cannot be null");
        return this;
    }

    @Override
    public IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> fallback(boolean fallback) {
        this.fallback = Objects.requireNonNull(fallback, "Fallback cannot be null");
        return this;
    }

    @Override
    public IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> abort(boolean abort) {
        this.abort = Objects.requireNonNull(abort, "Abort cannot be null");
        return this;
    }

    @Override
    protected IRuntimeStepCatch doBuild() throws DslException {
        return new RuntimeStepCatch(exception, code, fallback, abort);
    }

    @Override
    protected void doAutoDetection() throws DslException {
        Objects.requireNonNull(this.catchAnnotationForAutoDetection, "Catch annotation cannot be null");
        this.abort = this.catchAnnotationForAutoDetection.abort();
        this.code = this.catchAnnotationForAutoDetection.code();
        this.fallback = this.catchAnnotationForAutoDetection.fallback();
    }

}
