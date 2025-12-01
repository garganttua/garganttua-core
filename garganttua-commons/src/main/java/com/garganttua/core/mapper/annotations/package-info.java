/**
 * Object mapping annotations for declarative field and object mapping rules.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides annotations for defining declarative object mapping rules.
 * These annotations enable automatic mapping between different object types with
 * customizable field-to-field transformations and mapping strategies.
 * </p>
 *
 * <h2>Core Annotations</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.mapper.annotations.ObjectMappingRule} - Defines object-level mapping rules</li>
 *   <li>{@link com.garganttua.core.mapper.annotations.FieldMappingRule} - Defines field-level mapping rules</li>
 * </ul>
 *
 * <h2>Usage Example: Simple Mapping</h2>
 * <pre>{@code
 * @ObjectMappingRule(source = UserDTO.class, target = User.class)
 * public class UserMapper {
 *
 *     @FieldMappingRule(source = "firstName", target = "givenName")
 *     @FieldMappingRule(source = "lastName", target = "familyName")
 *     public void configure() {
 *         // Configuration marker method
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example: Custom Transformations</h2>
 * <pre>{@code
 * @ObjectMappingRule(source = OrderDTO.class, target = Order.class)
 * public class OrderMapper {
 *
 *     @FieldMappingRule(
 *         source = "orderDate",
 *         target = "createdAt",
 *         transformer = DateToTimestampTransformer.class
 *     )
 *     @FieldMappingRule(
 *         source = "totalAmount",
 *         target = "total",
 *         transformer = CurrencyConverter.class
 *     )
 *     public void configure() {
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example: Nested Mappings</h2>
 * <pre>{@code
 * @ObjectMappingRule(source = CustomerDTO.class, target = Customer.class)
 * public class CustomerMapper {
 *
 *     @FieldMappingRule(
 *         source = "billingAddress",
 *         target = "address",
 *         nestedMapper = AddressMapper.class
 *     )
 *     @FieldMappingRule(
 *         source = "contactInfo.email",
 *         target = "email"
 *     )
 *     @FieldMappingRule(
 *         source = "contactInfo.phone",
 *         target = "phoneNumber"
 *     )
 *     public void configure() {
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example: Collection Mappings</h2>
 * <pre>{@code
 * @ObjectMappingRule(source = OrderDTO.class, target = Order.class)
 * public class OrderMapper {
 *
 *     @FieldMappingRule(
 *         source = "items",
 *         target = "orderItems",
 *         elementMapper = OrderItemMapper.class
 *     )
 *     public void configure() {
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example: Conditional Mappings</h2>
 * <pre>{@code
 * @ObjectMappingRule(source = ProductDTO.class, target = Product.class)
 * public class ProductMapper {
 *
 *     @FieldMappingRule(
 *         source = "discountPrice",
 *         target = "price",
 *         condition = "source.onSale == true"
 *     )
 *     @FieldMappingRule(
 *         source = "regularPrice",
 *         target = "price",
 *         condition = "source.onSale == false"
 *     )
 *     public void configure() {
 *     }
 * }
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Declarative mapping configuration</li>
 *   <li>Field name transformation</li>
 *   <li>Custom type converters</li>
 *   <li>Nested object mapping</li>
 *   <li>Collection element mapping</li>
 *   <li>Conditional mapping rules</li>
 *   <li>Bidirectional mapping support</li>
 *   <li>Default value handling</li>
 *   <li>Null safety</li>
 *   <li>Deep vs shallow copy control</li>
 * </ul>
 *
 * <h2>Mapping Strategies</h2>
 * <ul>
 *   <li><b>Direct Mapping</b> - Field-to-field copy</li>
 *   <li><b>Transformed Mapping</b> - Apply transformation function</li>
 *   <li><b>Nested Mapping</b> - Map nested objects recursively</li>
 *   <li><b>Collection Mapping</b> - Map collection elements</li>
 *   <li><b>Conditional Mapping</b> - Map based on conditions</li>
 *   <li><b>Computed Mapping</b> - Calculate target value</li>
 * </ul>
 *
 * <h2>Integration</h2>
 * <p>
 * These annotations work with:
 * </p>
 * <ul>
 *   <li>Object mapper framework for automatic mapping</li>
 *   <li>DTO to entity conversion</li>
 *   <li>API request/response transformations</li>
 *   <li>Data layer abstraction</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.mapper
 * @see com.garganttua.core.mapper.annotations.ObjectMappingRule
 * @see com.garganttua.core.mapper.annotations.FieldMappingRule
 */
package com.garganttua.core.mapper.annotations;
