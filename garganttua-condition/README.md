# Garganttua Condition

## Description

The **garganttua-condition** module provides a powerful and expressive DSL for building, composing, and evaluating runtime conditions on dynamically supplied objects. It enables declarative condition definition using a fluent builder API that integrates seamlessly with the [garganttua-supply](../garganttua-supply/README.md) module.

This module is ideal for:
- **Business rule engines** - Define complex validation rules declaratively
- **Workflow orchestration** - Control execution flow based on runtime conditions
- **Dynamic filtering** - Apply conditional logic to data streams
- **Access control** - Implement permission checks with composable conditions
- **Feature flags** - Toggle features based on complex criteria

Key features:
- **Fluent DSL** - Intuitive builder pattern for constructing conditions
- **Logical operators** - Full support for AND, OR, XOR, NOR, NAND operations
- **Lazy evaluation** - Conditions are evaluated only when needed via suppliers
- **Type-safe** - Generic support ensures type safety at compile time
- **Composable** - Nest and chain conditions for complex logic
- **Custom predicates** - Define domain-specific conditions using functional interfaces
- **Extraction support** - Apply predicates to extracted properties (e.g., `String::length`)

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-condition</artifactId>
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

<!-- AUTO-GENERATED-END -->

## Core Concepts

### ICondition Interface

The core interface that all conditions implement. It defines a single method:

```java
boolean evaluate() throws ConditionException
```

All condition implementations (AND, OR, custom, etc.) conform to this contract, enabling uniform composition.

### Condition Builders

Each condition type has a corresponding builder implementing `IConditionBuilder` from the DSL package. Builders follow the fluent builder pattern, allowing method chaining for readability:

```java
IConditionBuilder builder = and(condition1, condition2);
ICondition condition = builder.build();
boolean result = condition.evaluate();
```

### Object Suppliers

Conditions operate on values provided by `IObjectSupplier<T>` implementations. Suppliers defer object retrieval until `evaluate()` is called, enabling:
- **Lazy evaluation** - Values are fetched only when needed
- **Dynamic values** - Conditions can work with changing data
- **Integration** - Seamless connection with the supply module

Common supplier types:
- `FixedObjectSupplierBuilder.of(value)` - Supplies a fixed value
- `NullObjectSupplierBuilder.of(Type.class)` - Supplies null
- Custom suppliers for database queries, API calls, etc.

### Logical Operators

The module provides complete boolean algebra support:

| Operator | Description | Truth Table |
|----------|-------------|-------------|
| **AND** | All conditions must be true | T && T = T |
| **OR** | At least one condition must be true | T \|\| F = T |
| **XOR** | Odd number of conditions must be true | T ⊕ F = T |
| **NAND** | NOT(AND) - At least one must be false | !(T && T) = F |
| **NOR** | NOT(OR) - All must be false | !(T \|\| F) = F |

### Custom Predicates

Two forms of custom conditions are supported:

1. **Direct predicate** - Test the supplied value directly:
   ```java
   custom(supplier, value -> value > 10)
   ```

2. **Extracted predicate** - Extract a property, then test it:
   ```java
   custom(supplier, String::length, length -> length > 5)
   ```

This extraction pattern is powerful for testing nested or computed properties without creating intermediate objects.

### Exception Handling

Two exception types are used:

- **`ConditionException`** - Thrown during condition evaluation (e.g., null values in custom predicates)
- **`DslException`** - Thrown during condition building (e.g., type mismatches in equality checks)

Both extend the framework's exception hierarchy for consistent error handling.

## Usage

### Quick Start

```java
import static com.garganttua.core.condition.Conditions.*;
import static com.garganttua.core.supply.dsl.FixedObjectSupplierBuilder.*;

// Simple null check
boolean isPresent = isNotNull(of("hello")).build().evaluate(); // true

// Numeric comparison
boolean isValid = custom(of(10), v -> v > 5).build().evaluate(); // true

// Combined logic
boolean result = and(
    isNotNull(of("data")),
    custom(of(10), v -> v > 5)
).build().evaluate(); // true
```

### Null Checks

```java
// Check if value is null
isNull(of(null)).build().evaluate(); // true
isNull(of("present")).build().evaluate(); // false

// Check if value is not null
isNotNull(of("hello")).build().evaluate(); // true
isNotNull(NullObjectSupplierBuilder.of(String.class)).build().evaluate(); // false
```

