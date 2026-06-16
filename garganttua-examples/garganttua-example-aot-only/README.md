# Garganttua Example — AOT only

## Description

A minimal, runnable consumer that answers one question: **can a project use the
Garganttua AOT reflection stack on its own, without dragging in the rest of the
framework?**

Short answer: **yes** — the runtime footprint is just the AOT starter plus the
reflection facade. No dependency injection, no runtime/workflow engine, no
expression language, no bootstrap. The example resolves a `@Reflected` record
through the compile-time-generated AOT descriptor and lists its fields, methods
and constructors, then scans for `@Reflected` classes — all with zero runtime
classpath scanning.

## Installation

This is a runnable example, not a published artifact — it is intentionally kept
out of the root reactor, so the auto-generated coordinates block stays empty. See
[Usage](#usage) to build and run it.

<!-- AUTO-GENERATED-START -->
<!-- AUTO-GENERATED-END -->

## Core Concepts

### The dependency footprint

```
garganttua-example-aot-only
├── garganttua-starter-aot        (pom)   → AOT reflection provider + annotation scanner
│   ├── garganttua-aot-reflection         → AOTReflectionProvider, AOTRegistry, descriptors
│   │   └── garganttua-aot-commons
│   └── garganttua-aot-annotation-scanner → AOTAnnotationScanner (reads compile-time index)
└── garganttua-reflection                 → ReflectionBuilder / IReflection facade
    ├── garganttua-commons                → IClass, IField, IMethod, @Reflected, …
    ├── garganttua-dsl
    └── garganttua-supply
```

That is the **entire** transitive closure. None of `injection`, `runtime`,
`expression`, `bootstrap`, `workflow`, `condition` or `execution` is present.

### Why `garganttua-reflection` is needed next to the starter

The starter ships the AOT **provider** and **scanner**, but they are designed to
plug **into** the `IReflection` facade. The generated `AOTClass_Product`
descriptor resolves its members' types through the static `IClass.getClass(...)`
facade, which only works once an `IReflection` has been installed via
`IClass.setReflection(...)`. `ReflectionBuilder` — the composition root — lives in
`garganttua-reflection`.

Applications built on `garganttua-bootstrap` get this wiring for free (Bootstrap
discovers the providers via `ServiceLoader` and installs the facade). A
no-bootstrap app like this one wires it by hand in three lines:

```java
IReflection aot = ReflectionBuilder.builder()
        .withProvider(new AOTReflectionProvider(), 20)
        .withScanner(new AOTAnnotationScanner(), 20)
        .build();
IClass.setReflection(aot);
```

### The compile-time half

`@Reflected` on `Product` is the whole contract. At compile time the
`garganttua-aot-annotation-processor` (wired in the root parent's
`maven-compiler-plugin`) emits, **only when `-Agarganttua.direct.binders=true`**:

- `target/generated-sources/annotations/.../AOTClass_Product.java` — the rich
  descriptor (fields, methods, constructors).
- `META-INF/services/com.garganttua.core.aot.commons.IAOTSelfRegistering` — the SPI
  entry that force-loads the descriptor into the `AOTRegistry` at cold start.
- `META-INF/garganttua/index/...Reflected` — the annotation index the scanner reads.

This module opts in with `<garganttua.direct.binders>true</garganttua.direct.binders>`
in its POM.

## Usage

Build it (the AOT starter and annotation processor must be installed first — a
root `mvn install` does that):

```bash
# from the repository root, build the upstream pieces once:
mvn -DskipTests install -pl :garganttua-starter-aot,:garganttua-aot-annotation-processor -am

# then build + run the example (requires JDK 25):
mvn -f garganttua-examples/pom.xml install
mvn -f garganttua-examples/pom.xml -pl garganttua-example-aot-only exec:java
```

Expected output:

```
=== Garganttua AOT-only example ===

Resolved descriptor : com.garganttua.example.aot.Product
Source              : AOT-generated (rich members)

-- Fields --
  String name
  int quantity
  double unitPrice

-- Methods --
  String name()
  ... (record accessors + equals/hashCode/toString) ...
  double total()

-- Constructors --
  <init> with 3 parameter(s)

-- Annotation scan: classes annotated with @Reflected --
  com.garganttua.example.aot.Product
  ...

Done — AOT reflection only, zero DI / runtime / bootstrap / workflow.
```

## Tips and best practices

- **Drop the descriptor and you fall back gracefully.** Remove `@Reflected` (or set
  `direct.binders=false`) and `getDeclaredFields()` returns empty — the provider
  synthesizes a type-identity-only descriptor instead of throwing. Member-level
  introspection is what requires the generated descriptor.
- **For native-image**, swap `garganttua-starter-aot` for `garganttua-starter-native`
  and add the `native-maven-plugin` — see `garganttua-starters/README.md`.
- **For a faster dev loop** where you don't want to re-run the processor, use
  `garganttua-starter-hybrid`: AOT wins for processed types, runtime reflection
  transparently covers the rest.

## License
This module is distributed under the MIT License.
