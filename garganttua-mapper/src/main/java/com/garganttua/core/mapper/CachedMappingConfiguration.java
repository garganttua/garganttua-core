package com.garganttua.core.mapper;

import java.util.List;

/**
 * Caches the pre-computed executors alongside the mapping configuration
 * to avoid recalculating them on every map() call.
 *
 * @param config the mapping configuration
 * @param destinationExecutors pre-computed executors for REGULAR direction
 * @param sourceExecutors pre-computed executors for REVERSE direction
 */
record CachedMappingConfiguration(
		MappingConfiguration config,
		List<IMappingRuleExecutor> destinationExecutors,
		List<IMappingRuleExecutor> sourceExecutors
) {}