### Equality Checks

```java
// Equals - compares two supplied values
equals(of(10), of(10)).build().evaluate(); // true
equals(of("abc"), of("ABC")).build().evaluate(); // false

// NotEquals
notEquals(of(10), of(20)).build().evaluate(); // true
notEquals(of("test"), of("test")).build().evaluate(); // false

// Type safety - throws DslException for type mismatch
equals(of(10), of(10.0)).build().evaluate(); // throws DslException: "Type mismatch Integer VS Double"
```

### Logical Operators

#### AND - All conditions must be true

```java
and(
    custom(of(10), v -> v > 5),
    custom(of(20), v -> v < 30)
).build().evaluate(); // true (both conditions are true)

and(
    custom(of(10), v -> v > 5),
    custom(of(50), v -> v < 30)
).build().evaluate(); // false (second condition is false)
```

#### OR - At least one condition must be true

```java
or(
    custom(of(5), v -> v > 3),
    custom(of(2), v -> v > 10)
).build().evaluate(); // true (first condition is true)

or(
    custom(of(1), v -> v > 3),
    custom(of(2), v -> v > 10)
).build().evaluate(); // false (both conditions are false)
```

#### XOR - Odd number of conditions must be true

```java
// XOR with 2 conditions - exactly one must be true
xor(
    custom(of(10), v -> v > 5),    // true
    custom(of(50), v -> v < 30)    // false
).build().evaluate(); // true (one true, one false)

xor(
    custom(of(10), v -> v > 5),    // true
    custom(of(20), v -> v < 30)    // true
).build().evaluate(); // false (both true)

// XOR with 3 conditions - odd number must be true
xor(
    custom(of(10), v -> v > 5),    // true
    custom(of(5), v -> v > 0),     // true
    custom(of(20), v -> v < 30)    // true
).build().evaluate(); // true (3 is odd)
```

#### NAND - NOT(AND) - At least one condition must be false

```java
nand(
    custom(of(10), v -> v > 5),
    custom(of(20), v -> v < 30)
).build().evaluate(); // false (both true, so AND is true, NAND is false)

nand(
    custom(of(10), v -> v > 5),
    custom(of(50), v -> v < 30)
).build().evaluate(); // true (one false, so AND is false, NAND is true)
```

#### NOR - NOT(OR) - All conditions must be false

```java
nor(
    custom(of(1), v -> v > 3),
    custom(of(2), v -> v > 3)
).build().evaluate(); // true (both false, so OR is false, NOR is true)

nor(
    custom(of(5), v -> v > 3),
    custom(of(2), v -> v > 3)
).build().evaluate(); // false (one true, so OR is true, NOR is false)
```

### Custom Conditions

#### Direct Predicate

```java
// Simple value test
custom(of(125), val -> val > 100).build().evaluate(); // true

// Boolean test
custom(of(true), val -> val).build().evaluate(); // true

// String test
custom(of(""), String::isEmpty, empty -> empty).build().evaluate(); // true
```

#### Extracted Predicate

```java
// Extract property, then test
custom(of("hello"), String::length, len -> len > 3).build().evaluate(); // true

// Extract with method reference
custom(of("abc"), String::isEmpty, empty -> !empty).build().evaluate(); // true

// Extract computed value
custom(of("hello"), str -> str.chars().sum(), sum -> sum > 500).build().evaluate(); // true

// Extract with identity
custom(of("identity"), Function.identity(), s -> s.startsWith("i")).build().evaluate(); // true
```

### Complex Compositions

```java
// Nested logical operators
and(
    or(
        custom(of(10), v -> v > 5),
        custom(of(2), v -> v < 0)
    ),
    xor(
        isNotNull(of("data")),
        isNull(of(null))
    )
).build().evaluate();

// Business rule example: validate order
and(
    isNotNull(orderSupplier),
    custom(orderSupplier, Order::getAmount, amount -> amount > 0),
    custom(orderSupplier, Order::getStatus, status -> status.equals("PENDING")),
    or(
        custom(orderSupplier, Order::isPriority, priority -> priority),
        custom(orderSupplier, Order::getCustomerTier, tier -> tier.equals("GOLD"))
    )
).build().evaluate();
```

### Real-World Examples

#### User Access Control

