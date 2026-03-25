# Comprehensive Suppliers and Supplier Builders Documentation

## Overview

The Garganttua framework uses a **Supplier** system to provide objects lazily (deferred evaluation). The architecture is split into:
- **Core suppliers** (`garganttua-commons` + `garganttua-supply`) — generic object provisioning
- **Injection suppliers** (`garganttua-commons` + `garganttua-injection`) — bean and property provisioning within the DI context

---

## 1. Core Interfaces

### 1.1 `ISupplier<Supplied>`

| | |
|---|---|
| **Module** | `garganttua-commons` |
| **Package** | `com.garganttua.core.supply` |
| **Type** | Interface |
| **Contextual** | No |
| **Associated annotation** | None |

Root interface for all suppliers. Provides an object of type `Supplied` lazily.

**Key methods:**
- `Optional<Supplied> supply()` — provides the object
- `Type getSuppliedType()` — generic Java type supplied
- `IClass<Supplied> getSuppliedClass()` — supplied class (`IClass` wrapper)
- `default boolean isContextual()` — returns `false`

---

### 1.2 `IContextualSupplier<Supplied, Context>`

| | |
|---|---|
| **Module** | `garganttua-commons` |
| **Package** | `com.garganttua.core.supply` |
| **Type** | Interface, extends `ISupplier<Supplied>` |
| **Contextual** | **Yes** |
| **Context type** | Generic `Context` (type parameter) |
| **Associated annotation** | None |

Supplier requiring a context to produce the object.

**Key methods:**
- `Optional<Supplied> supply(Context ownerContext, Object... otherContexts)` — supplies with context
- `IClass<Context> getOwnerContextType()` — owner context type
- `default boolean isContextual()` — returns `true`
- `default Optional<Supplied> supply()` — throws `SupplyException` (context required)

---

### 1.3 `IContextualSupply<Supplied, Context>`

| | |
|---|---|
| **Module** | `garganttua-commons` |
| **Package** | `com.garganttua.core.supply` |
| **Type** | Functional interface (`@FunctionalInterface`) |
| **Contextual** | **Yes** |
| **Context type** | Generic `Context` |
| **Associated annotation** | None |

Pure contextual creation lambda/method reference, without metadata. Typically used as a parameter in builders.

**Single method:**
- `Optional<Supplied> supply(Context context, Object... otherContexts)`

---

## 2. Concrete Supplier Implementations

### 2.1 `FixedSupplier<Supplied>`

| | |
|---|---|
| **Module** | `garganttua-supply` |
| **Package** | `com.garganttua.core.supply` |
| **Contextual** | No |
| **Associated annotation** | `@Fixed` |

Always returns the same pre-defined instance (singleton-like behavior).

**Usage:** Injection of constant/literal values.

---

### 2.2 `NewSupplier<SuppliedType>`

| | |
|---|---|
| **Module** | `garganttua-supply` |
| **Package** | `com.garganttua.core.supply` |
| **Contextual** | No |
| **Associated annotation** | None |

Creates a new instance on each call via an `IConstructorBinder<SuppliedType>` (reflection).

**Usage:** Prototype instantiation via constructor.

---

### 2.3 `NullSupplier<SuppliedType>`

| | |
|---|---|
| **Module** | `garganttua-supply` |
| **Package** | `com.garganttua.core.supply` |
| **Contextual** | No |
| **Associated annotation** | `@Null` |

Always returns `Optional.empty()`. Default supplier when no strategy is configured.

**Usage:** Explicitly null optional dependencies.

---

### 2.4 `NullableSupplier<SuppliedType>`

| | |
|---|---|
| **Module** | `garganttua-supply` |
| **Package** | `com.garganttua.core.supply` |
| **Contextual** | No |
| **Associated annotation** | None |

Wrapper/decorator around a delegate `ISupplier<SuppliedType>`. Controls whether `null` is an acceptable value.

**Fields:** `ISupplier<SuppliedType> delegate`, `boolean allowNull`

**Usage:** Nullability validation on an existing supplier.

---

### 2.5 `ContextualSupplier<Supplied, Context>`

