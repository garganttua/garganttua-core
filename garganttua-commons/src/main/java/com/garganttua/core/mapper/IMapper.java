package com.garganttua.core.mapper;

public interface IMapper {
	
	IMapper configure(MapperConfigurationItem element, Object value);

	<destination> destination map(Object source, Class<destination> destinationClass) throws MapperException;

	<destination> destination map(Object source, Class<destination> destinationClass, destination destinationObject) throws MapperException;

	<destination> destination map(Object source, destination destinationObject) throws MapperException;

	MappingConfiguration recordMappingConfiguration(Class<?> source, Class<?> destination) throws MapperException;

	MappingConfiguration getMappingConfiguration(Class<?> source, Class<?> destination) throws MapperException;

}
