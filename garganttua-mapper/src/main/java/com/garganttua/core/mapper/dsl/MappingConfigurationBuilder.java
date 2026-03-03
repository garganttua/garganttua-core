package com.garganttua.core.mapper.dsl;

import java.util.ArrayList;
import java.util.List;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.mapper.MappingConfiguration;
import com.garganttua.core.mapper.MappingDirection;
import com.garganttua.core.mapper.MappingRule;
import com.garganttua.core.reflection.IClass;

/**
 * Fluent builder for creating {@link MappingConfiguration} programmatically.
 *
 * <h2>Usage:</h2>
 * <pre>
 * MappingConfiguration config = MappingConfigurationBuilder.create()
 *     .from(sourceClass).to(destClass)
 *     .field("name").to("fullName").up()
 *     .field("age").to("years").withFromSourceMethod("Converter.ageToYears").up()
 *     .build();
 * </pre>
 */
public class MappingConfigurationBuilder implements IMappingConfigurationBuilder {

	private IClass<?> source;
	private IClass<?> destination;
	private MappingDirection direction = MappingDirection.REGULAR;
	private final List<MappingRuleBuilder> ruleBuilders = new ArrayList<>();

	private MappingConfigurationBuilder() {
	}

	public static IMappingConfigurationBuilder create() {
		return new MappingConfigurationBuilder();
	}

	@Override
	public IMappingConfigurationBuilder from(IClass<?> source) {
		this.source = source;
		return this;
	}

	@Override
	public IMappingConfigurationBuilder to(IClass<?> destination) {
		this.destination = destination;
		return this;
	}

	@Override
	public IMappingRuleBuilder field(String sourceFieldAddress) {
		MappingRuleBuilder builder = new MappingRuleBuilder(this, sourceFieldAddress, this.destination);
		this.ruleBuilders.add(builder);
		return builder;
	}

	@Override
	public IMappingConfigurationBuilder direction(MappingDirection direction) {
		this.direction = direction;
		return this;
	}

	@Override
	public MappingConfiguration build() throws DslException {
		if (this.source == null || this.destination == null) {
			throw new DslException("Source and destination classes are required");
		}

		List<MappingRule> rules = new ArrayList<>();
		for (MappingRuleBuilder rb : this.ruleBuilders) {
			rules.add(rb.build());
		}

		List<MappingRule> destinationRules;
		List<MappingRule> sourceRules;

		if (this.direction == MappingDirection.REGULAR) {
			destinationRules = rules;
			sourceRules = List.of();
		} else {
			destinationRules = List.of();
			sourceRules = rules;
		}

		return new MappingConfiguration(this.source, this.destination, sourceRules, destinationRules, this.direction);
	}
}