| | |
|---|---|
| **Module** | `garganttua-supply` |
| **Package** | `com.garganttua.core.supply` |
| **Contextual** | **Yes** |
| **Context type** | Generic `Context` |
| **Associated annotation** | None |

Concrete implementation of `IContextualSupplier`. Wraps an `IContextualSupply` with context type validation.

**Fields:** `IContextualSupply<Supplied, Context> supply`, `IClass<Supplied> suppliedClass`, `IClass<Context> contextClass`

---

### 2.6 `NewContextualSupplier<SuppliedType>`

| | |
|---|---|
| **Module** | `garganttua-supply` |
| **Package** | `com.garganttua.core.supply` |
| **Contextual** | **Yes** |
| **Context type** | `Void` (no specific context required) |
| **Associated annotation** | None |

Creates new instances via `IContextualConstructorBinder<SuppliedType>`. Context is `Void` — contextual by interface but does not require a concrete context.

---

### 2.7 `NullableContextualSupplier<SuppliedType, ContextType>`

| | |
|---|---|
| **Module** | `garganttua-supply` |
| **Package** | `com.garganttua.core.supply` |
| **Contextual** | **Yes** |
| **Context type** | Generic `ContextType` (inherited from delegate) |
| **Associated annotation** | None |

Nullability wrapper/decorator for `IContextualSupplier`. Contextual equivalent of `NullableSupplier`.

**Fields:** `IContextualSupplier<SuppliedType, ContextType> delegate`, `boolean allowNull`

---

### 2.8 `BlockingSupplier<Supplied>`

| | |
|---|---|
| **Module** | `garganttua-supply` |
| **Package** | `com.garganttua.core.supply` |
| **Contextual** | No |
| **Associated annotation** | None |

Provides an object from a `BlockingQueue<Supplied>`. Blocks the caller until available (with optional timeout).

**Fields:** `BlockingQueue<Supplied> queue`, `Long timeoutMillis`, `IClass<Supplied> suppliedClass`

**Usage:** Producer/consumer integration, asynchronous pipelines.

---

### 2.9 `FutureSupplier<Supplied>`

| | |
|---|---|
| **Module** | `garganttua-supply` |
| **Package** | `com.garganttua.core.supply` |
| **Contextual** | No |
| **Associated annotation** | None |

Provides an object from a `CompletableFuture<Supplied>`. Blocks until the future completes (with optional timeout).

**Fields:** `CompletableFuture<Supplied> future`, `Long timeoutMillis`, `IClass<Supplied> suppliedClass`

**Usage:** Asynchronous computation results, integration with non-blocking APIs.

---

## 3. Utility Class

### 3.1 `Supplier` (static class)

| | |
|---|---|
| **Module** | `garganttua-commons` |
| **Package** | `com.garganttua.core.supply` |

Utility for uniformly invoking both contextual and non-contextual suppliers.

**Methods:**
- `static <S> S contextualSupply(ISupplier<S> supplier, Object... contexts)` — automatic context type matching
- `static Object contextualRecursiveSupply(ISupplier<?> supplier, Object... contexts)` — recursive resolution of nested suppliers

---

## 4. Supplier Builder Interfaces

### 4.1 `ISupplierBuilder<Supplied, Built extends ISupplier<Supplied>>`

| | |
|---|---|
| **Module** | `garganttua-commons` |
| **Package** | `com.garganttua.core.supply.dsl` |
| **Type** | Interface, extends `IBuilder<Built>` |

Root builder interface for all suppliers. Provides metadata introspection.

**Methods:**
- `IClass<Supplied> getSuppliedClass()`
- `Type getSuppliedType()`
- `boolean isContextual()`

---

### 4.2 `ICommonSupplierBuilder<Supplied>`

| | |
|---|---|
| **Module** | `garganttua-commons` |
| **Package** | `com.garganttua.core.supply.dsl` |
| **Type** | Interface, extends `ISupplierBuilder<Supplied, ISupplier<Supplied>>` |

Main unified builder interface allowing configuration of any supply strategy.

