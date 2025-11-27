# Garganttua Condition

## Description
The **garganttua-condition** module provides a declarative DSL to build, compose, and evaluate runtime conditions on dynamically [supplied](../garganttua-supply/README.md) objects.
It allows developers to chain logical operators, create custom predicate-based conditions, and integrate conditions with object suppliers within the garganttua-core ecosystem.

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

### ConditionBuilder

A fluent builder used to construct declarative runtime conditions.

### Suppliers

Suppliers defer object retrieval until evaluation.
See the garganttua-supply module.

### Logical operators

Compose multiple conditions (AND, OR, XOR, NOR, NAND).

### Custom predicates

Define domain-specific conditions using functional interfaces.

### Exception handling

Errors such as type mismatches or invalid predicates are surfaced via:
 - `DslException`
 - `ConditionException`

## Usage

### Basic usage
```java
import static com.garganttua.core.condition.Conditions.*;
import static com.garganttua.core.supply.dsl.FixedObjectSupplierBuilder.*;

boolean result =
    and(
        isNotNull(of("hello")),
        custom(of(10), v -> v > 5)
    )
    .build()
    .evaluate(); // True
```

### Available operators

#### Null checks
```java
isNull(supplier);
isNotNull(supplier);
```
#### Equality
```java
equals(leftSupplier, rightSupplier);
notEquals(leftSupplier, rightSupplier);
```
#### Logical operators
```java
and(cond1, cond2, ...);
or(cond1, cond2, ...);
nor(cond1, cond2, ...);
nand(cond1, cond2, ...);
xor(cond1, cond2, ...);
```
#### Custom conditions
```java
custom(supplier, predicate);
custom(supplier, accessor, predicate);
```
Example:
```java
boolean result =
    custom(of("hello"), String::length, len -> len > 3)
        .build()
        .evaluate(); // True
```
## Tips and best practices

 - **Always use suppliers (of(...))** to delay value retrieval until evaluation.
 - **Chain several small conditions** instead of creating monolithic predicate blocks.
 - **Use custom(...)** when encoding domain-specific business rules.
 - **Check for type mismatches** when comparing values using equals(...) or notEquals(...).
 - **Prefer logical operators** for clarity instead of nested custom predicates.
 - **Catch DslException** to detect invalid supplier or operator usage.
 - **Use static import** to improve readability and consision

## License

This module is distributed under the MIT License.

