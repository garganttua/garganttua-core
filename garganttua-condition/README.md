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
boolean fullEvaluate() throws ConditionException
```

All condition implementations (AND, OR, custom, etc.) conform to this contract, enabling uniform composition.

### Condition Builders

Each condition type has a corresponding builder implementing `IConditionBuilder` from the DSL package. Builders follow the fluent builder pattern, allowing method chaining for readability:

```java
IConditionBuilder builder = and(condition1, condition2);
ICondition condition = builder.build();
boolean result = condition.fullEvaluate();
```

### Object Suppliers

Conditions operate on values provided by `ISupplier<T>` implementations. Suppliers defer object retrieval until `fullEvaluate()` is called, enabling:
- **Lazy evaluation** - Values are fetched only when needed
- **Dynamic values** - Conditions can work with changing data
- **Integration** - Seamless connection with the supply module

Common supplier types:
- `FixedSupplierBuilder.of(value)` - Supplies a fixed value
- `NullSupplierBuilder.of(Type.class)` - Supplies null
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
import static com.garganttua.core.supply.dsl.FixedSupplierBuilder.*;

// Simple null check
boolean isPresent = isNotNull(of("hello")).build().fullEvaluate(); // true

// Numeric comparison
boolean isValid = custom(of(10), v -> v > 5).build().fullEvaluate(); // true

// Combined logic
boolean result = and(
    isNotNull(of("data")),
    custom(of(10), v -> v > 5)
).build().fullEvaluate(); // true
```

### Null Checks

```java
// Check if value is null
isNull(of("null")).build().fullEvaluate(); // false
isNull("String").build().fullEvaluate(); // false
isNull(NullSupplierBuilder.of(String.class)).build().fullEvaluate(); // true

// Check if value is not null
isNotNull(of("null")).build().fullEvaluate(); // true
isNotNull("String").build().fullEvaluate(); // true
isNotNull(NullSupplierBuilder.of(String.class)).build().fullEvaluate(); // false
```

### Equality Checks

```java
// Equals - compares two supplied values
Conditions.equals(of(10), of(10)).build().fullEvaluate(); // true
Conditions.equals(of("abc"), of("abc")).build().fullEvaluate(); // true
Conditions.equals(of("abc"), of("ABC")).build().fullEvaluate(); // false

// NotEquals
notEquals(of(10), of(10)).build().fullEvaluate(); // false
notEquals(of(10), of(20)).build().fullEvaluate(); // true
notEquals(of("abc"), of("abc")).build().fullEvaluate(); // false
notEquals(of("abc"), of("ABC")).build().fullEvaluate(); // true

// Type safety - throws DslException for type mismatch
Conditions.equals(of(10), of(10.0)).build().fullEvaluate(); // throws DslException: "Type mismatch Integer VS Double"

// Object equality
Object o = new Object();
Conditions.equals(of(o), of(o)).build().fullEvaluate(); // true
Conditions.equals(of(o), of(new Object())).build().fullEvaluate(); // false
```

### Logical Operators

#### AND - All conditions must be true

```java
and(
    custom(of(10), v -> v > 5),
    custom(of(20), v -> v < 30)
).build().fullEvaluate(); // true (both conditions are true)

and(
    isNull(NullSupplierBuilder.of(String.class)),
    isNull(NullSupplierBuilder.of(String.class))
).build().fullEvaluate(); // true (both are null)

and(
    isNull(NullSupplierBuilder.of(String.class)),
    isNull(of("null"))
).build().fullEvaluate(); // false (second is not null)
```

#### OR - At least one condition must be true

```java
or(
    custom(of(5), v -> v > 3),
    custom(of(2), v -> v > 10)
).build().fullEvaluate(); // true (first condition is true)

or(
    custom(of(1), v -> v > 3),
    custom(of(2), v -> v > 10)
).build().fullEvaluate(); // false (both conditions are false)

or(
    custom(of("test"), String::isEmpty, e -> !e),
    custom(of(99), v -> v < 100)
).build().fullEvaluate(); // true (both conditions are true)
```

#### XOR - Odd number of conditions must be true

```java
// XOR with 2 conditions - exactly one must be true
xor(
    custom(of(10), v -> v > 5),    // true
    custom(of(50), v -> v < 30)    // false
).build().fullEvaluate(); // true (one true, one false)

xor(
    custom(of(10), v -> v > 5),    // true
    custom(of(20), v -> v < 30)    // true
).build().fullEvaluate(); // false (both true)

xor(
    custom(of(1), v -> v > 5),     // false
    custom(of(2), v -> v > 10)     // false
).build().fullEvaluate(); // false (both false)

// XOR with 3 conditions - odd number must be true
xor(
    custom(of(10), v -> v > 5),    // true
    custom(of(5), v -> v > 0),     // true
    custom(of(20), v -> v < 30)    // true
).build().fullEvaluate(); // true (3 is odd)

xor(
    custom(of(10), v -> v > 5),    // true
    custom(of(5), v -> v < 0),     // false
    custom(of(20), v -> v < 30)    // true
).build().fullEvaluate(); // false (2 is even)

// XOR with 4 conditions
xor(
    custom(of(10), v -> v > 5),    // true
    custom(of(20), v -> v < 30),   // true
    custom(of(0), v -> v < 0),     // false
    custom(of(1), v -> v > 10)     // false
).build().fullEvaluate(); // false (2 is even)
```