**Configuration methods:**
| Method | Produced supplier | Contextual |
|---|---|---|
| `withValue(Supplied value)` | `FixedSupplier` | No |
| `withConstructor(IConstructorBinder)` | `NewSupplier` | No |
| `withContext(IClass<Ctx>, IContextualSupply)` | `ContextualSupplier` | Yes |
| `withFuture(CompletableFuture)` | `FutureSupplier` | No |
| `withFuture(CompletableFuture, Long timeout)` | `FutureSupplier` | No |
| `withBlockingQueue(BlockingQueue)` | `BlockingSupplier` | No |
| `withBlockingQueue(BlockingQueue, Long timeout)` | `BlockingSupplier` | No |
| `nullable(boolean)` | Wraps with `Nullable*Supplier` | — |

---

## 5. Concrete Supplier Builder Implementations

### 5.1 `SupplierBuilder<Supplied>`

| | |
|---|---|
| **Module** | `garganttua-supply` |
| **Package** | `com.garganttua.core.supply.dsl` |
| **Implements** | `ICommonSupplierBuilder<Supplied>` |

Main builder. Depending on the configuration called, `build()` produces the appropriate supplier:

| Configuration | Built supplier |
|---|---|
| `withFuture(...)` | `FutureSupplier` (+ `NullableSupplier` if nullable) |
| `withBlockingQueue(...)` | `BlockingSupplier` (+ `NullableSupplier` if nullable) |
| `withValue(...)` | `FixedSupplier` (+ `NullableSupplier` if nullable) |
| `withConstructor(...)` | `NewSupplier` (+ `NullableSupplier` if nullable) |
| `withContext(...)` | `ContextualSupplier` (+ `NullableContextualSupplier` if nullable) |
| No config | `NullSupplier` |

---

### 5.2 `FixedSupplierBuilder<Supplied>`

| | |
|---|---|
| **Module** | `garganttua-supply` |
| **Package** | `com.garganttua.core.supply.dsl` |
| **Contextual** | No |

Specialized builder for `FixedSupplier`.

**Factory methods:**
- `of(Supplied object, IClass<Supplied> suppliedClass)`
- `of(Supplied object)` — type inference
- `ofNullable(Supplied object, IClass<Supplied> suppliedClass)` — handles `null`

---

### 5.3 `NullSupplierBuilder<SuppliedType>`

| | |
|---|---|
| **Module** | `garganttua-supply` |
| **Package** | `com.garganttua.core.supply.dsl` |
| **Contextual** | No |

Builder for `NullSupplier`.

**Factory method:** `of(IClass<SuppliedType> suppliedClass)`

---

### 5.4 `ContextualSupplierBuilder<Supplied, Context>`

| | |
|---|---|
| **Module** | `garganttua-supply` |
| **Package** | `com.garganttua.core.supply.dsl` |
| **Contextual** | **Yes** |

Specialized builder for `ContextualSupplier`.

---

### 5.5 `BlockingSupplierBuilder<Supplied>`

| | |
|---|---|
| **Module** | `garganttua-supply` |
| **Package** | `com.garganttua.core.supply.dsl` |
| **Contextual** | No |

Builder for `BlockingSupplier` with timeout configuration.

**Factory method:** `of(BlockingQueue<Supplied> queue, IClass<Supplied> suppliedClass)`
**Config:** `withTimeout(Long timeoutMillis)`

---

### 5.6 `FutureSupplierBuilder<Supplied>`

| | |
|---|---|
| **Module** | `garganttua-supply` |
| **Package** | `com.garganttua.core.supply.dsl` |
| **Contextual** | No |

Builder for `FutureSupplier` with timeout configuration.

**Factory method:** `of(CompletableFuture<Supplied> future, IClass<Supplied> suppliedClass)`
**Config:** `withTimeout(Long timeoutMillis)`

---

## 6. Dependency Injection Suppliers

### 6.1 DI Interfaces (garganttua-commons)

#### `IBeanSupplier<Bean>`

| | |
|---|---|
| **Extends** | `ISupplier<Bean>`, `Dependent` |
| **Contextual** | No |
| **Associated annotation** | None directly (beans are annotated with `@jakarta.inject.Inject` etc.) |

Bean supplier with dependency tracking for circular dependency detection.

---

#### `IContextualBeanSupplier<Bean>`

| | |
|---|---|
| **Extends** | `IContextualSupplier<Bean, IInjectionContext>`, `IBeanSupplier<Bean>` |
| **Contextual** | **Yes** |
| **Context type** | `IInjectionContext` |
| **Associated annotation** | None |

