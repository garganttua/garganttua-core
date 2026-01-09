package com.garganttua.core.dsl;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of builder dependency tracking and lifecycle management.
 *
 * <p>
 * BuilderDependency tracks the state of a dependency on another builder,
 * managing the transition from unresolved to resolved state when the
 * dependency is provided. It implements the observer pattern to receive
 * notifications when the dependency builder produces its built object.
 * </p>
 *
 * <h2>State Management</h2>
 * <p>
 * The dependency state follows a similar pattern to {@link com.garganttua.core.injection.context.dsl.ContextReadinessBuilder}:
 * </p>
 * <ul>
 *   <li><b>isEmpty()</b>: Returns true when both builder and builtObject are null (no dependency provided yet)</li>
 *   <li><b>isReady()</b>: Returns true when builder is not null AND builtObject is not null (dependency fully resolved)</li>
 * </ul>
 *
 * @param <Builder> the type of the observable builder being depended upon
 * @param <Built> the type of object built by the dependency
 * @since 2.0.0-ALPHA01
 */
@Slf4j
public class BuilderDependency<Builder extends IObservableBuilder<Builder, Built>, Built>
        implements IBuilderDependency<Builder, Built> {

    private static final String LOG_PRESENT = "present";
    private static final String LOG_ABSENT = "absent";

    private final Class<Builder> dependencyClass;
    private Builder builder;
    private Built builtObject;
    private final Set<String> packages = new HashSet<>();

    /**
     * Creates a new builder dependency for the specified class.
     *
     * @param dependencyClass the class of the builder being depended upon
     */
    @SuppressWarnings("unchecked")
    public BuilderDependency(Class<? extends IObservableBuilder<?, ?>> dependencyClass) {
        log.atTrace().log("Creating BuilderDependency for class: {}", dependencyClass);
        this.dependencyClass = (Class<Builder>) Objects.requireNonNull(dependencyClass,
            "Dependency class cannot be null");
        log.atDebug().log("BuilderDependency created for: {}, isReady: {}, isEmpty: {}",
            this.dependencyClass.getName(), isReady(), isEmpty());
    }

    /**
     * Handles the notification when the dependency builder provides its built object.
     * This is called internally when a dependency is satisfied.
     *
     * @param dependency the observable builder providing the dependency
     */
    @SuppressWarnings("unchecked")
    void handle(IObservableBuilder<?, ?> dependency) {
        log.atTrace().log("Handling dependency provision: {}", dependency);
        if (!dependencyClass.isAssignableFrom(dependency.getClass())) {
            log.atWarn().log("Dependency type mismatch: expected {}, got {}",
                dependencyClass.getName(), dependency.getClass().getName());
            return;
        }

        this.builder = (Builder) dependency;
        try {
            this.builtObject = this.builder.build();
            log.atInfo().log("Dependency ready: {} -> {}, isReady: {}", 
                dependencyClass.getName(), builtObject, isReady());
        } catch (DslException e) {
            log.atError().log("Failed to build dependency: {}", dependencyClass.getName(), e);
            this.builtObject = null; // Ensure consistent state on failure
        }
    }

    @Override
    public void handle(Built observable) {
        log.atTrace().log("Handling built object notification: {}", observable);
        this.builtObject = Objects.requireNonNull(observable, "Built object cannot be null");
        log.atDebug().log("Dependency marked with built object: {}, isReady: {}", observable, isReady());
    }

    /**
     * Checks if the dependency is ready for use.
     *
     * <p>
     * The dependency is considered ready when both the builder and built object are present.
     * This mirrors the pattern from ContextReadinessBuilder where both components must be
     * available for the dependency to be fully resolved.
     * </p>
     *
     * @return true if both builder and builtObject are not null, false otherwise
     */
    @Override
    public boolean isReady() {
        boolean ready = builder != null && builtObject != null;
        log.atTrace().log("Checking if dependency is ready: {} (builder: {}, builtObject: {})",
            ready,
            builder != null ? LOG_PRESENT : LOG_ABSENT,
            builtObject != null ? LOG_PRESENT : LOG_ABSENT);
        return ready;
    }

    /**
     * Checks if the dependency is empty (not yet provided).
     *
     * <p>
     * The dependency is considered empty when neither the builder nor the built object
     * have been provided. This mirrors the pattern from ContextReadinessBuilder where
     * an empty state means no components have been set.
     * </p>
     *
     * @return true if both builder and builtObject are null, false otherwise
     */
    @Override
    public boolean isEmpty() {
        boolean empty = builder == null && builtObject == null;
        log.atTrace().log("Checking if dependency is empty: {} (builder: {}, builtObject: {})",
            empty,
            builder != null ? LOG_PRESENT : LOG_ABSENT,
            builtObject != null ? LOG_PRESENT : LOG_ABSENT);
        return empty;
    }

    @Override
    public Class<Builder> getDependency() {
        log.atTrace().log("Getting dependency class: {}", dependencyClass);
        return dependencyClass;
    }

    @Override
    public Built get() {
        log.atTrace().log("Getting built object: {}", builtObject);
        if (!isReady()) {
            log.atWarn().log("Attempting to get built object from non-ready dependency - builder: {}, builtObject: {}",
                builder != null ? LOG_PRESENT : LOG_ABSENT,
                builtObject != null ? LOG_PRESENT : LOG_ABSENT);
            throw new IllegalStateException("Dependency is not ready: " + dependencyClass.getName());
        }
        return builtObject;
    }

    @Override
    public Builder builder() {
        log.atTrace().log("Getting builder: {}", builder);
        if (builder == null) {
            log.atWarn().log("Attempting to get null builder");
            throw new IllegalStateException("Builder not yet provided for: " + dependencyClass.getName());
        }
        return builder;
    }

    @Override
    public void ifReady(Consumer<Built> consumer) {
        log.atTrace().log("Executing ifReady with consumer");
        if (isReady()) {
            log.atDebug().log("Dependency is ready, executing consumer");
            consumer.accept(builtObject);
        } else {
            log.atDebug().log("Dependency not ready, skipping consumer");
        }
    }

    @Override
    public void ifReadyOrElse(Consumer<Built> consumer, Runnable fallbackAction) {
        log.atTrace().log("Executing ifReadyOrElse");
        if (isReady()) {
            log.atDebug().log("Dependency is ready, executing consumer");
            consumer.accept(builtObject);
        } else {
            log.atDebug().log("Dependency not ready, executing fallback action");
            fallbackAction.run();
        }
    }

    @Override
    public void ifReadyOrElseThrow(Consumer<Built> consumer) {
        log.atTrace().log("Executing ifReadyOrElseThrow");
        if (!isReady()) {
            log.atError().log("Dependency not ready, throwing exception - builder: {}, builtObject: {}",
                builder != null ? LOG_PRESENT : LOG_ABSENT,
                builtObject != null ? LOG_PRESENT : LOG_ABSENT);
            throw new IllegalStateException("Dependency is not ready: " + dependencyClass.getName());
        }
        
        log.atDebug().log("Dependency is ready, executing consumer");
        consumer.accept(builtObject);
    }

    @Override
    public <X extends Throwable> void ifReadyOrElseThrow(
            Consumer<Built> consumer,
            Supplier<? extends X> exceptionSupplier) throws X {
        log.atTrace().log("Executing ifReadyOrElseThrow with custom exception");
        if (!isReady()) {
            log.atError().log("Dependency not ready, throwing custom exception - builder: {}, builtObject: {}",
                builder != null ? LOG_PRESENT : LOG_ABSENT,
                builtObject != null ? LOG_PRESENT : LOG_ABSENT);
            throw exceptionSupplier.get();
        }
        
        log.atDebug().log("Dependency is ready, executing consumer");
        consumer.accept(builtObject);
    }

    @Override
    public void synchronizePackagesFromContext(Consumer<Set<String>> packageConsumer) {
        log.atTrace().log("Synchronizing packages from context");
        packageConsumer.accept(packages);
        log.atDebug().log("Packages synchronized: {}", packages.size());
    }
}
