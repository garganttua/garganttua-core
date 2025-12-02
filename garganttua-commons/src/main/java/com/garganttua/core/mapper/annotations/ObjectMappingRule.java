package com.garganttua.core.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;

/**
 * Defines object-level mapping rules for type transformations.
 * <p>
 * This annotation is applied to a class to specify custom transformation methods
 * that should be used when mapping the entire object. Unlike field-level mapping
 * rules, object-level rules apply to the whole object transformation and can be
 * used to implement complex conversion logic.
 * </p>
 *
 * <h2>Attributes:</h2>
 * <ul>
 *   <li><b>fromSourceMethod</b>: The method to call when converting from source to destination</li>
 *   <li><b>toSourceMethod</b>: The method to call when converting from destination to source (reverse mapping)</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * // Define a DTO with object-level transformation methods
 * {@literal @}ObjectMappingRule(
 *     fromSourceMethod = "UserConverter.toDTO",
 *     toSourceMethod = "UserConverter.fromDTO"
 * )
 * public class UserDTO {
 *     private String name;
 *     private String email;
 *     // ... getters and setters
 * }
 *
 * // Converter class with transformation methods
 * public class UserConverter {
 *
 *     public static UserDTO toDTO(User user) {
 *         UserDTO dto = new UserDTO();
 *         dto.setName(user.getFirstName() + " " + user.getLastName());
 *         dto.setEmail(user.getEmailAddress());
 *         return dto;
 *     }
 *
 *     public static User fromDTO(UserDTO dto) {
 *         User user = new User();
 *         String[] names = dto.getName().split(" ");
 *         user.setFirstName(names[0]);
 *         user.setLastName(names.length &gt; 1 ? names[1] : "");
 *         user.setEmailAddress(dto.getEmail());
 *         return user;
 *     }
 * }
 *
 * // Use the mapper
 * IMapper mapper = new Mapper();
 * UserDTO dto = mapper.map(user, UserDTO.class); // Calls UserConverter.toDTO
 * User user = mapper.map(dto, User.class);       // Calls UserConverter.fromDTO
 * </pre>
 *
 * @since 2.0.0-ALPHA01
 */
@Native
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ObjectMappingRule {

	/**
	 * The fully qualified method name to convert from source to destination.
	 * <p>
	 * Format: "ClassName.methodName"<br>
	 * Example: "UserConverter.toDTO"
	 * </p>
	 * <p>
	 * The method should be static and accept the source object as parameter,
	 * returning the destination object.
	 * </p>
	 *
	 * @return the method name for source to destination conversion
	 */
	String fromSourceMethod();

	/**
	 * The fully qualified method name to convert from destination to source.
	 * <p>
	 * Format: "ClassName.methodName"<br>
	 * Example: "UserConverter.fromDTO"
	 * </p>
	 * <p>
	 * The method should be static and accept the destination object as parameter,
	 * returning the source object.
	 * </p>
	 *
	 * @return the method name for destination to source conversion
	 */
	String toSourceMethod();

}