Bean supplier requiring the injection context to resolve dependencies.

---

#### `IPropertySupplier<Property>`

| | |
|---|---|
| **Extends** | `ISupplier<Property>` |
| **Contextual** | No |
| **Associated annotation** | `@Property` |

Configuration property supplier.

---

#### `IInjectionContextSupply<Supplied>`

| | |
|---|---|
| **Type** | Functional interface, extends `IContextualSupply<Supplied, IInjectionContext>` |
| **Contextual** | **Yes** |
| **Context type** | `IInjectionContext` |
| **Associated annotation** | None |

Lambda/method reference for injection-context-dependent creation.

---

### 6.2 DI Implementations (garganttua-injection)

#### `ContextualBeanSupplier<Bean>`

| | |
|---|---|
| **Module** | `garganttua-injection` |
| **Package** | `com.garganttua.core.injection.context.beans` |
| **Implements** | `IContextualBeanSupplier<Bean>` |
| **Contextual** | **Yes** |
| **Context type** | `IInjectionContext` |

Primary bean supplier of the DI container. Uses a `BeanReference<Bean>` for flexible resolution (by name, provider, strategy, qualifier).

---

#### `BeanSupplier<Bean>`

| | |
|---|---|
| **Module** | `garganttua-injection` |
| **Package** | `com.garganttua.core.injection.context.beans` |
| **Extends** | `ContextualBeanSupplier<Bean>` |
| **Contextual** | No (uses thread-local static context `InjectionContext.context`) |

Non-contextual wrapper around `ContextualBeanSupplier`. Resolves beans via the static context rather than an explicitly passed context.

---

#### `PropertySupplier<Property>`

| | |
|---|---|
| **Module** | `garganttua-injection` |
| **Package** | `com.garganttua.core.injection.context.properties` |
| **Implements** | `IPropertySupplier<Property>` |
| **Contextual** | No (uses static context `InjectionContext.context`) |
| **Associated annotation** | `@Property` |

Provides property values from `IPropertyProvider` instances registered in the DI context. Resolution by key and optional provider.

---

### 6.3 DI Builders

#### `IBeanSupplierBuilder<Bean>` (interface)

| | |
|---|---|
| **Module** | `garganttua-commons` |
| **Extends** | `ISupplierBuilder<Bean, IBeanSupplier<Bean>>`, `Dependent` |

**Configuration methods:**
- `name(String name)` — bean name criteria
- `provider(String provider)` — specific provider/scope
- `strategy(BeanStrategy strategy)` — `singleton` or `prototype`
- `qualifier(IClass<? extends Annotation> qualifier)` — qualifier annotation
- `useStaticContext(boolean bool)` — use static vs explicit context

---

#### `BeanSupplierBuilder<Bean>` (implementation)

| | |
|---|---|
| **Module** | `garganttua-injection` |
| **Package** | `com.garganttua.core.injection.context.dsl` |
| **Contextual** | Depends on `useStaticContext` flag |

Builds `BeanSupplier` (if `useStaticContext=true`) or `ContextualBeanSupplier` (otherwise).

**Constructors:**
- `BeanSupplierBuilder(IClass<Bean> type)`
- `BeanSupplierBuilder(Optional<String> provider, BeanReference<Bean> query)`
- `BeanSupplierBuilder(BeanReference<Bean> query)`
- `BeanSupplierBuilder(String provider, BeanReference<Bean> query)`

---

#### `IPropertySupplierBuilder<Property>` (interface)

| | |
|---|---|
| **Module** | `garganttua-commons` |
| **Extends** | `ISupplierBuilder<Property, IPropertySupplier<Property>>` |

**Methods:** `key(String name)`, `provider(String provider)`

---

#### `PropertySupplierBuilder<Property>` (implementation)

| | |
|---|---|
| **Module** | `garganttua-injection` |
| **Package** | `com.garganttua.core.injection.context.dsl` |
| **Contextual** | No |

Builds `PropertySupplier` from a key and optional provider.

---

## 7. Supplier-Related Annotations

