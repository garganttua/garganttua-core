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
- **Composite Reflection Builder** - Compose multiple `IReflectionProvider` instances with priority-based resolution via `ReflectionBuilder`
- **Force Access** - Access private/protected fields, methods, and constructors via `force` parameter on access managers
- **IExecutable Abstraction** - Common superinterface for `IMethod` and `IConstructor` enabling unified invocation patterns
- **Invoker/Accessor Classes** - High-level `FieldAccessor`, `MethodInvoker`, and `ConstructorInvoker` replacing raw reflection calls
- **Performance Optimized** - Caching and efficient field/method lookup strategies

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-reflection</artifactId>
    <version>2.0.0-ALPHA02</version>
</dependency>
```

### Actual version
2.0.0-ALPHA02

### Dependencies
 - `com.garganttua.core:garganttua-commons`
 - `com.garganttua.core:garganttua-dsl`
 - `com.garganttua.core:garganttua-supply`
 - `org.javatuples:javatuples`
 - `com.garganttua.core:garganttua-runtime-reflection:test`

<!-- AUTO-GENERATED-END -->

## Core Concepts

### ObjectAddress

The `ObjectAddress` class is the foundation of Garganttua Reflection. It represents a **path to a field or method** within an object graph using dot notation.

**Basic Syntax:**
```
simple_field             → Access direct field
parent.child             → Access nested field
innersInList.i           → Access field in list elements
innersInMap.#key.i       → Access field in map keys
innersInMap.#value.i     → Access field in map values
```

**Example from ObjectAddressTest.java:**
```java
// Create address with validation
ObjectAddress address = new ObjectAddress("field1.field2.field3");
assertEquals(3, address.length());
assertEquals("field1", address.getElement(0));
assertEquals("field2", address.getElement(1));
assertEquals("field3", address.getElement(2));

// Convert to string
assertEquals("field1.field2.field3", address.toString());

// Invalid addresses throw IllegalArgumentException
assertThrows(IllegalArgumentException.class, () -> new ObjectAddress(".field1.field2"));
assertThrows(IllegalArgumentException.class, () -> new ObjectAddress("field1.field2."));
assertThrows(IllegalArgumentException.class, () -> new ObjectAddress(""));
```

**Features:**
- Dot-separated path elements
- Support for nested objects (unlimited depth)
- Collection/list iteration support
- Map navigation with `#key` and `#value` operators
- Immutable and validates on creation
- Equals and hashCode implemented

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

### FieldAccessor & MethodInvoker

`FieldAccessor` and `MethodInvoker` are the primary utilities for field access and method invocation:
- `FieldAccessor` - Type-safe field get/set with support for nested field traversal via `ObjectAddress`
- `MethodInvoker` - Method execution with nested field traversal for deep object paths
- `ConstructorInvoker` - Constructor invocation with parameter resolution

All three use `AccessManager` classes internally with try-with-resources for automatic accessibility management.

### Field Utilities

**FieldResolver** - Advanced field resolution across complex object graphs:
- `fieldByFieldName()` - Locate field by name
- `fieldByField()` - Validate field matches address
- `fieldByAddress()` - Resolve field by ObjectAddress

### Method Utilities

**Methods** - Utility class for method-related operations:
- `prettyColored()` - ANSI-colored method signature representation

**MethodResolver** - Method resolution and validation utilities

### Composite Reflection Builder

`ReflectionBuilder` composes multiple `IReflectionProvider` and `IAnnotationScanner` instances into a single `CompositeReflection`:

```java
IReflection reflection = ReflectionBuilder.builder()
    .withProvider(runtimeProvider, 100)  // higher priority
    .withProvider(aotProvider, 50)
    .withScanner(scanner, 100)
    .build();
```

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

Utilities for managing field, method, and constructor accessibility. Support a `force` parameter to access private/protected members:

- `FieldAccessManager(field, force)` - Makes fields accessible for reflection; with `force=true`, accesses private/protected fields
- `MethodAccessManager(method, force)` - Manages method accessibility; with `force=true`, accesses private/protected methods
- `ConstructorAccessManager(constructor, force)` - Handles constructor access; with `force=true`, accesses private/protected constructors

Access managers implement `AutoCloseable` and restore original accessibility state when closed.

## Usage

### 9. Constructor Binder with Parameters (ConstructorBinderBuilderTest.java)

Build and invoke constructors with parameters:

```java
class TargetClass {
    public final String name;
    public final int value;

    public TargetClass(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public TargetClass(String name) {
        this(name, 0);
    }

    public TargetClass() {
        this("default", -1);
    }
}

// Build constructor binder with raw values
ConcreteConstructorBinderBuilder builder = new ConcreteConstructorBinderBuilder(TargetClass.class);
builder.withParam("Hello").withParam(123);

IConstructorBinder<TargetClass> binder = builder.build();
assertNotNull(binder);

Optional<? extends TargetClass> obj = binder.execute();
assertTrue(obj.isPresent());
TargetClass tc = obj.get();
assertEquals("Hello", tc.name);
assertEquals(123, tc.value);

// Build constructor with suppliers
builder = new ConcreteConstructorBinderBuilder(TargetClass.class);
builder.withParam(new FixedSupplierBuilder<>("Dynamic"))
       .withParam(new FixedSupplierBuilder<>(999));

binder = builder.build();
tc = binder.execute().get();
assertEquals("Dynamic", tc.name);
assertEquals(999, tc.value);

// Use default constructor
builder = new ConcreteConstructorBinderBuilder(TargetClass.class);
binder = builder.build();
tc = binder.execute().get();
assertEquals("default", tc.name);
assertEquals(-1, tc.value);

// Nullable parameters
builder = new ConcreteConstructorBinderBuilder(TargetClass.class);
builder.withParam(0, new NullSupplierBuilder<String>(String.class), true);
builder.withParam(1, 77);

binder = builder.build();
tc = binder.execute().get();
assertNull(tc.name);
assertEquals(77, tc.value);
```

