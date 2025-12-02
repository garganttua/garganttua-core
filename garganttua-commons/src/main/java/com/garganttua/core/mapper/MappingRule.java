package com.garganttua.core.mapper;

import java.util.Objects;

import com.garganttua.core.reflection.ObjectAddress;

/**
 * Represents a single mapping rule between source and destination fields.
 * <p>
 * A mapping rule defines how a specific field in a source object is mapped to
 * a corresponding field in a destination object. It includes optional transformation
 * methods that can be applied during the mapping process.
 * </p>
 *
 * <h2>Components:</h2>
 * <ul>
 *   <li><b>sourceFieldAddress</b>: Address of the source field to read from</li>
 *   <li><b>destinationFieldAddress</b>: Address of the destination field to write to</li>
 *   <li><b>destinationClass</b>: Type of the destination field</li>
 *   <li><b>fromSourceMethodAddress</b>: Optional method to transform data from source format</li>
 *   <li><b>toSourceMethodAddress</b>: Optional method to transform data to source format (for reverse mapping)</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * // Create a simple field-to-field mapping rule
 * MappingRule simpleRule = new MappingRule(
 *     new ObjectAddress("user.email"),
 *     new ObjectAddress("dto.emailAddress"),
 *     String.class,
 *     null,
 *     null
 * );
 *
 * // Create a mapping rule with transformation methods
 * MappingRule withTransform = new MappingRule(
 *     new ObjectAddress("user.birthDate"),
 *     new ObjectAddress("dto.age"),
 *     Integer.class,
 *     new ObjectAddress("DateUtils.calculateAge"),
 *     new ObjectAddress("DateUtils.calculateBirthDate")
 * );
 *
 * // Mapping rules are typically used within MappingConfiguration
 * MappingConfiguration config = new MappingConfiguration(
 *     User.class,
 *     UserDTO.class,
 *     List.of(simpleRule, withTransform),
 *     List.of(),
 *     MappingDirection.REGULAR
 * );
 * </pre>
 *
 * @param sourceFieldAddress the address of the source field
 * @param destinationFieldAddress the address of the destination field
 * @param destinationClass the type of the destination field
 * @param fromSourceMethodAddress optional method to transform from source format
 * @param toSourceMethodAddress optional method to transform to source format
 *
 * @since 2.0.0-ALPHA01
 */
public record MappingRule (
		ObjectAddress sourceFieldAddress,
		ObjectAddress destinationFieldAddress,
		Class<?> destinationClass,
		ObjectAddress fromSourceMethodAddress,
		ObjectAddress toSourceMethodAddress
	){

    @Override
    public String toString() {
        return "APIMappingRule{" +
                "sourceFieldAddress='" + sourceFieldAddress + '\'' +
                ", destinationFieldAddress='" + destinationFieldAddress + '\'' +
                ", destinationClass=" + destinationClass +
                ", fromSourceMethod=" + fromSourceMethodAddress +
                ", toSourceMethod=" + toSourceMethodAddress +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MappingRule that = (MappingRule) o;
        return Objects.equals(sourceFieldAddress, that.sourceFieldAddress) &&
                Objects.equals(destinationFieldAddress, that.destinationFieldAddress) &&
                Objects.equals(destinationClass, that.destinationClass) &&
                Objects.equals(fromSourceMethodAddress, that.fromSourceMethodAddress) &&
                Objects.equals(toSourceMethodAddress, that.toSourceMethodAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceFieldAddress, destinationFieldAddress, destinationClass, fromSourceMethodAddress, toSourceMethodAddress);
    }

}
