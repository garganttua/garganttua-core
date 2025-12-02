/**
 * Declarative object-to-object mapping engine with annotation-based rules.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides a flexible object mapping framework for converting between different
 * object types. It supports annotation-based mapping rules, field-level transformations,
 * and bidirectional mapping between DTOs and domain entities.
 * </p>
 *
 * <h2>Core Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.mapper.IMapper} - Main mapper interface</li>
 *   <li><b>IMappingRule</b> - Mapping rule definition (provided by implementations)</li>
 *   <li><b>IFieldMapping</b> - Field-to-field mapping (provided by implementations)</li>
 * </ul>
 *
 * <h2>Annotation-Based Mapping</h2>
 * <pre>{@code
 * @ObjectMappingRule(source = UserDTO.class, target = User.class)
 * public class UserMapper {
 *
 *     // Simple field mapping with transformation
 *     @FieldMappingRule(sourceField = "fullName", targetField = "name")
 *     public void mapName(UserDTO dto, User user) {
 *         user.setName(dto.getFullName().toUpperCase());
 *     }
 *
 *     // Derived field mapping
 *     @FieldMappingRule(sourceField = "birthDate", targetField = "age")
 *     public void calculateAge(UserDTO dto, User user) {
 *         LocalDate birthDate = dto.getBirthDate();
 *         int age = Period.between(birthDate, LocalDate.now()).getYears();
 *         user.setAge(age);
 *     }
 *
 *     // Complex object mapping
 *     @FieldMappingRule(sourceField = "addressDTO", targetField = "address")
 *     public void mapAddress(UserDTO dto, User user) {
 *         AddressDTO addressDTO = dto.getAddressDTO();
 *         Address address = new Address(
 *             addressDTO.getStreet(),
 *             addressDTO.getCity(),
 *             addressDTO.getZipCode()
 *         );
 *         user.setAddress(address);
 *     }
 * }
 *
 * // Use mapper
 * IMapper<UserDTO, User> mapper = new Mapper<>(UserMapper.class);
 * UserDTO dto = new UserDTO("John Doe", LocalDate.of(1990, 1, 1));
 * User user = mapper.map(dto);
 * }</pre>
 *
 * <h2>Bidirectional Mapping</h2>
 * <pre>{@code
 * @ObjectMappingRule(source = UserDTO.class, target = User.class)
 * @BidirectionalMapping
 * public class BidirectionalUserMapper {
 *
 *     @FieldMappingRule(sourceField = "fullName", targetField = "name")
 *     @ReverseMapping(sourceField = "name", targetField = "fullName")
 *     public void mapName(UserDTO dto, User user) {
 *         user.setName(dto.getFullName().toUpperCase());
 *     }
 *
 *     public void mapNameReverse(User user, UserDTO dto) {
 *         dto.setFullName(user.getName().toLowerCase());
 *     }
 * }
 *
 * // Forward mapping: DTO -> Entity
 * User user = mapper.map(dto);
 *
 * // Reverse mapping: Entity -> DTO
 * UserDTO dto = mapper.mapReverse(user);
 * }</pre>
 *
 * <h2>Programmatic Mapping</h2>
 * <pre>{@code
 * IMapper<OrderDTO, Order> mapper = new MapperBuilder<>(OrderDTO.class, Order.class)
 *     .mapField("orderId", "id")
 *     .mapField("customerName", "customer.name")
 *     .mapField("total", "amount", (dto, order) -> {
 *         order.setAmount(dto.getTotal().multiply(new BigDecimal("1.10"))); // Add 10% tax
 *     })
 *     .build();
 *
 * Order order = mapper.map(orderDTO);
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Annotation-based mapping rules</li>
 *   <li>Field-level transformation support</li>
 *   <li>Bidirectional mapping</li>
 *   <li>Nested object mapping</li>
 *   <li>Custom mapping logic</li>
 *   <li>Collection mapping</li>
 *   <li>Null-safe operations</li>
 * </ul>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li><b>DTO to Entity</b> - Convert API DTOs to domain entities</li>
 *   <li><b>Entity to DTO</b> - Serialize entities for API responses</li>
 *   <li><b>Data transformation</b> - Format conversion, validation</li>
 *   <li><b>Legacy integration</b> - Adapt between different data models</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.mapper.annotations} - Mapping annotations</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.mapper.IMapper
 * @see com.garganttua.core.mapper.annotations
 */
package com.garganttua.core.mapper;
