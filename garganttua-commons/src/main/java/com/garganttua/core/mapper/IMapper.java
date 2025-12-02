package com.garganttua.core.mapper;

/**
 * Provides object-to-object mapping capabilities with configurable behavior.
 * <p>
 * The mapper facilitates data transformation between objects of different types,
 * typically used for converting between domain models, DTOs, and API representations.
 * It supports both automatic field-to-field mapping based on conventions and
 * explicit mapping rules defined through annotations or configuration.
 * </p>
 *
 * <h2>Thread Safety:</h2>
 * <p>
 * Implementations may or may not be thread-safe. Refer to specific implementation
 * documentation for thread-safety guarantees.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * // Configure the mapper
 * IMapper mapper = new Mapper();
 * mapper.configure(MapperConfigurationItem.FAIL_ON_ERROR, false);
 * mapper.configure(MapperConfigurationItem.DO_VALIDATION, true);
 *
 * // Simple mapping - create new destination object
 * UserDTO dto = mapper.map(user, UserDTO.class);
 *
 * // Mapping into existing object
 * UserDTO existingDto = new UserDTO();
 * mapper.map(user, UserDTO.class, existingDto);
 *
 * // Record and retrieve mapping configuration
 * MappingConfiguration config = mapper.recordMappingConfiguration(User.class, UserDTO.class);
 * System.out.println("Mapping rules: " + config.sourceRules());
 * </pre>
 *
 * @since 2.0.0-ALPHA01
 */
public interface IMapper {

	/**
	 * Configures the mapper behavior with a specific configuration item.
	 *
	 * @param element the configuration item to set
	 * @param value the value for the configuration item
	 * @return this mapper instance for method chaining
	 */
	IMapper configure(MapperConfigurationItem element, Object value);

	/**
	 * Maps a source object to a new instance of the destination class.
	 * <p>
	 * Creates a new instance of the destination class and populates its fields
	 * with values from the source object according to the mapping rules.
	 * </p>
	 *
	 * @param <destination> the type of the destination object
	 * @param source the source object to map from
	 * @param destinationClass the class of the destination object
	 * @return a new instance of the destination class populated with mapped values
	 * @throws MapperException if mapping fails or destination cannot be instantiated
	 */
	<destination> destination map(Object source, Class<destination> destinationClass) throws MapperException;

	/**
	 * Maps a source object into an existing destination object.
	 * <p>
	 * Populates the provided destination object with values from the source object.
	 * The destination class parameter allows the mapper to resolve the correct
	 * mapping configuration.
	 * </p>
	 *
	 * @param <destination> the type of the destination object
	 * @param source the source object to map from
	 * @param destinationClass the class of the destination object
	 * @param destinationObject the existing destination object to populate
	 * @return the populated destination object
	 * @throws MapperException if mapping fails
	 */
	<destination> destination map(Object source, Class<destination> destinationClass, destination destinationObject) throws MapperException;

	/**
	 * Maps a source object into an existing destination object.
	 * <p>
	 * Populates the provided destination object with values from the source object.
	 * The destination class is inferred from the destination object's runtime type.
	 * </p>
	 *
	 * @param <destination> the type of the destination object
	 * @param source the source object to map from
	 * @param destinationObject the existing destination object to populate
	 * @return the populated destination object
	 * @throws MapperException if mapping fails
	 */
	<destination> destination map(Object source, destination destinationObject) throws MapperException;

	/**
	 * Records and returns the mapping configuration between source and destination classes.
	 * <p>
	 * Analyzes the classes and their mapping annotations to build a complete
	 * mapping configuration that describes how fields are mapped between the
	 * source and destination types.
	 * </p>
	 *
	 * @param source the source class
	 * @param destination the destination class
	 * @return the mapping configuration describing the field mappings
	 * @throws MapperException if configuration cannot be determined
	 */
	MappingConfiguration recordMappingConfiguration(Class<?> source, Class<?> destination) throws MapperException;

	/**
	 * Retrieves the previously recorded mapping configuration between classes.
	 * <p>
	 * Returns the cached mapping configuration if it exists. If no configuration
	 * has been recorded, behavior depends on the implementation (may return null
	 * or create a new configuration).
	 * </p>
	 *
	 * @param source the source class
	 * @param destination the destination class
	 * @return the mapping configuration, or null if not recorded
	 * @throws MapperException if configuration cannot be retrieved
	 */
	MappingConfiguration getMappingConfiguration(Class<?> source, Class<?> destination) throws MapperException;

}
