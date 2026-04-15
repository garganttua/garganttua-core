# 🧩 Garganttua Core

**Garganttua Core** is the foundational framework of the Garganttua ecosystem. It provides a comprehensive suite of modular, low-level building blocks designed for building high-performance, enterprise-grade Java applications. With a focus on modularity, type safety, and developer experience, Garganttua Core offers essential capabilities for reflection, dependency injection, workflow orchestration, mapping, encryption, and runtime management.

## ⚠️ Disclaimer

This documentation is partially generated using AI and may not always reflect the exact implementation.
If you find anything incorrect or unclear in this documentation or its sub-sections, please let me know at jeremy.colombet@garganttua.com.

If you need additional information, code snippets, examples, or further explanations, feel free to reach out.

## 🎯 Key Features

- **Modular Architecture** - 20+ independent modules, each solving a specific technical concern
- **Dependency Injection** - Lightweight IoC container with context management and bean lifecycle
- **Runtime Workflows** - Sophisticated orchestration engine with steps and exception handling
- **Script Engine** - Scripting language with REPL console, retry logic, synchronization, and expression evaluation
- **Workflow Orchestration** - High-level DSL for composing multi-stage pipelines with script generation
- **Compile-Time Indexing** - Annotation processor for zero-overhead annotation discovery at startup
- **Advanced Reflection** - Type-safe reflection utilities with annotation scanning and dynamic binding
- **Declarative DSL** - Fluent builder APIs for creating domain-specific languages
- **Condition Engine** - Expressive DSL for defining and evaluating complex runtime conditions
- **Object Mapping** - Declarative field-level mapping with bidirectional support
- **Execution Chains** - Chain-of-responsibility pattern with fallback handling
- **Lifecycle Management** - Thread-safe state transitions and component lifecycle hooks
- **Native Image Support** - GraalVM compatibility with Maven plugin for native compilation
- **Zero Reflection at Runtime** - Optional compile-time code generation for optimal performance
- **Cryptography** - Secure encryption, hashing, and key management utilities

## 💡 Philosophy

Garganttua Core is built on three core principles:

1. **Modularity** - Use only what you need. Each module is independently versioned and has minimal dependencies.
2. **Performance** - Designed for high-throughput, low-latency scenarios with optional native compilation.
3. **Developer Experience** - Intuitive APIs, comprehensive documentation, and type-safe interfaces.

## Installation
<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua</groupId>
    <artifactId>garganttua-core</artifactId>
    <version>2.0.0-ALPHA02</version>
