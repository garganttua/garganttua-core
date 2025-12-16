/**
 * Lifecycle management framework implementation for component state management.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the concrete implementation of the lifecycle management framework.
 * It implements lifecycle state transitions with support for start/stop callbacks,
 * state validation, and lifecycle event handling.
 * </p>
 *
 * <h2>Main Implementation Classes</h2>
 * <ul>
 *   <li>{@code AbstractLifecycle} - Base abstract lifecycle implementation</li>
 * </ul>
 *
 * <h2>Lifecycle States</h2>
 * <ul>
 *   <li><b>NEW</b> - Component created but not started</li>
 *   <li><b>STARTING</b> - Component is starting</li>
 *   <li><b>STARTED</b> - Component is running</li>
 *   <li><b>STOPPING</b> - Component is stopping</li>
 *   <li><b>STOPPED</b> - Component is stopped</li>
 *   <li><b>FAILED</b> - Component failed to start or stop</li>
 * </ul>
 *
 * <h2>Usage Example: Custom Lifecycle Component (from AbstractLifecycleTest)</h2>
 * <pre>{@code
 * import java.util.concurrent.atomic.AtomicInteger;
 *
 * private static class TestLifecycle extends AbstractLifecycle {
 *
 *     AtomicInteger initCount = new AtomicInteger();
 *     AtomicInteger startCount = new AtomicInteger();
 *     AtomicInteger flushCount = new AtomicInteger();
 *     AtomicInteger stopCount = new AtomicInteger();
 *
 *     @Override
 *     protected ILifecycle doInit() {
 *         initCount.incrementAndGet();
 *         return this;
 *     }
 *
 *     @Override
 *     protected ILifecycle doStart() {
 *         startCount.incrementAndGet();
 *         return this;
 *     }
 *
 *     @Override
 *     protected ILifecycle doFlush() {
 *         flushCount.incrementAndGet();
 *         return this;
 *     }
 *
 *     @Override
 *     protected ILifecycle doStop() {
 *         stopCount.incrementAndGet();
 *         return this;
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example: Init and Start (from testInitAndStart)</h2>
 * <pre>{@code
 * TestLifecycle lifecycle = new TestLifecycle();
 * lifecycle.onInit().onStart();
 *
 * assertTrue(lifecycle.isInitialized());
 * assertTrue(lifecycle.isStarted());
 * assertFalse(lifecycle.isStopped());
 *
 * assertEquals(1, lifecycle.initCount.get());
 * assertEquals(1, lifecycle.startCount.get());
 * }</pre>
 *
 * <h2>Usage Example: Stop After Start (from testStop)</h2>
 * <pre>{@code
 * lifecycle.onInit().onStart().onStop();
 *
 * assertTrue(lifecycle.isStopped());
 * assertFalse(lifecycle.isStarted());
 * assertEquals(1, lifecycle.stopCount.get());
 * }</pre>
 *
 * <h2>Usage Example: Reload Sequence (from testReload)</h2>
 * <pre>{@code
 * lifecycle.onInit().onStart();
 * lifecycle.onReload();
 *
 * assertTrue(lifecycle.isInitialized());
 * assertTrue(lifecycle.isStarted());
 * assertTrue(lifecycle.isFlushed());
 *
 * // Counters: each phase must be executed
 * assertTrue(lifecycle.initCount.get() >= 2, "init must be recalled");
 * assertTrue(lifecycle.startCount.get() >= 2, "start must be recalled");
 * assertTrue(lifecycle.flushCount.get() >= 1, "flush must be executed");
 * assertTrue(lifecycle.stopCount.get() >= 1, "stop must be executed");
 * }</pre>
 *
 * <h2>Usage Example: Error Handling (from AbstractLifecycleTest)</h2>
 * <pre>{@code
 * // Start without init fails
 * assertThrows(LifecycleException.class, () -> lifecycle.onStart());
 *
 * // Double init fails
 * lifecycle.onInit();
 * assertThrows(LifecycleException.class, () -> lifecycle.onInit());
 *
 * // Double start fails
 * lifecycle.onInit().onStart();
 * assertThrows(LifecycleException.class, () -> lifecycle.onStart());
 *
 * // Flush after start fails
 * assertThrows(LifecycleException.class, () -> lifecycle.onInit().onStart().onFlush());
 * }</pre>
 *
 * <h2>State Transitions</h2>
 * <pre>
 * NEW {@literal ->} STARTING {@literal ->} STARTED
 *  |                    |
 *  |                    v
 *  |                STOPPING {@literal ->} STOPPED
 *  |                    |
 *  {@literal +}------{@literal >} FAILED {@literal <}-----{@literal +}
 * </pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>State machine implementation</li>
 *   <li>Automatic state transitions</li>
 *   <li>Start/stop callback hooks</li>
 *   <li>State validation</li>
 *   <li>Error handling and FAILED state</li>
 *   <li>Thread-safe state management</li>
 *   <li>Idempotent start/stop operations</li>
 *   <li>State query methods (isStarted, isStopped, etc.)</li>
 * </ul>
 *
 * <h2>Integration</h2>
 * <p>
 * This lifecycle implementation is used by:
 * </p>
 * <ul>
 *   <li>Dependency injection context (start/stop)</li>
 *   <li>Runtime execution engine</li>
 *   <li>Service components</li>
 *   <li>Resource managers</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.lifecycle
 */
package com.garganttua.core.lifecycle;
