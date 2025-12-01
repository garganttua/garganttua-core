/**
 * Mapping rule executors and field transformation strategies.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides executor implementations for different mapping rule types.
 * Executors handle the actual field-to-field copying, transformation, and type
 * conversion during the mapping process.
 * </p>
 *
 * <h2>Executor Classes</h2>
 * <ul>
 *   <li>{@code SimpleFieldMappingExecutor} - Executes simple field-to-field mapping</li>
 *   <li>{@code SimpleMapableFieldMappingExecutor} - Executes mapping with transformations</li>
 *   <li>{@code MethodMappingExecutor} - Executes mapping via getter/setter methods</li>
 *   <li>{@code SimpleCollectionMappingExecutor} - Executes collection element mapping</li>
 *   <li>{@code MapableCollectionMappingExecutor} - Executes collection mapping with transformations</li>
 * </ul>
 *
 * <h2>Mapping Strategies</h2>
 * <ul>
 *   <li><b>Direct Mapping</b> - Copy value directly from source to target field</li>
 *   <li><b>Transformed Mapping</b> - Apply transformation function before copying</li>
 *   <li><b>Method Mapping</b> - Use getter/setter methods instead of direct field access</li>
 *   <li><b>Collection Mapping</b> - Map collection elements individually</li>
 *   <li><b>Nested Mapping</b> - Recursively map nested objects</li>
 * </ul>
 *
 * <h2>Execution Process</h2>
 * <ol>
 *   <li>Extract value from source field</li>
 *   <li>Apply transformation if configured</li>
 *   <li>Perform type conversion if needed</li>
 *   <li>Set value to target field</li>
 *   <li>Handle null values according to rules</li>
 * </ol>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.mapper
 */
package com.garganttua.core.mapper.rules;
