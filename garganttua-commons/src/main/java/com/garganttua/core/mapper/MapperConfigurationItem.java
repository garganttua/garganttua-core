package com.garganttua.core.mapper;

/**
 * Defines the available configuration items for the mapper.
 *
 * @since 2.0.0-ALPHA01
 */
public enum MapperConfigurationItem {

	/**
	 * Controls whether the mapper throws exceptions on mapping errors.
	 * Type: Boolean. Default: true
	 */
	FAIL_ON_ERROR,

	/**
	 * Controls whether the mapper performs validation during mapping.
	 * Type: Boolean. Default: true
	 */
	DO_VALIDATION,

	/**
	 * Controls whether the mapper throws on object reference cycles.
	 * Type: Boolean. Default: true
	 */
	FAIL_ON_CYCLE,

	/**
	 * Controls whether the mapper automatically maps fields by name convention
	 * when no mapping annotations are found.
	 * Type: Boolean. Default: true
	 */
	AUTO_CONVENTION_MAPPING,

	/**
	 * Controls whether the mapper validates that every destination field is covered
	 * by a mapping rule (excluding static, transient, synthetic, and @MappingIgnore fields).
	 * Type: Boolean. Default: false
	 */
	STRICT_MODE

}
