package com.garganttua.core.mapper.dsl;

import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.mapper.MappingConfiguration;
import com.garganttua.core.mapper.MappingDirection;
import com.garganttua.core.reflection.IClass;

/**
 * Builder for creating {@link MappingConfiguration} instances programmatically.
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * MappingConfiguration config = MappingConfigurationBuilder.create()
 *     .from(sourceClass).to(destClass)
 *     .field("name").to("fullName").up()
 *     .field("age").to("years").up()
 *     .direction(MappingDirection.REGULAR)
 *     .build();
 * </pre>
 *
 * @since 2.0.0-ALPHA01
 */
public interface IMappingConfigurationBuilder extends IBuilder<MappingConfiguration> {

	/**
	 * Sets the source class to map from.
	 *
	 * @param source the source class
	 * @return this builder for chaining
	 */
	IMappingConfigurationBuilder from(IClass<?> source);

	/**
	 * Sets the destination class to map into.
	 *
	 * @param destination the destination class
	 * @return this builder for chaining
	 */
	IMappingConfigurationBuilder to(IClass<?> destination);

	/**
	 * Starts a new mapping rule for the given source field, returning a nested rule
	 * builder; call {@link IMappingRuleBuilder#up()} to return to this builder.
	 *
	 * @param sourceFieldAddress the address of the source field to map from
	 * @return the nested rule builder
	 */
	IMappingRuleBuilder field(String sourceFieldAddress);

	/**
	 * Sets the mapping direction.
	 *
	 * @param direction the mapping direction
	 * @return this builder for chaining
	 */
	IMappingConfigurationBuilder direction(MappingDirection direction);
}
