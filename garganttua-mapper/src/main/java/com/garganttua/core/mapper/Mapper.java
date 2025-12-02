package com.garganttua.core.mapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Mapper implements IMapper {

	protected final Map<MappingKey, MappingConfiguration> mappingConfigurations = new ConcurrentHashMap<>();

	private final MapperConfiguration configuration = new MapperConfiguration();

	/**
	 * Thread-safe key for mapping configuration lookup.
	 * Uses source and destination classes as composite key.
	 */
	private static record MappingKey(Class<?> source, Class<?> destination) {
		private MappingKey {
			Objects.requireNonNull(source, "Source class cannot be null");
			Objects.requireNonNull(destination, "Destination class cannot be null");
		}
	}

	@Override
	public <destination> destination map(Object source, destination destination) throws MapperException {
		log.atTrace().log("Entering map(source={}, destination={})", source, destination);

		if (destination == null)
			throw new MapperException("destination cannot be null");

		destination result = this.map(source, (Class<destination>) destination.getClass(), destination);
		log.atTrace().log("Exiting map() with result={}", result);
		return result;
	}

	@Override
	public <destination> destination map(Object source, Class<destination> destinationClass) throws MapperException {
		log.atTrace().log("Entering map(source={}, destinationClass={})", source, destinationClass);

		if (destinationClass == null)
			throw new MapperException("detination class cannot be null");

		destination result = this.map(source, destinationClass, null);
		log.atTrace().log("Exiting map() with result={}", result);
		return result;
	}

	@Override
	public <destination> destination map(Object source, Class<destination> destinationClass, destination destination)
			throws MapperException {
		log.atTrace().log("Entering map(source={}, destinationClass={}, destination={})", source, destinationClass, destination);
		log.atDebug().log("Mapping source type {} to destination type {}", source != null ? source.getClass().getSimpleName() : "null", destinationClass);

		try {

			if (destinationClass == null)
				destinationClass = (Class<destination>) destination.getClass();

			MappingConfiguration mappingConfiguration = this.getMappingConfiguration(source.getClass(),
					destinationClass);

			log.atDebug().log("Using mapping direction: {}", mappingConfiguration.mappingDirection());

			switch (mappingConfiguration.mappingDirection()) {
				case REGULAR:
					log.atDebug().log("Applying REGULAR mapping with {} destination rules", mappingConfiguration.destinationRules().size());
					destination result = this.doMapping(mappingConfiguration.mappingDirection(), destinationClass, destination,
							source, mappingConfiguration.destinationRules());
					log.atInfo().log("Mapping complete: {} -> {}", source.getClass().getSimpleName(), destinationClass.getSimpleName());
					log.atTrace().log("Exiting map() with result={}", result);
					return result;
				case REVERSE:
					log.atDebug().log("Applying REVERSE mapping with {} source rules", mappingConfiguration.sourceRules().size());
					destination result2 = this.doMapping(mappingConfiguration.mappingDirection(), destinationClass, destination,
							source, mappingConfiguration.sourceRules());
					log.atInfo().log("Mapping complete: {} -> {}", source.getClass().getSimpleName(), destinationClass.getSimpleName());
					log.atTrace().log("Exiting map() with result={}", result2);
					return result2;
			}

			destination result3 = this.doMapping(mappingConfiguration.mappingDirection(), destinationClass, destination, source,
					mappingConfiguration.destinationRules());
			log.atInfo().log("Mapping complete: {} -> {}", source.getClass().getSimpleName(), destinationClass.getSimpleName());
			log.atTrace().log("Exiting map() with result={}", result3);
			return result3;

		} catch (MapperException e) {
			log.atError().log("Mapping failed from {} to {}: {}", source != null ? source.getClass().getSimpleName() : "null", destinationClass, e.getMessage());
			throw new MapperException(e.getMessage(), e);
		}
	}

	private <destination> destination doMapping(MappingDirection mappingDirection,
			Class<destination> destinationClass, destination destObject, Object source, List<MappingRule> rules)
			throws MapperException {
		log.atTrace().log("Entering doMapping(direction={}, destinationClass={}, rules count={})", mappingDirection, destinationClass, rules.size());
		log.atDebug().log("Processing {} mapping rules for {} -> {}", rules.size(), source != null ? source.getClass().getSimpleName() : "null", destinationClass.getSimpleName());

		for (MappingRule rule : rules) {
			log.atDebug().log("Processing mapping rule: {}", rule);
			try {
				IMappingRuleExecutor executor = MappingRules.getRuleExecutor(this, mappingDirection, rule, source,
						destinationClass);
				log.atDebug().log("Executing mapping rule with executor: {}", executor != null ? executor.getClass().getSimpleName() : "null");
				destObject = executor.doMapping(destinationClass, destObject, source);
			} catch (MapperException e) {
				if (this.configuration.failOnError()) {
					log.atError().log("Mapping rule execution failed, aborting: {}", e.getMessage());
					throw new MapperException("Unable to do mapping, aborting", e);
				} else {
					log.atWarn().log("Mapping rule execution failed, ignoring: {}", e.getMessage());
					continue;
				}
			}
		}

		log.atTrace().log("Exiting doMapping() with destObject={}", destObject);
		return destObject;
	}

	private MappingDirection determineMapingDirection(List<MappingRule> sourceRules,
			List<MappingRule> destinationRules) throws MapperException {
		log.atTrace().log("Entering determineMapingDirection(sourceRules count={}, destinationRules count={})", sourceRules.size(), destinationRules.size());
		log.atDebug().log("Determining mapping direction: source rules={}, destination rules={}", sourceRules.size(), destinationRules.size());

		if (sourceRules.size() == 0 && destinationRules.size() != 0) {
			log.atDebug().log("Determined mapping direction: REGULAR");
			log.atTrace().log("Exiting determineMapingDirection() with REGULAR");
			return MappingDirection.REGULAR;
		} else if (sourceRules.size() != 0 && destinationRules.size() == 0) {
			log.atDebug().log("Determined mapping direction: REVERSE");
			log.atTrace().log("Exiting determineMapingDirection() with REVERSE");
			return MappingDirection.REVERSE;
		} else {
			log.atError().log("Cannot determine mapping direction: sourceRules={}, destinationRules={}", sourceRules.size(), destinationRules.size());
			throw new MapperException(
					"Cannot determine mapping direction as source and destination are annotated with mapping rules, or neither has mapping rule");
		}
	}

	@Override
	public Mapper configure(MapperConfigurationItem element, Object value) {
		log.atTrace().log("Entering configure(element={}, value={})", element, value);
		log.atDebug().log("Configuring mapper: {} = {}", element, value);

		this.configuration.configure(element, value);

		log.atTrace().log("Exiting configure()");
		return this;
	}

	/**
	 * Creates a mapping configuration without storing it in the map.
	 * Used internally by computeIfAbsent to avoid recursive update.
	 */
	private MappingConfiguration createMappingConfiguration(Class<?> source, Class<?> destination)
			throws MapperException {
		log.atTrace().log("Entering createMappingConfiguration(source={}, destination={})", source, destination);
		log.atDebug().log("Creating mapping configuration: {} -> {}", source.getSimpleName(), destination.getSimpleName());

		List<MappingRule> destinationRules = MappingRules.parse(destination);
		log.atDebug().log("Parsed {} destination rules", destinationRules.size());

		List<MappingRule> sourceRules = MappingRules.parse(source);
		log.atDebug().log("Parsed {} source rules", sourceRules.size());

		MappingDirection mappingDirection = this.determineMapingDirection(sourceRules, destinationRules);
		MappingConfiguration configuration = new MappingConfiguration(source, destination, sourceRules,
				destinationRules, mappingDirection);

		try {
			if (this.configuration.doValidation()) {
				log.atDebug().log("Validating mapping configuration for direction: {}", mappingDirection);

				if (mappingDirection == MappingDirection.REVERSE)
					MappingRules.validate(destination, destinationRules);
				if (mappingDirection == MappingDirection.REGULAR)
					MappingRules.validate(source, sourceRules);

				log.atDebug().log("Mapping configuration validation successful");
			}
		} catch (MapperException e) {
			if (this.configuration.failOnError()) {
				log.atError().log("Mapping configuration validation failed, aborting: {}", e.getMessage());
				throw new MapperException("Unable to validate mapping, aborting", e);
			} else {
				log.atWarn().log("Mapping configuration validation failed, ignoring: {}", e.getMessage());
			}
		}

		log.atDebug().log("Created mapping configuration: {}", configuration);
		log.atTrace().log("Exiting createMappingConfiguration() with configuration={}", configuration);
		return configuration;
	}

	@Override
	public MappingConfiguration recordMappingConfiguration(Class<?> source, Class<?> destination)
			throws MapperException {
		log.atTrace().log("Entering recordMappingConfiguration(source={}, destination={})", source, destination);
		log.atDebug().log("Recording mapping configuration: {} -> {}", source.getSimpleName(), destination.getSimpleName());

		MappingConfiguration configuration = createMappingConfiguration(source, destination);
		MappingKey key = new MappingKey(source, destination);
		this.mappingConfigurations.put(key, configuration);

		log.atDebug().log("Recorded mapping configuration: {}", configuration);
		log.atTrace().log("Exiting recordMappingConfiguration() with configuration={}", configuration);
		return configuration;
	}

	@Override
	public MappingConfiguration getMappingConfiguration(Class<?> source, Class<?> destination)
			throws MapperException {
		log.atTrace().log("Entering getMappingConfiguration(source={}, destination={})", source, destination);
		log.atDebug().log("Getting mapping configuration: {} -> {}", source.getSimpleName(), destination.getSimpleName());

		MappingKey key = new MappingKey(source, destination);

		// Thread-safe compute-if-absent pattern to avoid race conditions
		// computeIfAbsent automatically stores the returned value, so we must NOT call put() inside
		try {
			MappingConfiguration result = this.mappingConfigurations.computeIfAbsent(key, k -> {
				log.atDebug().log("Mapping configuration not found in cache, creating new one");
				try {
					return this.createMappingConfiguration(source, destination);
				} catch (MapperException e) {
					// Wrap checked exception in unchecked for computeIfAbsent
					throw new MapperRuntimeException("Failed to create mapping configuration", e);
				}
			});
			log.atDebug().log("Retrieved mapping configuration: {}", result);
			log.atTrace().log("Exiting getMappingConfiguration() with configuration={}", result);
			return result;
		} catch (MapperRuntimeException e) {
			log.atError().log("Failed to get/create mapping configuration: {}", e.getMessage());
			// Unwrap and rethrow original MapperException
			throw (MapperException) e.getCause();
		}
	}

	/**
	 * Runtime exception wrapper for use in computeIfAbsent lambda.
	 * Unwrapped in calling code.
	 */
	private static class MapperRuntimeException extends RuntimeException {
		public MapperRuntimeException(String message, MapperException cause) {
			super(message, cause);
		}
	}
}
