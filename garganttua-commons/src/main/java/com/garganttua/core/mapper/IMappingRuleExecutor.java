package com.garganttua.core.mapper;

@FunctionalInterface
public interface IMappingRuleExecutor {

	<destination> destination doMapping(Class<destination> destinationClass, destination destinationObject,
			Object sourceObject) throws MapperException;

}
