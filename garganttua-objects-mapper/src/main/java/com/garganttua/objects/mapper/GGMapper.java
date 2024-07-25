package com.garganttua.objects.mapper;

import java.util.List;

import com.garganttua.objects.mapper.rules.GGMappingRule;
import com.garganttua.objects.mapper.rules.GGMappingRules;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GGMapper implements IGGMapper {
	
	private GGMapperConfiguration configuration = new GGMapperConfiguration();

	@Override
	public <destination> destination map(Object source, Class<destination> destinationClass) throws GGMapperException {
		if( log.isDebugEnabled() ) {
			log.debug("Mapping {} to {}", source, destinationClass);
		}
		try {
			List<GGMappingRule> destinationRules = GGMappingRules.parse(destinationClass);
			List<GGMappingRule> sourceRules = GGMappingRules.parse(source.getClass());
			
			GGMappingDirection mappingDirection = this.determineMapingDirection(sourceRules, destinationRules);
			
			switch( mappingDirection ) {
			case REGULAR:
				return this.doMapping(mappingDirection, destinationClass, source, destinationRules);
			case REVERSE:
				return this.doMapping(mappingDirection, destinationClass, source, sourceRules);
			}
			
		} catch (GGMapperException e) {
			throw new GGMapperException(e.getMessage(), e);
		}
		return null;
	}
	
	private <destination> destination doMapping(GGMappingDirection mappingDirection, Class<destination> destinationClass, Object source, List<GGMappingRule> rules) throws GGMapperException {
		if( this.configuration.doValidation() ) {
			try {
				if( mappingDirection == GGMappingDirection.REVERSE )
					GGMappingRules.validate(destinationClass, rules);
				if( mappingDirection == GGMappingDirection.REGULAR )
					GGMappingRules.validate(source.getClass(), rules);
			} catch (GGMapperException e) {
				if( this.configuration.failOnError() ) {
					if( log.isDebugEnabled() ) {
						log.debug("Unable to validate mapping, aborting", e);
					}
					throw new GGMapperException("Unable to validate mapping, aborting", e);
				} else {
					log.warn("Unable to validate mapping, ignoring", e);
				}
			}
		}
		
		destination destObject = null;
		
		for( GGMappingRule rule: rules ) {		
			try {
				IGGMappingRuleExecutor executor = GGMappingRules.getRuleExecutor(mappingDirection, rule, source, destinationClass);		
				destObject = executor.doMapping(destinationClass, destObject, source);
			} catch (GGMapperException e) {
				if( this.configuration.failOnError() ) {
					if( log.isDebugEnabled() ) {
						log.debug("Unable to do mapping, aborting", e);
					}
					throw new GGMapperException("Unable to do mapping, aborting", e);
				} else {
					log.warn("Unable to do mapping, ignoring", e);
					continue;
				}
			}
		}
		return destObject;
	}

	private GGMappingDirection determineMapingDirection(List<GGMappingRule> sourceRules, List<GGMappingRule> destinationRules) throws GGMapperException {
		if( sourceRules.size() ==0 && destinationRules.size() != 0 ) {
			return GGMappingDirection.REGULAR;
		} else if( sourceRules.size() !=0 && destinationRules.size() == 0 ){
			return GGMappingDirection.REVERSE;
		} else {
			throw new GGMapperException("Cannot determine mapping direction as source and destination are annotated with mapping rules, or neither has mapping rule");
		}
	}

	@Override
	public GGMapper configure(GGMapperConfigurationItem element, Object value) {
		this.configuration.configure(element, value);
		return this;
	}
}