</dependency>
```

### Actual version
2.0.0-ALPHA02

### Dependencies
 - `org.junit.jupiter:junit-jupiter-engine:test`
 - `org.slf4j:slf4j-api:provided`
 - `org.projectlombok:lombok:provided`

<!-- AUTO-GENERATED-END -->

## 🧠 Architecture Overview

Garganttua Core is organized into independent modules, each focusing on a specific technical concern:

<!-- AUTO-GENERATED-ARCHITECTURE-START -->
| Module | Description |
|:--|:--|
| [**garganttua-core**](././README.md) | Garganttua Core - Foundational Java framework for dependency injection, workflow orchestration, reflection utilities, and more. |
| \|- [**garganttua-aot**](./garganttua-aot/README.md) | Garganttua AOT (Ahead-of-Time) compilation support - parent module. |
| \|    \|- [**garganttua-aot-annotation-processor**](./garganttua-aot/garganttua-aot-annotation-processor/README.md) | Compile-time annotation processor for generating AOT class descriptors. |
| \|    \|- [**garganttua-aot-annotation-scanner**](./garganttua-aot/garganttua-aot-annotation-scanner/README.md) | Compile-time annotation scanner for AOT class descriptor generation. |
| \|    \|- [**garganttua-aot-commons**](./garganttua-aot/garganttua-aot-commons/README.md) | Common interfaces and types for Garganttua AOT (Ahead-of-Time) compilation support. |
| \|    \|- [**garganttua-aot-maven-plugin**](./garganttua-aot/garganttua-aot-maven-plugin/README.md) | Maven plugin for Garganttua AOT processing. |
| \|    \|- [**garganttua-aot-reflection**](./garganttua-aot/garganttua-aot-reflection/README.md) | AOT reflection descriptors and registry for Garganttua Core. |
| \|- [**garganttua-bindings**](./garganttua-bindings/README.md) | Modules providing bindings to external libs and frameworks. |
| \|    \|- [**garganttua-mutex-redis**](./garganttua-bindings/garganttua-mutex-redis/README.md) | Distributed mutex over redis. |
| \|    \|- [**garganttua-native**](./garganttua-bindings/garganttua-native/README.md) | Garganttua Native support - parent module. |
| \|    \|    \|- [**garganttua-native-annotation-processor**](./garganttua-bindings/garganttua-native/garganttua-native-annotation-processor/README.md) | Compile-time annotation processor for GraalVM native image configuration generation. |
| \|    \|    \|- [**garganttua-native-commons**](./garganttua-bindings/garganttua-native/garganttua-native-commons/README.md) | Low-level native integrations and system abstractions. |
| \|    \|    \|- [**garganttua-native-image-maven-plugin**](./garganttua-bindings/garganttua-native/garganttua-native-image-maven-plugin/README.md) | Maven plugin to build native images (GraalVM support). |
| \|    \|- [**garganttua-reflections**](./garganttua-bindings/garganttua-reflections/README.md) | Annotation scanner implementation based on org.reflections:reflections |
| \|    \|- [**garganttua-spring**](./garganttua-bindings/garganttua-spring/README.md) | Spring framework integration for Garganttua Core modules. |
| \|- [**garganttua-bootstrap**](./garganttua-bootstrap/README.md) | Bootstrap and application initialization framework. |
| \|- [**garganttua-commons**](./garganttua-commons/README.md) | Shared components, interfaces, annotations, and exceptions. |
| \|- [**garganttua-condition**](./garganttua-condition/README.md) | DSL to define, combine, and evaluate runtime conditions. |
| \|- [**garganttua-configuration**](./garganttua-configuration/README.md) | Multi-format configuration loading and builder population for Garganttua DSLs. |
| \|- [**garganttua-console**](./garganttua-console/README.md) | Interactive REPL console for Garganttua Script. |
| \|- [**garganttua-crypto**](./garganttua-crypto/README.md) | Encryption, hashing, and secure key management utilities. |
| \|- [**garganttua-dsl**](./garganttua-dsl/README.md) | Declarative language and builder framework for Garganttua DSLs. |
| \|- [**garganttua-execution**](./garganttua-execution/README.md) | Task execution, orchestration, and fallback handling engine. |
| \|- [**garganttua-expression**](./garganttua-expression/README.md) | Advanced expression language for object supplying. |
| \|    \|- [**bin**](./garganttua-expression/bin/README.md) | Advanced expression language for object supplying. |
| \|- [**garganttua-injection**](./garganttua-injection/README.md) | Dependency injection container with modular context support. |
| \|- [**garganttua-lifecycle**](./garganttua-lifecycle/README.md) | Abstract lifecycle management with thread-safe state transitions. |
| \|- [**garganttua-mapper**](./garganttua-mapper/README.md) | Declarative object-to-object mapping engine. |
| \|- [**garganttua-mutex**](./garganttua-mutex/README.md) | Thread-safe mutex synchronization with configurable acquisition strategies. |
| \|- [**garganttua-reflection**](./garganttua-reflection/README.md) | Advanced reflection utilities for classes, methods, and annotations. |
| \|- [**garganttua-runtime**](./garganttua-runtime/README.md) | Runtime context management and lifecycle orchestration. |
| \|- [**garganttua-runtime-reflection**](./garganttua-runtime-reflection/README.md) | Runtime reflection utilities for Garganttua Core. |
| \|- [**garganttua-script**](./garganttua-script/README.md) | Scripting language engine with variables, control flow, and expression evaluation. |
| \|    \|- [**bin**](./garganttua-script/bin/README.md) | Scripting language engine with variables, control flow, and expression evaluation. |
| \|- [**garganttua-script-maven-plugin**](./garganttua-script-maven-plugin/README.md) | Maven plugin to build JARs that can be included in Garganttua scripts (.gs files). Automatically adds Garganttua-Packages manifest attribute. |
| \|- [**garganttua-supply**](./garganttua-supply/README.md) | Object suppliers and contextual provisioning utilities. |
| \|- [**garganttua-workflow**](./garganttua-workflow/README.md) | Workflow orchestration module - DSL builder for chaining scripts with dynamic script generation |





















<!-- AUTO-GENERATED-ARCHITECTURE-STOP -->

## 📚 Module Categories

### Foundation Modules

These modules provide the core infrastructure used by all other modules:

- **[garganttua-commons](./garganttua-commons/README.md)** - The central dependency containing shared interfaces, annotations, exceptions, and utilities. Over 100 files organized in 13 packages covering DI, runtime, reflection, and common patterns.

- **[garganttua-dsl](./garganttua-dsl/README.md)** - Builder pattern framework providing `IBuilder` and `ILinkedBuilder` interfaces for creating fluent, hierarchical DSLs throughout the ecosystem.

- **[garganttua-lifecycle](./garganttua-lifecycle/README.md)** - Thread-safe lifecycle management with hooks for `onStart()`, `onStop()`, `onInit()`, `onReload()`, and `onFlush()`.

- **[garganttua-supply](./garganttua-supply/README.md)** - Lazy object provisioning framework with suppliers for deferred object creation and contextual provisioning.

### Core Framework Modules

These modules implement the primary framework capabilities:

- **[garganttua-injection](./garganttua-injection/README.md)** - Lightweight dependency injection container supporting singleton and prototype scopes, property injection, provider methods, and child contexts.

- **[garganttua-runtime](./garganttua-runtime/README.md)** - Workflow orchestration engine with sequential step execution, conditional steps, exception handling, and fallback mechanisms. Supports both annotation-based and programmatic definitions.

- **[garganttua-reflection](./garganttua-reflection/README.md)** - Advanced reflection utilities providing annotation scanning, object graph navigation via dot-notation paths, and type-safe field/method/constructor binding.

- **[garganttua-execution](./garganttua-execution/README.md)** - Chain-of-responsibility implementation for sequential task processing with optional fallback handlers and configurable exception propagation.

- **[garganttua-condition](./garganttua-condition/README.md)** - Expressive DSL for building and evaluating runtime conditions with full boolean algebra support (AND, OR, XOR, NAND, NOR), custom predicates, and property extraction.

- **[garganttua-expression](./garganttua-expression/README.md)** - ANTLR4-based expression language supporting function calls, method invocations, constructors, and type-safe evaluation with supplier integration.

- **[garganttua-script](./garganttua-script/README.md)** - Scripting language engine with retry logic, synchronization primitives, and composable runtime steps with exception handling.

- **[garganttua-console](./garganttua-console/README.md)** - Interactive REPL console for Garganttua Script with JLine terminal support, tab completion, and session variables.

- **[garganttua-workflow](./garganttua-workflow/README.md)** - High-level orchestration DSL for composing multi-stage pipelines. Generates Garganttua Script code from a fluent builder API with automatic variable management, input/output mapping, and runtime stage filtering.

### Utility Modules

Specialized modules for specific technical needs:

- **[garganttua-mapper](./garganttua-mapper/README.md)** - Declarative object-to-object mapping with annotation-based rules, bidirectional mapping, and field-level transformation support.

- **[garganttua-crypto](./garganttua-crypto/README.md)** - Cryptography utilities for encryption, decryption, hashing, and secure key management.

### Integration Modules

Modules providing integration with external frameworks and tools:

- **[garganttua-bindings](./garganttua-bindings/README.md)** - Parent module for external framework integrations.
  - **[garganttua-reflections](./garganttua-bindings/garganttua-reflections/README.md)** - Annotation scanner implementation based on the Reflections library.
  - **[garganttua-spring](./garganttua-bindings/garganttua-spring/README.md)** - Spring Framework integration enabling Garganttua components within Spring applications.

### Build & Native Modules

Modules for build-time tooling and native compilation:

- **[garganttua-native-annotation-processor](./garganttua-bindings/garganttua-native/garganttua-native-annotation-processor/)** - Compile-time annotation indexing processor. Generates index files in `META-INF/garganttua/index/` for zero-overhead annotation discovery at runtime. Also generates direct-call binder classes for `@Expression` methods and DI bean constructors (toggleable via `-Dgarganttua.direct.binders=false`).

- **[garganttua-native-commons](./garganttua-bindings/garganttua-native/garganttua-native-commons/README.md)** - Native image shared utilities and system abstractions.

- **[garganttua-native-image-maven-plugin](./garganttua-bindings/garganttua-native/garganttua-native-image-maven-plugin/README.md)** - Maven plugin for building GraalVM native images with automatic reflection configuration generation.

- **[garganttua-script-maven-plugin](./garganttua-script-maven-plugin/README.md)** - Maven plugin for packaging JARs for dynamic inclusion in scripts via `include()`.

## 🚀 Quick Start

### Basic Dependency Injection

```java
import com.garganttua.core.injection.*;

