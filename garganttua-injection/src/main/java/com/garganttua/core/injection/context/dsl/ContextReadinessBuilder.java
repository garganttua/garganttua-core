package com.garganttua.core.injection.context.dsl;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.lifecycle.LifecycleStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * Manages dependency injection context build readiness state.
 *
 * <p>
 * This class encapsulates the logic for:
 * </p>
 * <ul>
 *   <li>Managing the {@link IInjectionContextBuilder} instance</li>
 *   <li>Tracking whether the {@link IInjectionContext} has been provided</li>
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
 *   <li><b>Initialization</b>: Instance created with optional {@link IInjectionContextBuilder}</li>
 *   <li><b>Context Handling</b>: {@link #handle(IInjectionContext)} called when context becomes available</li>
 *   <li><b>Authorization Check</b>: {@link #isReady()} returns true after context is provided</li>
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
 * readiness.handle(injectionContext);
 *
 * // Now build is authorized
 * assert readiness.canBuild();
 *
 * // Access the context
 * IInjectionContext context = readiness.getContext();
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see IInjectionContextBuilder
 * @see IInjectionContext
 */
@Slf4j
public class ContextReadinessBuilder<Builder> implements IContextReadinessBuilder<Builder>, IContextBuilderObserver {

    private static final String LOG_PRESENT = "present";
    private static final String LOG_ABSENT = "absent";
    private static final String ERROR_CONSUMER_NULL = "Context consumer cannot be null";
    private static final String LOG_CONTEXT_READY = "Context is ready, executing consumer";

    private final Object lock = new Object();
    private volatile IInjectionContextBuilder contextBuilder;
    private volatile IInjectionContext context;
    private Builder builder;

    /**
     * Constructs a new ContextReadinessBuilder with an optional context builder.
     *
     * @param contextBuilder the optional context builder
     * @throws NullPointerException if the Optional itself is null
     */
    public ContextReadinessBuilder(Optional<IInjectionContextBuilder> contextBuilder, Builder builder) {
        Objects.requireNonNull(contextBuilder, "Optional context builder cannot be null");
        synchronized (lock) {
            this.contextBuilder = contextBuilder.orElse(null);
            this.context = null;
            this.builder = Objects.requireNonNull(builder, "Builder cannot be null");

            if( this.contextBuilder != null ) {
                this.contextBuilder.observer(this);
            }

            log.atDebug().log("ContextReadinessBuilder initialized with contextBuilder: {}, isReady: {}",
                    this.contextBuilder != null ? LOG_PRESENT : LOG_ABSENT,
                    isReady());
        }
    }

    /**
     * Constructs a new ContextReadinessBuilder with a context builder.
     *
     * @param contextBuilder the context builder (may be null)
     */
    public ContextReadinessBuilder(IInjectionContextBuilder contextBuilder, Builder builder) {
        this(Optional.ofNullable(contextBuilder), builder);
    }

    /**
     * Returns the context builder.
     *
     * @return the context builder, or null if not set
     */
    public IInjectionContextBuilder getContextBuilder() {
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
    public void setContextBuilder(IInjectionContextBuilder contextBuilder) {
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
    public boolean isReady() {
        synchronized (lock) {
            return contextBuilder != null && context != null && context.status() == LifecycleStatus.STARTED;
        }
    }

    public boolean isEmpty() {
        synchronized (lock) {
            return contextBuilder == null && context == null;
        }
    }

    /**
     * Returns the resolved dependency injection context.
     *
     * @return the context, or null if not yet provided
     */
    public IInjectionContext getContext() {
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
     * {@link #isReady()} will return true if a contextBuilder is also present,
     * or false if contextBuilder is null (mismatched state).
     * </p>
     *
     * @param context the dependency injection context (must not be null)
     * @throws NullPointerException if context is null
     */
    public void handle(IInjectionContext context) {
        log.atTrace().log("Handling context provision");

        Objects.requireNonNull(context, "Context cannot be null");

        synchronized (lock) {
            this.context = context;

            log.atInfo().log("Context provided, canBuild: {}", isReady());
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
            if (!isReady()) {
                log.atError().log("Operation attempted with invalid state - contextBuilder: {}, context: {}",
                        contextBuilder != null ? LOG_PRESENT : LOG_ABSENT,
                        context != null ? LOG_PRESENT : LOG_ABSENT);
                throw new IllegalStateException("Build is not authorized. Either both contextBuilder and context must be present, or both must be absent.");
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder context(IInjectionContextBuilder context) {
        log.atTrace().log("Setting context builder");

        Objects.requireNonNull(context, "Context cannot be null");

        synchronized (lock) {
            this.contextBuilder = context;
            this.contextBuilder.observer(this);

            log.atInfo().log("Context builder set, canBuild: {}", isReady());
        }
        return (Builder) this;
    }

    /**
     * Executes the provided action if the context is ready (build is authorized).
     *
     * <p>
     * This method checks if the context is ready using {@link #isReady()}.
     * If ready, it invokes the consumer with the available {@link IInjectionContext}.
     * If not ready, the consumer is not executed.
     * </p>
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * readinessBuilder.ifReady(context -> {
     *     // Use the context when it's ready
     *     context.queryBeans(someReference);
     * });
     * }</pre>
     *
     * @param contextConsumer the action to execute with the context if ready
     * @return this builder for method chaining
     * @throws NullPointerException if contextConsumer is null
     */
    public ContextReadinessBuilder<Builder> ifReady(Consumer<IInjectionContext> contextConsumer) {
        Objects.requireNonNull(contextConsumer, ERROR_CONSUMER_NULL);

        log.atTrace().log("Entering ifReady()");

        synchronized (lock) {
            if (isReady()) {
                log.atDebug().log(LOG_CONTEXT_READY);
                contextConsumer.accept(this.context);
            } else {
                log.atTrace().log("Context not ready, skipping consumer execution");
            }
        }

        log.atTrace().log("Exiting ifReady()");
        return this;
    }

    /**
     * Executes the provided action if the context is ready, otherwise executes the fallback action.
     *
     * <p>
     * This method checks if the context is ready using {@link #isReady()}.
     * If ready, it invokes the contextConsumer with the available {@link IInjectionContext}.
     * If not ready, it executes the fallbackAction instead.
     * </p>
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * readinessBuilder.ifReadyOrElse(
     *     context -> log.info("Using context"),
     *     () -> log.warn("Context not available")
     * );
     * }</pre>
     *
     * @param contextConsumer the action to execute with the context if ready
     * @param fallbackAction the action to execute if context is not ready
     * @return this builder for method chaining
     * @throws NullPointerException if contextConsumer or fallbackAction is null
     */
    public ContextReadinessBuilder<Builder> ifReadyOrElse(Consumer<IInjectionContext> contextConsumer, Runnable fallbackAction) {
        Objects.requireNonNull(contextConsumer, ERROR_CONSUMER_NULL);
        Objects.requireNonNull(fallbackAction, "Fallback action cannot be null");

        log.atTrace().log("Entering ifReadyOrElse()");

        synchronized (lock) {
            if (isReady()) {
                log.atDebug().log(LOG_CONTEXT_READY);
                contextConsumer.accept(this.context);
            } else {
                log.atDebug().log("Context not ready, executing fallback action");
                fallbackAction.run();
            }
        }

        log.atTrace().log("Exiting ifReadyOrElse()");
        return this;
    }

    /**
     * Executes the provided action if the context is ready, otherwise throws an exception.
     *
     * <p>
     * This method checks if the context is ready using {@link #isReady()}.
     * If ready, it invokes the contextConsumer with the available {@link IInjectionContext}.
     * If not ready, it throws an {@link IllegalStateException}.
     * </p>
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * readinessBuilder.ifReadyOrElseThrow(context -> {
     *     // Use the context - guaranteed to be ready
     *     context.queryBeans(someReference);
     * });
     * }</pre>
     *
     * @param contextConsumer the action to execute with the context if ready
     * @return this builder for method chaining
     * @throws NullPointerException if contextConsumer is null
     * @throws IllegalStateException if context is not ready
     */
    public ContextReadinessBuilder<Builder> ifReadyOrElseThrow(Consumer<IInjectionContext> contextConsumer) {
        Objects.requireNonNull(contextConsumer, ERROR_CONSUMER_NULL);

        log.atTrace().log("Entering ifReadyOrElseThrow()");

        synchronized (lock) {
            if (!isReady()) {
                log.atError().log("Context not ready, throwing exception - contextBuilder: {}, context: {}",
                        contextBuilder != null ? LOG_PRESENT : LOG_ABSENT,
                        context != null ? LOG_PRESENT : LOG_ABSENT);
                throw new IllegalStateException("Context is not ready. Either both contextBuilder and context must be present, or both must be absent.");
            }

            log.atDebug().log(LOG_CONTEXT_READY);
            contextConsumer.accept(this.context);
        }

        log.atTrace().log("Exiting ifReadyOrElseThrow()");
        return this;
    }

    /**
     * Executes the provided action if the context is ready, otherwise throws a custom exception.
     *
     * <p>
     * This method checks if the context is ready using {@link #isReady()}.
     * If ready, it invokes the contextConsumer with the available {@link IInjectionContext}.
     * If not ready, it calls the exceptionSupplier to create and throw a custom exception.
     * </p>
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * readinessBuilder.ifReadyOrElseThrow(
     *     context -> context.queryBeans(someReference),
     *     () -> new CustomException("Context initialization failed")
     * );
     * }</pre>
     *
     * @param <X> the type of exception to be thrown
     * @param contextConsumer the action to execute with the context if ready
     * @param exceptionSupplier the supplier of the exception to throw if not ready
     * @return this builder for method chaining
     * @throws NullPointerException if contextConsumer or exceptionSupplier is null
     * @throws X if context is not ready
     */
    public <X extends Throwable> ContextReadinessBuilder<Builder> ifReadyOrElseThrow(
            Consumer<IInjectionContext> contextConsumer,
            Supplier<? extends X> exceptionSupplier) throws X {
        Objects.requireNonNull(contextConsumer, ERROR_CONSUMER_NULL);
        Objects.requireNonNull(exceptionSupplier, "Exception supplier cannot be null");

        log.atTrace().log("Entering ifReadyOrElseThrow() with custom exception");

        synchronized (lock) {
            if (!isReady()) {
                log.atError().log("Context not ready, throwing custom exception - contextBuilder: {}, context: {}",
                        contextBuilder != null ? LOG_PRESENT : LOG_ABSENT,
                        context != null ? LOG_PRESENT : LOG_ABSENT);
                throw exceptionSupplier.get();
            }

            log.atDebug().log(LOG_CONTEXT_READY);
            contextConsumer.accept(this.context);
        }

        log.atTrace().log("Exiting ifReadyOrElseThrow() with custom exception");
        return this;
    }

    /**
     * Synchronizes packages from the InjectionContextBuilder to the provided package set.
     *
     * <p>
     * This method retrieves packages from the associated {@link IInjectionContextBuilder}
     * and adds them to the provided package set using the consumer callback.
     * This enables builders to automatically include packages declared in the
     * DI context when performing auto-detection.
     * </p>
     *
     * <p>
     * The synchronization is performed by:
     * </p>
     * <ol>
     *   <li>Checking if a context builder is present</li>
     *   <li>Retrieving packages from the context builder via {@code getPackages()}</li>
     *   <li>Converting the package array to a Set</li>
     *   <li>Calling the consumer callback with the package set to merge them</li>
     * </ol>
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * // In a builder's doAutoDetection() method:
     * this.readinessBuilder.synchronizePackagesFromContext(packages -> {
     *     int beforeSize = this.packages.size();
     *     this.packages.addAll(packages);
     *     int addedCount = this.packages.size() - beforeSize;
     *     if (addedCount > 0) {
     *         log.atDebug().log("Synchronized {} new packages", addedCount);
     *     }
     * });
     * }</pre>
     *
     * @param packageConsumer consumer that receives the packages from InjectionContextBuilder
     *                       and merges them with the builder's package set
     */
    public void synchronizePackagesFromContext(Consumer<Set<String>> packageConsumer) {
        log.atTrace().log("Entering synchronizePackagesFromContext()");

        synchronized (lock) {
            if (this.contextBuilder != null) {
                String[] contextPackages = this.contextBuilder.getPackages();
                if (contextPackages != null && contextPackages.length > 0) {
                    Set<String> packageSet = Set.of(contextPackages);
                    log.atDebug().log("Synchronizing {} packages from InjectionContextBuilder", contextPackages.length);
                    packageConsumer.accept(packageSet);
                } else {
                    log.atTrace().log("No packages to synchronize from InjectionContextBuilder");
                }
            } else {
                log.atTrace().log("No context builder present, skipping package synchronization");
            }
        }

        log.atTrace().log("Exiting synchronizePackagesFromContext()");
    }

}
