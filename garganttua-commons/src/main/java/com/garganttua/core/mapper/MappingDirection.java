
package com.garganttua.core.mapper;

/**
 * Defines the direction of a mapping operation.
 * <p>
 * The mapping direction indicates whether objects are being mapped in the
 * originally defined direction or in the reverse direction. This is particularly
 * useful for bidirectional mappings where both forward and reverse transformations
 * are supported.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * // Regular mapping: User -&gt; UserDTO
 * MappingConfiguration regularConfig = new MappingConfiguration(
 *     User.class,
 *     UserDTO.class,
 *     sourceRules,
 *     destinationRules,
 *     MappingDirection.REGULAR
 * );
 *
 * // Reverse mapping: UserDTO -&gt; User
 * MappingConfiguration reverseConfig = new MappingConfiguration(
 *     UserDTO.class,
 *     User.class,
 *     destinationRules,
 *     sourceRules,
 *     MappingDirection.REVERSE
 * );
 *
 * // The mapper can use direction to apply appropriate transformation methods
 * if (config.mappingDirection() == MappingDirection.REGULAR) {
 *     // Apply fromSourceMethod
 * } else {
 *     // Apply toSourceMethod
 * }
 * </pre>
 *
 * @since 2.0.0-ALPHA01
 */
public enum MappingDirection {

	/**
	 * Indicates mapping in the originally defined direction (source to destination).
	 * <p>
	 * In regular mode, the mapper applies the fromSourceMethod transformations
	 * defined in the mapping rules.
	 * </p>
	 */
	REGULAR,

	/**
	 * Indicates mapping in the reverse direction (destination to source).
	 * <p>
	 * In reverse mode, the mapper applies the toSourceMethod transformations
	 * defined in the mapping rules to convert back to the source format.
	 * </p>
	 */
	REVERSE

}
