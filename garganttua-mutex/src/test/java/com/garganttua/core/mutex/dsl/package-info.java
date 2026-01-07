/**
 * Tests for the mutex manager DSL builder with auto-detection capabilities.
 *
 * <h2>Auto-Detection Testing</h2>
 * <p>
 * This package contains comprehensive tests for the {@link com.garganttua.core.mutex.dsl.MutexManagerBuilder}
 * auto-detection functionality, which automatically discovers and registers mutex factories
 * annotated with {@link com.garganttua.core.mutex.annotations.MutexFactory}.
 * </p>
 *
 * <h2>Test Structure</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.mutex.dsl.MutexManagerBuilderAutoDetectionTest} - Tests for auto-detection configuration and package scanning</li>
 *   <li>{@link com.garganttua.core.mutex.dsl.MutexManagerBuilderIntegrationTest} - Integration tests with real DI context</li>
 *   <li>{@link com.garganttua.core.mutex.dsl.fixtures} - Test fixtures including annotated factories for testing</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create DI context with package scanning
 * IDiContextBuilder contextBuilder = DiContextBuilder.builder()
 *     .withPackage("com.example.mutex");
 *
 * // Build context
 * contextBuilder.build();
 *
 * // Create mutex manager with auto-detection
 * IMutexManager manager = MutexManagerBuilder.builder(contextBuilder)
 *     .withPackage("com.example.mutex")
 *     .autoDetect(true)
 *     .build();
 *
 * // Use the manager
 * MutexName name = MutexName.fromString("CustomMutex::resource");
 * IMutex mutex = manager.mutex(name);
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 */
package com.garganttua.core.mutex.dsl;
