/**
 * Mapping rule parsing, validation, and execution.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the core functionality for parsing mapping annotations,
 * validating mapping rules, and executing field transformations during the mapping process.
 * </p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@code MappingRules} - Parses and validates mapping rules from annotations</li>
 *   <li>{@code MappingRule} - Represents a single field mapping rule with source/destination addresses</li>
 * </ul>
 *
 * <h2>Mapping Rule Parsing</h2>
 * <p>
 * The MappingRules class parses {@code @FieldMappingRule} and {@code @ObjectMappingRule}
 * annotations from destination classes to create a list of mapping rules:
 * </p>
 * <pre>{@code
 * class GenericDto {
 *     @FieldMappingRule(sourceFieldAddress = "uuid")
 *     protected String uuid;
 *
 *     @FieldMappingRule(sourceFieldAddress = "id")
 *     protected String id;
 * }
 *
 * List<MappingRule> rules = MappingRules.parse(GenericDto.class);
 * // rules.size() == 2
 * }</pre>
 *
 * <h2>Mapping Rule Validation</h2>
 * <p>
 * The validation process ensures that:
 * </p>
 * <ul>
 *   <li>Source fields exist in the source class</li>
 *   <li>Conversion method signatures match field types</li>
 *   <li>Required methods are present and accessible</li>
 * </ul>
 * <pre>{@code
 * List<MappingRule> rules = MappingRules.parse(CorrectDestination.class);
 * MappingRules.validate(Source.class, rules);  // Throws MapperException if invalid
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.mapper
 * @see com.garganttua.core.mapper.annotations.FieldMappingRule
 * @see com.garganttua.core.mapper.annotations.ObjectMappingRule
 */
package com.garganttua.core.mapper.rules;
