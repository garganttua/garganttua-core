package com.garganttua.core.runtime;

import java.util.Optional;
import java.util.Set;

import com.garganttua.core.condition.ICondition;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.runtime.dsl.IRuntimeStepCatch;

public class RuntimeStep<ExecutionReturn> implements IRuntimeStep {

    private final String stepName;
    private Class<ExecutionReturn> executionReturn;
    private IMethodBinder<ExecutionReturn> iMethodBinder;
    private Optional<IMethodBinder<ExecutionReturn>> ofNullable;
    private Set<IRuntimeStepCatch> builtCatches;
    private Optional<ICondition> condition;
    private Optional<String> storeReturnInVariable;
    private Boolean output;

    public RuntimeStep(String stepName, Class<ExecutionReturn> executionReturn,
            IMethodBinder<ExecutionReturn> iMethodBinder, Optional<IMethodBinder<ExecutionReturn>> ofNullable,
            Set<IRuntimeStepCatch> builtCatches, Optional<ICondition> condition, Optional<String> storeReturnInVariable,
            Boolean output) {
                this.stepName = stepName;
                this.executionReturn = executionReturn;
                this.iMethodBinder = iMethodBinder;
                this.ofNullable = ofNullable;
                this.builtCatches = builtCatches;
                this.condition = condition;
                this.storeReturnInVariable = storeReturnInVariable;
                this.output = output;
    }

    @Override
    public String getStepName() {
        return stepName;
    }

}
