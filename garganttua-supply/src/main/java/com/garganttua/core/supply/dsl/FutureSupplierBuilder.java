package com.garganttua.core.supply.dsl;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.FutureSupplier;
import com.garganttua.core.supply.ISupplier;

import lombok.extern.slf4j.Slf4j;

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
 * ISupplier<String> supplier = new FutureSupplierBuilder<>(future, String.class)
 *     .withTimeout(5000L)
 *     .build();
 * }</pre>
 *
 * @param <Supplied> the type of object supplied by the built supplier
 * @since 2.0.0-ALPHA01
 * @see FutureSupplier
 */
@Slf4j
public class FutureSupplierBuilder<Supplied>
        implements ISupplierBuilder<Supplied, ISupplier<Supplied>> {

    private final CompletableFuture<Supplied> future;
    private final Class<Supplied> suppliedType;
    private Long timeoutMillis;

    /**
     * Creates a FutureSupplierBuilder.
     *
     * @param future the CompletableFuture to wrap
     * @param suppliedType the type of the supplied object
     */
    public FutureSupplierBuilder(CompletableFuture<Supplied> future, Class<Supplied> suppliedType) {
        log.atTrace().log("Entering FutureSupplierBuilder constructor");
        this.future = Objects.requireNonNull(future, "Future cannot be null");
        this.suppliedType = Objects.requireNonNull(suppliedType, "Supplied type cannot be null");
        log.atTrace().log("Exiting FutureSupplierBuilder constructor");
    }

    /**
     * Configures a timeout for waiting on the future.
     *
     * @param timeoutMillis the timeout in milliseconds
     * @return this builder instance for method chaining
     */
    public FutureSupplierBuilder<Supplied> withTimeout(Long timeoutMillis) {
        log.atTrace().log("Entering withTimeout method with timeout: {}", timeoutMillis);
        this.timeoutMillis = timeoutMillis;
        log.atTrace().log("Exiting withTimeout method");
        return this;
    }

    @Override
    public ISupplier<Supplied> build() throws DslException {
        log.atTrace().log("Entering build method");
        log.atDebug().log("Building FutureSupplier with timeout: {}", timeoutMillis);
        ISupplier<Supplied> result = new FutureSupplier<>(future, suppliedType, timeoutMillis);
        log.atInfo().log("Build completed for FutureSupplier");
        log.atTrace().log("Exiting build method");
        return result;
    }

    @Override
    public Type getSuppliedType() {
        return suppliedType;
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
     * @param suppliedType the type of the supplied object
     * @return a new FutureSupplierBuilder instance
     */
    public static <Supplied> FutureSupplierBuilder<Supplied> of(CompletableFuture<Supplied> future, Class<Supplied> suppliedType) {
        log.atTrace().log("Entering static of method");
        log.atDebug().log("Creating FutureSupplierBuilder");
        FutureSupplierBuilder<Supplied> result = new FutureSupplierBuilder<>(future, suppliedType);
        log.atTrace().log("Exiting static of method");
        return result;
    }
}
