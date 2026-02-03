package com.garganttua.core.supply;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.extern.slf4j.Slf4j;

/**
 * Supplier that provides values asynchronously using CompletableFuture.
 *
 * <p>
 * {@code FutureSupplier} wraps a {@link CompletableFuture} and provides
 * asynchronous supply capabilities. The supplier can be configured with
 * an optional timeout for waiting on the future's completion.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
 *     // Some asynchronous computation
 *     return "result";
 * });
 *
 * ISupplier<String> supplier = new FutureSupplier<>(future, 5000); // 5 second timeout
 * Optional<String> result = supplier.supply(); // Blocks until future completes or timeout
 * }</pre>
 *
 * @param <Supplied> the type of object this supplier provides
 * @since 2.0.0-ALPHA01
 * @see ISupplier
 * @see CompletableFuture
 */
@Slf4j
public class FutureSupplier<Supplied> implements ISupplier<Supplied> {

    private final CompletableFuture<Supplied> future;
    private final Long timeoutMillis;
    private final Class<Supplied> suppliedType;

    /**
     * Creates a FutureSupplier with no timeout.
     *
     * @param future the CompletableFuture to wrap
     * @param suppliedType the type of the supplied object
     */
    public FutureSupplier(CompletableFuture<Supplied> future, Class<Supplied> suppliedType) {
        this(future, suppliedType, null);
    }

    /**
     * Creates a FutureSupplier with a timeout.
     *
     * @param future the CompletableFuture to wrap
     * @param suppliedType the type of the supplied object
     * @param timeoutMillis the timeout in milliseconds, or null for no timeout
     */
    public FutureSupplier(CompletableFuture<Supplied> future, Class<Supplied> suppliedType, Long timeoutMillis) {
        log.atTrace().log("Entering FutureSupplier constructor with timeout: {}", timeoutMillis);
        this.future = Objects.requireNonNull(future, "Future cannot be null");
        this.suppliedType = Objects.requireNonNull(suppliedType, "Supplied type cannot be null");
        this.timeoutMillis = timeoutMillis;
        log.atTrace().log("Exiting FutureSupplier constructor");
    }

    @Override
    public Optional<Supplied> supply() throws SupplyException {
        log.atTrace().log("Entering supply method");
        log.atDebug().log("Waiting for future completion with timeout: {}", timeoutMillis);

        try {
            Supplied result;
            if (timeoutMillis != null) {
                result = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
                log.atDebug().log("Future completed within timeout of {} ms", timeoutMillis);
            } else {
                result = future.get();
                log.atDebug().log("Future completed without timeout");
            }

            log.atTrace().log("Exiting supply method");
            return Optional.ofNullable(result);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.atError().log("Future was interrupted", e);
            throw new SupplyException(e);
        } catch (ExecutionException e) {
            log.atError().log("Future execution failed", e);
            throw new SupplyException(e);
        } catch (TimeoutException e) {
            log.atError().log("Future timed out after {} ms", timeoutMillis, e);
            throw new SupplyException(new RuntimeException("Future timed out after " + timeoutMillis + " ms", e));
        }
    }

    @Override
    public Class<Supplied> getSuppliedType() {
        return suppliedType;
    }
}
