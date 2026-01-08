package com.garganttua.core.runtime.dsl;

import java.util.Map;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.injection.context.dsl.IContextReadinessBuilder;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.runtime.IRuntime;

/**
 * Builder for creating multiple runtime instances programmatically.
 *
 * <p>
 * IRuntimesBuilder is the entry point for building runtime workflows using the fluent DSL approach.
 * It allows creating multiple named runtimes, each with their own input/output types, stages, and steps.
 * This provides a programmatic alternative to the annotation-based approach.
 * </p>
 *
 * <p>
 * The builder supports dependency injection integration through {@link IInjectionContextBuilder}, allowing
 * runtimes to access beans, properties, and other DI-managed resources.
 * </p>
 *
 * <h2>Usage Example - Single Runtime</h2>
 * <pre>{@code
 * IRuntimesBuilder builder = new RuntimesBuilder();
 *
 * builder
 *     .runtime("orderProcessing", Order.class, OrderResult.class)
 *         .stage("validation")
 *             .step("validate", () -> new OrderValidator(), Void.class)
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
 * Map<String, IRuntime<?, ?>> runtimes = builder.build();
 * IRuntime<Order, OrderResult> runtime = (IRuntime<Order, OrderResult>) runtimes.get("orderProcessing");
 * }</pre>
 *
 * <h2>Usage Example - Multiple Runtimes with DI Context</h2>
 * <pre>{@code
 * IInjectionContextBuilder contextBuilder = new InjectionContextBuilder();
 *
 * IRuntimesBuilder builder = new RuntimesBuilder()
 *     .context(contextBuilder)
 *     .runtime("orderProcessing", Order.class, OrderResult.class)
 *         // ... define order runtime
 *         .end()
 *     .runtime("userProcessing", User.class, UserResult.class)
 *         // ... define user runtime
 *         .end();
 *
 * Map<String, IRuntime<?, ?>> runtimes = builder.build();
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see IRuntimeBuilder
 * @see IRuntime
 * @see com.garganttua.core.runtime.annotations.RuntimeDefinition
 */
public interface IRuntimesBuilder
                extends IAutomaticBuilder<IRuntimesBuilder, Map<String, IRuntime<?, ?>>>, IContextReadinessBuilder<IRuntimesBuilder> {

        /**
         * Creates a new runtime with the specified name and types.
         *
         * <p>
         * This method begins the definition of a new runtime workflow. The returned builder
         * is used to configure stages, steps, and other runtime settings.
         * </p>
         *
         * @param <InputType> the input type for this runtime
         * @param <OutputType> the output type for this runtime
         * @param string the unique name for this runtime
         * @param inputType the class representing the input type
         * @param outputType the class representing the output type
         * @return a builder for configuring the runtime
         * @see IRuntimeBuilder
         */
        <InputType, OutputType> IRuntimeBuilder<InputType, OutputType> runtime(String string,
                        Class<InputType> inputType,
                        Class<OutputType> outputType);

        /**
         * Sets the dependency injection context for all runtimes.
         *
         * <p>
         * The DI context provides access to beans, properties, and other resources
         * that can be injected into runtime steps. All runtimes built by this builder
         * will share the same DI context.
         * </p>
         *
         * @param context the DI context builder to use
         * @return this builder for method chaining
         * @see com.garganttua.core.injection.context.dsl.IInjectionContextBuilder
         */
        IRuntimesBuilder context(IInjectionContextBuilder context);

}
