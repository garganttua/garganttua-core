package com.garganttua.tooling.objects.mapper;

import java.util.HashMap;
import java.util.Map;

public class GGAPIMapperConfiguration {
	
	private Map<GGAPIMapperConfigurationItem, Object> configurations = new HashMap<GGAPIMapperConfigurationItem, Object>();
	
	public GGAPIMapperConfiguration() {
		this.configurations.put(GGAPIMapperConfigurationItem.FAIL_ON_ERROR, true);
		this.configurations.put(GGAPIMapperConfigurationItem.DO_VALIDATION, true);
	}

	public void configure(GGAPIMapperConfigurationItem element, Object value) {
		this.configurations.put(element, value);
	}
	
	public Object getConfiguration(GGAPIMapperConfigurationItem element) {
		return this.configurations.get(element);
	}

	public boolean doValidation() {
		return (boolean) this.configurations.get(GGAPIMapperConfigurationItem.DO_VALIDATION);
	}

	public boolean failOnError() {
		return (boolean) this.configurations.get(GGAPIMapperConfigurationItem.FAIL_ON_ERROR);
	}

}