#### NAND - NOT(AND) - At least one condition must be false

```java
nand(
    custom(of(10), v -> v > 5),
    custom(of(20), v -> v < 30)
).build().fullEvaluate(); // false (both true, so AND is true, NAND is false)

nand(
    custom(of(10), v -> v > 5),
    custom(of(50), v -> v < 30)
).build().fullEvaluate(); // true (one false, so AND is false, NAND is true)

nand(
    custom(of(1), v -> v > 5),
    custom(of(2), v -> v > 10)
).build().fullEvaluate(); // true (both false, so AND is false, NAND is true)
```

#### NOR - NOT(OR) - All conditions must be false

```java
nor(
    custom(of(1), v -> v > 3),
    custom(of(2), v -> v > 3)
).build().fullEvaluate(); // true (both false, so OR is false, NOR is true)

nor(
    custom(of(5), v -> v > 3),
    custom(of(2), v -> v > 3)
).build().fullEvaluate(); // false (one true, so OR is true, NOR is false)

nor(
    custom(of(5), v -> v > 3),
    custom(of(8), v -> v > 3)
).build().fullEvaluate(); // false (both true, so OR is true, NOR is false)
```

### Custom Conditions

#### Direct Predicate

```java
// Simple numeric test
custom(of(125), val -> val > 3).build().fullEvaluate(); // true
custom(of(10), val -> val < 5).build().fullEvaluate(); // false

// Boolean test
custom(of(true), val -> val).build().fullEvaluate(); // true
custom(of(false), val -> val).build().fullEvaluate(); // false

// Zero check
custom(of(0), val -> val == 0).build().fullEvaluate(); // true

// Double comparison
custom(of(3.14), val -> val > 3).build().fullEvaluate(); // true
```

#### Extracted Predicate

```java
// Extract property, then test
custom(of("hello"), String::length, len -> len > 3).build().fullEvaluate(); // true

// Extract with method reference
custom(of("abc"), String::isEmpty, empty -> !empty).build().fullEvaluate(); // true
custom(of(""), String::isEmpty, empty -> !empty).build().fullEvaluate(); // false

// String length checks
custom(of("abc123"), String::length, len -> len == 6).build().fullEvaluate(); // true

// Extract computed value
custom(of("hello"), str -> str.chars().sum(), sum -> sum > 500).build().fullEvaluate(); // true

// Extract with identity function
custom(of("identity"), Function.identity(), s -> s.startsWith("i")).build().fullEvaluate(); // true
```

### Complex Compositions

```java
// Combining AND with custom conditions
and(
    custom(of(10), val -> val > 5),
    custom(of(20), val -> val < 30)
).build().fullEvaluate(); // true
```

## Tips and best practices

### General Principles

1. **Always use suppliers** - Use `of(...)` or custom suppliers to delay value retrieval until evaluation. This enables lazy evaluation and dynamic conditions.

2. **Use static imports** - Import `Conditions.*` and `FixedSupplierBuilder.*` statically for cleaner, more readable code:
   ```java
   import static com.garganttua.core.condition.Conditions.*;
   import static com.garganttua.core.supply.dsl.FixedSupplierBuilder.*;
   ```

3. **Chain small conditions** - Break complex logic into smaller, composable conditions instead of monolithic predicate blocks. This improves readability and reusability.

4. **Prefer logical operators over nested predicates** - Use `and()`, `or()` instead of complex lambda expressions for clarity.

### Type Safety

5. **Be aware of type mismatches** - `equals()` and `notEquals()` throw `DslException` when comparing different types:
   ```java
   equals(of(10), of(10.0)).build().fullEvaluate(); // throws DslException
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
       boolean result = condition.build().fullEvaluate();
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
    boolean result1 = condition.fullEvaluate(); // Reuse
    boolean result2 = condition.fullEvaluate(); // Reuse
    ```

12. **Order matters for AND/OR** - Place cheaper or more likely-to-fail conditions first to short-circuit evaluation when possible.

### Domain-Specific Usage

13. **Create domain helpers** - Wrap common condition patterns in helper methods:
    ```java
    public static IConditionBuilder isAdult(ISupplierBuilder<User, ?> user) {
        return custom(user, User::getAge, age -> age >= 18);
    }
    ```

14. **Document complex logic** - Add comments explaining business rules encoded in conditions, especially for complex nested logic.

15. **Test thoroughly** - Write unit tests for all condition combinations, including edge cases like null values and type mismatches.

## License

This module is distributed under the MIT License.
