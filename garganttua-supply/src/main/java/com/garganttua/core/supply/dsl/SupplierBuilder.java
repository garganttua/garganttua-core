package com.garganttua.core.supply.dsl;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

import com.garganttua.core.dsl.DslException;
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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SupplierBuilder<Supplied>
        implements ICommonSupplierBuilder<Supplied> {

    private Class<?> contextType;
    private IContextualSupply<Supplied, ?> supply;
    private Supplied value;
    private IConstructorBinder<Supplied> constructorBinder;
    private boolean nullable = false;
    private Class<Supplied> suppliedType;
    private CompletableFuture<Supplied> future;
    private BlockingQueue<Supplied> blockingQueue;
    private Long timeoutMillis;

    public SupplierBuilder(Class<Supplied> suppliedType) {
        log.atTrace().log("Entering SupplierBuilder constructor with suppliedType={}", suppliedType);
        this.suppliedType = Objects.requireNonNull(suppliedType, "Supplied type cannot be null");
        log.atDebug().log("SupplierBuilder created for type {}", this.suppliedType);
        log.atTrace().log("Exiting SupplierBuilder constructor");
    }

    @Override
    public Type getSuppliedType() {
        return this.suppliedType;
    }

    @Override
    public boolean isContextual() {
        return this.contextType != null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public ISupplier<Supplied> build() throws DslException {
        log.atTrace().log("Entering build for suppliedType={}", this.suppliedType);
        ISupplier<Supplied> supplier;

        if (this.future != null) {
            log.atDebug().log("Building FutureSupplier with timeout={}", this.timeoutMillis);
            supplier = new FutureSupplier<>(this.future, this.suppliedType, this.timeoutMillis);
            log.atInfo().log("Built FutureSupplier for type {}, nullable={}", this.suppliedType, this.nullable);
            log.atTrace().log("Exiting build");
            return wrapNullable(supplier, this.nullable);
        }

        if (this.blockingQueue != null) {
            log.atDebug().log("Building BlockingSupplier with timeout={}", this.timeoutMillis);
            supplier = new BlockingSupplier<>(this.blockingQueue, this.suppliedType, this.timeoutMillis);
            log.atInfo().log("Built BlockingSupplier for type {}, nullable={}", this.suppliedType, this.nullable);
            log.atTrace().log("Exiting build");
            return wrapNullable(supplier, this.nullable);
        }

        if (this.value != null) {
            log.atDebug().log("Building FixedSupplier with value of type {}", this.value.getClass().getName());
            supplier = new FixedSupplier<>(this.value);
            log.atInfo().log("Built FixedSupplier for type {}, nullable={}", this.suppliedType, this.nullable);
            log.atTrace().log("Exiting build");
            return wrapNullable(supplier, this.nullable);
        }

        if (this.contextType != null) {
            if (this.constructorBinder != null) {
                log.atDebug().log("Building NewContextualSupplier with contextType={}", this.contextType);
                if (!(this.constructorBinder instanceof IContextualConstructorBinder<Supplied> contextualCtorBinder)) {
                    log.atError().log("Constructor binder is not contextual: {}", this.constructorBinder.getClass().getSimpleName());
                    throw new DslException(
                            "Context expected but constructor binder is not contextual: "
                                    + this.constructorBinder.getClass().getSimpleName());
                }

                supplier = new NewContextualSupplier<>(this.suppliedType, contextualCtorBinder);

            } else {
                log.atDebug().log("Building ContextualSupplier with contextType={}", this.contextType);
                supplier = new ContextualSupplier(this.supply, this.suppliedType, this.contextType);
            }

            log.atInfo().log("Built contextual supplier for type {}, contextType={}, nullable={}", this.suppliedType, this.contextType, this.nullable);
            log.atTrace().log("Exiting build");
            return wrapNullableContextual(
                    (IContextualSupplier<Supplied, ?>) supplier,
                    this.nullable);
        }

        if (this.constructorBinder != null) {
            log.atDebug().log("Building NewSupplier with constructorBinder");
            supplier = new NewSupplier<>(this.suppliedType, this.constructorBinder);
            log.atInfo().log("Built NewSupplier for type {}, nullable={}", this.suppliedType, this.nullable);
            log.atTrace().log("Exiting build");
            return wrapNullable(supplier, this.nullable);
        }

        log.atDebug().log("Building NullSupplier for type {}", this.suppliedType);
        supplier = new NullSupplier<>(this.suppliedType);
        log.atInfo().log("Built NullSupplier for type {}", this.suppliedType);
        log.atTrace().log("Exiting build");
        return wrapNullable(supplier, true);
    }

    @Override
    public ICommonSupplierBuilder<Supplied> nullable(boolean nullable) {
        this.nullable = Objects.requireNonNull(nullable, "Nullable cannot be null");
        return this;
    }

    @Override
    public <ContextType> ICommonSupplierBuilder<Supplied> withContext(
            Class<ContextType> contextType,
            IContextualSupply<Supplied, ContextType> supply) throws DslException {
        log.atTrace().log("Entering withContext with contextType={}", contextType);
        this.contextType = Objects.requireNonNull(contextType, "Context type cannot be null");
        this.supply = Objects.requireNonNull(supply, "Supply cannot be null");
        log.atDebug().log("Context configured for type {} with contextType={}", this.suppliedType, contextType);
        log.atTrace().log("Exiting withContext");
        return this;

    }

    @Override
    public ICommonSupplierBuilder<Supplied> withValue(Supplied value) throws DslException {
        log.atTrace().log("Entering withValue with value type={}", value != null ? value.getClass().getName() : "null");
        this.value = value;
        log.atDebug().log("Value configured for type {}", this.suppliedType);
        log.atTrace().log("Exiting withValue");
        return this;
    }

    @Override
    public ICommonSupplierBuilder<Supplied> withConstructor(
            IConstructorBinder<Supplied> constructorBinder)
            throws DslException {
        log.atTrace().log("Entering withConstructor for type {}", this.suppliedType);
        this.constructorBinder = Objects.requireNonNull(constructorBinder, "Constructor binder cannot be null");
        log.atDebug().log("Constructor binder configured for type {}", this.suppliedType);
        log.atTrace().log("Exiting withConstructor");
        return this;
    }

    private ISupplier<Supplied> wrapNullable(ISupplier<Supplied> supplier, boolean nullable) {
        return new NullableSupplier<>(supplier, nullable);
    }

    private ISupplier<Supplied> wrapNullableContextual(
            IContextualSupplier<Supplied, ?> supplier, boolean nullable) {

        return new NullableContextualSupplier<>(supplier, nullable);
    }

    public static <T> ICommonSupplierBuilder<T> fixed(Class<T> type, T value) {
        log.atTrace().log("Creating fixed supplier builder for type {}", type);
        return new SupplierBuilder<>(type).withValue(value).nullable(false);
    }

    public static <T> ICommonSupplierBuilder<T> newObject(Class<T> type, IConstructorBinder<T> binder) {
        log.atTrace().log("Creating newObject supplier builder for type {}", type);
        return new SupplierBuilder<>(type).withConstructor(binder);
    }

    public static <T> ICommonSupplierBuilder<T> nullObject(Class<T> type) {
        log.atTrace().log("Creating nullObject supplier builder for type {}", type);
        return new SupplierBuilder<>(type).nullable(true);
    }

    public static <T, C> ICommonSupplierBuilder<T> contextual(
            Class<T> type,
            Class<C> contextType,
            IContextualSupply<T, C> supply) throws DslException {
        log.atTrace().log("Creating contextual supplier builder for type {} with contextType={}", type, contextType);
        return new SupplierBuilder<>(type).withContext(contextType, supply);
    }

    public static <T, C> ICommonSupplierBuilder<T> newContextual(
            Class<T> type,
            Class<C> contextType,
            IContextualConstructorBinder<T> binder) {
        log.atTrace().log("Creating newContextual supplier builder for type {} with contextType={}", type, contextType);
        SupplierBuilder<T> builder = new SupplierBuilder<>(type);
        builder.contextType = contextType;
        builder.constructorBinder = binder;
        return builder;
    }

    @Override
    public ICommonSupplierBuilder<Supplied> withFuture(CompletableFuture<Supplied> future) throws DslException {
        log.atTrace().log("Entering withFuture with future");
        this.future = Objects.requireNonNull(future, "Future cannot be null");
        log.atDebug().log("Future configured for type {}", this.suppliedType);
        log.atTrace().log("Exiting withFuture");
        return this;
    }

    @Override
    public ICommonSupplierBuilder<Supplied> withFuture(CompletableFuture<Supplied> future, Long timeoutMillis)
            throws DslException {
        log.atTrace().log("Entering withFuture with future and timeout={}", timeoutMillis);
        this.future = Objects.requireNonNull(future, "Future cannot be null");
        this.timeoutMillis = timeoutMillis;
        log.atDebug().log("Future configured for type {} with timeout={}", this.suppliedType, timeoutMillis);
        log.atTrace().log("Exiting withFuture");
        return this;
    }

    @Override
    public ICommonSupplierBuilder<Supplied> withBlockingQueue(BlockingQueue<Supplied> queue) throws DslException {
        log.atTrace().log("Entering withBlockingQueue with queue");
        this.blockingQueue = Objects.requireNonNull(queue, "BlockingQueue cannot be null");
        log.atDebug().log("BlockingQueue configured for type {}", this.suppliedType);
        log.atTrace().log("Exiting withBlockingQueue");
        return this;
    }

    @Override
    public ICommonSupplierBuilder<Supplied> withBlockingQueue(BlockingQueue<Supplied> queue, Long timeoutMillis)
            throws DslException {
        log.atTrace().log("Entering withBlockingQueue with queue and timeout={}", timeoutMillis);
        this.blockingQueue = Objects.requireNonNull(queue, "BlockingQueue cannot be null");
        this.timeoutMillis = timeoutMillis;
        log.atDebug().log("BlockingQueue configured for type {} with timeout={}", this.suppliedType, timeoutMillis);
        log.atTrace().log("Exiting withBlockingQueue");
        return this;
    }

}