// Create a DI context
IInjectionContext context = InjectionContextBuilder.builder()
    .withBean(UserService.class)
    .withBean(UserRepository.class)
    .withProperty("app.name", "MyApp")
    .withProperty("app.version", "1.0.0")
    .build();

// Start lifecycle
context.onStart();

// Retrieve and use beans
UserService service = context.getBean(UserService.class);
service.registerUser("john.doe@example.com");

// Shutdown
context.onStop();
```

### Runtime Workflow

```java
import com.garganttua.core.runtime.*;
import com.garganttua.core.runtime.annotations.*;

@RuntimeDefinition(input = Order.class, output = OrderResult.class)
@Named("order-processing")
public class OrderProcessingRuntime {

    @Steps
    public List<Class<?>> steps = List.of(
        ValidateOrderStep.class,
        ProcessPaymentStep.class,
        SendConfirmationStep.class
    );
}

@Step
@Named("validate-order")
public class ValidateOrderStep {

    @Operation(abortOnUncatchedException = true)
    public void validateOrder(@Input Order order) {
        if (order.getAmount() <= 0) {
            throw new RuntimeException("Invalid order amount");
        }
    }
}

// Execute runtime
IRuntime<Order, OrderResult> runtime = ...;
IRuntimeResult<Order, OrderResult> result = runtime.execute(myOrder).orElseThrow();
```

### Condition Evaluation

```java
import static com.garganttua.core.condition.Conditions.*;
import static com.garganttua.core.supply.dsl.FixedSupplierBuilder.*;

