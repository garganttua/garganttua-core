package com.garganttua.core.supply;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;

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
 * ISupplier<String> supplier = new FutureSupplier<>(future, suppliedClass, 5000L);
 * Optional<String> result = supplier.supply(); // Blocks until future completes or timeout
 * }</pre>
 *
 * @param <Supplied> the type of object this supplier provides
 * @since 2.0.0-ALPHA01
 * @see ISupplier
 * @see CompletableFuture
 */
public class FutureSupplier<Supplied> implements ISupplier<Supplied> {
    private static final Logger log = Logger.getLogger(FutureSupplier.class);

    private final CompletableFuture<Supplied> future;
    private final Long timeoutMillis;
    private final Type suppliedType;
    private final IClass<Supplied> suppliedClass;

    /**
     * Creates a FutureSupplier with no timeout.
     *
     * @param future the CompletableFuture to wrap
     * @param suppliedClass the IClass of the supplied object
     */
    public FutureSupplier(CompletableFuture<Supplied> future, IClass<Supplied> suppliedClass) {
        this(future, suppliedClass, null);
    }

    /**
     * Creates a FutureSupplier with a timeout.
     *
     * @param future the CompletableFuture to wrap
     * @param suppliedClass the IClass of the supplied object
     * @param timeoutMillis the timeout in milliseconds, or null for no timeout
     */
    public FutureSupplier(CompletableFuture<Supplied> future, IClass<Supplied> suppliedClass, Long timeoutMillis) {
        log.trace("Entering FutureSupplier constructor with timeout: {}", timeoutMillis);
        this.future = Objects.requireNonNull(future, "Future cannot be null");
        this.suppliedClass = Objects.requireNonNull(suppliedClass, "Supplied class cannot be null");
        this.suppliedType = suppliedClass.getType();
        this.timeoutMillis = timeoutMillis;
        log.trace("Exiting FutureSupplier constructor");
    }

    @Override
    public Optional<Supplied> supply() throws SupplyException {
        log.trace("Entering supply method");
        log.debug("Waiting for future completion with timeout: {}", timeoutMillis);

        try {
            Supplied result;
            if (timeoutMillis != null) {
                result = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
                log.debug("Future completed within timeout of {} ms", timeoutMillis);
            } else {
                result = future.get();
                log.debug("Future completed without timeout");
            }

            log.trace("Exiting supply method");
            return Optional.ofNullable(result);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Future was interrupted", e);
            throw new SupplyException(e);
        } catch (ExecutionException e) {
            log.error("Future execution failed", e);
            throw new SupplyException(e);
        } catch (TimeoutException e) {
            log.error("Future timed out after {} ms", timeoutMillis, e);
            throw new SupplyException(new RuntimeException("Future timed out after " + timeoutMillis + " ms", e));
        }
    }

    @Override
    public Type getSuppliedType() {
        return suppliedType;
    }

    @Override
    public IClass<Supplied> getSuppliedClass() {
        return this.suppliedClass;
    }
}
