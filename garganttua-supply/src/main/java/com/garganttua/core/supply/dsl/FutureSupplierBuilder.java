package com.garganttua.core.supply.dsl;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.FutureSupplier;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Builder for creating FutureSupplier instances.
 *
 * <p>
 * This builder provides a fluent API for configuring and creating
 * {@link FutureSupplier} instances with optional timeout configuration.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "result");
 *
 * ISupplier<String> supplier = new FutureSupplierBuilder<>(future, String.class, suppliedClass)
 *     .withTimeout(5000L)
 *     .build();
 * }</pre>
 *
 * @param <Supplied> the type of object supplied by the built supplier
 * @since 2.0.0-ALPHA01
 * @see FutureSupplier
 */
@Reflected
public class FutureSupplierBuilder<Supplied>
        implements ISupplierBuilder<Supplied, ISupplier<Supplied>> {
    private static final Logger log = Logger.getLogger(FutureSupplierBuilder.class);

    private final CompletableFuture<Supplied> future;
    private final IClass<Supplied> suppliedClass;
    private Long timeoutMillis;

    /**
     * Creates a FutureSupplierBuilder.
     *
     * @param future the CompletableFuture to wrap
     * @param suppliedClass the IClass of the supplied object
     */
    public FutureSupplierBuilder(CompletableFuture<Supplied> future, IClass<Supplied> suppliedClass) {
        log.trace("Entering FutureSupplierBuilder constructor");
        this.future = Objects.requireNonNull(future, "Future cannot be null");
        this.suppliedClass = Objects.requireNonNull(suppliedClass, "Supplied class cannot be null");
        log.trace("Exiting FutureSupplierBuilder constructor");
    }

    /**
     * Configures a timeout for waiting on the future.
     *
     * @param timeoutMillis the timeout in milliseconds
     * @return this builder instance for method chaining
     */
    public FutureSupplierBuilder<Supplied> withTimeout(Long timeoutMillis) {
        log.trace("Entering withTimeout method with timeout: {}", timeoutMillis);
        this.timeoutMillis = timeoutMillis;
        log.trace("Exiting withTimeout method");
        return this;
    }

    @Override
    public ISupplier<Supplied> build() throws DslException {
        log.trace("Entering build method");
        log.debug("Building FutureSupplier with timeout: {}", timeoutMillis);
        ISupplier<Supplied> result = new FutureSupplier<>(future, suppliedClass, timeoutMillis);
        log.debug("Build completed for FutureSupplier");
        log.trace("Exiting build method");
        return result;
    }

    @Override
    public Type getSuppliedType() {
        return suppliedClass.getType();
    }

    @Override
    public IClass<Supplied> getSuppliedClass() {
        return this.suppliedClass;
    }

    @Override
    public boolean isContextual() {
        return false;
    }

    /**
     * Static factory method for creating a FutureSupplierBuilder.
     *
     * @param <Supplied> the type of object supplied
     * @param future the CompletableFuture to wrap
     * @param suppliedClass the IClass of the supplied object
     * @return a new FutureSupplierBuilder instance
     */
    public static <Supplied> FutureSupplierBuilder<Supplied> of(CompletableFuture<Supplied> future, IClass<Supplied> suppliedClass) {
        log.trace("Entering static of method");
        log.debug("Creating FutureSupplierBuilder");
        FutureSupplierBuilder<Supplied> result = new FutureSupplierBuilder<>(future, suppliedClass);
        log.trace("Exiting static of method");
        return result;
    }
}