// Define complex business rule
ICondition eligibilityCondition = and(
    isNotNull(userSupplier),
    custom(userSupplier, User::getAge, age -> age >= 18),
    custom(userSupplier, User::getCountry, country -> country.equals("US")),
    or(
        custom(userSupplier, User::isPremium, premium -> premium),
        custom(userSupplier, User::getAccountAge, age -> age > 365)
    )
).build();

// Evaluate
boolean isEligible = eligibilityCondition.evaluate();
```

### Executor Chain

```java
import com.garganttua.core.execution.*;

// Build processing chain
ExecutorChain<OrderRequest> chain = new ExecutorChain<>();

chain.addExecutor((order, next) -> {
    validateOrder(order);
    next.execute(order);
});

chain.addExecutor((order, next) -> {
    enrichOrder(order);
    next.execute(order);
}, (order, next) -> {
    // Fallback: use default values
    useDefaults(order);
    next.execute(order);
});

chain.addExecutor((order, next) -> {
    persistOrder(order);
    next.execute(order);
});

// Execute
chain.execute(myOrder);
```

### Object Mapping

```java
import com.garganttua.core.mapper.*;
import com.garganttua.core.mapper.annotations.*;

@ObjectMappingRule(source = UserDTO.class, target = User.class)
public class UserMapper {

    @FieldMappingRule(sourceField = "fullName", targetField = "name")
    public void mapName(UserDTO dto, User user) {
        user.setName(dto.getFullName().toUpperCase());
    }

    @FieldMappingRule(sourceField = "birthDate", targetField = "age")
    public void calculateAge(UserDTO dto, User user) {
        user.setAge(calculateAgeFromBirthDate(dto.getBirthDate()));
    }
}

