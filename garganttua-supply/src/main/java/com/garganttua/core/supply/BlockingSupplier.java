package com.garganttua.core.supply;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * Supplier that provides values from a blocking queue.
 *
 * <p>
 * {@code BlockingSupplier} wraps a {@link BlockingQueue} and provides
 * blocking supply capabilities. The supplier can be configured with
 * an optional timeout for waiting on queue elements.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * BlockingQueue<String> queue = new LinkedBlockingQueue<>();
 * queue.put("item1");
 * queue.put("item2");
 *
 * ISupplier<String> supplier = new BlockingSupplier<>(queue, String.class, 5000L);
 * Optional<String> result = supplier.supply(); // Blocks until item available or timeout
 * }</pre>
 *
 * @param <Supplied> the type of object this supplier provides
 * @since 2.0.0-ALPHA01
 * @see ISupplier
 * @see BlockingQueue
 */
@Slf4j
public class BlockingSupplier<Supplied> implements ISupplier<Supplied> {

    private final BlockingQueue<Supplied> queue;
    private final Long timeoutMillis;
    private final Type suppliedType;

    /**
     * Creates a BlockingSupplier with no timeout.
     *
     * @param queue the BlockingQueue to poll from
     * @param suppliedType the type of the supplied object
     */
    public BlockingSupplier(BlockingQueue<Supplied> queue, Type suppliedType) {
        this(queue, suppliedType, null);
    }

    /**
     * Creates a BlockingSupplier with a timeout.
     *
     * @param queue the BlockingQueue to poll from
     * @param suppliedType the type of the supplied object
     * @param timeoutMillis the timeout in milliseconds, or null for indefinite wait
     */
    public BlockingSupplier(BlockingQueue<Supplied> queue, Type suppliedType, Long timeoutMillis) {
        log.atTrace().log("Entering BlockingSupplier constructor with timeout: {}", timeoutMillis);
        this.queue = Objects.requireNonNull(queue, "Queue cannot be null");
        this.suppliedType = Objects.requireNonNull(suppliedType, "Supplied type cannot be null");
        this.timeoutMillis = timeoutMillis;
        log.atTrace().log("Exiting BlockingSupplier constructor");
    }

    @Override
    public Optional<Supplied> supply() throws SupplyException {
        log.atTrace().log("Entering supply method");
        log.atDebug().log("Polling queue with timeout: {}", timeoutMillis);

        try {
            Supplied result;
            if (timeoutMillis != null) {
                result = queue.poll(timeoutMillis, TimeUnit.MILLISECONDS);
                if (result == null) {
                    log.atWarn().log("Queue poll timed out after {} ms", timeoutMillis);
                } else {
                    log.atInfo().log("Queue poll completed within timeout of {} ms", timeoutMillis);
                }
            } else {
                result = queue.take();
                log.atInfo().log("Queue take completed");
            }

            log.atTrace().log("Exiting supply method");
            return Optional.ofNullable(result);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.atError().log("Queue operation was interrupted", e);
            throw new SupplyException(e);
        }
    }

    @Override
    public Type getSuppliedType() {
        return suppliedType;
    }
}
