package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.dsl.dependency.IDependentBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.runtime.IRuntime;
import com.garganttua.core.runtime.IRuntimeStep;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.utils.OrderedMapPosition;

/**
 * Builder for defining a single runtime workflow and its steps.
 *
 * <p>
 * Returned by {@link IRuntimesBuilder#runtime(String, IClass, IClass)}, this builder
 * declares the runtime's variables and ordered sequence of steps. It is the programmatic
 * counterpart of the {@code @RuntimeDefinition} annotation and links back to its parent
 * {@link IRuntimesBuilder} via {@code up()}.
 * </p>
 *
 * @param <InputType>  the runtime input type
 * @param <OutputType> the runtime output type
 * @since 2.0.0-ALPHA01
 * @see IRuntimesBuilder
 * @see IRuntimeStepBuilder
 * @see com.garganttua.core.runtime.annotations.RuntimeDefinition
 */
public interface IRuntimeBuilder<InputType, OutputType> extends IAutomaticLinkedBuilder<IRuntimeBuilder<InputType, OutputType>, IRuntimesBuilder, IRuntime<InputType, OutputType>>, IDependentBuilder<IRuntimeBuilder<InputType, OutputType>, IRuntime<InputType, OutputType>> {

    /**
     * Declares a runtime variable with a constant initial value.
     *
     * @param name  the variable name
     * @param value the initial value
     * @return this builder for method chaining
     */
    IRuntimeBuilder<InputType, OutputType> variable(String name, Object value);

    /**
     * Declares a runtime variable whose initial value is produced lazily by a supplier.
     *
     * @param name  the variable name
     * @param value the supplier builder providing the value
     * @return this builder for method chaining
     */
    IRuntimeBuilder<InputType, OutputType> variable(String name, ISupplierBuilder<?, ? extends ISupplier<?>> value);

    /**
     * Adds a pre-built step to this runtime under the given name.
     *
     * @param name the step name
     * @param step the step to add
     * @return this builder for method chaining
     */
    IRuntimeBuilder<InputType, OutputType> step(String name, IRuntimeStep<?, InputType, OutputType> step);

    /**
     * Begins definition of a new step backed by the object supplied by {@code objectSupplier}.
     *
     * @param <StepObjectType> the type of object containing the step method
     * @param <ExecutionReturn> the return type of the step method
     * @param string         the step name
     * @param objectSupplier the supplier of the object the step method is invoked on
     * @param returnType     the class of the step method's return type
     * @return a builder for configuring the step
     * @see IRuntimeStepBuilder
     */
    <StepObjectType, ExecutionReturn> IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> step(String string, ISupplierBuilder<StepObjectType, ISupplier<StepObjectType>> objectSupplier, IClass<ExecutionReturn> returnType);

    /**
     * Begins definition of a new step inserted at a specific ordered position.
     *
     * @param <StepObjectType> the type of object containing the step method
     * @param <ExecutionReturn> the return type of the step method
     * @param string         the step name
     * @param position       the ordered position at which to insert the step
     * @param objectSupplier the supplier of the object the step method is invoked on
     * @param returnType     the class of the step method's return type
     * @return a builder for configuring the step
     * @see IRuntimeStepBuilder
     */
    <StepObjectType, ExecutionReturn> IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> step(String string, OrderedMapPosition<String> position, ISupplierBuilder<StepObjectType, ISupplier<StepObjectType>> objectSupplier, IClass<ExecutionReturn> returnType);

}
