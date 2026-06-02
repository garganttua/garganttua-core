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

	/**
	 * Creates a configuration pre-populated with the framework defaults
	 * ({@code FAIL_ON_ERROR}, {@code DO_VALIDATION}, {@code FAIL_ON_CYCLE} and
	 * {@code AUTO_CONVENTION_MAPPING} enabled, {@code STRICT_MODE} disabled).
	 */
	public MapperConfiguration() {
		this.configurations.put(MapperConfigurationItem.FAIL_ON_ERROR, true);
		this.configurations.put(MapperConfigurationItem.DO_VALIDATION, true);
		this.configurations.put(MapperConfigurationItem.FAIL_ON_CYCLE, true);
		this.configurations.put(MapperConfigurationItem.AUTO_CONVENTION_MAPPING, true);
		this.configurations.put(MapperConfigurationItem.STRICT_MODE, false);
	}

	/**
	 * Sets the value of a single configuration item, overwriting any previous value.
	 *
	 * @param element the configuration item to set
	 * @param value   the value to associate (expected type depends on the item)
	 */
	public void configure(MapperConfigurationItem element, Object value) {
		this.configurations.put(element, value);
	}

	/**
	 * Returns the raw value currently associated with the given configuration item.
	 *
	 * @param element the configuration item to read
	 * @return the current value, or {@code null} if unset
	 */
	public Object getConfiguration(MapperConfigurationItem element) {
		return this.configurations.get(element);
	}

	/**
	 * @return whether validation is performed during mapping
	 *         ({@link MapperConfigurationItem#DO_VALIDATION})
	 */
	public boolean doValidation() {
		return (boolean) this.configurations.get(MapperConfigurationItem.DO_VALIDATION);
	}

	/**
	 * @return whether mapping errors raise a {@link MapperException}
	 *         ({@link MapperConfigurationItem#FAIL_ON_ERROR})
	 */
	public boolean failOnError() {
		return (boolean) this.configurations.get(MapperConfigurationItem.FAIL_ON_ERROR);
	}

	/**
	 * @return whether object reference cycles raise a {@link MapperException}
	 *         ({@link MapperConfigurationItem#FAIL_ON_CYCLE})
	 */
	public boolean failOnCycle() {
		return (boolean) this.configurations.get(MapperConfigurationItem.FAIL_ON_CYCLE);
	}

	/**
	 * @return whether fields are auto-mapped by name convention when no rule exists
	 *         ({@link MapperConfigurationItem#AUTO_CONVENTION_MAPPING})
	 */
	public boolean autoConventionMapping() {
		return (boolean) this.configurations.get(MapperConfigurationItem.AUTO_CONVENTION_MAPPING);
	}

	/**
	 * @return whether strict mode is enabled, requiring every destination field to be
	 *         covered by a rule ({@link MapperConfigurationItem#STRICT_MODE})
	 */
	public boolean strictMode() {
		return (boolean) this.configurations.get(MapperConfigurationItem.STRICT_MODE);
	}

}
