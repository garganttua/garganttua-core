/**
 * Declarative object-to-object mapping engine with annotation-based rules.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides a flexible object mapping framework for converting between different
 * object types. It supports annotation-based mapping rules, field-level transformations,
 * and bidirectional mapping between DTOs and domain entities.
 * </p>
 *
 * <h2>Core Types</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.mapper.IMapper} - main mapper interface</li>
 *   <li>{@link com.garganttua.core.mapper.MappingConfiguration} - rules and metadata for a source/destination pair</li>
 *   <li>{@link com.garganttua.core.mapper.MappingRule} - a single field-to-field rule</li>
 *   <li>{@link com.garganttua.core.mapper.IMappingRuleExecutor} - custom transformation hook</li>
 *   <li>{@link com.garganttua.core.mapper.MapperConfiguration} - global mapper behavior flags</li>
 * </ul>
 *
 * <h2>Annotation-Based Mapping</h2>
 * <p>
 * Mapping rules are declared on the <em>destination</em> type via
 * {@link com.garganttua.core.mapper.annotations.FieldMappingRule} (per field) and
 * {@link com.garganttua.core.mapper.annotations.ObjectMappingRule} (whole object).
 * </p>
 * <pre>{@code
 * public class UserDTO {
 *
 *     // Map from the source's "email" field
 *     @FieldMappingRule(sourceFieldAddress = "email")
 *     private String emailAddress;
 *
 *     // Map with a static converter method "ClassName.methodName"
 *     @FieldMappingRule(
 *         sourceFieldAddress = "birthDate",
 *         fromSourceMethod = "DateUtils.calculateAge")
 *     private Integer age;
 * }
 *
 * // Map a source instance into a new destination instance
 * UserDTO dto = mapper.map(user, IClass.getClass(UserDTO.class));
 * }</pre>
 *
 * <h2>Reverse Mapping</h2>
 * <p>
 * Rules carrying a {@code toSourceMethod} (or a registered reverse
 * {@link com.garganttua.core.mapper.MappingConfiguration} with
 * {@link com.garganttua.core.mapper.MappingDirection#REVERSE}) allow mapping back from
 * the destination to the source format using the same {@code map} overloads.
 * </p>
 *
 * <h2>Programmatic Mapping</h2>
 * <p>
 * Configurations can be built without annotations through
 * {@link com.garganttua.core.mapper.dsl.IMappingConfigurationBuilder} and registered via
 * {@link com.garganttua.core.mapper.IMapper#register(com.garganttua.core.mapper.MappingConfiguration)}.
 * </p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Annotation-based and programmatic mapping rules</li>
 *   <li>Field-level transformation via static converter methods</li>
 *   <li>Forward and reverse mapping</li>
 *   <li>Nested object and nested field-path mapping</li>
 *   <li>Custom mapping logic via {@link com.garganttua.core.mapper.IMappingRuleExecutor}</li>
 *   <li>Cycle detection and configurable error handling</li>
 * </ul>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li><b>DTO to Entity</b> - Convert API DTOs to domain entities</li>
 *   <li><b>Entity to DTO</b> - Serialize entities for API responses</li>
 *   <li><b>Data transformation</b> - Format conversion, validation</li>
 *   <li><b>Legacy integration</b> - Adapt between different data models</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.mapper.annotations} - Mapping annotations</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.mapper.IMapper
 * @see com.garganttua.core.mapper.annotations
 */
package com.garganttua.core.mapper;
