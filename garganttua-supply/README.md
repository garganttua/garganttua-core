# Garganttua Supply

## Description

The **garganttua-supply** module provides a flexible, type-safe object
supplying system used throughout the garganttua-core ecosystem.\
It supports fixed suppliers, contextual suppliers, and a unified API to
resolve objects dynamically at runtime.

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-supply</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `com.garganttua.core:garganttua-commons`

<!-- AUTO-GENERATED-END -->

## Core Concepts

usage of DSL and builder to build a usefull object supplier at runtime

Simple suppliers
 - `FixedObjectSupplier` : Creates suppliers that always
    return a fixed object.
 - `NullObjectSupplier` : Creates suppliers that always
    return a null object.

Complex suppliers : these suppliers offer more complex features
 - `NewObjectSupplier` : Creates suppliers that always create a new Object
 - `NullableObjectSupplier` : Wrapper that manages null values

Contextual suppliers : these suppliers are able to use a given context to supply the object. It's particularely usefull if the construction of an object depends on other objects managed by an external system, such as Spring Context for example.
 - `ContextualObjectSupplier`: Create suppliers that request the given context to supply the appropriate object
 - `NewContextualObjectSupplier` : Creates suppliers that always create a new Object regarding to the given context
- `NullableContextualObjectSupplier` : Wrapper that manages null values

`NewObjectSupplier` and `NewContextualSupplier` need a `IConstructorBinder` or `IContextualConstructorBinder` to work properly. These binder are responsible to do the injection of constructors parameters. More infos here(garganttua-reflexion).
`garganttua-reflexion` module offers usefull classes to create and manage constructor binders. 

## Usage

### Simple object suppliers

**Example: Using the builder**
Use the `SupplierBuilder` to autmatically construct a supplier that matches the need. It automatically creates the appropriate supplier regarding the given parameters.

```java
IConstructorBinder<String> ctorBinder = ...;

new SupplierBuilder(String.class)
    .withValue("Hello") //Define a fixed value to supply
    .nullable(true) //Indicate if the supplied value can be null or not. Supplier will throw an exception if a null value is supplied but is indicated as not nullable
    .withContext(Context.class) //Indicate that a context object is needed to complete the value supplying
    .withConstructor(ctorBinder) //Indicate a constructor to use to supply a new object. Must be of type IContextualConstructorBinder in case of context type is declared.
    .build() //Build the supplier
    .supply() //Supply the object according to the parameters given to the builder
    .get(); //Get the supplied object from the returned Optional. 
```

The built supplier is automatically wrapped in an `NullableObjectSupplier` or `NullableContextualObjectSupplier` in order to throw an exception if the supplied value is indicated as not nullable.

**Example: Using the static methods**

Use the set of predefined methods that construct the appropriate supplier.

```java
import static com.garganttua.core.supply.dsl.supplierBuilder.*;

String value = fixed(String.class, "Hello").build().supply().get(); // "Hello"
```
```java
IConstructorBinder<String> ctorBinder = ...;
String value = newObject(String.class, ctorBinder).build().supply().get(); // new String 
```
```java
String value = nullObject(String.class).build().supply().get(); // null
```
```java
StringContext context = ...;
String value = contextual(String.class, StringContext.class, c -> c.newString("Hello")).build().supply(context).get(); // "Hello"
```
```java
IContextualConstructorBinder<String> ctorBinder = ...;
StringContext context = ...;
String value = newContextual(String.class, StringContext.class, ctorBinder).build().supply(context).get(); // new instance of String according to context

```

#### Fixed object supplier

Use `FixedObjectSupplierBuilder` to create suppliers that always return
the same object.

**Example: fixed object supplier**

``` java
FixedObjectSupplierBuilder<String> builder = new FixedObjectSupplierBuilder<>("hello"); //creates a builder
FixedObjectSupplierBuilder<String> builder = FixedObjectSupplierBuilder.of("hello"); //creates a builder
IObjectSupplier<String> supplier = builder.build(); //Build the supplier
String value = supplier.supply().get(); // "hello"
```

**Example: fixed object supplier from static import**

``` java
import static com.garganttua.core.supply.dsl.FixedObjectSupplierBuilder.*;
...
String value = of("hello").build().supply().get(); // "hello"
```

#### Null object supplier

Use `NullObjectSupplierBuilder` to create suppliers that always return null.

```java

```

## Tips and best practices

-   Favor contextual suppliers for dynamic runtime evaluation.
-   When possible, expose strongly typed suppliers instead of using raw
    contexts.
-   Wrap complex supplier logic using builders for clarity and
    reusability.
-   Always handle `SupplyException` and `DslException` during supplier
    creation or evaluation.

## License

This module is distributed under the MIT License.
