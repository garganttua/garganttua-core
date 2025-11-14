package com.garganttua.core.runtime.dsl;

import java.util.Objects;

import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.runtime.RuntimeStepCatch;

public class RuntimeStepCatchBuilder extends AbstractAutomaticLinkedBuilder<IRuntimeStepCatchBuilder, IRuntimeStepBuilder<?, ?>, IRuntimeStepCatch> implements IRuntimeStepCatchBuilder {

    private Class<? extends Throwable> exception;
    private Integer code;
    private Boolean fallback;
    private Boolean abort;

    public RuntimeStepCatchBuilder(Class<? extends Throwable> exception, IRuntimeStepMethodBuilder<?,?> method, IRuntimeStepBuilder<?, ?> link) {
        super(link);
        Objects.requireNonNull(method, "Method step builder cannot be null");
        this.exception = Objects.requireNonNull(exception, "Exception cannot be null");
        if( !method.isThrown(exception) ){
            throw new DslException("Exception "+exception.getSimpleName()+" is not thrown by method");
        }
    }

    @Override
    public IRuntimeStepCatchBuilder code(int i) {
        this.code = Objects.requireNonNull(i, "Code cannot be null");
        return this;
    }

    @Override
    public IRuntimeStepCatchBuilder fallback(boolean fallback) {
        this.fallback = Objects.requireNonNull(fallback, "Fallback cannot be null");
        return this;
    }

    @Override
    public IRuntimeStepCatchBuilder abort(boolean abort) {
        this.abort = Objects.requireNonNull(abort, "Abort cannot be null");
        return this;
    }

    @Override
    protected IRuntimeStepCatch doBuild() throws DslException {
        return new RuntimeStepCatch(exception, code, fallback, abort);
    }

    @Override
    protected void doAutoDetection() throws DslException {
        
    }

}
