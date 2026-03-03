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

	IMappingRuleBuilder to(String destinationFieldAddress);

	IMappingRuleBuilder withFromSourceMethod(String methodAddress);

	IMappingRuleBuilder withToSourceMethod(String methodAddress);
}
