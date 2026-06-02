package com.garganttua.core.mapper.dsl;

import com.garganttua.core.dsl.ILinkedBuilder;
import com.garganttua.core.mapper.MappingRule;

/**
 * Builder for creating individual {@link MappingRule} instances within
 * a {@link IMappingConfigurationBuilder}.
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * builder.field("name").to("fullName").up()
 *        .field("age").to("years").withFromSourceMethod("Converter.ageToYears").up()
 * </pre>
 *
 * @since 2.0.0-ALPHA01
 */
public interface IMappingRuleBuilder extends ILinkedBuilder<IMappingConfigurationBuilder, MappingRule> {

	/**
	 * Sets the destination field this rule writes to.
	 *
	 * @param destinationFieldAddress the address of the destination field
	 * @return this builder for chaining
	 */
	IMappingRuleBuilder to(String destinationFieldAddress);

	/**
	 * Sets the address of the method used to transform the value from source to
	 * destination format.
	 *
	 * @param methodAddress the transformation method address
	 * @return this builder for chaining
	 */
	IMappingRuleBuilder withFromSourceMethod(String methodAddress);

	/**
	 * Sets the address of the method used to transform the value from destination back
	 * to source format (reverse mapping).
	 *
	 * @param methodAddress the reverse transformation method address
	 * @return this builder for chaining
	 */
	IMappingRuleBuilder withToSourceMethod(String methodAddress);
}
