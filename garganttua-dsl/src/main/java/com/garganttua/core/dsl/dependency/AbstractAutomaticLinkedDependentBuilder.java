package com.garganttua.core.dsl.dependency;

import java.util.Objects;
import java.util.Set;

import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.ILinkedBuilder;
import com.garganttua.core.dsl.IObservableBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * Abstract builder combining automatic detection, linked navigation, and dependency management.
 *
 * <p>
 * This class extends {@link AbstractAutomaticBuilder} and implements both {@link ILinkedBuilder}
 * and {@link IDependentBuilder} to provide:
 * <ul>
 *   <li>Auto-detection capabilities via {@code autoDetect()}</li>
 *   <li>Linked builder navigation via {@code up()}</li>
 *   <li>Dependency declaration and tracking via {@code provide()}, {@code use()}, and {@code require()}</li>
 * </ul>
 * </p>
 *
 * <h2>Dependency Lifecycle</h2>
 * <p>
 * Dependencies are managed through three main phases:
 * </p>
 * <ol>
 *   <li><b>Declaration</b>: Dependencies are declared in the constructor via use/require sets</li>
 *   <li><b>Provision</b>: Concrete dependency instances are provided via {@code provide()}</li>
 *   <li><b>Resolution</b>: During {@code build()}, dependencies are validated and processed</li>
 * </ol>
 *
 * <h2>Build Process with Auto-Detection and Linked Navigation</h2>
 * <p>
 * The build process follows this sequence:
 * </p>
 * <ol>
 *   <li>Auto-detection: If enabled, {@code doAutoDetection()} is called</li>
 *   <li>Auto-detection with dependencies: {@code doAutoDetectionWithDependency()} for each ready dependency</li>
 *   <li>Pre-build: Process ready dependencies via {@code doPreBuildWithDependency()}</li>
 *   <li>Build: Execute {@code doBuild()} to create the target object</li>
 *   <li>Post-build: Process ready dependencies via {@code doPostBuildWithDependency()}</li>
 * </ol>
 *
 * @param <B> the concrete builder type for method chaining
 * @param <L> the type of the linked parent builder
 * @param <T> the type of object this builder constructs
 * @since 2.0.0-ALPHA01
 * @see AbstractAutomaticBuilder
 * @see ILinkedBuilder
 * @see IDependentBuilder
 */
@Slf4j
public abstract class AbstractAutomaticLinkedDependentBuilder<B extends IBuilder<T>, L, T>
        extends AbstractAutomaticBuilder<B, T>
        implements IAutomaticLinkedBuilder<B, L, T>, IDependentBuilder<B, T> {

    private L link;
    protected final DependentBuilderSupport support;

    /**
     * Constructs a new AbstractAutomaticLinkedDependentBuilder with specified link and dependency sets.
     *
     * @param link                the parent builder to link to
     * @param useDependencies    the set of optional dependency classes
     * @param requireDependencies the set of required dependency classes
     * @throws NullPointerException if any parameter is null
     */
    @Deprecated(since = "2.0.0-ALPHA01", forRemoval = true)
    protected AbstractAutomaticLinkedDependentBuilder(
            L link,
            Set<Class<? extends IObservableBuilder<?, ?>>> useDependencies,
            Set<Class<? extends IObservableBuilder<?, ?>>> requireDependencies) {
        super();
        log.atTrace().log("Entering AbstractAutomaticLinkedDependentBuilder constructor");
        this.link = Objects.requireNonNull(link, "Link cannot be null");
        this.support = new DependentBuilderSupport(useDependencies, requireDependencies);
        this.autoDetect = false;
        log.atDebug().log("AbstractAutomaticLinkedDependentBuilder initialized with link, {} use and {} require dependencies",
            useDependencies.size(), requireDependencies.size());
        log.atTrace().log("Exiting AbstractAutomaticLinkedDependentBuilder constructor");
    }

    protected AbstractAutomaticLinkedDependentBuilder(
            L link,
            Set<DependencySpec> dependencies) {
        super();
        log.atTrace().log("Entering AbstractAutomaticLinkedDependentBuilder constructor");
        this.link = Objects.requireNonNull(link, "Link cannot be null");
        this.support = new DependentBuilderSupport(dependencies);
        this.autoDetect = false;
        log.atDebug().log("AbstractAutomaticLinkedDependentBuilder initialized with link, {} dependencies",
            dependencies.size());
        log.atTrace().log("Exiting AbstractAutomaticLinkedDependentBuilder constructor");
    }

    @Override
    public L up() {
        log.atTrace().log("Entering up() method");
        log.atDebug().log("Returning link: {}", this.link);
        log.atTrace().log("Exiting up() method");
        return this.link;
    }

    @SuppressWarnings("unchecked")
    @Override
    public B setUp(L link) {
        log.atTrace().log("Entering setUp() with link: {}", link);
        this.link = Objects.requireNonNull(link, "Link cannot be null");
        log.atDebug().log("Link updated to: {}", this.link);
        log.atTrace().log("Exiting setUp()");
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public B provide(IObservableBuilder<?, ?> dependency) throws DslException {
        this.support.provide(dependency);
        return (B) this;
    }

    @Override
    public Set<Class<? extends IObservableBuilder<?, ?>>> use() {
        return this.support.use();
    }

    @Override
    public Set<Class<? extends IObservableBuilder<?, ?>>> require() {
        return this.support.require();
    }

    @Override
    public T build() throws DslException {
        log.atTrace().log("Entering build method");

        if (this.built != null) {
            log.atDebug().log("Returning previously built instance: {}", this.built);
            log.atTrace().log("Exiting build method (cached)");
            return this.built;
        }

        try {
            // Phase 1: Auto-detection
            if (this.autoDetect.booleanValue()) {
                log.atDebug().log("Auto-detection is enabled, performing auto-detection");
                this.doAutoDetection();
                log.atDebug().log("Base auto-detection completed");

                // Auto-detection with dependencies
                this.support.processAutoDetectionWithDependencies(this::doAutoDetectionWithDependency);
            } else {
                log.atDebug().log("Auto-detection is disabled, skipping auto-detection");
            }

            // Phase 2: Pre-build with dependencies
            this.support.processPreBuildDependencies(this::doPreBuildWithDependency);

            // Phase 3: Build the target object
            log.atDebug().log("Building the instance");
            this.built = this.doBuild();
            log.atDebug().log("Built instance: {}", this.built);

            // Phase 4: Post-build with dependencies
            this.support.processPostBuildDependencies(this::doPostBuildWithDependency);

            log.atTrace().log("Exiting build method");
            return this.built;
        } catch (DslException e) {
            log.atError().log("Critical error during build", e);
            throw e;
        }
    }

    /**
     * Performs auto-detection enhanced with a ready dependency.
     * This method is called for each ready dependency during the auto-detection phase.
     *
     * @param dependency the resolved dependency object
     * @throws DslException if an error occurs during auto-detection with dependency
     */
    protected abstract void doAutoDetectionWithDependency(Object dependency) throws DslException;

    /**
     * Processes a dependency before building the target object.
     * This method is called for each ready dependency during the pre-build phase.
     *
     * @param dependency the resolved dependency object
     */
    protected abstract void doPreBuildWithDependency(Object dependency);

    /**
     * Processes a dependency after building the target object.
     * This method is called for each ready dependency during the post-build phase.
     *
     * @param dependency the resolved dependency object
     */
    protected abstract void doPostBuildWithDependency(Object dependency);
}