### 10. Method Binder for Instance and Static Methods (MethodBinderTest.java)

Invoke methods using method binders:

```java
class MethodObject {
    String echo(String message) {
        return message;
    }

    static String staticEcho(String message) {
        return message;
    }
}

// Invoke instance method
ConcreteMethodBinderBuilder b = new ConcreteMethodBinderBuilder(
    new Object(),
    FixedSupplierBuilder.of(new MethodObject())
);
b.method("echo").withReturn(String.class).withParam("Hello");
IMethodBinder<String> mb = b.build();

assertEquals("Hello", mb.supply().get());

// Invoke static method (supplier returns null for static)
ConcreteMethodBinderBuilder staticBuilder = new ConcreteMethodBinderBuilder(
    new Object(),
    new NullSupplierBuilder<>(MethodObject.class)
);
staticBuilder.method("staticEcho").withReturn(String.class).withParam("Hello");
IMethodBinder<String> staticBinder = staticBuilder.build();

assertEquals("Hello", staticBinder.supply().get());
```

### 11. Null Value Handling (ObjectFieldGetterTest.java)

Handle null values in object graphs:

```java
class ObjectTest {
    private ObjectTest inner;
    private int i;
}

// Create object with null inner
ObjectTest o = new ObjectTest(null, 0, null, 0, 0, null, null, null);

// Get null value
List<Object> fieldInfos = new ArrayList<Object>();
fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));

ObjectAddress address = new ObjectAddress("inner");
ObjectFieldGetter getter = new ObjectFieldGetter(ObjectTest.class, fieldInfos, address);

Object value = getter.getValue(o);
assertNull(value);  // Returns null

// Get value from null inner object
List<Object> nestedFieldInfos = new ArrayList<Object>();
nestedFieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
nestedFieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "i"));

ObjectAddress nestedAddress = new ObjectAddress("inner.i");
ObjectFieldGetter nestedGetter = new ObjectFieldGetter(ObjectTest.class, nestedFieldInfos, nestedAddress);

Object nestedValue = nestedGetter.getValue(o);
assertNull(nestedValue);  // Returns null when traversing through null
```

### Advanced Patterns

### Complex Nested Structure Access (ObjectFieldGetterTest.java)

Access values through complex nested structures with lists, arrays, and maps:

```java
class ObjectTest {
    private ObjectTest inner;
    private List<ObjectTest> innersInList;
    private ObjectTest[] innersInArray;
    private Map<ObjectTest, ObjectTest> innersInMap;
    private int i;
}

// Create deeply nested object (6 levels)
ObjectTest o = createNestedObject(6);

// Access through: inner -> innersInList -> innersInArray -> innersInMap (value) -> inner -> i
List<Object> fieldInfos = new ArrayList<Object>();
fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "innersInList"));
fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "innersInArray"));
fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "innersInMap"));
fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "i"));

ObjectAddress address = new ObjectAddress("inner.innersInList.innersInArray.innersInMap.#value.inner.i", false);
ObjectFieldGetter getter = new ObjectFieldGetter(ObjectTest.class, fieldInfos, address);

Object value = getter.getValue(o);
assertNotNull(value);
// Returns nested list structure: List<List<List<int>>>
assertEquals(1, ((List<Object>) ((List<Object>) ((List<Object>) value).get(0)).get(0)).get(0));
```

### Setting Values in Sets (ObjectFieldSetterTest.java)

Set values in Set fields:

```java
class ObjectTest {
    private Set<ObjectTest> innersInSet;
    private List<ObjectTest> innersInList;
    private long l;
}

// Set values in set with nested list
List<Object> fieldInfos = new ArrayList<Object>();
fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "innersInSet"));
fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "innersInList"));
fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "l"));

ObjectAddress address = new ObjectAddress("innersInSet.innersInList.l");
ObjectFieldSetter setter = new ObjectFieldSetter(ObjectTest.class, fieldInfos, address);

ObjectTest object = (ObjectTest) setter.setValue(List.of(List.of(1L, 2L, 3L), List.of(1L, 2L, 3L)));

assertNotNull(object);
assertEquals(2, object.getInnersInSet().size());
object.getInnersInSet().forEach(objectTest -> {
    assertEquals(1L, objectTest.getInnersInList().get(0).getL());
    assertEquals(2L, objectTest.getInnersInList().get(1).getL());
    assertEquals(3L, objectTest.getInnersInList().get(2).getL());
});
```

### Deep Nesting Support

The library supports unlimited depth nesting. Example from tests shows 6-level deep access:

```java
// Create object nested 6 levels deep
ObjectTest deepObject = createNestedObject(6);

// Access: inner.inner.inner.inner.inner.l (5 levels of inner, then field l)
List<Object> fieldInfos = new ArrayList<Object>();
fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "l"));

ObjectAddress address = new ObjectAddress("inner.inner.inner.inner.inner.l", false);
ObjectFieldSetter setter = new ObjectFieldSetter(ObjectTest.class, fieldInfos, address);

ObjectTest object = (ObjectTest) setter.setValue(1L);
assertNotNull(object);
assertEquals(1L, object.getInner().getInner().getInner().getInner().getInner().getL());
```


### Performance

#### Caching Strategy

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

### Error Handling

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
40. **Avoid String Concatenation** - Build ObjectAddress properly, don't concatenate strings manually
42. **Security Considerations** - Be cautious with reflection in security-sensitive contexts

## License
This module is distributed under the MIT License.
