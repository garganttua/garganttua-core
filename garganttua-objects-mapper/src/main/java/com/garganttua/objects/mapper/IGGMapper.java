package com.garganttua.objects.mapper;

public interface IGGMapper {
	
	GGMapper configure(GGMapperConfigurationItem element, Object value);

	<destination> destination map(Object source, Class<destination> destinationClass) throws GGMapperException;

	<destination> destination map(Object source, Class<destination> destinationClass, destination destinationObject) throws GGMapperException;

	<destination> destination map(Object source, destination destinationObject) throws GGMapperException;

	GGMappingConfiguration recordMappingConfiguration(Class<?> source, Class<?> destination) throws GGMapperException;

	GGMappingConfiguration getMappingConfiguration(Class<?> source, Class<?> destination) throws GGMapperException;

}
