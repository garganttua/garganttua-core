package com.garganttua.core.mutex.dsl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.context.dsl.ContextReadinessBuilder;
import com.garganttua.core.injection.context.dsl.IDiContextBuilder;
import com.garganttua.core.mutex.IMutex;
import com.garganttua.core.mutex.IMutexFactory;
import com.garganttua.core.mutex.IMutexManager;
import com.garganttua.core.mutex.MutexManager;
import com.garganttua.core.mutex.dsl.IMutexManagerBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * Builder for constructing {@link IMutexManager} instances with configurable mutex factories.
 *
 * <p>
 * This builder allows configuring different mutex factories for different mutex types,
 * enabling flexible mutex management strategies within an application.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * IMutexManager manager = MutexManagerBuilder.builder()
 *     .withFactory(InterruptibleLeaseMutex.class, new InterruptibleLeaseMutexFactory())
 *     .build();
 * }</pre>
 *
 * <h2>Auto-detection</h2>
 * <p>
 * When auto-detection is enabled, the builder will scan the dependency injection context
 * for beans annotated with {@code @MutexFactory} and automatically register them.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This builder is not thread-safe. Each instance should be used by a single thread
 * during construction.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see IMutexManager
 * @see IMutexFactory
 */
@Slf4j
public class MutexManagerBuilder extends AbstractAutomaticBuilder<IMutexManagerBuilder, IMutexManager>
        implements IMutexManagerBuilder {

    private Map<Class<? extends IMutex>, IMutexFactory> factories = new HashMap<>();
    private ContextReadinessBuilder<IMutexManagerBuilder> readinessBuilder;

    private MutexManagerBuilder(Optional<IDiContextBuilder> contextBuilder) {
        super();
        this.readinessBuilder = new ContextReadinessBuilder<>(contextBuilder, this);

        log.atInfo().log("MutexManagerBuilder initialized");
    }

    @Override
    public IMutexManagerBuilder withFactory(Class<? extends IMutex> type, IMutexFactory factory) {
        log.atTrace().log("Entering withFactory({}, {})", type.getSimpleName(), factory.getClass().getSimpleName());

        Objects.requireNonNull(type, "Mutex type cannot be null");
        Objects.requireNonNull(factory, "Mutex factory cannot be null");

        this.factories.put(type, factory);

        log.atInfo().log("Registered factory {} for mutex type {}",
                factory.getClass().getSimpleName(), type.getSimpleName());

        log.atTrace().log("Exiting withFactory()");
        return this;
    }

    @Override
    protected IMutexManager doBuild() throws DslException {
        log.atTrace().log("Entering doBuild() method");

        if (this.readinessBuilder != null) {
            this.readinessBuilder.requireBuildAuthorization();
        }

        log.atInfo().log("Building MutexManager with {} registered factories", factories.size());

        IMutexManager manager = new MutexManager(this.factories);

        log.atTrace().log("Exiting doBuild() method");
        return manager;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("Entering doAutoDetection() method");

        if (this.readinessBuilder == null || !this.readinessBuilder.hasContext()) {
            log.atWarn().log("Auto-detection requested but no DI context available");
            return;
        }

        log.atInfo().log("Auto-detecting mutex factories from DI context");

        // TODO: Implement auto-detection logic to scan for @MutexFactory beans
        // This will query the DI context for beans with @MutexFactory annotation
        // and register them automatically

        log.atTrace().log("Exiting doAutoDetection() method");
    }

    @Override
    public IMutexManagerBuilder context(IDiContextBuilder context) {
        log.atTrace().log("Entering context() method");

        Objects.requireNonNull(context, "Context builder cannot be null");

        this.readinessBuilder.setContextBuilder(context);

        log.atInfo().log("Context builder configured");

        log.atTrace().log("Exiting context() method");
        return this;
    }

    /**
     * Creates a new builder instance without a dependency injection context.
     *
     * @return a new MutexManagerBuilder instance
     */
    public static IMutexManagerBuilder builder() {
        log.atTrace().log("Entering builder() with no parameters");
        IMutexManagerBuilder result = new MutexManagerBuilder(Optional.empty());
        log.atTrace().log("Exiting builder() with no parameters");
        return result;
    }

    /**
     * Creates a new builder instance with a dependency injection context.
     *
     * @param contextBuilder the context builder to use
     * @return a new MutexManagerBuilder instance
     */
    public static IMutexManagerBuilder builder(IDiContextBuilder contextBuilder) {
        log.atTrace().log("Entering builder() with IDiContextBuilder parameter");
        IMutexManagerBuilder result = new MutexManagerBuilder(Optional.ofNullable(contextBuilder));
        log.atTrace().log("Exiting builder() with IDiContextBuilder parameter");
        return result;
    }

    /**
     * Creates a new builder instance with an optional dependency injection context.
     *
     * @param contextBuilder the optional context builder to use
     * @return a new MutexManagerBuilder instance
     */
    public static IMutexManagerBuilder builder(Optional<IDiContextBuilder> contextBuilder) {
        log.atTrace().log("Entering builder() with Optional<IDiContextBuilder> parameter");
        IMutexManagerBuilder result = new MutexManagerBuilder(contextBuilder);
        log.atTrace().log("Exiting builder() with Optional<IDiContextBuilder> parameter");
        return result;
    }

}
