package com.garganttua.objects.mapper;

import java.util.HashMap;
import java.util.Map;

public class GGMapperConfiguration {
	
	private Map<GGMapperConfigurationItem, Object> configurations = new HashMap<GGMapperConfigurationItem, Object>();
	
	public GGMapperConfiguration() {
		this.configurations.put(GGMapperConfigurationItem.FAIL_ON_ERROR, true);
		this.configurations.put(GGMapperConfigurationItem.DO_VALIDATION, true);
	}

	public void configure(GGMapperConfigurationItem element, Object value) {
		this.configurations.put(element, value);
	}
	
	public Object getConfiguration(GGMapperConfigurationItem element) {
		return this.configurations.get(element);
	}

	public boolean doValidation() {
		return (boolean) this.configurations.get(GGMapperConfigurationItem.DO_VALIDATION);
	}

	public boolean failOnError() {
		return (boolean) this.configurations.get(GGMapperConfigurationItem.FAIL_ON_ERROR);
	}

}
