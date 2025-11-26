# Garganttua Condition

## Description
The **garganttua-condition** module provides a declarative DSL to build, compose, and evaluate runtime conditions on dynamically supplied objects.  
It allows developers to chain logical operators, create custom predicate-based conditions, and integrate conditions with object suppliers within the garganttua-core ecosystem.

## Installation
Add the module to your project:

```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-condition</artifactId>
    <version>LATEST</version>
</dependency>
```

## Core Concepts

ConditionBuilder — A fluent builder used to construct conditions.
Suppliers — Provide objects at evaluation time. See [**here**](../garganttua-supplying/README.md) 
Logical operators — Compose multiple conditions (AND, OR, XOR, etc.).
Custom predicates — Create fully customized conditions from functional interfaces.
Exception handling — Validation and type mismatch errors are surfaced via DslException or ConditionException.

## Usage

### Basic usage

Conditions are created using the DSL from Conditions.*.

import static com.garganttua.core.condition.Conditions.*;
import static com.garganttua.core.supplying.dsl.FixedObjectSupplierBuilder.*;

All conditions must be finished with .build().evaluate().

### Available operators

Null checks

isNull(supplier)

isNotNull(supplier)

Equality

equals(leftSupplier, rightSupplier)

notEquals(leftSupplier, rightSupplier)

Logical operators

and(cond1, cond2, ...)

or(cond1, cond2, ...)

nor(cond1, cond2, ...)

nand(cond1, cond2, ...)

xor(cond1, cond2, ...)

Custom condition

custom(supplier, predicate)

custom(supplier, transformer, predicate)

These allow full custom evaluation logic, including mapping the value before checking.

Example
boolean result =
    and(
        isNotNull(of("hello")),
        custom(of(10), v -> v > 5)
    )
    .build()
    .evaluate();

Tips and best practices

Use suppliers (of(...)) to defer object retrieval until evaluation.

Combine multiple lightweight conditions using logical operators for readability.

Prefer custom(...) when encoding domain-specific rules.

Always handle DslException for type mismatches in equality checks.

License

This module is distributed under the MIT License.