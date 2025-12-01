/**
 * Object mapping framework implementation for DTO/entity transformations.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the concrete implementation of the object mapping framework.
 * It enables automatic mapping between different object types with support for
 * field-to-field copying, custom transformations, collection mapping, and nested
 * object mapping.
 * </p>
 *
 * <h2>Main Implementation Classes</h2>
 * <ul>
 *   <li>{@code Mapper} - Main object mapper implementation</li>
 *   <li>{@code MappingRules} - Mapping rule configuration and management</li>
 * </ul>
 *
 * <h2>Usage Example: Simple Mapping</h2>
 * <pre>{@code
 * // Define source and target
 * UserDTO source = new UserDTO();
 * source.setFirstName("John");
 * source.setLastName("Doe");
 * source.setEmail("john.doe@example.com");
 *
 * User target = new User();
 *
 * // Perform mapping
 * Mapper mapper = new Mapper();
 * mapper.map(source, target);
 *
 * // Or create new target instance
 * User user = mapper.map(source, User.class);
 * }</pre>
 *
 * <h2>Usage Example: Mapping Rules</h2>
 * <pre>{@code
 * // Configure mapping rules
 * MappingRules rules = new MappingRules()
 *     .rule(UserDTO.class, User.class)
 *         .field("firstName", "givenName")
 *         .field("lastName", "familyName")
 *         .field("email", "emailAddress")
 *         .done();
 *
 * // Create mapper with rules
 * Mapper mapper = new Mapper(rules);
 * User user = mapper.map(userDTO, User.class);
 * }</pre>
 *
 * <h2>Usage Example: Custom Transformations</h2>
 * <pre>{@code
 * // Define transformation
 * MappingRules rules = new MappingRules()
 *     .rule(OrderDTO.class, Order.class)
 *         .field("orderDate", "createdAt")
 *             .transform(date -> Instant.ofEpochMilli(date.getTime()))
 *             .done()
 *         .field("totalAmount", "total")
 *             .transform(amount -> new BigDecimal(amount).setScale(2))
 *             .done()
 *         .done();
 *
 * Mapper mapper = new Mapper(rules);
 * Order order = mapper.map(orderDTO, Order.class);
 * }</pre>
 *
 * <h2>Usage Example: Collection Mapping</h2>
 * <pre>{@code
 * // Map collections
 * List<UserDTO> userDTOs = Arrays.asList(dto1, dto2, dto3);
 *
 * Mapper mapper = new Mapper();
 * List<User> users = mapper.mapCollection(userDTOs, User.class);
 * }</pre>
 *
 * <h2>Usage Example: Nested Object Mapping</h2>
 * <pre>{@code
 * // Configure nested mapping
 * MappingRules rules = new MappingRules()
 *     .rule(CustomerDTO.class, Customer.class)
 *         .field("billingAddress", "address")
 *             .nested(AddressDTO.class, Address.class)
 *             .done()
 *         .done();
 *
 * Mapper mapper = new Mapper(rules);
 * Customer customer = mapper.map(customerDTO, Customer.class);
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Automatic field-to-field mapping</li>
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
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.mapper.rules} - Mapping rule executors</li>
 *   <li>{@link com.garganttua.core.mapper.annotations} - Mapping annotations (commons)</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.mapper.annotations
 * @see com.garganttua.core.mapper.rules
 */
package com.garganttua.core.mapper;
