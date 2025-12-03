package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.injection.context.dsl.IContextBuilderObserver;
import com.garganttua.core.runtime.IMutex;
import com.garganttua.core.runtime.IRuntime;
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;
import com.garganttua.core.utils.OrderedMapPosition;

/**
 * Builder for configuring a single runtime workflow.
 *
 * <p>
 * IRuntimeBuilder provides a fluent DSL for programmatically defining runtime workflows.
 * It enables configuration of initial variables, stages, steps, and their execution logic
 * without using annotations.
 * </p>
 *
 * <p>
 * The builder supports method chaining and allows precise control over stage ordering
 * through positional insertion. It integrates with the dependency injection system
 * through the {@link IContextBuilderObserver} interface.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * IRuntimeBuilder<Order, OrderResult> builder = runtimesBuilder
 *     .runtime("orderProcessing", Order.class, OrderResult.class)
 *         .variable("maxRetries", 3)
 *         .variable("timeout", Duration.ofSeconds(30))
 *         .stage("validation")
 *             .step("validateAmount", () -> new AmountValidator(), Void.class)
 *                 .method().name("validate").parameter(Input.class).end()
 *                 .end()
 *             .end()
 *         .stage("processing")
 *             .step("process", () -> new OrderProcessor(), OrderResult.class)
 *                 .method().name("process").parameter(Input.class).output(true).end()
 *                 .end()
 *             .end()
 *         .end();
 *
 * IRuntime<Order, OrderResult> runtime = builder.build();
 * }</pre>
 *
 * @param <InputType> the input type for this runtime
 * @param <OutputType> the output type for this runtime
 * @since 2.0.0-ALPHA01
 * @see IRuntimesBuilder
 * @see IRuntimeStageBuilder
 * @see IRuntime
 */
public interface IRuntimeBuilder<InputType, OutputType> extends IAutomaticLinkedBuilder<IRuntimeBuilder<InputType, OutputType>, IRuntimesBuilder, IRuntime<InputType, OutputType>>, IContextBuilderObserver {

    IRuntimeBuilder<InputType, OutputType> mutex(IObjectSupplierBuilder<? extends IMutex, ? extends IObjectSupplier<? extends IMutex>> mutex);

    /**
     * Sets an initial variable with a constant value.
     *
     * <p>
     * The variable will be available in the runtime context before any step executes.
     * This is equivalent to the {@code @Variables} annotation.
     * </p>
     *
     * @param name the variable name
     * @param value the variable value
     * @return this builder for method chaining
     * @see com.garganttua.core.runtime.annotations.Variables
     */
    IRuntimeBuilder<InputType, OutputType> variable(String name, Object value);

    /**
     * Sets an initial variable with a dynamically supplied value.
     *
     * <p>
     * The supplier will be invoked to obtain the variable value when the runtime context
     * is created. This allows for dynamic or lazily-computed initial values.
     * </p>
     *
     * @param name the variable name
     * @param value the object supplier builder for the variable value
     * @return this builder for method chaining
     * @see com.garganttua.core.supply.dsl.IObjectSupplierBuilder
     */
    IRuntimeBuilder<InputType, OutputType> variable(String name, IObjectSupplierBuilder<?, ? extends IObjectSupplier<?>> value);

    /**
     * Creates a new stage at the end of the runtime.
     *
     * <p>
     * Stages are executed sequentially in the order they are added.
     * </p>
     *
     * @param string the stage name
     * @return a builder for configuring the stage
     * @see IRuntimeStageBuilder
     */
    IRuntimeStageBuilder<InputType, OutputType> stage(String string);

    /**
     * Creates a new stage at a specific position relative to existing stages.
     *
     * <p>
     * This allows inserting stages before or after existing stages by name,
     * enabling precise control over execution order.
     * </p>
     *
     * @param string the stage name
     * @param position the position where to insert the stage
     * @return a builder for configuring the stage
     * @see com.garganttua.core.runtime.RuntimeStagePosition
     */
    IRuntimeStageBuilder<InputType, OutputType> stage(String string, OrderedMapPosition<String> position);

}
