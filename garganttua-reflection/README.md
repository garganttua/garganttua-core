# Garganttua Reflection

## Description

Garganttua Reflection is a **powerful Java reflection utility library** that simplifies working with complex object graphs through an intuitive, address-based API. It provides a high-level abstraction over Java's reflection API, enabling type-safe field access, method invocation, and deep object traversal without boilerplate code.

Traditional Java reflection is verbose, error-prone, and difficult to work with for nested objects and collections. Garganttua Reflection solves these problems by introducing **ObjectAddress** - a path expression system that allows you to navigate object hierarchies using simple dot notation, similar to property paths in JavaScript or XPath.

**Key Features:**
- **ObjectAddress System** - Navigate object graphs using intuitive path expressions (`user.profile.email`)
- **Deep Field Access** - Get/set values in deeply nested object structures with single method calls
- **Collection Traversal** - Automatic handling of Lists, Sets, Maps, and arrays in object paths
- **Method Invocation** - Invoke methods on nested objects using address notation
- **Type Safety** - Generic-friendly API with compile-time type checking where possible
- **Field Resolution** - Automatically locate fields across class hierarchies and nested structures
- **Binder Pattern** - Reactive field and method binding with supplier-based value management
- **Reflection Helpers** - Utility classes for field instantiation, generic type resolution, and access management
- **Performance Optimized** - Caching and efficient field/method lookup strategies

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-reflection</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `com.garganttua.core:garganttua-commons`
 - `com.garganttua.core:garganttua-dsl`
 - `com.garganttua.core:garganttua-supply`
 - `com.garganttua.core:garganttua-reflections:test`
 - `org.javatuples:javatuples`

<!-- AUTO-GENERATED-END -->

## Core Concepts

### ObjectAddress

The `ObjectAddress` class is the foundation of Garganttua Reflection. It represents a **path to a field or method** within an object graph using dot notation.

**Basic Syntax:**
```
simple_field             → Access direct field
parent.child             → Access nested field
users.#item.name         → Access field in collection elements
settings.#value.enabled  → Access field in map values
settings.#key            → Access map keys
```

**Features:**
- Dot-separated path elements
- Support for nested objects (unlimited depth)
- Collection iteration with `#item` operator
- Map navigation with `#key` and `#value` operators
- Immutable and cloneable
- Full integration with ObjectQuery

### ObjectQuery

`ObjectQuery` is the primary interface for querying and manipulating objects using reflection. It provides a fluent API for:
- **Finding fields/methods** by ObjectAddress
- **Getting/setting field values** at any depth
- **Invoking methods** on nested objects
- **Generating field structures** for complex initialization

**Creation:**
```java
// From class (creates new instance)
ObjectQuery query = ObjectQueryFactory.objectQuery(User.class);

// From existing object
ObjectQuery query = ObjectQueryFactory.objectQuery(userInstance);

// With specific class and instance
ObjectQuery query = ObjectQueryFactory.objectQuery(User.class, userInstance);
```

### ObjectAccessor

`ObjectAccessor` provides static utility methods for common reflection operations with functional programming support. It uses `ThrowingFunction` interfaces to enable declarative field access patterns.

**Use Cases:**
- Get/set values using functional configuration
- Invoke methods with annotation-driven addressing
- Generic utility for frameworks and libraries

### Field Utilities

**Fields** - Utility class for field-related operations:
- `getGenericType()` - Extract generic type information from fields
- `isNotPrimitive()` - Check if a field type is not a primitive or wrapper
- `isArrayOrMapOrCollectionField()` - Detect collection fields
- `instanciate()` - Create instances based on field type (including collections/maps)
- `prettyColored()` - ANSI-colored field representation for logging

**FieldResolver** - Advanced field resolution across complex object graphs:
- `fieldByFieldName()` - Locate field by name
- `fieldByField()` - Validate field matches address
- `fieldByAddress()` - Resolve field by ObjectAddress

### Method Utilities

**Methods** - Utility class for method-related operations:
- `prettyColored()` - ANSI-colored method signature representation

**MethodResolver** - Method resolution and validation utilities

### Binders

**Binder Pattern** - Reactive binding between object fields/methods and value suppliers:

- `FieldBinder` - Binds a field to a supplier, enabling reactive get/set operations
- `MethodBinder` - Binds a method invocation to suppliers for parameters
- `ConstructorBinder` - Binds constructor invocation to argument suppliers
- `ContextualBinders` - Context-aware binders for execution framework integration

**Benefits:**
- Deferred execution until values are needed
- Dynamic value resolution
- Clean separation of data source and destination

### Access Managers

Internal utilities for managing field, method, and constructor accessibility:
- `FieldAccessManager` - Makes fields accessible for reflection
- `MethodAccessManager` - Manages method accessibility
- `ConstructorAccessManager` - Handles constructor access

## Usage

### 1. Simple Field Access

Get and set field values in a straightforward object:

```java
class User {
    private String username;
    private String email;
    private int age;
}

User user = new User();

// Create query
ObjectQuery query = ObjectQueryFactory.objectQuery(user);

// Set values
query.setValue("username", "alice");
query.setValue("email", "alice@example.com");
query.setValue("age", 30);

// Get values
String username = query.getValue("username");  // "alice"
String email = query.getValue("email");         // "alice@example.com"
Integer age = query.getValue("age");             // 30
```

### 2. Nested Object Navigation

Access deeply nested fields using dot notation:

```java
class Address {
    private String city;
    private String zipCode;
}

class Profile {
    private Address address;
}

class User {
    private Profile profile;
}

User user = new User();
user.profile = new Profile();
user.profile.address = new Address();

ObjectQuery query = ObjectQueryFactory.objectQuery(user);

// Set nested values
query.setValue("profile.address.city", "Paris");
query.setValue("profile.address.zipCode", "75001");

// Get nested values
String city = query.getValue("profile.address.city");        // "Paris"
String zipCode = query.getValue("profile.address.zipCode");  // "75001"
```

### 3. Collection Element Access

Work with collections using the `#item` operator:

```java
class Order {
    private List<OrderItem> items = new ArrayList<>();
}

class OrderItem {
    private String productName;
    private int quantity;
}

Order order = new Order();
OrderItem item1 = new OrderItem();
item1.productName = "Widget";
item1.quantity = 5;
order.items.add(item1);

ObjectQuery query = ObjectQueryFactory.objectQuery(order);

// Access collection elements
List<String> productNames = query.getValue("items.#item.productName");
// Returns: ["Widget"]

List<Integer> quantities = query.getValue("items.#item.quantity");
// Returns: [5]
```

### 4. Map Value Access

Navigate map structures with `#key` and `#value` operators:

```java
class Configuration {
    private Map<String, Setting> settings = new HashMap<>();
}

class Setting {
    private boolean enabled;
    private String value;
}

Configuration config = new Configuration();
Setting setting1 = new Setting();
setting1.enabled = true;
setting1.value = "production";
config.settings.put("environment", setting1);

ObjectQuery query = ObjectQueryFactory.objectQuery(config);

// Access map values
List<Boolean> enabledFlags = query.getValue("settings.#value.enabled");
// Returns: [true]

List<String> values = query.getValue("settings.#value.value");
// Returns: ["production"]

// Access map keys
List<String> keys = query.getValue("settings.#key");
// Returns: ["environment"]
```

### 5. Method Invocation

Invoke methods on objects using ObjectAddress:

```java
class Calculator {
    public int add(int a, int b) {
        return a + b;
    }

    public String format(String prefix, int value) {
        return prefix + ": " + value;
    }
}

Calculator calc = new Calculator();
ObjectQuery query = ObjectQueryFactory.objectQuery(calc);

// Invoke methods
Integer sum = query.invoke("add", 10, 20);
// Returns: 30

String formatted = query.invoke("format", "Result", 42);
// Returns: "Result: 42"
```

### 6. Nested Method Invocation

Invoke methods on nested objects:

```java
class User {
    private Profile profile;
}

class Profile {
    private String name;

    public String getDisplayName() {
        return "User: " + name;
    }
}

User user = new User();
user.profile = new Profile();
user.profile.name = "Alice";

ObjectQuery query = ObjectQueryFactory.objectQuery(user);

// Invoke method on nested object
String displayName = query.invoke("profile.getDisplayName");
// Returns: "User: Alice"
```

### 7. ObjectAddress Creation and Resolution

Work with ObjectAddress objects directly:

```java
class Department {
    private List<Employee> employees;
}

class Employee {
    private String name;
    private Address address;
}

ObjectQuery query = ObjectQueryFactory.objectQuery(Department.class);

// Create ObjectAddress
ObjectAddress nameAddress = new ObjectAddress("employees.#item.name", true);
ObjectAddress cityAddress = new ObjectAddress("employees.#item.address.city", true);

// Find field structure
List<Object> nameStructure = query.find(nameAddress);
// Returns: [Field(employees), Field(name)]

List<Object> cityStructure = query.find(cityAddress);
// Returns: [Field(employees), Field(address), Field(city)]
```

### 8. Field Resolution

Resolve fields across complex hierarchies:

```java
class BaseEntity {
    private Long id;
    private LocalDateTime createdAt;
}

class User extends BaseEntity {
    private String username;
    private String email;
}

ObjectQuery query = ObjectQueryFactory.objectQuery(User.class);

// Resolve field by name (searches through inheritance hierarchy)
ObjectAddress idAddress = FieldResolver.fieldByFieldName("id", query, User.class);
// Successfully finds 'id' in BaseEntity

ObjectAddress usernameAddress = FieldResolver.fieldByFieldName("username", query, User.class);
// Finds 'username' in User

// Resolve with type validation
ObjectAddress emailAddress = FieldResolver.fieldByFieldName("email", query, User.class, String.class);
// Validates that 'email' is of type String
```

### 9. Field Instantiation

Automatically instantiate field values based on type:

```java
class Container {
    private List<String> items;
    private Map<String, Integer> counts;
    private int[] numbers;
}

Container container = new Container();

// Get field
Field itemsField = Container.class.getDeclaredField("items");
Field countsField = Container.class.getDeclaredField("counts");
Field numbersField = Container.class.getDeclaredField("numbers");

// Instantiate based on field type
Object itemsList = Fields.instanciate(itemsField);
// Returns: new ArrayList<String>()

Object countsMap = Fields.instanciate(countsField);
// Returns: new HashMap<String, Integer>()

Object numbersArray = Fields.instanciate(numbersField);
// Returns: new int[0]
```

### 10. Generic Type Extraction

Extract generic type information from fields:

```java
class Repository {
    private List<User> users;
    private Map<String, Product> products;
    private Set<Order> orders;
}

Field usersField = Repository.class.getDeclaredField("users");
Field productsField = Repository.class.getDeclaredField("products");

// Extract generic types
Class<?> userType = Fields.getGenericType(usersField, 0);
// Returns: User.class

Class<?> keyType = Fields.getGenericType(productsField, 0);
Class<?> valueType = Fields.getGenericType(productsField, 1);
// keyType: String.class, valueType: Product.class
```

### 11. Field Binder Pattern

Reactive field binding with suppliers:

```java
class Config {
    private String environment;
    private int maxConnections;
}

// Create suppliers
ISupplier<Config> configSupplier = () -> Optional.of(new Config());
ISupplier<String> envSupplier = () -> Optional.of("production");
ISupplier<Integer> maxConnSupplier = () -> Optional.of(100);

// Create binders
FieldBinder<Config, String> envBinder = new FieldBinder<>(
    configSupplier,
    new ObjectAddress("environment", true),
    envSupplier
);

FieldBinder<Config, Integer> maxConnBinder = new FieldBinder<>(
    configSupplier,
    new ObjectAddress("maxConnections", true),
    maxConnSupplier
);

// Set values (deferred until execution)
envBinder.setValue();
maxConnBinder.setValue();

// Get values
String env = envBinder.getValue();           // "production"
Integer maxConn = maxConnBinder.getValue();  // 100
```

### 12. ObjectAccessor Functional API

Use functional approach for reflection operations:

```java
class UserDto {
    private String username;
    private String email;
}

class UserEntity {
    private String username;
    private String email;
}

UserEntity entity = new UserEntity();
entity.username = "alice";
entity.email = "alice@example.com";

// Define functional getters
ThrowingFunction<Class<?>, UserDto> getDtoClass = (clazz) -> new UserDto();
ThrowingFunction<UserDto, ObjectAddress> getUsernameAddress =
    (dto) -> new ObjectAddress("username", true);

// Get value using functional API
String username = ObjectAccessor.getValue(
    entity,
    (clazz) -> entity,
    (obj) -> new ObjectAddress("username", true)
);
// Returns: "alice"

// Set value using functional API
ObjectAccessor.setValue(
    entity,
    (clazz) -> entity,
    (obj) -> new ObjectAddress("email", true),
    "newemail@example.com"
);
// entity.email is now "newemail@example.com"
```

