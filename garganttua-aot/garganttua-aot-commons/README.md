# Garganttua AOT Commons

## Description

Common interfaces and types for Garganttua AOT (Ahead-of-Time) compilation support. This module defines the shared contracts used by other AOT submodules for class descriptor generation, reflection metadata, and annotation scanning.

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-aot-commons</artifactId>
    <version>2.0.0-ALPHA02</version>
</dependency>
```

### Actual version
2.0.0-ALPHA02

### Dependencies
 - `com.garganttua.core:garganttua-commons`

<!-- AUTO-GENERATED-END -->

## Core Concepts

This module exposes **only interfaces** — no implementation. The contracts
fall into two groups:

### 1. AOT registry & generated descriptors (used by the annotation processor)

- **`IAOTRegistry`** — the singleton registry where descriptors live at
  runtime. Looked up by FQN string. Implementation: `AOTRegistry.getInstance()`.
- **`IAOTClassDescriptor`** — base type implemented by every generated
  `AOTClass_*`. Carries the precomputed reflection metadata for a single class.
- **`IAOTSelfRegistering`** — marker extended by `IAOTClassDescriptor` so
  generated descriptors are discoverable via `ServiceLoader`. The annotation
  processor writes one `META-INF/services/com.garganttua.core.aot.commons.IAOTSelfRegistering`
  per consumer JAR.
- **`IAOTClassBuilder<T>`** — fluent builder used by the processor (and by
  advanced consumers shipping plugin types) to construct rich descriptors
  with explicit member configuration.
- **`AOTRegistry`** / **`AOTException`** / **`AOTMetadataConstants`** —
  helper singleton + exception + canonical file-format constants.

You don't normally touch these directly — the annotation processor and the
runtime provider read/write them under the hood. They're documented here
so downstream consumers can write advanced extensions.

### 2. Extension SPI (used by higher-layer frameworks)

The garganttua core can't know about types declared in frameworks built
on top of it (garganttua-api, garganttua-events, your in-house DSL). Two
companion interfaces let each framework pre-register its public types at
cold start:

#### `IAOTInfrastructureSeed` — the contract you implement

```java
public interface IAOTInfrastructureSeed {
    void seed(IAOTSeedContext context);
}
```

Discovered via `ServiceLoader.load(IAOTInfrastructureSeed.class)`,
sorted by `@jakarta.annotation.Priority` (higher first, default 0),
called once per JVM during the AOT provider's static init.

#### `IAOTSeedContext` — the toolkit handed to your seed

```java
public interface IAOTSeedContext {
    void registerClass(Class<?> type);     // synthesizes + registers
    void registerInterface(Class<?> type); // same, forces INTERFACE modifier
    IAOTRegistry registry();               // escape hatch for pre-built descriptors
}
```

The context masks the `AOTClass` synthesis machinery behind two trivial
methods. Your seed depends only on `garganttua-aot-commons` — no
transitive pull on `aot-reflection`, no need to construct descriptors by hand.

### Why two interfaces

- `IAOTInfrastructureSeed` is the **inversion-of-control contract**: each
  framework commits to "I will register my types when asked."
- `IAOTSeedContext` is the **toolkit**: it hides the synthesis details so
  the seed needs zero knowledge of `AOTClass`, its 20-field constructor,
  or the JVM modifier flag conventions.

Together they form the same pattern as JDK `ServiceLoader` for
`IReflectionProvider` / `IAnnotationScanner` that the bootstrap uses —
decentralisation through SPI.

## Usage

```bash
mvn clean install -pl garganttua-aot/garganttua-aot-commons
```

### Writing an extension seed

You typically write a seed when:

- You ship a reusable framework on top of garganttua-core.
- You expose public interfaces or annotations that downstream user
  `@Reflected` classes will reference.
- You want consumers to get rich descriptors for your public types
  without having to hand-seed them.

**Step 1 — declare the dep** (optional so consumers in runtime-only mode
don't pull AOT machinery):

```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-aot-commons</artifactId>
    <version>${garganttua.core.version}</version>
    <optional>true</optional>
</dependency>
```

**Step 2 — implement the seed**:

```java
package com.example.myframework.aot;

import com.garganttua.core.aot.commons.IAOTInfrastructureSeed;
import com.garganttua.core.aot.commons.IAOTSeedContext;

import com.example.myframework.IMyPublicContract;
import com.example.myframework.IMyOtherContract;
import com.example.myframework.MyPublicAnnotation;

public class MyFrameworkInfrastructureSeed implements IAOTInfrastructureSeed {

    @Override
    public void seed(IAOTSeedContext ctx) {
        ctx.registerInterface(IMyPublicContract.class);
        ctx.registerInterface(IMyOtherContract.class);
        ctx.registerClass(MyPublicAnnotation.class);
    }
}
```

**Step 3 — register the seed via ServiceLoader**:

`src/main/resources/META-INF/services/com.garganttua.core.aot.commons.IAOTInfrastructureSeed`

```
com.example.myframework.aot.MyFrameworkInfrastructureSeed
```

That's it. Any consumer that pulls your framework + the AOT pipeline
will discover and run your seed at cold start.

### Ordering between competing seeds

If you need your seed to run before/after another, annotate with
`@jakarta.annotation.Priority`:

```java
@Priority(50)                       // higher first; default 0
public class MyFrameworkInfrastructureSeed implements IAOTInfrastructureSeed {
    …
}
```

Seeds are tolerated for partial failure: an exception thrown by one
seed does not prevent the others from running (logged to stderr).

### Reference seeds shipped inside this repo

Two modules already use this SPI as an integration test:

- `garganttua-configuration` → `ConfigurationInfrastructureSeed` for
  `@Configurable`, `@ConfigProperty`, `@ConfigIgnore`, `@ConfigurationFormat`.
- `garganttua-observability` → `ObservabilityInfrastructureSeed` for
  `@Observer`.

Both declare `garganttua-aot-commons` as `<optional>true</optional>` —
the seed only loads if AOT is on the consumer's classpath.

## Tips and best practices

- **Idempotency is free** — `registerClass` / `registerInterface` skip
  duplicates (first registration wins). You don't need to track what
  you've already registered.
- **Prefer `registerInterface` only when needed** — most types Java
  considers an interface already have the `INTERFACE` modifier set on the
  class file. The forced flag is for type-by-convention markers where
  the JVM modifier doesn't match the semantic expectation.
- **Don't seed JDK types you don't own** — `CoreInfrastructureSeed`
  covers JDK collections and primitives. Don't duplicate.
- **Don't seed user `@Reflected` types** — they self-register via the
  annotation processor's generated `AOTClass_*`. Seeding them would race
  with the processor.
- **Stick to public-surface types** — interfaces, annotations, and
  contracts in your framework's public API. Internal helpers don't need
  seeding; they're either reachable via the user-side processor or
  handled by the runtime provider's fallback synthesis.

## License
This module is distributed under the MIT License.
