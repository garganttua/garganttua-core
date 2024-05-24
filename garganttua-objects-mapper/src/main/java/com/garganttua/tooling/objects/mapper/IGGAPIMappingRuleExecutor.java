package com.garganttua.tooling.objects.mapper;

import com.garganttua.api.core.mapper.rules.GGAPIMappingRuleExecutorException;

public interface IGGAPIMappingRuleExecutor {

	<destination> destination doMapping(Class<destination> destinationClass, destination destinationObject,
			Object sourceObject) throws GGAPIMappingRuleExecutorException;

}
