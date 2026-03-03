package com.garganttua.core.mapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages global configuration settings for the mapper.
 * Thread-safe via ConcurrentHashMap.
 *
 * @since 2.0.0-ALPHA01
 */
public class MapperConfiguration {

	private final Map<MapperConfigurationItem, Object> configurations = new ConcurrentHashMap<>();

	public MapperConfiguration() {
		this.configurations.put(MapperConfigurationItem.FAIL_ON_ERROR, true);
		this.configurations.put(MapperConfigurationItem.DO_VALIDATION, true);
		this.configurations.put(MapperConfigurationItem.FAIL_ON_CYCLE, true);
		this.configurations.put(MapperConfigurationItem.AUTO_CONVENTION_MAPPING, true);
		this.configurations.put(MapperConfigurationItem.STRICT_MODE, false);
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

	public boolean failOnCycle() {
		return (boolean) this.configurations.get(MapperConfigurationItem.FAIL_ON_CYCLE);
	}

	public boolean autoConventionMapping() {
		return (boolean) this.configurations.get(MapperConfigurationItem.AUTO_CONVENTION_MAPPING);
	}

	public boolean strictMode() {
		return (boolean) this.configurations.get(MapperConfigurationItem.STRICT_MODE);
	}

}
