package com.garganttua.core.injection.context.dsl;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.injection.IDiContext;

import lombok.extern.slf4j.Slf4j;

/**
 * Manages dependency injection context build readiness state.
 *
 * <p>
 * This class encapsulates the logic for:
 * </p>
 * <ul>
 *   <li>Managing the {@link IDiContextBuilder} instance</li>
 *   <li>Tracking whether the {@link IDiContext} has been provided</li>
 *   <li>Determining if build operations are authorized</li>
 *   <li>Providing access to the resolved context</li>
 * </ul>
 *
 * <h2>Build Authorization Logic</h2>
 * <p>
 * Build operations are authorized when:
 * </p>
 * <ul>
 *   <li>Both contextBuilder and context are present (contextBuilder != null AND context != null)</li>
 *   <li>OR both are absent (contextBuilder == null AND context == null)</li>
 * </ul>
 * <p>
 * This ensures that either a full DI context setup is available, or the builder
 * operates in standalone mode without dependency injection.
 * </p>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li><b>Initialization</b>: Instance created with optional {@link IDiContextBuilder}</li>
 *   <li><b>Context Handling</b>: {@link #handle(IDiContext)} called when context becomes available</li>
 *   <li><b>Authorization Check</b>: {@link #canBuild()} returns true after context is provided</li>
 *   <li><b>Context Access</b>: {@link #getContext()} provides access to resolved context</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is thread-safe. All mutable state is protected by synchronized blocks
 * to ensure safe concurrent access from multiple threads.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create readiness tracker with context builder
 * ContextReadinessBuilder readiness = new ContextReadinessBuilder(contextBuilder);
 *
 * // Initially, build is not authorized
 * assert !readiness.canBuild();
 *
 * // Handle context when it becomes available
 * readiness.handle(diContext);
 *
 * // Now build is authorized
 * assert readiness.canBuild();
 *
 * // Access the context
 * IDiContext context = readiness.getContext();
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see IDiContextBuilder
 * @see IDiContext
 */
@Slf4j
public class ContextReadinessBuilder<Builder> implements IContextReadinessBuilder<Builder>, IContextBuilderObserver {

    private final Object lock = new Object();
    private volatile IDiContextBuilder contextBuilder;
    private volatile IDiContext context;
    private Builder builder;

    /**
     * Constructs a new ContextReadinessBuilder with an optional context builder.
     *
     * @param contextBuilder the optional context builder
     * @throws NullPointerException if the Optional itself is null
     */
    public ContextReadinessBuilder(Optional<IDiContextBuilder> contextBuilder, Builder builder) {
        Objects.requireNonNull(contextBuilder, "Optional context builder cannot be null");
        synchronized (lock) {
            this.contextBuilder = contextBuilder.orElse(null);
            this.context = null;
            this.builder = Objects.requireNonNull(builder, "Builder cannot be null");

            if( this.contextBuilder != null ) {
                this.contextBuilder.observer(this);
            }
            boolean canBuildResult = canBuild();
            log.atDebug().log("ContextReadinessBuilder initialized with contextBuilder: {}, canBuild: {}",
                    this.contextBuilder != null ? "present" : "absent",
                    canBuildResult);
        }
    }

    /**
     * Constructs a new ContextReadinessBuilder with a context builder.
     *
     * @param contextBuilder the context builder (may be null)
     */
    public ContextReadinessBuilder(IDiContextBuilder contextBuilder, Builder builder) {
        this(Optional.ofNullable(contextBuilder), builder);
    }

    /**
     * Returns the context builder.
     *
     * @return the context builder, or null if not set
     */
    public IDiContextBuilder getContextBuilder() {
        synchronized (lock) {
            return contextBuilder;
        }
    }

    /**
     * Sets the context builder.
     *
     * @param contextBuilder the context builder to set (must not be null)
     * @throws NullPointerException if contextBuilder is null
     */
    public void setContextBuilder(IDiContextBuilder contextBuilder) {
        Objects.requireNonNull(contextBuilder, "Context builder cannot be null");
        synchronized (lock) {
            this.contextBuilder = contextBuilder;
            this.contextBuilder.observer(this);
            log.atDebug().log("Context builder updated");
        }
    }

    /**
     * Returns whether build operations are authorized.
     *
     * <p>
     * Build operations are authorized when:
     * </p>
     * <ul>
     *   <li>Both contextBuilder and context are present (contextBuilder != null AND context != null)</li>
     *   <li>OR both are absent (contextBuilder == null AND context == null)</li>
     * </ul>
     *
     * @return true if build is authorized, false otherwise
     */
    public boolean canBuild() {
        synchronized (lock) {
            return (contextBuilder != null && context != null) ||
                   (contextBuilder == null && context == null);
        }
    }

    /**
     * Returns the resolved dependency injection context.
     *
     * @return the context, or null if not yet provided
     */
    public IDiContext getContext() {
        synchronized (lock) {
            return context;
        }
    }

    /**
     * Checks if a context builder is present.
     *
     * @return true if context builder is not null, false otherwise
     */
    public boolean hasContextBuilder() {
        synchronized (lock) {
            return contextBuilder != null;
        }
    }

    /**
     * Checks if a context has been provided.
     *
     * @return true if context is not null, false otherwise
     */
    public boolean hasContext() {
        synchronized (lock) {
            return context != null;
        }
    }

    /**
     * Handles the provision of the dependency injection context.
     *
     * <p>
     * This method stores the provided context. After calling this method,
     * {@link #canBuild()} will return true if a contextBuilder is also present,
     * or false if contextBuilder is null (mismatched state).
     * </p>
     *
     * @param context the dependency injection context (must not be null)
     * @throws NullPointerException if context is null
     */
    public void handle(IDiContext context) {
        log.atTrace().log("Handling context provision");

        Objects.requireNonNull(context, "Context cannot be null");

        synchronized (lock) {
            this.context = context;

            log.atInfo().log("Context provided, canBuild: {}", canBuild());
        }
    }

    /**
     * Validates that the context builder is present.
     *
     * @throws NullPointerException if context builder is null
     */
    public void requireContextBuilder() {
        synchronized (lock) {
            Objects.requireNonNull(this.contextBuilder, "Context builder cannot be null");
        }
    }

    /**
     * Validates that build is authorized.
     *
     * @throws IllegalStateException if build is not authorized (mismatched contextBuilder and context state)
     */
    public void requireBuildAuthorization() {
        synchronized (lock) {
            if (!canBuild()) {
                log.atError().log("Operation attempted with invalid state - contextBuilder: {}, context: {}",
                        contextBuilder != null ? "present" : "absent",
                        context != null ? "present" : "absent");
                throw new IllegalStateException("Build is not authorized. Either both contextBuilder and context must be present, or both must be absent.");
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder context(IDiContextBuilder context) {
        log.atTrace().log("Setting context builder");

        Objects.requireNonNull(context, "Context cannot be null");

        synchronized (lock) {
            this.contextBuilder = context;
            this.contextBuilder.observer(this);

            log.atInfo().log("Context builder set, canBuild: {}", canBuild());
        }
        return (Builder) this;
    }

}
