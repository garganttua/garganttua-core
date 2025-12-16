/**
 * Object mapping framework implementation for DTO/entity transformations.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the concrete implementation of the object mapping framework.
 * It enables automatic mapping between different object types with support for
 * field-to-field copying, custom transformations, collection mapping, and nested
 * object mapping.
 * </p>
 *
 * <h2>Main Implementation Classes</h2>
 * <ul>
 *   <li>{@code Mapper} - Main object mapper implementation</li>
 *   <li>{@code MappingRules} - Mapping rule configuration and management</li>
 * </ul>
 *
 * <h2>Usage Example: Simple Mapping</h2>
 * <pre>{@code
 * // Define source entity
 * GenericEntity entity = new GenericEntity();
 * entity.setUuid("uuid");
 * entity.setId("id");
 *
 * // Perform mapping to DTO
 * Mapper mapper = new Mapper().configure(MapperConfigurationItem.FAIL_ON_ERROR, false);
 * GenericDto dto = mapper.map(entity, GenericDto.class);
 *
 * // dto.getUuid() == "uuid"
 * // dto.getId() == "id"
 * }</pre>
 *
 * <h2>Usage Example: Field Mapping Rules</h2>
 * <pre>{@code
 * // Define DTO with field mapping rules
 * class GenericDto {
 *     @FieldMappingRule(sourceFieldAddress = "uuid")
 *     protected String uuid;
 *
 *     @FieldMappingRule(sourceFieldAddress = "id")
 *     protected String id;
 * }
 *
 * // Parse mapping rules from annotations
 * List<MappingRule> rules = MappingRules.parse(GenericDto.class);
 * // rules.size() == 2
 * }</pre>
 *
 * <h2>Usage Example: Custom Transformations</h2>
 * <pre>{@code
 * // Define DTO with custom conversion methods
 * class OtherGenericDto extends GenericDto {
 *     @FieldMappingRule(sourceFieldAddress = "longField", fromSourceMethod = "fromMethod", toSourceMethod = "toMethod")
 *     String longField;
 *
 *     private String fromMethod(long longField) {
 *         return String.valueOf(longField);
 *     }
 *
 *     private long toMethod(String value) {
 *         return Long.valueOf(value);
 *     }
 * }
 *
 * // Mapper automatically uses conversion methods
 * Mapper mapper = new Mapper();
 * OtherGenericDto dto = mapper.map(entity, OtherGenericDto.class);
 * }</pre>
 *
 * <h2>Usage Example: Collection Mapping</h2>
 * <pre>{@code
 * // Define collection mapping
 * class SourceList {
 *     public int sourceField;
 * }
 *
 * class Source {
 *     public List<SourceList> sourceList = new ArrayList<>();
 * }
 *
 * class DestList {
 *     @FieldMappingRule(sourceFieldAddress = "sourceField")
 *     public int destField;
 * }
 *
 * class Dest {
 *     @FieldMappingRule(sourceFieldAddress = "sourceList")
 *     public List<DestList> destList = new ArrayList<>();
 * }
 *
 * // Map collections
 * Source source = new Source();
 * for(int i = 0; i < 10; i++)
 *     source.sourceList.add(new SourceList(i));
 *
 * Mapper mapper = new Mapper().configure(MapperConfigurationItem.FAIL_ON_ERROR, true);
 * Dest dest = mapper.map(source, Dest.class);
 * // dest.destList.size() == 10
 * }</pre>
 *
 * <h2>Usage Example: Object-Level Mapping</h2>
 * <pre>{@code
 * // Configure object-level mapping with custom methods
 * @ObjectMappingRule(fromSourceMethod = "fromMethod", toSourceMethod = "toMethod")
 * class GenericDtoWithObjectMapping extends GenericDto {
 *     @FieldMappingRule(sourceFieldAddress = "longField", fromSourceMethod = "fromMethod", toSourceMethod = "toMethod")
 *     String longField;
 *
 *     private void fromMethod(GenericEntityWithObjectMapping entity) {
 *         this.id = entity.getId();
 *         this.uuid = entity.getUuid();
 *         this.longField = String.valueOf(entity.getLongField());
 *     }
 *
 *     private void toMethod(GenericEntityWithObjectMapping entity) {
 *         // Reverse mapping logic
 *     }
 * }
 *
 * Mapper mapper = new Mapper();
 * GenericDtoWithObjectMapping dto = mapper.map(entity, GenericDtoWithObjectMapping.class);
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Automatic field-to-field mapping</li>
 *   <li>Field name transformation</li>
 *   <li>Custom type converters</li>
 *   <li>Nested object mapping</li>
 *   <li>Collection element mapping</li>
 *   <li>Conditional mapping rules</li>
 *   <li>Bidirectional mapping support</li>
 *   <li>Default value handling</li>
 *   <li>Null safety</li>
 *   <li>Deep vs shallow copy control</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.mapper.rules} - Mapping rule executors</li>
 *   <li>{@link com.garganttua.core.mapper.annotations} - Mapping annotations (commons)</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.mapper.annotations
 * @see com.garganttua.core.mapper.rules
 */
package com.garganttua.core.mapper;
