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

	public void recordMapping(long durationNanos, int rulesExecuted) {
		this.totalMappings.incrementAndGet();
		this.totalRulesExecuted.addAndGet(rulesExecuted);
		this.totalMappingTimeNanos.addAndGet(durationNanos);
	}

	public void recordFailure() {
		this.failedMappings.incrementAndGet();
	}

	public long getTotalMappings() {
		return this.totalMappings.get();
	}

	public long getFailedMappings() {
		return this.failedMappings.get();
	}

	public long getTotalRulesExecuted() {
		return this.totalRulesExecuted.get();
	}

	public long getTotalMappingTimeNanos() {
		return this.totalMappingTimeNanos.get();
	}

	public void reset() {
		this.totalMappings.set(0);
		this.failedMappings.set(0);
		this.totalRulesExecuted.set(0);
		this.totalMappingTimeNanos.set(0);
	}
}
