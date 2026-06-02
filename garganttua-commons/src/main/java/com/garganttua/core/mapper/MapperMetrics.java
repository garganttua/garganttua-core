package com.garganttua.core.mapper;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe metrics for mapping operations.
 *
 * @since 2.0.0-ALPHA01
 */
public class MapperMetrics {

	private final AtomicLong totalMappings = new AtomicLong();
	private final AtomicLong failedMappings = new AtomicLong();
	private final AtomicLong totalRulesExecuted = new AtomicLong();
	private final AtomicLong totalMappingTimeNanos = new AtomicLong();

	/**
	 * Records one completed mapping: increments the mapping count and accumulates the
	 * elapsed time and number of rules executed.
	 *
	 * @param durationNanos the mapping duration in nanoseconds
	 * @param rulesExecuted the number of mapping rules executed for this mapping
	 */
	public void recordMapping(long durationNanos, int rulesExecuted) {
		this.totalMappings.incrementAndGet();
		this.totalRulesExecuted.addAndGet(rulesExecuted);
		this.totalMappingTimeNanos.addAndGet(durationNanos);
	}

	/** Records one failed mapping by incrementing the failure count. */
	public void recordFailure() {
		this.failedMappings.incrementAndGet();
	}

	/** @return the total number of mappings recorded so far */
	public long getTotalMappings() {
		return this.totalMappings.get();
	}

	/** @return the total number of failed mappings recorded so far */
	public long getFailedMappings() {
		return this.failedMappings.get();
	}

	/** @return the cumulative number of mapping rules executed */
	public long getTotalRulesExecuted() {
		return this.totalRulesExecuted.get();
	}

	/** @return the cumulative mapping time in nanoseconds */
	public long getTotalMappingTimeNanos() {
		return this.totalMappingTimeNanos.get();
	}

	/** Resets all counters back to zero. */
	public void reset() {
		this.totalMappings.set(0);
		this.failedMappings.set(0);
		this.totalRulesExecuted.set(0);
		this.totalMappingTimeNanos.set(0);
	}
}
