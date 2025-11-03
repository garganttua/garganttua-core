package com.garganttua.core.mapper;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Mapper implements IMapper {

	protected Set<MappingConfiguration> mappingConfigurations = new HashSet<MappingConfiguration>();

	private MapperConfiguration configuration = new MapperConfiguration();

	@Override
	public <destination> destination map(Object source, destination destination) throws MapperException {
		if (destination == null)
			throw new MapperException("destination cannot be null");

		return this.map(source, (Class<destination>) destination.getClass(), destination);
	}

	@Override
	public <destination> destination map(Object source, Class<destination> destinationClass) throws MapperException {
		if (destinationClass == null)
			throw new MapperException("detination class cannot be null");
		return this.map(source, destinationClass, null);
	}

	@Override
	public <destination> destination map(Object source, Class<destination> destinationClass, destination destination)
			throws MapperException {
		if (log.isDebugEnabled()) {
			log.debug("Mapping {} to {}", source, destinationClass);
		}
		try {

			if (destinationClass == null)
				destinationClass = (Class<destination>) destination.getClass();

			MappingConfiguration mappingConfiguration = this.getMappingConfiguration(source.getClass(),
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

		} catch (MapperException e) {
			throw new MapperException(e.getMessage(), e);
		}
	}

	private <destination> destination doMapping(MappingDirection mappingDirection,
			Class<destination> destinationClass, destination destObject, Object source, List<MappingRule> rules)
			throws MapperException {

		for (MappingRule rule : rules) {
			try {
				IMappingRuleExecutor executor = MappingRules.getRuleExecutor(this, mappingDirection, rule, source,
						destinationClass);
				destObject = executor.doMapping(destinationClass, destObject, source);
			} catch (MapperException e) {
				if (this.configuration.failOnError()) {
					if (log.isDebugEnabled()) {
						log.debug("Unable to do mapping, aborting", e);
					}
					throw new MapperException("Unable to do mapping, aborting", e);
				} else {
					log.warn("Unable to do mapping, ignoring", e);
					continue;
				}
			}
		}
		return destObject;
	}

	private MappingDirection determineMapingDirection(List<MappingRule> sourceRules,
			List<MappingRule> destinationRules) throws MapperException {
		if (sourceRules.size() == 0 && destinationRules.size() != 0) {
			return MappingDirection.REGULAR;
		} else if (sourceRules.size() != 0 && destinationRules.size() == 0) {
			return MappingDirection.REVERSE;
		} else {
			throw new MapperException(
					"Cannot determine mapping direction as source and destination are annotated with mapping rules, or neither has mapping rule");
		}
	}

	@Override
	public Mapper configure(MapperConfigurationItem element, Object value) {
		this.configuration.configure(element, value);
		return this;
	}

	//@Override
	public MappingConfiguration recordMappingConfiguration(Class<?> source, Class<?> destination)
			throws MapperException {
		if (log.isDebugEnabled()) {
			log.debug("Recording mapping configuration from " + source.getSimpleName() + " to "
					+ destination.getSimpleName());
		}
		List<MappingRule> destinationRules = MappingRules.parse(destination);
		List<MappingRule> sourceRules = MappingRules.parse(source);
		MappingDirection mappingDirection = this.determineMapingDirection(sourceRules, destinationRules);
		MappingConfiguration configuration = new MappingConfiguration(source, destination, sourceRules,
				destinationRules, mappingDirection);

		try {
			if (this.configuration.doValidation())
				configuration.validate();
		} catch (MapperException e) {
			if (this.configuration.failOnError()) {
				if (log.isDebugEnabled()) {
					log.debug("Unable to validate mapping, aborting", e);
				}
				throw new MapperException("Unable to validate mapping, aborting", e);
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

	//@Override
	public MappingConfiguration getMappingConfiguration(Class<?> source, Class<?> destination)
			throws MapperException {
		MappingConfiguration lookup = new MappingConfiguration(source, destination, null, null, null);
		Optional<MappingConfiguration> found = this.mappingConfigurations.parallelStream().filter(configuration -> {
			return configuration.equals(lookup);
		}).findFirst();
		if (found.isPresent()) {
			return found.get();
		} else {
			return this.recordMappingConfiguration(source, destination);
		}
	}
}
