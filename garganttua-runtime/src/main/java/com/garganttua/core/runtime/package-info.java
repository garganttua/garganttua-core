/**
 * Runtime execution framework implementation for workflow orchestration.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the concrete implementation of the Garganttua runtime execution
 * framework. It implements workflow orchestration with support for stages, steps, exception
 * handling, context management, and dynamic method invocation. The runtime enables
 * declarative and programmatic definition of complex business processes.
 * </p>
 *
 * <h2>Main Implementation Classes</h2>
 * <ul>
 *   <li>{@code Runtime} - Main runtime execution engine</li>
 *   <li>{@code RuntimeContext} - Execution context with variables and state</li>
 *   <li>{@code RuntimeContextFactory} - Factory for creating runtime contexts</li>
 *   <li>{@code RuntimeProcess} - Represents a runtime process instance</li>
 *   <li>{@code RuntimeResult} - Execution result with output and status</li>
 *   <li>{@code RuntimeStage} - Execution stage grouping related steps</li>
 *   <li>{@code RuntimeStep} - Individual execution step</li>
 * </ul>
 *
 * <h2>Exception Handling Classes</h2>
 * <ul>
 *   <li>{@code RuntimeStepCatch} - Exception catching configuration</li>
 *   <li>{@code RuntimeStepOnException} - Exception handler definition</li>
 *   <li>{@code RuntimeStepFallbackBinder} - Fallback method binding</li>
 *   <li>{@code RuntimeStepMethodBinder} - Step method binding</li>
 *   <li>{@code RuntimeStepExecutionTools} - Execution utilities</li>
 * </ul>
 *
 * <h2>Usage Example: Auto-Detection Runtime with Annotations</h2>
 * <pre>{@code
 * import static com.garganttua.core.supply.dsl.FixedSupplierBuilder.of;
 *
 * // Define the runtime with annotations
 * @RuntimeDefinition(input=String.class, output=String.class)
 * @Named("runtime-1")
 * public class OneStepRuntime {
 *
 *     @Stages
 *     public Map<String, List<Class<?>>> stages = Map.of(
 *             "stage-1", List.of(DummyRuntimeProcessOutputStep.class));
 *
 *     @Variables
 *     public Map<String, ISupplierBuilder<?, ? extends ISupplier<?>>> presetVariables =
 *         Map.of("variable", of("preset-variable"));
 * }
 *
 * // Build with auto-detection
 * IInjectionContextBuilder contextBuilder = InjectionContext.builder()
 *     .autoDetect(true)
 *     .withPackage("com.garganttua.core.runtime.annotations")
 *     .withPackage("com.garganttua.core.runtime");
 *
 * contextBuilder.build().onInit().onStart();
 *
 * IRuntimesBuilder runtimesBuilder = RuntimesBuilder.builder()
 *     .context(contextBuilder)
 *     .autoDetect(true);
 *
 * Map<String, IRuntime<?, ?>> runtimes = runtimesBuilder.build();
 *
 * // Execute
 * IRuntime<String, String> runtime = (IRuntime<String, String>) runtimes.get("runtime-1");
 * IRuntimeResult<String, String> result = runtime.execute("input").orElseThrow();
 * }</pre>
 *
 * <h2>Usage Example: Step with Exception Handling</h2>
 * <pre>{@code
 * import static com.garganttua.core.condition.Conditions.custom;
 * import static com.garganttua.core.supply.dsl.FixedSupplierBuilder.of;
 *
 * @Step
 * @Named("output-step")
 * public class DummyRuntimeProcessOutputStep {
 *
 *     @Condition
 *     IConditionBuilder condition = custom(of(10), i -> 1 > 0);
 *
 *     @Operation(abortOnUncatchedException=true)
 *     @Output
 *     @Catch(exception = DiException.class, code = 401)
 *     @Variable(name = "method-returned")
 *     @Code(201)
 *     @Nullable
 *     String method(
 *             @Input String input,
 *             @Fixed(valueString = "fixed-value-in-method") String fixedValue,
 *             @Variable(name = "variable") String variable,
 *             @Context IRuntimeContext<String, String> context)
 *             throws DiException, CustomException {
 *
 *         if (variable.equals("di-exception")) {
 *             throw new DiException(input + "-processed-" + fixedValue + "-" + variable);
 *         }
 *
 *         return input + "-processed-" + fixedValue + "-" + variable;
 *     }
 *
 *     @FallBack
 *     @Output
 *     @Nullable
 *     @OnException(exception = DiException.class)
 *     @Variable(name = "fallback-returned")
 *     String fallbackMethod(
 *             @Input String input,
 *             @Fixed(valueString = "fixed-value-in-fallback") String fixedValue,
 *             @Exception DiException exception,
 *             @Code Integer code,
 *             @Nullable @ExceptionMessage String exceptionMessage,
 *             @Context IRuntimeContext<String, String> context) {
 *         return input + "-fallback-" + fixedValue + "-" + code + "-" + exceptionMessage;
 *     }
 * }
 * }</pre>
 *
 * <h2>Runtime Lifecycle</h2>
 * <ol>
 *   <li><b>Initialization</b> - Runtime created and configured</li>
 *   <li><b>Context Creation</b> - Execution context initialized with input</li>
 *   <li><b>Stage Execution</b> - Stages executed in order</li>
 *   <li><b>Step Execution</b> - Steps within stage executed sequentially</li>
 *   <li><b>Method Invocation</b> - Step methods invoked with parameter binding</li>
 *   <li><b>Exception Handling</b> - Exceptions caught and handled if configured</li>
 *   <li><b>Completion</b> - Result created with output and status</li>
 * </ol>
 *
 * <h2>Multi-Step Runtime Example</h2>
 * <pre>{@code
 * // Runtime definition with two steps
 * @RuntimeDefinition(input = String.class, output = String.class)
 * @Named("two-steps-runtime")
 * public class TwoStepsRuntimeDefinition {
 *     @Stages
 *     public Map<String, List<Class<?>>> stages = Map.of(
 *         "stage-1", List.of(StepOne.class, StepOutput.class));
 * }
 *
 * // First step stores result in variable
 * @Step
 * @Named("step-one")
 * public class StepOne {
 *     @Operation(abortOnUncatchedException = false)
 *     @Catch(exception = DiException.class, code = 401)
 *     @Variable(name = "step-one-returned")
 *     @Nullable
 *     String method(
 *             @Input String input,
 *             @Variable(name = "step-one-variable") String variable)
 *             throws DiException {
 *         return input + "-step-one-processed-" + variable;
 *     }
 * }
 *
 * // Second step uses variable from first step
 * @Step
 * @Named("output-step")
 * public class StepOutput {
 *     @Output
 *     @Code(222)
 *     @Operation(abortOnUncatchedException = true)
 *     @Catch(exception = DiException.class, code = 444)
 *     @Nullable
 *     String method(
 *             @Variable(name = "step-one-returned") String input,
 *             @Variable(name = "output-step-variable") String outputStepVariable)
 *             throws DiException {
 *         return input + "-output-step-processed-" + outputStepVariable;
 *     }
 * }
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Declarative workflow definition via annotations</li>
 *   <li>Programmatic workflow construction via DSL</li>
 *   <li>Stage-based execution organization</li>
 *   <li>Sequential step execution</li>
 *   <li>Dynamic method invocation</li>
 *   <li>Parameter binding (input, output, context, variables)</li>
 *   <li>Exception handling and recovery</li>
 *   <li>Fallback strategies</li>
 *   <li>Conditional step execution</li>
 *   <li>Runtime variable management</li>
 *   <li>Execution result tracking</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.runtime.dsl} - Fluent builder implementations</li>
 *   <li>{@link com.garganttua.core.runtime.supply} - Runtime value suppliers</li>
 *   <li>{@link com.garganttua.core.runtime.resolver} - Parameter resolution</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.runtime.annotations
 * @see com.garganttua.core.runtime.dsl
 */
package com.garganttua.core.runtime;
