package com.garganttua.objects.mapper;

public interface IGGMappingRuleExecutor {

	<destination> destination doMapping(Class<destination> destinationClass, destination destinationObject,
			Object sourceObject) throws GGMapperException;

}
