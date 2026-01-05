package com.garganttua.core.mutex;

import java.util.concurrent.TimeUnit;

/**
 * Configuration strategy for mutex acquisition behavior.
 *
 * <p>
 * This record defines the parameters controlling how a mutex is acquired,
 * including wait timeout, retry logic, and automatic lease expiration.
 * </p>
 *
 * <h2>Configuration Parameters</h2>
 * <ul>
 *   <li><b>Wait Time</b>: Time to wait for mutex availability
 *     <ul>
 *       <li>{@code -1}: Wait forever until mutex is available</li>
 *       <li>{@code 0}: Try immediately, fail if mutex is not available</li>
 *       <li>{@code > 0}: Wait for specified duration</li>
 *     </ul>
 *   </li>
 *   <li><b>Retries</b>: Number of acquisition attempts after initial failure</li>
 *   <li><b>Retry Interval</b>: Delay between retry attempts</li>
 *   <li><b>Lease Time</b>: Maximum time to hold the mutex before automatic release</li>
 * </ul>
 *
 * @param waitTime the time to wait for mutex acquisition (-1 for forever, 0 for immediate, >0 for timeout)
 * @param waitTimeUnit the time unit for wait time
 * @param retries the number of retry attempts if acquisition fails
 * @param retryInterval the interval between retry attempts
 * @param retryIntervalUnit the time unit for retry interval
 * @param leaseTime the maximum lease time for holding the mutex
 * @param leaseTimeUnit the time unit for lease time
 * @since 2.0.0-ALPHA01
 * @see IMutex
 */
public record MutexStrategy(
    int waitTime,
    TimeUnit waitTimeUnit,
    int retries,
    int retryInterval,
    TimeUnit retryIntervalUnit,
    int leaseTime,
    TimeUnit leaseTimeUnit
) {

}
