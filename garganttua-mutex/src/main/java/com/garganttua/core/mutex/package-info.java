/**
 * Thread-safe mutex synchronization framework with configurable acquisition strategies.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides a mutex synchronization framework for managing mutually exclusive
 * access to critical sections. It supports configurable timeout, retry logic, and automatic
 * lease expiration for robust distributed locking scenarios.
 * </p>
 *
 * <h2>Core Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.mutex.IMutex} - Mutex interface for executing code in critical sections</li>
 *   <li>{@link com.garganttua.core.mutex.IMutexManager} - Manager for creating and retrieving named mutexes</li>
 *   <li>{@link com.garganttua.core.mutex.MutexStrategy} - Configuration for mutex acquisition behavior</li>
 * </ul>
 *
 * <h2>Key Concepts</h2>
 * <h3>Mutex Acquisition</h3>
 * <p>
 * Mutexes can be acquired using two methods:
 * </p>
 * <ul>
 *   <li><b>Simple Acquisition</b>: Wait indefinitely until mutex is available</li>
 *   <li><b>Strategy-Based Acquisition</b>: Configure timeout, retries, and lease time</li>
 * </ul>
 *
 * <h3>Wait Time Configuration</h3>
 * <ul>
 *   <li>{@code -1}: Wait forever until mutex is available</li>
 *   <li>{@code 0}: Try immediately, fail if mutex is not available</li>
 *   <li>{@code > 0}: Wait for specified duration before failing</li>
 * </ul>
 *
 * <h3>Retry Logic</h3>
 * <p>
 * Mutexes support automatic retry with configurable intervals when acquisition fails.
 * This is useful for transient failures in distributed lock scenarios.
 * </p>
 *
 * <h3>Lease Time</h3>
 * <p>
 * Lease time defines the maximum duration a mutex can be held before automatic release.
 * This prevents deadlocks when a holder fails to release the mutex explicitly.
 * </p>
 *
 * <h2>Usage Patterns</h2>
 * <ul>
 *   <li>Database transaction serialization</li>
 *   <li>Shared resource access control</li>
 *   <li>Distributed lock coordination</li>
 *   <li>Cache invalidation synchronization</li>
 * </ul>
 *
 * <h2>Exception Handling</h2>
 * <p>
 * All mutex operations throw {@link com.garganttua.core.mutex.MutexException} on failure,
 * including acquisition timeout, retry exhaustion, and execution errors within the
 * critical section.
 * </p>
 *
 * <h2>GraalVM Native Image Support</h2>
 * <p>
 * Types that use mutexes should be annotated with
 * {@link com.garganttua.core.mutex.annotations.Mutex @Mutex} to ensure proper
 * native image configuration.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.mutex.IMutex
 * @see com.garganttua.core.mutex.IMutexManager
 * @see com.garganttua.core.mutex.MutexStrategy
 */
package com.garganttua.core.mutex;
