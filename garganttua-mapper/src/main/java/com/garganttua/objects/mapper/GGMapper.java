package com.garganttua.objects.mapper;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.garganttua.objects.mapper.rules.GGMappingRule;
import com.garganttua.objects.mapper.rules.GGMappingRules;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GGMapper implements IGGMapper {

	protected Set<GGMappingConfiguration> mappingConfigurations = new HashSet<GGMappingConfiguration>();

	private GGMapperConfiguration configuration = new GGMapperConfiguration();

	@Override
	public <destination> destination map(Object source, destination destination) throws GGMapperException {
		if (destination == null)
			throw new GGMapperException("destination cannot be null");

		return this.map(source, (Class<destination>) destination.getClass(), destination);
	}

	@Override
	public <destination> destination map(Object source, Class<destination> destinationClass) throws GGMapperException {
		if (destinationClass == null)
			throw new GGMapperException("detination class cannot be null");
		return this.map(source, destinationClass, null);
	}

	@Override
	public <destination> destination map(Object source, Class<destination> destinationClass, destination destination)
			throws GGMapperException {
		if (log.isDebugEnabled()) {
			log.debug("Mapping {} to {}", source, destinationClass);
		}
		try {

			if (destinationClass == null)
				destinationClass = (Class<destination>) destination.getClass();

			GGMappingConfiguration mappingConfiguration = this.getMappingConfiguration(source.getClass(),
					destinationClass);

			switch (mappingConfiguration.mappingDirection()) {
				case REGULAR:
					return this.doMapping(mappingConfiguration.mappingDirection(), destinationClass, destination,
							source, mappingConfiguration.destinationRules());
				case REVERSE:
					return this.doMapping(mappingConfiguration.mappingDirection(), destinationClass, destination,
							source, mappingConfiguration.sourceRules());
			}

			return this.doMapping(mappingConfiguration.mappingDirection(), destinationClass, destination, source,
					mappingConfiguration.destinationRules());

		} catch (GGMapperException e) {
			throw new GGMapperException(e.getMessage(), e);
		}
	}

	private <destination> destination doMapping(GGMappingDirection mappingDirection,
			Class<destination> destinationClass, destination destObject, Object source, List<GGMappingRule> rules)
			throws GGMapperException {

		for (GGMappingRule rule : rules) {
			try {
				IGGMappingRuleExecutor executor = GGMappingRules.getRuleExecutor(this, mappingDirection, rule, source,
						destinationClass);
				destObject = executor.doMapping(destinationClass, destObject, source);
			} catch (GGMapperException e) {
				if (this.configuration.failOnError()) {
					if (log.isDebugEnabled()) {
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

	private GGMappingDirection determineMapingDirection(List<GGMappingRule> sourceRules,
			List<GGMappingRule> destinationRules) throws GGMapperException {
		if (sourceRules.size() == 0 && destinationRules.size() != 0) {
			return GGMappingDirection.REGULAR;
		} else if (sourceRules.size() != 0 && destinationRules.size() == 0) {
			return GGMappingDirection.REVERSE;
		} else {
			throw new GGMapperException(
					"Cannot determine mapping direction as source and destination are annotated with mapping rules, or neither has mapping rule");
		}
	}

	@Override
	public GGMapper configure(GGMapperConfigurationItem element, Object value) {
		this.configuration.configure(element, value);
		return this;
	}

	@Override
	public GGMappingConfiguration recordMappingConfiguration(Class<?> source, Class<?> destination)
			throws GGMapperException {
		if (log.isDebugEnabled()) {
			log.debug("Recording mapping configuration from " + source.getSimpleName() + " to "
					+ destination.getSimpleName());
		}
		List<GGMappingRule> destinationRules = GGMappingRules.parse(destination);
		List<GGMappingRule> sourceRules = GGMappingRules.parse(source);
		GGMappingDirection mappingDirection = this.determineMapingDirection(sourceRules, destinationRules);
		GGMappingConfiguration configuration = new GGMappingConfiguration(source, destination, sourceRules,
				destinationRules, mappingDirection);

		try {
			if (this.configuration.doValidation())
				configuration.validate();
		} catch (GGMapperException e) {
			if (this.configuration.failOnError()) {
				if (log.isDebugEnabled()) {
					log.debug("Unable to validate mapping, aborting", e);
				}
				throw new GGMapperException("Unable to validate mapping, aborting", e);
			} else {
				log.warn("Unable to validate mapping, ignoring", e);
			}
		}

		this.mappingConfigurations.add(configuration);

		if (log.isDebugEnabled()) {
			log.debug("Recorded mapping configuration " + configuration);
		}
		return configuration;
	}

	@Override
	public GGMappingConfiguration getMappingConfiguration(Class<?> source, Class<?> destination)
			throws GGMapperException {
		GGMappingConfiguration lookup = new GGMappingConfiguration(source, destination, null, null, null);
		Optional<GGMappingConfiguration> found = this.mappingConfigurations.parallelStream().filter(configuration -> {
			return configuration.equals(lookup);
		}).findFirst();
		if (found.isPresent()) {
			return found.get();
		} else {
			return this.recordMappingConfiguration(source, destination);
		}
	}
}
