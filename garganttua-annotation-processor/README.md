# Garganttua Annotation Processor

## Description

The **garganttua-annotation-processor** module provides compile-time annotation indexing for the Garganttua framework. Instead of expensive classpath scanning at runtime, it generates index files during compilation that enable O(1) annotation lookups at startup.

**Key Features:**
- **Compile-Time Indexing** - Generates annotation indices during `javac` compilation
- **Direct Binder Generation** - Generates direct-call binder classes for zero-reflection method/constructor invocation
- **Zero Runtime Scanning** - No classpath scanning needed at application startup
- **Incremental Compilation** - Merges with existing index files during recompilation
- **JSR-330 Support** - Automatically indexes `javax.inject.*` and `jakarta.inject.*` annotations
- **Fat JAR Support** - Index files merge correctly via Maven Shade `AppendingTransformer`
- **Hybrid Scanning** - Optional fallback to runtime scanner (e.g., Reflections library) for non-indexed annotations
- **Thread-Safe** - `ConcurrentHashMap` caching with lazy loading
- **Toggleable** - Direct binder generation can be enabled/disabled via Maven property

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-annotation-processor</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `com.garganttua.core:garganttua-commons`

<!-- AUTO-GENERATED-END -->

## How It Works

### 1. Mark Annotations with `@Indexed`

Any annotation marked with `@Indexed` will be automatically indexed at compile time:

```java
import com.garganttua.core.reflection.annotations.Indexed;

@Indexed
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Expression {
    String name();
}
```

### 2. Compile-Time Processing

During `javac`, the `IndexedAnnotationProcessor` discovers all elements annotated with indexed annotations and generates index files:

```
target/classes/META-INF/garganttua/index/com.garganttua.core.expression.annotations.Expression
```

### 3. Runtime Lookup

At runtime, `AnnotationIndex` loads pre-computed indices from the classpath for instant lookups:

```java
import com.garganttua.core.annotation.processor.AnnotationIndex;

AnnotationIndex index = new AnnotationIndex();

// Find all classes with @Expression
Set<Class<?>> classes = index.getClassesWithAnnotation(
    Expression.class, "com.garganttua");

// Find all methods with @Expression
Set<Method> methods = index.getMethodsWithAnnotation(
    Expression.class, "com.garganttua");
```

## Index File Format

Index files are generated in `META-INF/garganttua/index/`, one file per annotation (named by fully qualified annotation class name).

Each file contains one entry per line:

```
C:com.garganttua.core.mutex.redis.RedisMutexFactory
M:com.garganttua.core.expression.functions.Expressions#concatenate(java.lang.Object,java.lang.Object)
M:com.garganttua.core.script.functions.ScriptFunctions#retry(int,long,com.garganttua.core.supply.ISupplier)
```

**Prefixes:**
- `C:` - Class entry (includes interfaces, enums, records)
- `M:` - Method entry with full signature: `ClassName#methodName(ParamType1,ParamType2)`

## Key Classes

### IndexedAnnotationProcessor

The compile-time annotation processor (`@SupportedAnnotationTypes("*")`, `@SupportedSourceVersion(RELEASE_21)`):
- Discovers annotations marked with `@Indexed`
- Automatically indexes JSR-330 annotations (`javax.inject.*`, `jakarta.inject.*`)
- Generates index files at end of compilation (`processingOver()`)
- Supports incremental compilation by merging with existing index files
- Registered via SPI: `META-INF/services/javax.annotation.processing.Processor`

### AnnotationIndex

Runtime implementation of `IAnnotationIndex`:
- Lazy loading on first access
- Thread-safe `ConcurrentHashMap` caching
- Loads from all classpath JARs via `ClassLoader.getResources()`
- Supports package-prefix filtering
- Graceful handling of missing classes/methods

### IndexedAnnotationScanner

Implements `IAnnotationScanner` with a hybrid approach:
- Primary: Uses compile-time indices (fast)
- Fallback: Optional delegate to runtime scanner (e.g., `ReflectionsAnnotationScanner`)

```java
// Index-only (no fallback)
IAnnotationScanner scanner = new IndexedAnnotationScanner();

// Hybrid with Reflections fallback
IAnnotationScanner scanner = new IndexedAnnotationScanner(
    new ReflectionsAnnotationScanner());
```

## Maven Configuration

