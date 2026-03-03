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

	IMappingConfigurationBuilder from(IClass<?> source);

	IMappingConfigurationBuilder to(IClass<?> destination);

	IMappingRuleBuilder field(String sourceFieldAddress);

	IMappingConfigurationBuilder direction(MappingDirection direction);
}
