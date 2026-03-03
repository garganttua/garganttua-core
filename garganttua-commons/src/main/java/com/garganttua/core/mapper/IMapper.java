package com.garganttua.core.mapper;

import com.garganttua.core.reflection.IClass;

/**
 * Provides object-to-object mapping capabilities with configurable behavior.
 *
 * @since 2.0.0-ALPHA01
 */
public interface IMapper {

	IMapper configure(MapperConfigurationItem element, Object value);

	<destination> destination map(Object source, IClass<destination> destinationClass) throws MapperException;

	<destination> destination map(Object source, IClass<destination> destinationClass, destination destinationObject) throws MapperException;

	<destination> destination map(Object source, destination destinationObject) throws MapperException;

	MappingConfiguration recordMappingConfiguration(IClass<?> source, IClass<?> destination) throws MapperException;

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
