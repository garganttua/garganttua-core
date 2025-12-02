package com.garganttua.core.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;

/**
 * Defines field-level mapping rules for property transformations.
 * <p>
 * This annotation is applied to a field in a destination class to specify which
 * source field it should be mapped from and optionally which transformation methods
 * should be applied during the mapping process.
 * </p>
 *
 * <h2>Attributes:</h2>
 * <ul>
 *   <li><b>sourceFieldAddress</b>: The address of the source field to map from (required)</li>
 *   <li><b>fromSourceMethod</b>: Optional method to transform data from source format</li>
 *   <li><b>toSourceMethod</b>: Optional method to transform data to source format (for reverse mapping)</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * // Source class
 * public class User {
 *     private String firstName;
 *     private String lastName;
 *     private Date birthDate;
 *     private String email;
 *     // ... getters and setters
 * }
 *
 * // Destination class with field mapping rules
 * public class UserDTO {
 *
 *     // Simple field mapping
 *     {@literal @}FieldMappingRule(sourceFieldAddress = "email")
 *     private String emailAddress;
 *
 *     // Field mapping with transformation
 *     {@literal @}FieldMappingRule(
 *         sourceFieldAddress = "birthDate",
 *         fromSourceMethod = "DateUtils.calculateAge",
 *         toSourceMethod = "DateUtils.estimateBirthYear"
 *     )
 *     private Integer age;
 *
 *     // Nested field mapping
 *     {@literal @}FieldMappingRule(sourceFieldAddress = "address.city")
 *     private String city;
 *
 *     // ... getters and setters
 * }
 *
 * // Transformation utility class
 * public class DateUtils {
 *     public static Integer calculateAge(Date birthDate) {
 *         // Calculate age from birth date
 *         return age;
 *     }
 *
 *     public static Date estimateBirthYear(Integer age) {
 *         // Estimate birth date from age
 *         return estimatedDate;
 *     }
 * }
 *
 * // Use the mapper
 * IMapper mapper = new Mapper();
 * UserDTO dto = mapper.map(user, UserDTO.class);
 * // dto.emailAddress will contain user.email
 * // dto.age will be calculated from user.birthDate using DateUtils.calculateAge
 * </pre>
 *
 * @since 2.0.0-ALPHA01
 */
@Native
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FieldMappingRule {

	/**
	 * The address of the source field to map from.
	 * <p>
	 * This can be a simple field name or a nested field path using dot notation.
	 * Examples:
	 * <ul>
	 *   <li>"email" - direct field mapping</li>
	 *   <li>"address.street" - nested field mapping</li>
	 *   <li>"contact.phoneNumbers[0]" - indexed field mapping</li>
	 * </ul>
	 * </p>
	 *
	 * @return the source field address
	 */
	String sourceFieldAddress();

	/**
	 * The fully qualified method name to transform data from source format.
	 * <p>
	 * Format: "ClassName.methodName"<br>
	 * Example: "DateUtils.calculateAge"
	 * </p>
	 * <p>
	 * The method should be static and accept the source field value as parameter,
	 * returning the transformed value for the destination field.
	 * </p>
	 * <p>
	 * If empty, no transformation is applied and direct field copying is used.
	 * </p>
	 *
	 * @return the transformation method name, or empty string for no transformation
	 */
	String fromSourceMethod() default "";

	/**
	 * The fully qualified method name to transform data to source format.
	 * <p>
	 * Format: "ClassName.methodName"<br>
	 * Example: "DateUtils.estimateBirthYear"
	 * </p>
	 * <p>
	 * The method should be static and accept the destination field value as parameter,
	 * returning the transformed value for the source field. This is used for reverse mapping.
	 * </p>
	 * <p>
	 * If empty, no transformation is applied during reverse mapping.
	 * </p>
	 *
	 * @return the reverse transformation method name, or empty string for no transformation
	 */
	String toSourceMethod() default "";

}
