package com.garganttua.core.runtime.dsl;

import java.lang.reflect.Method;

import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;

public class RuntimeStepCatchBuilder extends AbstractAutomaticLinkedBuilder<IRuntimeStepCatchBuilder, IRuntimeStepOperationBuilder<?>, IRuntimeStepCatch> implements IRuntimeStepCatchBuilder {

    public RuntimeStepCatchBuilder(Class<? extends Throwable> exception, Method method, IRuntimeStepOperationBuilder<?> link) {
        super(link);
    }

    @Override
    public IRuntimeStepCatchBuilder code(int i) {
        return this;
    }

    @Override
    public IRuntimeStepCatchBuilder failback(boolean failback) {
        return this;
    }

    @Override
    public IRuntimeStepCatchBuilder abort(boolean abord) {
        return this;
    }

    @Override
    protected IRuntimeStepCatch doBuild() throws DslException {
        return null;
    }

    @Override
    protected void doAutoDetection() throws DslException {

    }

}
