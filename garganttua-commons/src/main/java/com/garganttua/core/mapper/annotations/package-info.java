/**
 * Object mapping annotations for declarative field and object mapping rules.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides annotations for defining declarative object mapping rules.
 * These annotations enable automatic mapping between different object types with
 * customizable field-to-field transformations and mapping strategies.
 * </p>
 *
 * <h2>Core Annotations</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.mapper.annotations.ObjectMappingRule} - Defines object-level mapping rules</li>
 *   <li>{@link com.garganttua.core.mapper.annotations.FieldMappingRule} - Defines field-level mapping rules</li>
 * </ul>
 *
 * <h2>Usage Example: Field-Level Mapping</h2>
 * <p>
 * {@link com.garganttua.core.mapper.annotations.FieldMappingRule} is placed on a
 * destination field and names the source field it reads from. Declare it more than
 * once (one per {@code source} class) to map a single DTO from multiple entity types.
 * </p>
 * <pre>{@code
 * public class UserDTO {
 *
 *     // Direct field mapping from the source's "email" field
 *     @FieldMappingRule(sourceFieldAddress = "email")
 *     private String emailAddress;
 *
 *     // Nested field path
 *     @FieldMappingRule(sourceFieldAddress = "address.city")
 *     private String city;
 *
 *     // Multi-source: one rule per source class
 *     @FieldMappingRule(source = UserEntity.class, sourceFieldAddress = "firstName")
 *     @FieldMappingRule(source = LegacyUser.class, sourceFieldAddress = "prenom")
 *     private String name;
 * }
 * }</pre>
 *
 * <h2>Usage Example: Field Transformations</h2>
 * <p>
 * {@code fromSourceMethod} and {@code toSourceMethod} name static converter methods in
 * {@code "ClassName.methodName"} form, applied during forward and reverse mapping.
 * </p>
 * <pre>{@code
 * public class UserDTO {
 *
 *     @FieldMappingRule(
 *         sourceFieldAddress = "birthDate",
 *         fromSourceMethod = "DateUtils.calculateAge",
 *         toSourceMethod = "DateUtils.estimateBirthYear"
 *     )
 *     private Integer age;
 * }
 * }</pre>
 *
 * <h2>Usage Example: Object-Level Mapping</h2>
 * <p>
 * {@link com.garganttua.core.mapper.annotations.ObjectMappingRule} is placed on the
 * destination class to delegate the whole transformation to static converter methods.
 * </p>
 * <pre>{@code
 * @ObjectMappingRule(
 *     fromSourceMethod = "UserConverter.toDTO",
 *     toSourceMethod = "UserConverter.fromDTO"
 * )
 * public class UserDTO {
 *     private String name;
 *     private String email;
 * }
 * }</pre>
 *
 * <h2>Usage Example: Excluding Fields</h2>
 * <p>
 * {@link com.garganttua.core.mapper.annotations.MappingIgnore} excludes a field from
 * convention-based mapping.
 * </p>
 * <pre>{@code
 * public class UserDTO {
 *     private String name;        // mapped by convention
 *
 *     @MappingIgnore
 *     private String internalId;  // never mapped
 * }
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Declarative field- and object-level mapping rules</li>
 *   <li>Static converter methods for forward and reverse transformation</li>
 *   <li>Nested field paths via dot notation</li>
 *   <li>Per-source rules on a single destination (repeatable annotations)</li>
 *   <li>Field exclusion via {@code @MappingIgnore}</li>
 *   <li>Convention-based mapping by field name</li>
 * </ul>
 *
 * <h2>Integration</h2>
 * <p>
 * These annotations work with:
 * </p>
 * <ul>
 *   <li>Object mapper framework for automatic mapping</li>
 *   <li>DTO to entity conversion</li>
 *   <li>API request/response transformations</li>
 *   <li>Data layer abstraction</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.mapper
 * @see com.garganttua.core.mapper.annotations.ObjectMappingRule
 * @see com.garganttua.core.mapper.annotations.FieldMappingRule
 */
package com.garganttua.core.mapper.annotations;
