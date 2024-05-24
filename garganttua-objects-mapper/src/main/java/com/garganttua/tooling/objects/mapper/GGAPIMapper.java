package com.garganttua.tooling.objects.mapper;

import java.util.List;

import com.garganttua.api.core.mapper.rules.GGAPIMappingRule;
import com.garganttua.api.core.mapper.rules.GGAPIMappingRuleException;
import com.garganttua.api.core.mapper.rules.GGAPIMappingRuleExecutorException;
import com.garganttua.api.core.mapper.rules.GGAPIMappingRules;
import com.garganttua.api.spec.GGAPICoreExceptionCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GGAPIMapper implements IGGAPIMapper {
	
	private GGAPIMapperConfiguration configuration = new GGAPIMapperConfiguration();

	@Override
	public <destination> destination map(Object source, Class<destination> destinationClass) throws GGAPIMapperException {
		if( log.isDebugEnabled() ) {
			log.debug("Mapping {} to {}", source, destinationClass);
		}
		try {
			List<GGAPIMappingRule> destinationRules = GGAPIMappingRules.parse(destinationClass);
			List<GGAPIMappingRule> sourceRules = GGAPIMappingRules.parse(source.getClass());
			
			GGAPIMappingDirection mappingDirection = this.determineMapingDirection(sourceRules, destinationRules);
			
			switch( mappingDirection ) {
			case REGULAR:
				return this.doMapping(mappingDirection, destinationClass, source, destinationRules);
			case REVERSE:
				return this.doMapping(mappingDirection, destinationClass, source, sourceRules);
			}
			
		} catch (GGAPIMappingRuleException e) {
			throw new GGAPIMapperException(GGAPICoreExceptionCode.OBJECT_MAPPING, e.getMessage(), e);
		}
		return null;
	}
	
	private <destination> destination doMapping(GGAPIMappingDirection mappingDirection, Class<destination> destinationClass, Object source, List<GGAPIMappingRule> rules) throws GGAPIMapperException {
		if( this.configuration.doValidation() ) {
			try {
				GGAPIMappingRules.validate(source.getClass(), rules);
			} catch (GGAPIMappingRuleException e) {
				if( this.configuration.failOnError() ) {
					if( log.isDebugEnabled() ) {
						log.debug("Unable to validate mapping, aborting", e);
					}
					throw new GGAPIMapperException(GGAPICoreExceptionCode.OBJECT_MAPPING, "Unable to validate mapping, aborting", e);
				} else {
					log.warn("Unable to validate mapping, ignoring", e);
				}
			}
		}
		
		destination destObject = null;
		
		for( GGAPIMappingRule rule: rules ) {		
			try {
				IGGAPIMappingRuleExecutor executor = GGAPIMappingRules.getRuleExecutor(mappingDirection, rule, source, destinationClass);		
				destObject = executor.doMapping(destinationClass, destObject, source);
			} catch (GGAPIMappingRuleExecutorException | GGAPIMapperException e) {
				if( this.configuration.failOnError() ) {
					if( log.isDebugEnabled() ) {
						log.debug("Unable to do mapping, aborting", e);
					}
					throw new GGAPIMapperException(GGAPICoreExceptionCode.OBJECT_MAPPING, "Unable to do mapping, aborting", e);
				} else {
					log.warn("Unable to do mapping, ignoring", e);
					continue;
				}
			}
		}
		return destObject;
	}

	private GGAPIMappingDirection determineMapingDirection(List<GGAPIMappingRule> sourceRules, List<GGAPIMappingRule> destinationRules) throws GGAPIMapperException {
		if( sourceRules.size() ==0 && destinationRules.size() != 0 ) {
			return GGAPIMappingDirection.REGULAR;
		} else if( sourceRules.size() !=0 && destinationRules.size() == 0 ){
			return GGAPIMappingDirection.REVERSE;
		} else {
			throw new GGAPIMapperException(GGAPICoreExceptionCode.OBJECT_MAPPING, "Cannot determine mapping direction as source and destination are annotated with mapping rules");
		}
	}

	@Override
	public GGAPIMapper configure(GGAPIMapperConfigurationItem element, Object value) {
		this.configuration.configure(element, value);
		return this;
	}
}
