package com.garganttua.core.supply.dsl;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.BlockingSupplier;
import com.garganttua.core.supply.ISupplier;

import lombok.extern.slf4j.Slf4j;

/**
 * Builder for creating BlockingSupplier instances.
 *
 * <p>
 * This builder provides a fluent API for configuring and creating
 * {@link BlockingSupplier} instances with optional timeout configuration.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * BlockingQueue<String> queue = new LinkedBlockingQueue<>();
 *
 * ISupplier<String> supplier = new BlockingSupplierBuilder<>(queue, String.class)
 *     .withTimeout(5000L)
 *     .build();
 * }</pre>
 *
 * @param <Supplied> the type of object supplied by the built supplier
 * @since 2.0.0-ALPHA01
 * @see BlockingSupplier
 */
@Slf4j
public class BlockingSupplierBuilder<Supplied>
        implements ISupplierBuilder<Supplied, ISupplier<Supplied>> {

    private final BlockingQueue<Supplied> queue;
    private final Type suppliedType;
    private Long timeoutMillis;

    /**
     * Creates a BlockingSupplierBuilder.
     *
     * @param queue the BlockingQueue to poll from
     * @param suppliedType the type of the supplied object
     */
    public BlockingSupplierBuilder(BlockingQueue<Supplied> queue, Type suppliedType) {
        log.atTrace().log("Entering BlockingSupplierBuilder constructor");
        this.queue = Objects.requireNonNull(queue, "Queue cannot be null");
        this.suppliedType = Objects.requireNonNull(suppliedType, "Supplied type cannot be null");
        log.atTrace().log("Exiting BlockingSupplierBuilder constructor");
    }

    /**
     * Configures a timeout for waiting on queue elements.
     *
     * @param timeoutMillis the timeout in milliseconds
     * @return this builder instance for method chaining
     */
    public BlockingSupplierBuilder<Supplied> withTimeout(Long timeoutMillis) {
        log.atTrace().log("Entering withTimeout method with timeout: {}", timeoutMillis);
        this.timeoutMillis = timeoutMillis;
        log.atTrace().log("Exiting withTimeout method");
        return this;
    }

    @Override
    public ISupplier<Supplied> build() throws DslException {
        log.atTrace().log("Entering build method");
        log.atDebug().log("Building BlockingSupplier with timeout: {}", timeoutMillis);
        ISupplier<Supplied> result = new BlockingSupplier<>(queue, suppliedType, timeoutMillis);
        log.atDebug().log("Build completed for BlockingSupplier");
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
     * Static factory method for creating a BlockingSupplierBuilder.
     *
     * @param <Supplied> the type of object supplied
     * @param queue the BlockingQueue to poll from
     * @param suppliedType the type of the supplied object
     * @return a new BlockingSupplierBuilder instance
     */
    public static <Supplied> BlockingSupplierBuilder<Supplied> of(BlockingQueue<Supplied> queue, Type suppliedType) {
        log.atTrace().log("Entering static of method");
        log.atDebug().log("Creating BlockingSupplierBuilder");
        BlockingSupplierBuilder<Supplied> result = new BlockingSupplierBuilder<>(queue, suppliedType);
        log.atTrace().log("Exiting static of method");
        return result;
    }
}