## Advanced Patterns

### Dynamic Field Discovery

Discover available fields at runtime:

```java
class DynamicObject {
    private String field1;
    private int field2;
    private List<String> field3;
}

ObjectQuery query = ObjectQueryFactory.objectQuery(DynamicObject.class);

// Find all fields matching a name
try {
    ObjectAddress address = query.address("field1");
    System.out.println("Found field at: " + address);
} catch (ReflectionException e) {
    System.out.println("Field not found");
}
```

### Field Structure Generation

Generate nested object structures automatically:

```java
class Order {
    private Customer customer;
}

class Customer {
    private Address address;
}

class Address {
    private String city;
}

ObjectQuery query = ObjectQueryFactory.objectQuery(Order.class);

// Generate structure for nested field
Object structure = query.fieldValueStructure("customer.address.city");
// Returns: fully initialized Customer → Address → city hierarchy
```

### Blacklist Pattern for Circular References

Prevent infinite recursion with class blacklisting:

```java
// Add classes to blacklist to prevent traversal
Fields.BlackList.addClassToBlackList(java.lang.Object.class);
Fields.BlackList.addClassToBlackList(java.lang.Class.class);

// Check if class is blacklisted
boolean isBlacklisted = Fields.BlackList.isBlackListed(Object.class);
// Returns: true
```

### Type Checking Utilities

Validate field types before operations:

```java
Field field = SomeClass.class.getDeclaredField("someField");

// Check if field is not primitive
boolean isComplex = Fields.isNotPrimitive(field.getType());

// Check if field is not primitive and not in java.*/javax.* packages
boolean isCustomType = Fields.isNotPrimitiveOrInternal(field.getType());

// Check if field is collection/array/map
boolean isContainer = Fields.isArrayOrMapOrCollectionField(field);
```

### Logging with Pretty Printing

Use colored console output for debugging:

```java
Field userField = User.class.getDeclaredField("username");
Method getUserMethod = User.class.getMethod("getUsername");

// Pretty colored output (ANSI colors)
String fieldStr = Fields.prettyColored(userField);
// Output: "User.username : String" (with colors)

String methodStr = Methods.prettyColored(getUserMethod);
// Output: "User.getUsername()" (with colors)
```

## Performance

### Caching Strategy

Garganttua Reflection implements intelligent caching for:
- **Field lookups** - Resolved fields cached per class
- **Method lookups** - Method resolution cached
- **ObjectAddress parsing** - Address structures cached
- **Generic type resolution** - Generic type info cached

### Optimization Tips

1. **Reuse ObjectQuery instances** - Create once, use multiple times
2. **Use ObjectAddress objects** - Avoid string parsing overhead for repeated operations
3. **Batch operations** - Group multiple field access operations when possible
4. **Pre-resolve addresses** - Resolve ObjectAddress during initialization, not per-operation
5. **Limit traversal depth** - Avoid excessively deep object graphs when possible

### Performance Characteristics

| Operation | First Call | Subsequent Calls |
|-----------|-----------|------------------|
| Field lookup | ~0.1-1ms | ~0.01-0.1ms (cached) |
| Nested field access (3 levels) | ~0.5-2ms | ~0.1-0.5ms |
| Collection traversal | ~1-5ms per element | ~0.5-2ms per element |
| Method invocation | ~0.2-1ms | ~0.1-0.5ms |

## Error Handling

### ReflectionException

All reflection operations throw `ReflectionException` when errors occur:

```java
try {
    ObjectQuery query = ObjectQueryFactory.objectQuery(User.class);
    String value = query.getValue("nonexistent.field");
} catch (ReflectionException e) {
    // Handle reflection errors
    System.err.println("Reflection error: " + e.getMessage());

    // Common causes:
    // - Field doesn't exist
    // - Access denied (private field, security manager)
    // - Type mismatch
    // - Null values in traversal path
}
```

### Common Error Scenarios

**Field Not Found:**
```java
// Throws: ReflectionException("Field xyz not found in class User")
query.getValue("xyz");
```

