package com.garganttua.core.supply.dsl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.binders.IConstructorBinder;
import com.garganttua.core.supply.IContextualSupply;
import com.garganttua.core.supply.ISupplier;

public interface ICommonSupplierBuilder<Supplied> extends ISupplierBuilder<Supplied, ISupplier<Supplied>>{

    /**
     * Configures whether the supplier can return null/empty values.
     *
     * <p>
     * When set to {@code true}, the supplier is allowed to return {@link java.util.Optional#empty()}.
     * When {@code false} (default), the supplier should always return a present Optional,
     * and returning empty may be considered an error condition.
     * </p>
     *
     * @param nullable {@code true} to allow null/empty returns, {@code false} to require values
     * @return this builder instance for method chaining
     */
    ICommonSupplierBuilder<Supplied> nullable(boolean nullable);

    /**
     * Configures the supplier to use context-aware object creation logic.
     *
     * <p>
     * This method transforms the builder to create a contextual supplier that requires
     * the specified context type for object creation. The provided supply logic will
     * be invoked with the matching context at supply time.
     * </p>
     *
     * @param <ContextType> the type of context required for object creation
     * @param contextType the class representing the required context type
     * @param supply the functional supply logic that creates objects using the context
     * @return this builder instance for method chaining
     * @throws DslException if a supply strategy has already been configured, or if the
     *                     context type or supply logic is invalid
     */
    <ContextType> ICommonSupplierBuilder<Supplied> withContext(Class<ContextType> contextType, IContextualSupply<Supplied, ContextType> supply) throws DslException;

    /**
     * Configures the supplier to return a fixed value.
     *
     * <p>
     * The built supplier will always return the specified value wrapped in an Optional.
     * This is the simplest supply strategy, suitable for singleton-like objects or
     * pre-configured instances.
     * </p>
     *
     * @param value the fixed value to be supplied (may be {@code null} if nullable is enabled)
     * @return this builder instance for method chaining
     * @throws DslException if a supply strategy has already been configured, or if the
     *                     value is null when nullable is disabled
     */
    ICommonSupplierBuilder<Supplied> withValue(Supplied value) throws DslException;

    /**
     * Configures the supplier to create instances using the specified constructor.
     *
     * <p>
     * The built supplier will use reflection to instantiate objects via the constructor
     * defined by the provided binder. This enables dynamic object creation with
     * parameterized constructors.
     * </p>
     *
     * @param constructorBinder the constructor binder that defines how to create instances
     * @return this builder instance for method chaining
     * @throws DslException if a supply strategy has already been configured, or if the
     *                     constructor binder is invalid or incompatible
     */
    ICommonSupplierBuilder<Supplied> withConstructor(IConstructorBinder<Supplied> constructorBinder) throws DslException;

    /**
     * Configures the supplier to provide values asynchronously from a CompletableFuture.
     *
     * <p>
     * The built supplier will wait for the future to complete and return its value.
     * An optional timeout can be specified to limit waiting time.
     * </p>
     *
     * @param future the CompletableFuture to wait on
     * @return this builder instance for method chaining
     * @throws DslException if a supply strategy has already been configured
     */
    ICommonSupplierBuilder<Supplied> withFuture(CompletableFuture<Supplied> future) throws DslException;

    /**
     * Configures the supplier to provide values asynchronously from a CompletableFuture with timeout.
     *
     * <p>
     * The built supplier will wait for the future to complete and return its value,
     * with a timeout limit specified in milliseconds.
     * </p>
     *
     * @param future the CompletableFuture to wait on
     * @param timeoutMillis the timeout in milliseconds
     * @return this builder instance for method chaining
     * @throws DslException if a supply strategy has already been configured
     */
    ICommonSupplierBuilder<Supplied> withFuture(CompletableFuture<Supplied> future, Long timeoutMillis) throws DslException;

    /**
     * Configures the supplier to provide values from a blocking queue.
     *
     * <p>
     * The built supplier will poll or take elements from the queue.
     * Without a timeout, the supplier will block indefinitely until an element is available.
     * </p>
     *
     * @param queue the BlockingQueue to poll from
     * @return this builder instance for method chaining
     * @throws DslException if a supply strategy has already been configured
     */
    ICommonSupplierBuilder<Supplied> withBlockingQueue(BlockingQueue<Supplied> queue) throws DslException;

    /**
     * Configures the supplier to provide values from a blocking queue with timeout.
     *
     * <p>
     * The built supplier will poll elements from the queue with a timeout limit
     * specified in milliseconds. If no element is available within the timeout,
     * the supplier returns empty.
     * </p>
     *
     * @param queue the BlockingQueue to poll from
     * @param timeoutMillis the timeout in milliseconds
     * @return this builder instance for method chaining
     * @throws DslException if a supply strategy has already been configured
     */
    ICommonSupplierBuilder<Supplied> withBlockingQueue(BlockingQueue<Supplied> queue, Long timeoutMillis) throws DslException;

}