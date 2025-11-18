package com.garganttua.core.runtime;

import java.util.Optional;
import java.util.Set;

import com.garganttua.core.condition.ICondition;
import com.garganttua.core.execution.ExecutorException;
import com.garganttua.core.execution.IExecutorChain;
import com.garganttua.core.reflection.ReflectionException;

public class RuntimeStep<ExecutionReturn, InputType, OutputType>
        implements IRuntimeStep<ExecutionReturn, InputType, OutputType> {

    private final String stepName;
    private Class<ExecutionReturn> executionReturn;
    private IRuntimeStepMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>> operationBinder;
    private Optional<IRuntimeStepMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>>> fallbackBinder;
    private Set<IRuntimeStepCatch> catches;
    private Optional<ICondition> condition;
    private String stageName;
    private String runtimeName;

    public RuntimeStep(String runtimeName, String stageName, String stepName, Class<ExecutionReturn> executionReturn,
            IRuntimeStepMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>> operationBinder,
            Optional<IRuntimeStepMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>>> fallbackBinder,
            Set<IRuntimeStepCatch> catches, Optional<ICondition> condition) {
        this.runtimeName = runtimeName;
        this.stageName = stageName;
        this.stepName = stepName;
        this.executionReturn = executionReturn;
        this.operationBinder = operationBinder;
        this.fallbackBinder = fallbackBinder;
        this.catches = catches;
        this.condition = condition;
    }

    @Override
    public String getStepName() {
        return stepName;
    }

    public Optional<ExecutionReturn> executeOperation(IRuntimeContext<InputType, OutputType> context)
            throws RuntimeException {
        try {
            if ((condition.isPresent() && condition.get().evaluate()) || condition.isEmpty()) {
                return operationBinder.execute(context);
            }
        } catch (ReflectionException e) {
            throw new RuntimeException(logLineHeader()+" Step operation "+operationBinder.getExecutableReference()+" execution failed",e);
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void defineExecutionStep(IExecutorChain<IRuntimeContext<InputType, OutputType>> chain) {

        if (this.catches.size() > 0 && this.fallbackBinder.isPresent())
            chain.addExecutor((c, n) -> {

                Optional<ExecutionReturn> returned = this.executeOperation(c);

                if (this.operationBinder.isOutput()) {
                    if (returned.isEmpty())
                        throw new ExecutorException(
                                logLineHeader()+ "is defined to be output but did not returned any value");

                    if (!c.isOfOutputType(returned.get().getClass()))
                        throw new ExecutorException(
                                logLineHeader()+"is defined to be output, but returned type "+returned.get().getClass().getSimpleName()+" is not output type "+c.getOutputType().getSimpleName());

                    c.setOutput((OutputType) returned.get());

                    this.operationBinder.setSuccessCode(c);

                }

                n.execute(c);

            }, (c, n) -> {

            });
        else
            chain.addExecutor((c, n) -> {
                this.executeOperation(c);
                n.execute(c);
            });

    }

    private String logLineHeader() {
        return "[Runtime "+runtimeName+"][Stage "+stageName+"][Step "+stepName+"] ";
    }

}