| Annotation | Target | Module | Associated supplier | Description |
|---|---|---|---|---|
| `@Fixed` | `FIELD`, `PARAMETER` | `garganttua-commons` | `FixedSupplier` | Injects a constant literal value. Attributes: `valueInt`, `valueDouble`, `valueFloat`, `valueLong`, `valueString`, `valueByte`, `valueShort`, `valueBoolean`, `valueChar` |
| `@Null` | `FIELD`, `PARAMETER` | `garganttua-commons` | `NullSupplier` | Explicitly injects `null` (absent optional dependency) |
| `@Property` | `FIELD`, `PARAMETER` | `garganttua-commons` | `PropertySupplier` | Injects a configuration property. Attribute: `value` (property key) |
| `@Prototype` | `TYPE`, `FIELD`, `PARAMETER` | `garganttua-commons` | `BeanSupplier` (strategy=prototype) | Requests a new instance on each injection |
| `@Provider` | `FIELD`, `PARAMETER` | `garganttua-commons` | `BeanSupplier` (with specified provider) | Specifies the provider/scope source for resolution |
| `@Resolver` | `TYPE` | `garganttua-commons` | None directly | Declares a class as an injectable element resolver for specific annotations |

All annotations carry `@Indexed` and `@Reflected` for compile-time indexing and reflective access.

---

## 8. Summary Table

| Supplier | Contextual | Context type | Dedicated builder | Annotation | Module |
|---|---|---|---|---|---|
| `FixedSupplier` | No | — | `FixedSupplierBuilder` | `@Fixed` | supply |
| `NewSupplier` | No | — | via `SupplierBuilder` | — | supply |
| `NullSupplier` | No | — | `NullSupplierBuilder` | `@Null` | supply |
| `NullableSupplier` | No | — | via `SupplierBuilder` (nullable) | — | supply |
| `ContextualSupplier` | **Yes** | Generic `Context` | `ContextualSupplierBuilder` | — | supply |
| `NewContextualSupplier` | **Yes** | `Void` | via `SupplierBuilder` | — | supply |
| `NullableContextualSupplier` | **Yes** | Generic `ContextType` | via `SupplierBuilder` (nullable+contextual) | — | supply |
| `BlockingSupplier` | No | — | `BlockingSupplierBuilder` | — | supply |
| `FutureSupplier` | No | — | `FutureSupplierBuilder` | — | supply |
| `BeanSupplier` | No* | `IInjectionContext` (static) | `BeanSupplierBuilder` | — | injection |
| `ContextualBeanSupplier` | **Yes** | `IInjectionContext` | `BeanSupplierBuilder` | — | injection |
| `PropertySupplier` | No* | `IInjectionContext` (static) | `PropertySupplierBuilder` | `@Property` | injection |

\* Uses the thread-local static injection context (`InjectionContext.context`) rather than an explicitly passed context.

---

## 9. Hierarchy Diagram

```
ISupplier<Supplied>
├── IContextualSupplier<Supplied, Context>
│   ├── ContextualSupplier<S, C>
│   ├── NewContextualSupplier<S>          (Context = Void)
│   ├── NullableContextualSupplier<S, C>  (decorator)
│   └── IContextualBeanSupplier<Bean>     (Context = IInjectionContext)
│       └── ContextualBeanSupplier<Bean>
│           └── BeanSupplier<Bean>        (static context)
├── FixedSupplier<S>
├── NewSupplier<S>
├── NullSupplier<S>
├── NullableSupplier<S>                   (decorator)
├── BlockingSupplier<S>
├── FutureSupplier<S>
├── IBeanSupplier<Bean>
│   └── (see IContextualBeanSupplier above)
└── IPropertySupplier<Property>
    └── PropertySupplier<P>

ISupplierBuilder<Supplied, Built>
├── ICommonSupplierBuilder<Supplied>
│   └── SupplierBuilder<S>               (unified builder)
├── FixedSupplierBuilder<S>
├── NullSupplierBuilder<S>
├── ContextualSupplierBuilder<S, C>
├── BlockingSupplierBuilder<S>
├── FutureSupplierBuilder<S>
├── IBeanSupplierBuilder<Bean>
│   └── BeanSupplierBuilder<Bean>
└── IPropertySupplierBuilder<Property>
    └── PropertySupplierBuilder<P>
```
