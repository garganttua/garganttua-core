/**
 * Fluent builder API implementations for runtime execution workflow construction.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides concrete implementations of the fluent DSL interfaces defined
 * in garganttua-commons for building runtime execution workflows. It implements the
 * builder pattern to provide a type-safe, readable API for programmatic workflow
 * construction.
 * </p>
 *
 * <h2>Implementation Classes</h2>
 * <p>
 * This package contains implementations of the builder interfaces from
 * {@link com.garganttua.core.runtime.dsl} (commons package).
 * </p>
 *
 * <h2>Usage Example: Programmatic Runtime Builder from Tests</h2>
 * <pre>{@code
 * import static com.garganttua.core.condition.Conditions.custom;
 * import static com.garganttua.core.runtime.RuntimeContext.*;
 * import static com.garganttua.core.supply.dsl.FixedSupplierBuilder.of;
 *
 * // Build runtime programmatically
 * DummyRuntimeProcessOutputStep step = new DummyRuntimeProcessOutputStep();
 *
 * IRuntimesBuilder builder = RuntimesBuilder.builder()
 *     .context(contextBuilder)
 *     .runtime("runtime-1", String.class, String.class)
 *         .stage("stage-1")
 *             .step("step-1", of(step), String.class)
 *                 .method()
 *                     .condition(custom(of(10), i -> true))
 *                     .output(true)
 *                     .variable("method-returned")
 *                     .method("method")
 *                     .code(201)
 *                     .katch(DiException.class).code(401).up()
 *                     .withParam(input(String.class))
 *                     .withParam(of("fixed-value-in-method"))
 *                     .withParam(variable("variable", String.class))
 *                     .withParam(context()).up()
 *                 .fallBack()
 *                     .onException(DiException.class).up()
 *                     .output(true)
 *                     .variable("fallback-returned")
 *                     .method("fallbackMethod")
 *                     .withParam(input(String.class))
 *                     .withParam(of("fixed-value-in-method"))
 *                     .withParam(exception(DiException.class))
 *                     .withParam(code())
 *                     .withParam(exceptionMessage())
 *                     .withParam(context())
 *                     .up().up().up()
 *         .variable("variable", of("preset-variable"))
 *         .up();
 *
 * Map<String, IRuntime<?, ?>> runtimes = builder.build();
 * IRuntime<String, String> runtime = (IRuntime<String, String>) runtimes.get("runtime-1");
 *
 * // Execute
 * IRuntimeResult<String, String> result = runtime.execute("input").orElseThrow();
 * assertEquals("input-processed-fixed-value-in-method-preset-variable", result.output());
 * assertEquals(201, result.code());
 * }</pre>
 *
 * <h2>Usage Example: Auto-Detection Builder</h2>
 * <pre>{@code
 * // Build with auto-detection
 * IDiContextBuilder contextBuilder = DiContext.builder()
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
 * // All annotated runtimes are automatically detected and built
 * assertTrue(runtimes.containsKey("runtime-1"));
 * assertTrue(runtimes.containsKey("two-steps-runtime"));
 * }</pre>
 *
 * <h2>Usage Example: Runtime with Variables</h2>
 * <pre>{@code
 * // Build runtime with preset variables
 * IRuntime<String, String> runtime = builder
 *     .runtime("two-steps-runtime", String.class, String.class)
 *         .variable("step-one-variable", "step-one-variable")
 *         .variable("output-step-variable", "output-step-variable")
 *         .up()
 *     .build()
 *     .get("two-steps-runtime");
 *
 * IRuntimeResult<String, String> result = runtime.execute("test").orElseThrow();
 *
 * // Variables are shared between steps
 * assertEquals(222, result.code());
 * assertEquals("test-step-one-processed-step-one-variable-output-step-processed-output-step-variable",
 *     result.output());
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Fluent, chainable API</li>
 *   <li>Type-safe workflow construction</li>
 *   <li>Stage and step organization</li>
 *   <li>Method binding with parameter mapping</li>
 *   <li>Exception handling configuration</li>
 *   <li>Fallback strategy definition</li>
 *   <li>Variable declaration and usage</li>
 *   <li>Conditional step execution</li>
 *   <li>Context-aware parameter resolution</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.runtime.dsl
 * @see com.garganttua.core.runtime
 */
package com.garganttua.core.runtime.dsl;
