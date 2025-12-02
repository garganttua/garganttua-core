package com.garganttua.core.mapper;

/**
 * Defines the available configuration items for the mapper.
 * <p>
 * These enumeration values represent the various configuration options that can
 * be set to control mapper behavior.
 * </p>
 *
 * <h2>Configuration Items:</h2>
 * <ul>
 *   <li><b>FAIL_ON_ERROR</b>: Controls whether the mapper throws exceptions when mapping errors occur.
 *       When true (default), mapping exceptions are thrown. When false, errors are logged but mapping continues.</li>
 *   <li><b>DO_VALIDATION</b>: Controls whether the mapper performs validation during mapping.
 *       When true (default), validation is performed. When false, validation is skipped for better performance.</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * IMapper mapper = new Mapper();
 *
 * // Configure mapper to not fail on errors (lenient mode)
 * mapper.configure(MapperConfigurationItem.FAIL_ON_ERROR, false);
 *
 * // Disable validation for better performance
 * mapper.configure(MapperConfigurationItem.DO_VALIDATION, false);
 *
 * // Now mapping will be more lenient and faster
 * UserDTO dto = mapper.map(user, UserDTO.class);
 * </pre>
 *
 * @since 2.0.0-ALPHA01
 */
public enum MapperConfigurationItem {

	/**
	 * Controls whether the mapper throws exceptions on mapping errors.
	 * <p>
	 * Type: Boolean<br>
	 * Default: true
	 * </p>
	 */
	FAIL_ON_ERROR,

	/**
	 * Controls whether the mapper performs validation during mapping.
	 * <p>
	 * Type: Boolean<br>
	 * Default: true
	 * </p>
	 */
	DO_VALIDATION

}
