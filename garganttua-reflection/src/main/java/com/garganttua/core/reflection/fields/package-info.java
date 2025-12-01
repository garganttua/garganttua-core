/**
 * Field access, manipulation, and introspection utilities.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides utilities for accessing and manipulating object fields via
 * reflection. It offers type-safe field access with automatic accessibility handling,
 * type conversion, and null safety.
 * </p>
 *
 * <h2>Usage Example: Field Access</h2>
 * <pre>{@code
 * public class User {
 *     private String email;
 *     private int loginCount;
 * }
 *
 * User user = new User();
 *
 * // Create field accessor
 * FieldAccessor emailAccessor = new FieldAccessor(User.class, "email");
 *
 * // Set field value (handles private access automatically)
 * emailAccessor.set(user, "user@example.com");
 *
 * // Get field value
 * String email = emailAccessor.get(user);
 * }</pre>
 *
 * <h2>Usage Example: Field Queries</h2>
 * <pre>{@code
 * // Find all fields with @Inject annotation
 * List<Field> injectableFields = FieldQuery.findFieldsWithAnnotation(
 *     UserService.class,
 *     Inject.class
 * );
 *
 * // Find all fields of specific type
 * List<Field> repositoryFields = FieldQuery.findFieldsOfType(
 *     UserService.class,
 *     Repository.class
 * );
 *
 * // Find field by name
 * Field emailField = FieldQuery.findField(User.class, "email");
 * }</pre>
 *
 * <h2>Usage Example: Field Manipulation</h2>
 * <pre>{@code
 * // Copy field values between objects
 * User source = new User();
 * source.setEmail("test@example.com");
 *
 * User target = new User();
 *
 * FieldCopier copier = new FieldCopier();
 * copier.copyField(source, target, "email");
 *
 * // Bulk copy all fields
 * copier.copyAllFields(source, target);
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Type-safe field access</li>
 *   <li>Automatic accessibility handling (private, protected, public)</li>
 *   <li>Type conversion support</li>
 *   <li>Null safety</li>
 *   <li>Field queries by name, type, annotation</li>
 *   <li>Field value copying</li>
 *   <li>Generic type preservation</li>
 *   <li>Exception wrapping</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.reflection
 */
package com.garganttua.core.reflection.fields;
