package com.garganttua.core.bootstrap.dsl;

/**
 * Listener invoked by Bootstrap around each high-level orchestration stage.
 * Implementations can plug into the lifecycle to add cross-cutting concerns
 * — telemetry, custom logging, integration bridges (Spring lifecycle, …) —
 * without modifying Bootstrap's core flow.
 *
 * <h2>Stage timeline</h2>
 * <pre>
 *  Bootstrap.build()
 *    onStageStart(REGISTRATION)   // builders discovered + sorted
 *    onStageEnd  (REGISTRATION)
 *    onStageStart(RESOLVE)        // provide() each declared dep
 *    onStageEnd  (RESOLVE)
 *    onStageStart(CONFIGURATION)  // doConfigureWithDependencyBuilder hooks
 *    onStageEnd  (CONFIGURATION)
 *    onStageStart(BUILD)          // per-builder build + lifecycle init/start
 *    onStageEnd  (BUILD)
 * </pre>
 *
 * <p>Implementations are discovered via the standard Java
 * {@link java.util.ServiceLoader} mechanism on the classpath — register a
 * {@code META-INF/services/com.garganttua.core.bootstrap.dsl.IBootstrapStageListener}
 * descriptor listing your implementation classes.
 *
 * <p>Hooks fire in registration order (manual first, then SPI). Exceptions
 * thrown by a listener are caught and logged at WARN; they do <strong>not</strong>
 * abort the Bootstrap build.
 *
 * @since 2.0.0-ALPHA02
 */
public interface IBootstrapStageListener {

    /**
     * High-level Bootstrap orchestration stages observable by listeners.
     */
    enum Stage {
        /** Builders are discovered and sorted into dependency order. */
        REGISTRATION,
        /** Each declared dependency is provided to its dependents. */
        RESOLVE,
        /** Per-builder {@code doConfigureWithDependencyBuilder} hooks run. */
        CONFIGURATION,
        /** Each builder is built and its lifecycle initialized and started. */
        BUILD
    }

    /**
     * Fired right before the named stage starts. Default no-op.
     */
    default void onStageStart(Stage stage) {
        // no-op
    }

    /**
     * Fired right after the named stage ends successfully. Default no-op.
     */
    default void onStageEnd(Stage stage) {
        // no-op
    }

    /**
     * Fired if the stage throws — the exception propagates after every
     * registered listener has been notified. Default no-op.
     */
    default void onStageError(Stage stage, Throwable error) {
        // no-op
    }
}
