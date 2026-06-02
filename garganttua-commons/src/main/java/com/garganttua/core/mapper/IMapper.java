package com.garganttua.core.mapper;

import com.garganttua.core.reflection.IClass;

/**
 * Provides object-to-object mapping capabilities with configurable behavior.
 *
 * @since 2.0.0-ALPHA01
 */
public interface IMapper {

	/**
	 * Sets a configuration item that tunes this mapper's behavior.
	 *
	 * @param element the configuration item to set
	 * @param value the value to assign to the configuration item
	 * @return this mapper for method chaining
	 */
	IMapper configure(MapperConfigurationItem element, Object value);

	/**
	 * Maps the given source object into a newly created instance of the destination class.
	 *
	 * @param <destination> the destination type
	 * @param source the source object to map from
	 * @param destinationClass the class of the destination object to create
	 * @return the populated destination instance
	 * @throws MapperException if the mapping fails
	 */
	<destination> destination map(Object source, IClass<destination> destinationClass) throws MapperException;

	/**
	 * Maps the given source object into the supplied destination instance, of the given class.
	 *
	 * @param <destination> the destination type
	 * @param source the source object to map from
	 * @param destinationClass the class of the destination object
	 * @param destinationObject the existing destination instance to populate
	 * @return the populated destination instance
	 * @throws MapperException if the mapping fails
	 */
	<destination> destination map(Object source, IClass<destination> destinationClass, destination destinationObject) throws MapperException;

	/**
	 * Maps the given source object into the supplied destination instance.
	 *
	 * @param <destination> the destination type
	 * @param source the source object to map from
	 * @param destinationObject the existing destination instance to populate
	 * @return the populated destination instance
	 * @throws MapperException if the mapping fails
	 */
	<destination> destination map(Object source, destination destinationObject) throws MapperException;

	/**
	 * Computes and caches the mapping configuration for the given source/destination pair.
	 *
	 * @param source the source class
	 * @param destination the destination class
	 * @return the recorded mapping configuration
	 * @throws MapperException if the configuration cannot be computed
	 */
	MappingConfiguration recordMappingConfiguration(IClass<?> source, IClass<?> destination) throws MapperException;

	/**
	 * Returns the mapping configuration for the given source/destination pair, computing it if absent.
	 *
	 * @param source the source class
	 * @param destination the destination class
	 * @return the mapping configuration
	 * @throws MapperException if the configuration cannot be resolved
	 */
	MappingConfiguration getMappingConfiguration(IClass<?> source, IClass<?> destination) throws MapperException;

	/**
	 * Registers a programmatically-built mapping configuration.
	 *
	 * @param config the mapping configuration to register
	 * @throws MapperException if registration fails
	 */
	void register(MappingConfiguration config) throws MapperException;

	/**
	 * Adds a listener for mapping events (before/after/error).
	 *
	 * @param listener the listener to add
	 */
	void addListener(IMappingListener listener);

	/**
	 * Returns the metrics collected by this mapper.
	 *
	 * @return the mapper metrics
	 */
	MapperMetrics getMetrics();

}
