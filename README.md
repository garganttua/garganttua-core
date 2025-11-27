# 🧩 Garganttua Core

**Garganttua Core** is the foundation layer of the Garganttua ecosystem.  
It provides modular, low-level building blocks for reflection, dependency injection, execution, mapping, encryption, and runtime management —  
all designed for high performance, interoperability, and simplicity.

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua</groupId>
    <artifactId>garganttua-core</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `org.junit.jupiter:junit-jupiter-engine:test`
 - `org.slf4j:slf4j-api:provided`
 - `org.projectlombok:lombok:provided`

<!-- AUTO-GENERATED-END -->

## 🧠 Architecture Overview

Garganttua Core is organized into independent modules, each focusing on a specific technical concern:

| Module | Description |
|:--|:--|
| [**garganttua-reflection**](./garganttua-reflection/README.md) | Dynamic inspection and manipulation of classes, methods, and annotations. |
| [**garganttua-mapper**](./garganttua-mapper/README.md) | Flexible and declarative object-to-object mapping engine. |
| [**garganttua-executor**](./garganttua-executor/README.md) | Execution management, including methods and fallbacks task orchestration. |
| [**garganttua-crypto**](./garganttua-crypto/README.md) | Encryption, hashing, and secure key management utilities. |
| [**garganttua-native**](./garganttua-native/README.md) | Native integrations and abstraction layer for low-level operations. |
| [**garganttua-native-image-maven-plugin**](./garganttua-native-image-maven-plugin/README.md) | Custom Maven plugin to build native images (GraalVM support). |
| [**garganttua-dsl**](./garganttua-dsl/README.md) | Declarative description language and interpreter engine for Garganttua DSLs. |
| [**garganttua-injection**](./garganttua-injection/README.md) | Modular dependency injection container with context composition. |
| [**garganttua-runtime**](./garganttua-runtime/README.md) | Runtime context manager and lifecycle orchestration. |
| [**garganttua-bindings**](./garganttua-bindings/README.md) | Bridges and adapters for external IoC frameworks (Spring, Quarkus, Micronaut…). |
| [**garganttua-commons**](./garganttua-commons/README.md) | Shared components, interfaces, annotations, and exception hierarchy. |

---

## 🧭 Internal Dependency Rules

- All modules depend on [`garganttua-commons`](./garganttua-commons/README.md).
- High-level modules (`runtime`, `injection`, `bindings`) may depend on lower-level ones.
- Circular dependencies are strictly prohibited.
- Runtime and injection modules define the execution graph for all other components.

```mermaid
graph TD
    commons["garganttua-commons"]
    reflection["garganttua-reflection"]
    mapper["garganttua-mapper"]
    executor["garganttua-executor"]
    crypto["garganttua-crypto"]
    dsl["garganttua-dsl"]
    injection["garganttua-injection"]
    runtime["garganttua-runtime"]
    bindings["garganttua-bindings"]

    reflection --> commons
    mapper --> commons
    executor --> commons
    crypto --> commons
    dsl --> commons
    injection --> commons
    runtime --> injection
    runtime --> executor
    bindings --> runtime
    bindings --> injection

## License
MIT License