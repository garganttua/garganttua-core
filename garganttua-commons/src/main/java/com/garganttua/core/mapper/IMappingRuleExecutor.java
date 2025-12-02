package com.garganttua.core.mapper;

/**
 * Executes custom mapping logic for specific field mappings.
 * <p>
 * This functional interface allows developers to define custom transformation
 * logic when mapping between source and destination objects. It is particularly
 * useful for complex transformations that cannot be handled by simple field-to-field
 * copying, such as data format conversions, aggregations, or calculations.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * // Create a custom mapping rule executor for date formatting
 * IMappingRuleExecutor dateFormatter = (destClass, destObj, sourceObj) -&gt; {
 *     if (sourceObj instanceof Date) {
 *         SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
 *         return formatter.format((Date) sourceObj);
 *     }
 *     return destObj;
 * };
 *
 * // Create an executor for currency conversion
 * IMappingRuleExecutor currencyConverter = (destClass, destObj, sourceObj) -&gt; {
 *     if (sourceObj instanceof BigDecimal) {
 *         BigDecimal amount = (BigDecimal) sourceObj;
 *         return amount.multiply(new BigDecimal("1.15")); // USD to EUR
 *     }
 *     return destObj;
 * };
 *
 * // Create an executor for complex object transformation
 * IMappingRuleExecutor addressAggregator = (destClass, destObj, sourceObj) -&gt; {
 *     Address address = (Address) sourceObj;
 *     return address.getStreet() + ", " + address.getCity() + ", " + address.getZip();
 * };
 * </pre>
 *
 * @since 2.0.0-ALPHA01
 */
@FunctionalInterface
public interface IMappingRuleExecutor {

	/**
	 * Executes the custom mapping logic.
	 * <p>
	 * Transforms the source object according to custom rules and returns the result.
	 * The method receives both the destination class for type information and the
	 * current destination object (which may be null if creating a new instance).
	 * </p>
	 *
	 * @param <destination> the type of the destination object
	 * @param destinationClass the class of the destination object
	 * @param destinationObject the current destination object, may be null
	 * @param sourceObject the source object to transform
	 * @return the transformed destination object
	 * @throws MapperException if the mapping transformation fails
	 */
	<destination> destination doMapping(Class<destination> destinationClass, destination destinationObject,
			Object sourceObject) throws MapperException;

}
