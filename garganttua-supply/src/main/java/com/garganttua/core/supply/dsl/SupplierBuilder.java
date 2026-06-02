package com.garganttua.core.supply.dsl;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.binders.IConstructorBinder;
import com.garganttua.core.reflection.binders.IContextualConstructorBinder;
import com.garganttua.core.supply.ContextualSupplier;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.IContextualSupply;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.NewContextualSupplier;
import com.garganttua.core.supply.NewSupplier;
import com.garganttua.core.supply.NullSupplier;
import com.garganttua.core.supply.NullableContextualSupplier;
import com.garganttua.core.supply.NullableSupplier;
import com.garganttua.core.supply.FutureSupplier;
import com.garganttua.core.supply.BlockingSupplier;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * General-purpose builder that produces the appropriate {@link ISupplier}
 * implementation from the configured value source.
 *
 * <p>
 * Depending on which {@code withXxx(...)} method was called, {@link #build()}
 * yields a future, blocking-queue, fixed, contextual, new-instance, or null
 * supplier. Unless explicitly forbidden, the result is wrapped in a nullable
 * guard reflecting the {@link #nullable(boolean)} setting.
 * </p>
 *
 * @param <Supplied> the type of object supplied by the built supplier
 * @since 2.0.0-ALPHA01
 * @see ICommonSupplierBuilder
 * @see ISupplier
 */
@Reflected
public class SupplierBuilder<Supplied>
        implements ICommonSupplierBuilder<Supplied> {
    private static final Logger log = Logger.getLogger(SupplierBuilder.class);

    private IClass<?> contextType;
    private IContextualSupply<Supplied, ?> supply;
    private Supplied value;
    private IConstructorBinder<Supplied> constructorBinder;
    private boolean nullable = false;
    private IClass<Supplied> suppliedClass;
    private CompletableFuture<Supplied> future;
    private BlockingQueue<Supplied> blockingQueue;
    private Long timeoutMillis;

    /**
     * Creates a SupplierBuilder for the given supplied type.
     *
     * @param suppliedClass the {@link IClass} of the supplied object
     */
    public SupplierBuilder(IClass<Supplied> suppliedClass) {
        log.trace("Entering SupplierBuilder constructor with suppliedClass={}", suppliedClass);
        this.suppliedClass = Objects.requireNonNull(suppliedClass, "Supplied class cannot be null");
        log.debug("SupplierBuilder created for type {}", this.suppliedClass);
        log.trace("Exiting SupplierBuilder constructor");
    }

    @Override
    public Type getSuppliedType() {
        return this.suppliedClass.getType();
    }

    @Override
    public IClass<Supplied> getSuppliedClass() {
        return this.suppliedClass;
    }

    @Override
    public boolean isContextual() {
        return this.contextType != null;
    }

    /**
     * Builds the supplier matching the configured value source.
     *
     * <p>
     * Source precedence is future, then blocking queue, then fixed value, then
     * context, then constructor binder; if none is set a {@link NullSupplier} is
     * produced. The result is wrapped in a nullable guard per
     * {@link #nullable(boolean)}.
     * </p>
     *
     * @return the constructed supplier
     * @throws DslException if a context is set but the constructor binder is not contextual
     */
    @SuppressWarnings({ "rawtypes" })
    @Override
    public ISupplier<Supplied> build() throws DslException {
        log.trace("Entering build for suppliedClass={}", this.suppliedClass);
        ISupplier<Supplied> supplier;

        if (this.future != null) {
            log.debug("Building FutureSupplier with timeout={}", this.timeoutMillis);
            supplier = new FutureSupplier<>(this.future, this.suppliedClass, this.timeoutMillis);
            log.debug("Built FutureSupplier for type {}, nullable={}", this.suppliedClass, this.nullable);
            log.trace("Exiting build");
            return wrapNullable(supplier, this.nullable);
        }

        if (this.blockingQueue != null) {
            log.debug("Building BlockingSupplier with timeout={}", this.timeoutMillis);
            supplier = new BlockingSupplier<>(this.blockingQueue, this.suppliedClass, this.timeoutMillis);
            log.debug("Built BlockingSupplier for type {}, nullable={}", this.suppliedClass, this.nullable);
            log.trace("Exiting build");
            return wrapNullable(supplier, this.nullable);
        }

        if (this.value != null) {
            log.debug("Building FixedSupplier with value of type {}", this.value.getClass().getName());
            supplier = new FixedSupplier<>(this.value, this.suppliedClass);
            log.debug("Built FixedSupplier for type {}, nullable={}", this.suppliedClass, this.nullable);
            log.trace("Exiting build");
            return wrapNullable(supplier, this.nullable);
        }

        if (this.contextType != null) {
            if (this.constructorBinder != null) {
                log.debug("Building NewContextualSupplier with contextType={}", this.contextType);
                if (!(this.constructorBinder instanceof IContextualConstructorBinder<Supplied> contextualCtorBinder)) {
                    log.error("Constructor binder is not contextual: {}", this.constructorBinder.getClass().getSimpleName());
                    throw new DslException(
                            "Context expected but constructor binder is not contextual: "
                                    + this.constructorBinder.getClass().getSimpleName());
                }

                supplier = new NewContextualSupplier<>(this.suppliedClass,
                        (IClass<Void>) this.contextType, contextualCtorBinder);

            } else {
                log.debug("Building ContextualSupplier with contextType={}", this.contextType);
                supplier = new ContextualSupplier(this.supply, this.suppliedClass, this.contextType);
            }

            log.debug("Built contextual supplier for type {}, contextType={}, nullable={}", this.suppliedClass, this.contextType, this.nullable);
            log.trace("Exiting build");
            return wrapNullableContextual(
                    (IContextualSupplier<Supplied, ?>) supplier,
                    this.nullable);
        }

        if (this.constructorBinder != null) {
            log.debug("Building NewSupplier with constructorBinder");
            supplier = new NewSupplier<>(this.suppliedClass, this.constructorBinder);
            log.debug("Built NewSupplier for type {}, nullable={}", this.suppliedClass, this.nullable);
            log.trace("Exiting build");
            return wrapNullable(supplier, this.nullable);
        }

        log.debug("Building NullSupplier for type {}", this.suppliedClass);
        supplier = new NullSupplier<>(this.suppliedClass);
        log.debug("Built NullSupplier for type {}", this.suppliedClass);
        log.trace("Exiting build");
        return wrapNullable(supplier, true);
    }

    /**
     * Sets whether the built supplier is allowed to supply a null value.
     *
     * @param nullable {@code true} to permit null results
     * @return this builder for chaining
     */
    @Override
    public ICommonSupplierBuilder<Supplied> nullable(boolean nullable) {
        this.nullable = Objects.requireNonNull(nullable, "Nullable cannot be null");
        return this;
    }

    /**
     * Configures a contextual supply source.
     *
     * @param <ContextType> the context type
     * @param contextType the {@link IClass} of the context
     * @param supply the contextual supply function
     * @return this builder for chaining
     * @throws DslException if the configuration is invalid
     */
    @Override
    public <ContextType> ICommonSupplierBuilder<Supplied> withContext(
            IClass<ContextType> contextType,
            IContextualSupply<Supplied, ContextType> supply) throws DslException {
        log.trace("Entering withContext with contextType={}", contextType);
        this.contextType = Objects.requireNonNull(contextType, "Context type cannot be null");
        this.supply = Objects.requireNonNull(supply, "Supply cannot be null");
        log.debug("Context configured for type {} with contextType={}", this.suppliedClass, contextType);
        log.trace("Exiting withContext");
        return this;

    }

    /**
     * Configures a fixed value source.
     *
     * @param value the value to supply, may be {@code null}
     * @return this builder for chaining
     * @throws DslException if the configuration is invalid
     */
    @Override
    public ICommonSupplierBuilder<Supplied> withValue(Supplied value) throws DslException {
        log.trace("Entering withValue with value type={}", value != null ? value.getClass().getName() : "null");
        this.value = value;
        log.debug("Value configured for type {}", this.suppliedClass);
        log.trace("Exiting withValue");
        return this;
    }

    /**
     * Configures a constructor binder used to create new instances at supply time.
     *
     * @param constructorBinder the constructor binder
     * @return this builder for chaining
     * @throws DslException if the configuration is invalid
     */
    @Override
    public ICommonSupplierBuilder<Supplied> withConstructor(
            IConstructorBinder<Supplied> constructorBinder)
            throws DslException {
        log.trace("Entering withConstructor for type {}", this.suppliedClass);
        this.constructorBinder = Objects.requireNonNull(constructorBinder, "Constructor binder cannot be null");
        log.debug("Constructor binder configured for type {}", this.suppliedClass);
        log.trace("Exiting withConstructor");
        return this;
    }

    private ISupplier<Supplied> wrapNullable(ISupplier<Supplied> supplier, boolean nullable) {
        return new NullableSupplier<>(supplier, nullable);
    }

    private ISupplier<Supplied> wrapNullableContextual(
            IContextualSupplier<Supplied, ?> supplier, boolean nullable) {

        return new NullableContextualSupplier<>(supplier, nullable);
    }

    /**
     * Creates a builder pre-configured with a fixed, non-null value.
     *
     * @param <T> the supplied type
     * @param suppliedClass the {@link IClass} of the supplied object
     * @param value the value to supply
     * @return a configured builder
     */
    public static <T> ICommonSupplierBuilder<T> fixed(IClass<T> suppliedClass, T value) {
        log.trace("Creating fixed supplier builder for type {}", suppliedClass);
        return new SupplierBuilder<>(suppliedClass).withValue(value).nullable(false);
    }

    /**
     * Creates a builder that produces new instances via a constructor binder.
     *
     * @param <T> the supplied type
     * @param suppliedClass the {@link IClass} of the supplied object
     * @param binder the constructor binder
     * @return a configured builder
     */
    public static <T> ICommonSupplierBuilder<T> newObject(IClass<T> suppliedClass, IConstructorBinder<T> binder) {
        log.trace("Creating newObject supplier builder for type {}", suppliedClass);
        return new SupplierBuilder<>(suppliedClass).withConstructor(binder);
    }

    /**
     * Creates a builder that produces a nullable {@link NullSupplier}.
     *
     * @param <T> the supplied type
     * @param suppliedClass the {@link IClass} of the supplied object
     * @return a configured builder
     */
    public static <T> ICommonSupplierBuilder<T> nullObject(IClass<T> suppliedClass) {
        log.trace("Creating nullObject supplier builder for type {}", suppliedClass);
        return new SupplierBuilder<>(suppliedClass).nullable(true);
    }

    /**
     * Creates a builder configured with a contextual supply function.
     *
     * @param <T> the supplied type
     * @param <C> the context type
     * @param suppliedClass the {@link IClass} of the supplied object
     * @param contextClass the {@link IClass} of the context
     * @param supply the contextual supply function
     * @return a configured builder
     * @throws DslException if the configuration is invalid
     */
    public static <T, C> ICommonSupplierBuilder<T> contextual(
            IClass<T> suppliedClass,
            IClass<C> contextClass,
            IContextualSupply<T, C> supply) throws DslException {
        log.trace("Creating contextual supplier builder for type {} with contextClass={}", suppliedClass, contextClass);
        return new SupplierBuilder<>(suppliedClass).withContext(contextClass, supply);
    }

    /**
     * Creates a builder that constructs new instances using a contextual
     * constructor binder.
     *
     * @param <T> the supplied type
     * @param <C> the context type
     * @param suppliedClass the {@link IClass} of the supplied object
     * @param contextClass the {@link IClass} of the context
     * @param binder the contextual constructor binder
     * @return a configured builder
     */
    public static <T, C> ICommonSupplierBuilder<T> newContextual(
            IClass<T> suppliedClass,
            IClass<C> contextClass,
            IContextualConstructorBinder<T> binder) {
        log.trace("Creating newContextual supplier builder for type {} with contextClass={}", suppliedClass, contextClass);
        SupplierBuilder<T> builder = new SupplierBuilder<>(suppliedClass);
        builder.contextType = contextClass;
        builder.constructorBinder = binder;
        return builder;
    }

    /**
     * Configures an asynchronous source backed by a {@link CompletableFuture},
     * with no timeout.
     *
     * @param future the future to await at supply time
     * @return this builder for chaining
     * @throws DslException if the configuration is invalid
     */
    @Override
    public ICommonSupplierBuilder<Supplied> withFuture(CompletableFuture<Supplied> future) throws DslException {
        log.trace("Entering withFuture with future");
        this.future = Objects.requireNonNull(future, "Future cannot be null");
        log.debug("Future configured for type {}", this.suppliedClass);
        log.trace("Exiting withFuture");
        return this;
    }

    /**
     * Configures an asynchronous source backed by a {@link CompletableFuture},
     * awaited up to the given timeout.
     *
     * @param future the future to await at supply time
     * @param timeoutMillis the timeout in milliseconds, or {@code null} for no timeout
     * @return this builder for chaining
     * @throws DslException if the configuration is invalid
     */
    @Override
    public ICommonSupplierBuilder<Supplied> withFuture(CompletableFuture<Supplied> future, Long timeoutMillis)
            throws DslException {
        log.trace("Entering withFuture with future and timeout={}", timeoutMillis);
        this.future = Objects.requireNonNull(future, "Future cannot be null");
        this.timeoutMillis = timeoutMillis;
        log.debug("Future configured for type {} with timeout={}", this.suppliedClass, timeoutMillis);
        log.trace("Exiting withFuture");
        return this;
    }

    /**
     * Configures a {@link BlockingQueue} source with no timeout.
     *
     * @param queue the queue to poll at supply time
     * @return this builder for chaining
     * @throws DslException if the configuration is invalid
     */
    @Override
    public ICommonSupplierBuilder<Supplied> withBlockingQueue(BlockingQueue<Supplied> queue) throws DslException {
        log.trace("Entering withBlockingQueue with queue");
        this.blockingQueue = Objects.requireNonNull(queue, "BlockingQueue cannot be null");
        log.debug("BlockingQueue configured for type {}", this.suppliedClass);
        log.trace("Exiting withBlockingQueue");
        return this;
    }

    /**
     * Configures a {@link BlockingQueue} source polled up to the given timeout.
     *
     * @param queue the queue to poll at supply time
     * @param timeoutMillis the timeout in milliseconds, or {@code null} for indefinite wait
     * @return this builder for chaining
     * @throws DslException if the configuration is invalid
     */
    @Override
    public ICommonSupplierBuilder<Supplied> withBlockingQueue(BlockingQueue<Supplied> queue, Long timeoutMillis)
            throws DslException {
        log.trace("Entering withBlockingQueue with queue and timeout={}", timeoutMillis);
        this.blockingQueue = Objects.requireNonNull(queue, "BlockingQueue cannot be null");
        this.timeoutMillis = timeoutMillis;
        log.debug("BlockingQueue configured for type {} with timeout={}", this.suppliedClass, timeoutMillis);
        log.trace("Exiting withBlockingQueue");
        return this;
    }

}
