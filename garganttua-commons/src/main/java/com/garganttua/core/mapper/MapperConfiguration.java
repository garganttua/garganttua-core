package com.garganttua.core.mapper;

import java.util.HashMap;
import java.util.Map;

public class MapperConfiguration {

	private Map<MapperConfigurationItem, Object> configurations = new HashMap<>();

	public MapperConfiguration() {
		this.configurations.put(MapperConfigurationItem.FAIL_ON_ERROR, true);
		this.configurations.put(MapperConfigurationItem.DO_VALIDATION, true);
	}

	public MapperConfiguration(Class<?> fromClass, Class<?> toClass) {
		// TODO Auto-generated constructor stub
	}

	public void configure(MapperConfigurationItem element, Object value) {
		this.configurations.put(element, value);
	}

	public Object getConfiguration(MapperConfigurationItem element) {
		return this.configurations.get(element);
	}

	public boolean doValidation() {
		return (boolean) this.configurations.get(MapperConfigurationItem.DO_VALIDATION);
	}

	public boolean failOnError() {
		return (boolean) this.configurations.get(MapperConfigurationItem.FAIL_ON_ERROR);
	}

}