**Null Traversal:**
```java
// If user.profile is null
// Throws: NullPointerException during traversal
query.getValue("user.profile.name");
```

**Type Mismatch:**
```java
// Trying to assign String to int field
// Throws: ClassCastException or IllegalArgumentException
query.setValue("age", "not a number");
```

### Best Practices for Error Handling

1. **Validate paths** - Check field existence before operations
2. **Null safety** - Ensure intermediate objects are non-null
3. **Type validation** - Validate types match expected values
4. **Graceful degradation** - Provide fallback values for missing fields
5. **Logging** - Enable trace/debug logging to diagnose issues

## Tips and best practices

### Design Principles

1. **ObjectQuery Reuse** - Create ObjectQuery instances once and reuse them for multiple operations
2. **ObjectAddress Immutability** - ObjectAddress objects are immutable; use `clone()` when modifications needed
3. **Type Safety** - Use generic parameters in ObjectQuery methods for compile-time type checking
4. **Fail Fast** - Configure strict mode during development to catch errors early
5. **Lazy Initialization** - Use Binder pattern for deferred field initialization

### Field Access Patterns

6. **Prefer Direct Access** - Use simple field names when possible (faster than nested paths)
7. **Batch Operations** - Group multiple setValue/getValue calls to minimize overhead
8. **Cache Addresses** - Store frequently-used ObjectAddress objects as constants
9. **Validate First** - Use `find()` to validate paths before bulk operations
10. **Null Checks** - Always check for null intermediate objects in deep paths

### Collection Handling

11. **Use #item Operator** - Leverage `#item` for uniform collection element access
12. **Map Navigation** - Use `#key` and `#value` to differentiate map traversal
13. **Generic Types** - Extract generic types with `Fields.getGenericType()` for type-safe operations
14. **Collection Initialization** - Use `Fields.instanciate()` to create properly typed collections
15. **Empty Collections** - Handle empty collections gracefully (operations return empty lists)

### Method Invocation

16. **Parameter Type Matching** - Ensure parameter types match method signatures exactly
17. **Varargs Support** - Pass varargs as individual arguments, not arrays
18. **Return Type Casting** - Cast method return values to expected types
19. **Exception Handling** - Catch ReflectionException for method invocation errors
20. **Method Overloading** - Specify exact parameter types for overloaded methods

### Performance Optimization

21. **Pre-resolve Paths** - Resolve ObjectAddress during initialization for hot paths
22. **Limit Depth** - Avoid unnecessarily deep object graphs (>5 levels)
23. **Monitor Caching** - Enable logging to verify cache effectiveness
24. **Profile Operations** - Measure reflection overhead in performance-critical code
25. **Batch Queries** - Group related field accesses to amortize lookup costs

### Debugging and Logging

26. **Enable Trace Logging** - Set `com.garganttua.core.reflection` to TRACE for detailed logs
27. **Pretty Printing** - Use `Fields.prettyColored()` and `Methods.prettyColored()` for readable output
28. **Validate Structures** - Use `find()` to inspect resolved field/method structures
29. **Test Incrementally** - Test simple paths before building complex ObjectAddress expressions
30. **Error Messages** - Read ReflectionException messages carefully for root cause

### Integration Patterns

31. **Dependency Injection** - Use with Garganttua Injection for dynamic bean property setting
32. **Mapper Integration** - Combine with Garganttua Mapper for advanced mapping scenarios
33. **Serialization** - Use for custom serialization/deserialization logic
34. **Validation Frameworks** - Integrate with validators for deep field validation
35. **ORM Layers** - Use for entity manipulation and metadata extraction

### Common Pitfalls to Avoid

36. **Don't Ignore Exceptions** - Always handle ReflectionException appropriately
37. **Don't Use Reflection for Simple Cases** - Direct field access is faster when possible
38. **Don't Traverse Circular References** - Use BlackList to prevent infinite loops
39. **Don't Assume Field Existence** - Always validate paths in dynamic scenarios
40. **Don't Mix Access Strategies** - Choose either ObjectQuery or ObjectAccessor, not both randomly
41. **Avoid String Concatenation** - Build ObjectAddress properly, don't concatenate strings manually
42. **Security Considerations** - Be cautious with reflection in security-sensitive contexts

## License

This module is distributed under the MIT License.