// Use mapper
IMapper<UserDTO, User> mapper = new Mapper<>(UserMapper.class);
User user = mapper.map(userDTO);
```

## 🧭 Internal Dependencies

The module dependency structure follows these principles:

- **All modules depend on `garganttua-commons`** - The commons module provides shared interfaces and utilities.
- **No circular dependencies** - The dependency graph is strictly acyclic to prevent coupling issues.
- **Layered architecture** - Lower-level modules (dsl, supply, lifecycle) are used by higher-level ones (injection, runtime).
- **Optional integrations** - Binding modules are optional and can be excluded if not needed.

### Dependency Layers

1. **Foundation Layer** - commons, dsl, supply, lifecycle, mutex
2. **Infrastructure Layer** - reflection, runtime-reflection, condition, execution, crypto, configuration
3. **Framework Layer** - injection, runtime, mapper, expression, bootstrap
4. **Application Layer** - script, console, workflow
5. **Integration Layer** - bindings (reflections, spring, native, mutex-redis)
6. **Build Layer** - native-image-maven-plugin, script-maven-plugin, annotation-processor
7. **AOT Layer** (WIP) - aot-commons, aot-reflection, aot-annotation-scanner, aot-annotation-processor, aot-maven-plugin

<!-- AUTO-GENERATED-DEPENDENCIES-GRAPH-START -->
```mermaid
graph TD

```
<!-- AUTO-GENERATED-DEPENDENCIES-GRAPH-STOP -->

## 🔧 Technology Stack

### Build & Runtime

| Technology | Version | Description |
|:--|:--|:--|
| **Java 21** | 21 | Modern Java with records, pattern matching, and sealed types |
| **Maven** | 3.8+ | Build automation, dependency management, and multi-module reactor |
| **GraalVM** | - | Native image compilation for optimal startup and memory footprint |

### Core Dependencies

| Library | Version | Scope | Used By |
|:--|:--|:--|:--|
| **[Lombok](https://projectlombok.org/)** | 1.18.36 | provided | All modules - annotation-based code generation for reducing boilerplate |
| **[SLF4J](https://www.slf4j.org/)** | 2.0.17 | provided | All modules - logging facade for flexible logging implementation |
| **[Logback](https://logback.qos.ch/)** | 1.5.16 | compile/test | `script`, `console` - logging implementation for runtime and tests |
| **[ANTLR4](https://www.antlr.org/)** | 4.13.0 | compile | `expression`, `script` - parser generator for grammar-based language processing |
| **[JLine3](https://github.com/jline/jline3)** | 3.25.1 | compile | `console` - terminal handling for the interactive REPL (line editing, history, completion) |
| **[Jackson Databind](https://github.com/FasterXML/jackson-databind)** | 2.15.2 | compile | `native` - JSON serialization for GraalVM reflection configuration |
| **[Javatuples](https://www.javatuples.org/)** | 1.2 | compile | `reflection` - type-safe tuple classes for multi-value returns |
| **[UUID Creator](https://github.com/f4b6a3/uuid-creator)** | 5.0.0 | compile | `runtime` - fast, spec-compliant UUID generation |
| **[Reflections](https://github.com/ronmamo/reflections)** | 0.10.2 | compile | `reflections` binding - runtime annotation and classpath scanning |

### Standards & Specifications

| Library | Version | Scope | Used By |
|:--|:--|:--|:--|
| **[javax.inject (JSR-330)](https://javax-inject.github.io/javax-inject/)** | 1 | compile | `commons`, `bootstrap`, `script-maven-plugin` - standard DI annotations |
| **[Jakarta Annotations](https://jakarta.ee/specifications/annotations/)** | 2.1.1 | compile | `commons`, `bootstrap` - standard lifecycle annotations (`@PostConstruct`, etc.) |

### Optional Integrations

| Library | Version | Scope | Used By |
|:--|:--|:--|:--|
| **[Spring Framework](https://spring.io/projects/spring-framework)** | 6.2.1 | provided | `spring` binding - Spring context and core integration |
| **[Red Utils](https://github.com/siahsang/red-utils)** | 1.0.4 | compile | `mutex-redis` binding - Redis-based distributed mutex |

### Testing

| Library | Version | Scope | Used By |
|:--|:--|:--|:--|
| **[JUnit 5](https://junit.org/junit5/)** | 5.9.1 | test | All modules - modern testing framework with parameterized tests and extensions |
| **[JFreeChart](https://www.jfree.org/jfreechart/)** | 1.5.4 | test | `runtime` - chart generation for performance test reporting |
| **[OpenPDF](https://github.com/LibrePDF/OpenPDF)** | 1.3.40 | test | `runtime` - PDF export for performance test reports |

### Maven Plugin Development

| Library | Version | Scope | Used By |
|:--|:--|:--|:--|
| **Maven Plugin API** | 3.0 | provided | `native-image-maven-plugin`, `script-maven-plugin` |
| **Maven Core** | 3.8.6 | provided | `native-image-maven-plugin`, `script-maven-plugin` |
| **Maven Plugin Annotations** | 3.5 | provided | `native-image-maven-plugin`, `script-maven-plugin` |

## 📖 Use Cases

### Enterprise Applications

- **Microservices** - Lightweight DI and runtime orchestration for service composition
- **Batch Processing** - Executor chains for ETL pipelines with fallback handling
- **Business Rules** - Condition engine for complex validation and decision logic
- **Integration Hubs** - Runtime workflows for multi-system integration

### API Development

- **Request Processing** - Executor chains for request validation, enrichment, and response transformation
- **Authentication/Authorization** - Condition-based access control with contextual evaluation
- **Data Transformation** - Object mapping for DTO-to-entity and API response serialization

### Cloud-Native Applications

- **Serverless Functions** - Fast startup with GraalVM native images
- **Container Optimization** - Minimal memory footprint for high-density deployments
- **Configuration Management** - Property injection and context-aware provisioning

## 🎓 Learning Path

1. **Start with Commons** - Understand the shared interfaces and patterns used throughout.
2. **Explore Injection** - Learn the DI container basics and bean management.
3. **Add Execution** - Chain executors for sequential processing with fallbacks.
4. **Introduce Conditions** - Define business rules declaratively with the condition DSL.
5. **Build Workflows** - Orchestrate complex processes with the runtime module.
6. **Write Scripts** - Compose runtime steps using the scripting language and REPL console.
7. **Build Workflows** - Orchestrate multi-stage pipelines with the workflow DSL.
8. **Integrate Bindings** - Connect with Spring or use advanced reflection features.
9. **Optimize with Native** - Compile to native images for production deployment.

## 🛠️ Development

### Building from Source

```bash
# Clone the repository
git clone https://github.com/garganttua/garganttua-core.git
cd garganttua-core

