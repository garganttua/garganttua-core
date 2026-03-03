package com.garganttua.core.mapper.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.mapper.MappingRule;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.ObjectAddress;

public class MappingRuleBuilder implements IMappingRuleBuilder {

	private final IMappingConfigurationBuilder parent;
	private final String sourceFieldAddress;
	private final IClass<?> destinationClass;
	private String destinationFieldAddress;
	private String fromSourceMethod;
	private String toSourceMethod;

	MappingRuleBuilder(IMappingConfigurationBuilder parent, String sourceFieldAddress, IClass<?> destinationClass) {
		this.parent = parent;
		this.sourceFieldAddress = sourceFieldAddress;
		this.destinationClass = destinationClass;
	}

	@Override
	public IMappingRuleBuilder to(String destinationFieldAddress) {
		this.destinationFieldAddress = destinationFieldAddress;
		return this;
	}

	@Override
	public IMappingRuleBuilder withFromSourceMethod(String methodAddress) {
		this.fromSourceMethod = methodAddress;
		return this;
	}

	@Override
	public IMappingRuleBuilder withToSourceMethod(String methodAddress) {
		this.toSourceMethod = methodAddress;
		return this;
	}

	@Override
	public IMappingConfigurationBuilder up() {
		return this.parent;
	}

	@Override
	public void setUp(IMappingConfigurationBuilder up) {
		// not used — parent set in constructor
	}

	@Override
	public MappingRule build() throws DslException {
		if (this.sourceFieldAddress == null || this.destinationFieldAddress == null) {
			throw new DslException("Both source and destination field addresses are required");
		}
		ObjectAddress srcAddr = new ObjectAddress(this.sourceFieldAddress);
		ObjectAddress destAddr = new ObjectAddress(this.destinationFieldAddress);
		ObjectAddress fromMethod = this.fromSourceMethod != null ? new ObjectAddress(this.fromSourceMethod) : null;
		ObjectAddress toMethod = this.toSourceMethod != null ? new ObjectAddress(this.toSourceMethod) : null;
		return new MappingRule(srcAddr, destAddr, this.destinationClass, fromMethod, toMethod);
	}
}
