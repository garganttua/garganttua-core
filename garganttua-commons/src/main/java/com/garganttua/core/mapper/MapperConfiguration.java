package com.garganttua.core.mapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages global configuration settings for the mapper.
 * <p>
 * This class provides a thread-safe storage for mapper configuration options
 * that control the behavior of mapping operations. The configuration is stored
 * in a concurrent map to allow safe concurrent access.
 * </p>
 *
 * <h2>Default Configuration:</h2>
 * <ul>
 *   <li><b>FAIL_ON_ERROR</b>: true - Mapping throws exception on errors</li>
 *   <li><b>DO_VALIDATION</b>: true - Validation is performed during mapping</li>
 * </ul>
 *
 * <h2>Thread Safety:</h2>
 * <p>
 * This class is thread-safe. Configuration changes are immediately visible to
 * all threads due to the use of ConcurrentHashMap.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * MapperConfiguration config = new MapperConfiguration();
 *
 * // Enable lenient mode (don't fail on errors)
 * config.configure(MapperConfigurationItem.FAIL_ON_ERROR, false);
 *
 * // Disable validation for performance
 * config.configure(MapperConfigurationItem.DO_VALIDATION, false);
 *
 * // Check configuration
 * if (config.failOnError()) {
 *     System.out.println("Mapper will throw exceptions on errors");
 * }
 *
 * if (config.doValidation()) {
 *     System.out.println("Mapper will validate mappings");
 * }
 *
 * // Retrieve raw configuration value
 * Object value = config.getConfiguration(MapperConfigurationItem.FAIL_ON_ERROR);
 * </pre>
 *
 * @since 2.0.0-ALPHA01
 */
public class MapperConfiguration {

	private final Map<MapperConfigurationItem, Object> configurations = new ConcurrentHashMap<>();

	/**
	 * Creates a new MapperConfiguration with default settings.
	 * <p>
	 * Default settings:
	 * </p>
	 * <ul>
	 *   <li>FAIL_ON_ERROR: true</li>
	 *   <li>DO_VALIDATION: true</li>
	 * </ul>
	 */
	public MapperConfiguration() {
		this.configurations.put(MapperConfigurationItem.FAIL_ON_ERROR, true);
		this.configurations.put(MapperConfigurationItem.DO_VALIDATION, true);
	}

	/**
	 * Sets a configuration value.
	 *
	 * @param element the configuration item to set
	 * @param value the value to assign to the configuration item
	 */
	public void configure(MapperConfigurationItem element, Object value) {
		this.configurations.put(element, value);
	}

	/**
	 * Retrieves a configuration value.
	 *
	 * @param element the configuration item to retrieve
	 * @return the value of the configuration item, or null if not set
	 */
	public Object getConfiguration(MapperConfigurationItem element) {
		return this.configurations.get(element);
	}

	/**
	 * Checks whether validation is enabled.
	 *
	 * @return true if validation should be performed during mapping, false otherwise
	 */
	public boolean doValidation() {
		return (boolean) this.configurations.get(MapperConfigurationItem.DO_VALIDATION);
	}

	/**
	 * Checks whether the mapper should fail on errors.
	 *
	 * @return true if the mapper should throw exceptions on errors, false for lenient mode
	 */
	public boolean failOnError() {
		return (boolean) this.configurations.get(MapperConfigurationItem.FAIL_ON_ERROR);
	}

}