# Build all modules
mvn clean install

# Build specific module
cd garganttua-injection
mvn clean install

# Run tests
mvn test

# Generate code coverage
mvn jacoco:report
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ExecutorChainTest

# Run with coverage
mvn clean test jacoco:report
```

### Building Native Image

```bash
# Using the native-image-maven-plugin
cd your-application
mvn package -Pnative

# Manual GraalVM build
native-image -jar target/myapp.jar
```

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. **Fork the repository** and create a feature branch
2. **Write tests** for all new functionality
3. **Follow code style** - Use the provided formatter configuration
4. **Document your changes** - Update READMEs and JavaDocs
5. **Submit a pull request** with a clear description

## 📄 License

This project is distributed under the MIT License. See individual module READMEs for specific details.

## 👥 Authors

- **Jérémy Colombet** - [jeremy.colombet@garganttua.com](mailto:jeremy.colombet@garganttua.com)

## 🔗 Resources

- **GitHub Repository**: https://github.com/garganttua/garganttua-core
- **Maven Central**: https://maven.pkg.github.com/garganttua/garganttua-core
- **Issue Tracker**: https://github.com/garganttua/garganttua-core/issues
- **Documentation**: See individual module READMEs

## 🗺️ Roadmap

### Version 2.0 (Current - ALPHA)

- Core modules stabilization
- Comprehensive documentation
- Performance benchmarks
- Cryptographic features development
- Native image support

---

**Built with ❤️ by the Garganttua team**