```java
// Check if user has permission to access resource
boolean hasAccess = and(
    isNotNull(userSupplier),
    custom(userSupplier, User::isActive, active -> active),
    or(
        custom(userSupplier, User::getRole, role -> role.equals("ADMIN")),
        and(
            custom(userSupplier, User::getRole, role -> role.equals("USER")),
            custom(resourceSupplier, Resource::isPublic, isPublic -> isPublic)
        )
    )
).build().evaluate();
```

#### Feature Flag System

```java
// Enable feature based on multiple criteria
boolean featureEnabled = and(
    custom(configSupplier, Config::getEnvironment, env -> env.equals("PRODUCTION")),
    or(
        custom(userSupplier, User::isBetaTester, isBeta -> isBeta),
        custom(rolloutSupplier, Rollout::getPercentage, pct -> pct >= 100)
    ),
    custom(featureSupplier, Feature::isStable, stable -> stable)
).build().evaluate();
```

#### Data Validation

```java
// Validate product before saving
boolean isValidProduct = and(
    isNotNull(productSupplier),
    custom(productSupplier, Product::getName, name -> name != null && !name.isBlank()),
    custom(productSupplier, Product::getPrice, price -> price > 0),
    custom(productSupplier, Product::getStock, stock -> stock >= 0),
    or(
        custom(productSupplier, Product::getSku, sku -> sku != null),
        custom(productSupplier, Product::getBarcode, barcode -> barcode != null)
    )
).build().evaluate();
```

## Tips and best practices

### General Principles

1. **Always use suppliers** - Use `of(...)` or custom suppliers to delay value retrieval until evaluation. This enables lazy evaluation and dynamic conditions.

2. **Use static imports** - Import `Conditions.*` and `FixedObjectSupplierBuilder.*` statically for cleaner, more readable code:
   ```java
   import static com.garganttua.core.condition.Conditions.*;
   import static com.garganttua.core.supply.dsl.FixedObjectSupplierBuilder.*;
   ```

3. **Chain small conditions** - Break complex logic into smaller, composable conditions instead of monolithic predicate blocks. This improves readability and reusability.

4. **Prefer logical operators over nested predicates** - Use `and()`, `or()` instead of complex lambda expressions for clarity.

### Type Safety

5. **Be aware of type mismatches** - `equals()` and `notEquals()` throw `DslException` when comparing different types:
   ```java
   equals(of(10), of(10.0)).build().evaluate(); // throws DslException
   ```

6. **Use generics appropriately** - Let type inference work for you, but specify types explicitly when needed for clarity.

### Custom Conditions

7. **Use extraction for property tests** - Instead of:
   ```java
   custom(of(user), u -> u.getAge() > 18)
   ```
   Prefer:
   ```java
   custom(of(user), User::getAge, age -> age > 18)
   ```
   This separates extraction from logic and makes the intent clearer.

8. **Handle null in custom predicates** - If your supplier might return null, use `isNotNull()` first:
   ```java
   and(
       isNotNull(userSupplier),
       custom(userSupplier, User::getAge, age -> age > 18)
   )
   ```

### Error Handling

9. **Catch appropriate exceptions** - `DslException` for build-time errors, `ConditionException` for evaluation-time errors:
   ```java
   try {
       boolean result = condition.build().evaluate();
   } catch (DslException e) {
       // Handle configuration error
   } catch (ConditionException e) {
       // Handle evaluation error
   }
   ```

10. **Validate early** - Build conditions once and reuse them. Build errors are caught early, reducing runtime issues.

### Performance

11. **Reuse built conditions** - Build once, evaluate multiple times:
    ```java
    ICondition condition = and(cond1, cond2).build(); // Build once
    boolean result1 = condition.evaluate(); // Reuse
    boolean result2 = condition.evaluate(); // Reuse
    ```

12. **Order matters for AND/OR** - Place cheaper or more likely-to-fail conditions first to short-circuit evaluation when possible.

### Domain-Specific Usage

13. **Create domain helpers** - Wrap common condition patterns in helper methods:
    ```java
    public static IConditionBuilder isAdult(IObjectSupplierBuilder<User, ?> user) {
        return custom(user, User::getAge, age -> age >= 18);
    }
    ```

14. **Document complex logic** - Add comments explaining business rules encoded in conditions, especially for complex nested logic.

15. **Test thoroughly** - Write unit tests for all condition combinations, including edge cases like null values and type mismatches.

## License

This module is distributed under the MIT License.