The annotation processor is configured in the parent POM's `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <compilerArgs>
            <arg>-parameters</arg>
            <!-- Controls direct binder generation (default: true) -->
            <arg>-Agarganttua.direct.binders=${garganttua.direct.binders}</arg>
        </compilerArgs>
        <annotationProcessorPaths>
            <path>
                <groupId>com.garganttua.core</groupId>
                <artifactId>garganttua-annotation-processor</artifactId>
                <version>${project.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

For fat JARs (e.g., `garganttua-script`), use `AppendingTransformer` to merge index files:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <configuration>
        <transformers>
            <!-- Annotation index files -->
            <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                <resource>META-INF/garganttua/index/com.garganttua.core.expression.annotations.Expression</resource>
            </transformer>
            <!-- Generated direct binder index files -->
            <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                <resource>META-INF/garganttua/generated-binders</resource>
            </transformer>
            <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                <resource>META-INF/garganttua/generated-constructor-binders</resource>
            </transformer>
        </transformers>
    </configuration>
</plugin>
```

## Indexed Annotations in the Framework

The following annotations are marked with `@Indexed`:

| Annotation | Module | Target |
|:--|:--|:--|
| `@Expression` | garganttua-commons | Methods |
| `@Prototype` | garganttua-commons | Types |
| `@Provider` | garganttua-commons | Types |
| `@Mutex` | garganttua-commons | Types |
| `@MutexFactory` | garganttua-commons | Types |
| `@ObjectMappingRule` | garganttua-commons | Types |
| `@NativeConfigurationBuilder` | garganttua-commons | Types |
| `@Bootstrap` | garganttua-commons | Types |

Additionally, JSR-330 annotations are indexed automatically: `@Inject`, `@Named`, `@Singleton`, `@Qualifier`.

## Direct Binder Generation (Zero Reflection at Runtime)

In addition to annotation indexing, this module provides **compile-time code generation** for direct-call binders. Instead of using `Method.invoke()` or `Constructor.newInstance()` at runtime, the `DirectBinderGenerator` annotation processor generates Java classes that call target methods and constructors directly, eliminating reflection overhead on hot paths.

### How It Works

```
Compile-time (javac):
  DirectBinderGenerator
    ├── scans @Expression static methods
    │   └── generates DirectBinder_Expressions_concatenate_Object_Object.java
    │       (implements IMethodBinder, calls Expressions.concatenate() directly)
    │
    ├── scans @Prototype / @Singleton classes
    │   └── generates DirectConstructorBinder_MyBean_String.java
    │       (implements IConstructorBinder, calls new MyBean(...) directly)
    │
    ├── scans @Inject constructors
    │   └── generates DirectConstructorBinder_MyService_Repo_Config.java
    │
    └── writes index files:
        ├── META-INF/garganttua/generated-binders             (method binders)
        └── META-INF/garganttua/generated-constructor-binders  (constructor binders)

```

### Supported Annotations

The `DirectBinderGenerator` processes the following annotations:

| Annotation | Generated Binder Type | Description |
|:--|:--|:--|
| `@Expression` | `IMethodBinder` | Static methods annotated with `@Expression` (expression language functions) |
| `@Prototype` | `IConstructorBinder` | All public constructors of the annotated class |
| `@Singleton` (javax/jakarta) | `IConstructorBinder` | All public constructors of the annotated class |
| `@Inject` (javax/jakarta) | `IConstructorBinder` | The annotated constructor specifically |

### Generated Index Format

**Method binders** (`META-INF/garganttua/generated-binders`):
```
M:com.garganttua.core.expression.functions.Expressions#concatenate(Object,Object)=com.garganttua.core.reflection.binders.generated.DirectBinder_Expressions_concatenate_Object_Object
M:com.garganttua.core.expression.functions.Expressions#string(Object)=com.garganttua.core.reflection.binders.generated.DirectBinder_Expressions_string_Object
```

**Constructor binders** (`META-INF/garganttua/generated-constructor-binders`):
```
C:com.example.MyBean()=com.garganttua.core.reflection.binders.generated.DirectConstructorBinder_MyBean
C:com.example.MyBean(String,int)=com.garganttua.core.reflection.binders.generated.DirectConstructorBinder_MyBean_String_int
```

### Generated Class Example

For `Expressions.concatenate(Object, Object)`:

```java
package com.garganttua.core.reflection.binders.generated;

public class DirectBinder_Expressions_concatenate_Object_Object
        extends ExecutableBinder<Object>
        implements IMethodBinder<Object> {

    private final ISupplier<?> objectSupplier;

    public DirectBinder_Expressions_concatenate_Object_Object(
            ISupplier<?> objectSupplier, List<ISupplier<?>> params) {
        super(params);
        this.objectSupplier = objectSupplier;
    }

    @Override
    public Optional<IMethodReturn<Object>> execute() throws ReflectionException {
        Object[] args = buildArguments();
        // Direct call — no Method.invoke()
        Object result = Expressions.concatenate(args[0], args[1]);
        return Optional.of(SingleMethodReturn.of(result));
    }
    // ...
}
```


### Enabling / Disabling

Direct binder generation is **enabled by default**. It is controlled by the Maven property `garganttua.direct.binders` which is passed to the annotation processor as a compiler argument (`-Agarganttua.direct.binders`).

**Disable globally** (all modules):
```bash
mvn clean install -Dgarganttua.direct.binders=false
```

**Disable for a specific module** (override in child POM):
```xml
<properties>
    <garganttua.direct.binders>false</garganttua.direct.binders>
</properties>
```

**Change the default** (in parent POM `<properties>`):
```xml
<!-- Set to false to disable compile-time binder generation by default -->
<garganttua.direct.binders>true</garganttua.direct.binders>
```

When disabled, the processor logs a NOTE message and produces no generated code. The runtime registries return empty results, and the framework falls back to reflective binders automatically.

### Fat JAR / Shade Plugin

For fat JARs (e.g., `garganttua-script`), use `AppendingTransformer` to merge the generated binder index files from all JARs:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <configuration>
        <transformers>
            <!-- ... other transformers ... -->
            <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                <resource>META-INF/garganttua/generated-binders</resource>
            </transformer>
            <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                <resource>META-INF/garganttua/generated-constructor-binders</resource>
            </transformer>
        </transformers>
    </configuration>
</plugin>
```

## Architecture

### Module Structure

```
garganttua-annotation-processor/
├── src/main/
│   ├── java/com/garganttua/core/annotation/processor/
│   │   ├── IndexedAnnotationProcessor.java   # Compile-time annotation indexing
│   │   ├── DirectBinderGenerator.java        # Compile-time direct binder generation
│   │   ├── AnnotationIndex.java              # Runtime index loader
│   │   └── IndexedAnnotationScanner.java     # IAnnotationScanner impl
│   └── resources/META-INF/services/
│       └── javax.annotation.processing.Processor  # SPI registration (both processors)
└── pom.xml
```

### Build Note

This module disables annotation processing on itself (`-proc:none`) to avoid self-processing during compilation.

## License

This module is distributed under the MIT License.
