# 🧠 Garganttua Expression

Advanced expression language for dynamic object creation and type-safe evaluation in Java applications.

## Description

The `garganttua-expression` module provides a powerful, extensible expression language built on ANTLR4 that enables runtime expression parsing, evaluation, and type-safe object creation. It integrates seamlessly with the Garganttua dependency injection framework to support dynamic configuration and complex object composition patterns.

### Key Features

- **Type-Safe Expression Evaluation**: Compile-time and runtime type checking with generic supplier types
- **ANTLR4-Based Grammar**: Robust parsing with comprehensive syntax support
- **Extensible Node System**: Create custom expression functions with leaf and composite nodes
- **Auto-Detection Support**: Automatic discovery of expression nodes via annotations
- **Contextual Evaluation**: Support for both contextual and non-contextual expression nodes
- **Rich Type System**: Full support for primitives, classes, generics, arrays, and collections
- **DSL Builder Pattern**: Fluent API for configuring expression contexts
- **Comprehensive Logging**: Structured Slf4j logging for debugging and monitoring

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-expression</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `com.garganttua.core:garganttua-commons`
 - `com.garganttua.core:garganttua-dsl`
 - `com.garganttua.core:garganttua-supply`
 - `com.garganttua.core:garganttua-reflection`
 - `com.garganttua.core:garganttua-reflections:test`
 - `org.antlr:antlr4-runtime:4.13.0`

<!-- AUTO-GENERATED-END -->

## Core Concepts

### Expression Nodes

Expression nodes are the building blocks of the expression system:

- **Leaf Nodes** (`ExpressionLeaf`): Terminal nodes that directly evaluate to values (e.g., string literals, numbers, type references)
- **Composite Nodes** (`ExpressionNode`): Non-terminal nodes that combine child nodes (e.g., function calls with arguments)
- **Contextual Nodes** (`ContextualExpressionNode`): Nodes that require runtime context for evaluation

### Expression Context

The `ExpressionContext` manages:
- Registration of expression node factories
- ANTLR4 parser integration
- Expression parsing and validation
- Node factory lookup and instantiation

### Expression Node Factories

Factories (`ExpressionNodeFactory`) bind Java methods to expression syntax:
- Map method signatures to expression function names
- Handle parameter type matching and conversion
- Support both static and instance methods
- Enable nullable parameter handling

## Usage

### Basic Expression Evaluation

From `ExpressionContextTest.testSimpleStringExpression()`:

```java
import com.garganttua.core.expression.context.ExpressionContext;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.supply.ISupplier;

// Create an expression context with factories
ExpressionContext expressionContext = new ExpressionContext(factories);

// Parse a simple string literal expression
IExpression<?, ? extends ISupplier<?>> expression = expressionContext.expression("\"Hello World\"");

assertNotNull(expression, "Expression should not be null");

// Evaluate the expression
ISupplier<?> result = expression.evaluate();

assertNotNull(result, "Result should not be null");

// Get the actual value
Optional<String> value = (Optional<String>) result.supply();

assertTrue(value.isPresent(), "Value should be present");
assertEquals("Hello World", value.get(), "Value should be 'Hello World'");
```

### Numeric Expressions

From `ExpressionContextTest.testSimpleIntegerExpression()` and `ExpressionContextTest.testAddExpression()`:

```java
// Integer literal
IExpression<?, ? extends ISupplier<?>> expression = expressionContext.expression("42");

assertNotNull(expression, "Expression should not be null");

// Evaluate the expression
ISupplier<?> result = expression.evaluate();

assertNotNull(result, "Result should not be null");

// Get the actual value
Optional<Integer> value = (Optional<Integer>) result.supply();

assertTrue(value.isPresent(), "Value should be present");
assertEquals(42, value.get(), "Value should be 42");
```

From `ExpressionContextTest.testAddExpression()`:

```java
// Parse an expression with a function call: add(42, 30)
IExpression<?, ? extends ISupplier<?>> expression = expressionContext.expression("add(42, 30)");

assertNotNull(expression, "Expression should not be null");

// Evaluate the expression
ISupplier<?> result = expression.evaluate();

assertNotNull(result, "Result should not be null");

// Get the actual value
Optional<Integer> value = (Optional<Integer>) result.supply();

assertTrue(value.isPresent(), "Value should be present");
assertEquals(72, value.get(), "Value should be 72 (42 + 30)");
```

From `ExpressionContextTest.testComplexAddExpression()`:

```java
// Parse an expression with nested function calls: add(8, add(42, 30))
IExpression<?, ? extends ISupplier<?>> expression = expressionContext.expression("add(8,add(42, 30))");

assertNotNull(expression, "Expression should not be null");

// Evaluate the expression
ISupplier<?> result = expression.evaluate();

assertNotNull(result, "Result should not be null");

// Get the actual value
Optional<Integer> value = (Optional<Integer>) result.supply();

assertTrue(value.isPresent(), "Value should be present");
assertEquals(80, value.get(), "Value should be 80 (8 + (42 + 30))");
```

### Type Expressions

From `ExpressionContextTest.testPrimitiveTypeExpression()`:

```java
// Test primitive type int
IExpression<?, ? extends ISupplier<?>> intTypeExpr = expressionContext.expression("int");
ISupplier<?> intTypeResult = intTypeExpr.evaluate();
Optional<Class<?>> intTypeValue = (Optional<Class<?>>) intTypeResult.supply();

assertTrue(intTypeValue.isPresent(), "int type should be present");
assertEquals(int.class, intTypeValue.get(), "Should return int.class");

// Test primitive type boolean
IExpression<?, ? extends ISupplier<?>> boolTypeExpr = expressionContext.expression("boolean");
ISupplier<?> boolTypeResult = boolTypeExpr.evaluate();
Optional<Class<?>> boolTypeValue = (Optional<Class<?>>) boolTypeResult.supply();

assertTrue(boolTypeValue.isPresent(), "boolean type should be present");
assertEquals(boolean.class, boolTypeValue.get(), "Should return boolean.class");
```

From `ExpressionContextTest.testClassTypeExpression()`:

```java
// Test fully qualified class name
IExpression<?, ? extends ISupplier<?>> stringTypeExpr = expressionContext.expression("java.lang.String");
ISupplier<?> stringTypeResult = stringTypeExpr.evaluate();
Optional<Class<?>> stringTypeValue = (Optional<Class<?>>) stringTypeResult.supply();

assertTrue(stringTypeValue.isPresent(), "String type should be present");
assertEquals(String.class, stringTypeValue.get(), "Should return String.class");

// Test Class<?> expression
IExpression<?, ? extends ISupplier<?>> classOfExpr = expressionContext.expression("Class<?>");
ISupplier<?> classOfResult = classOfExpr.evaluate();
Optional<Class<?>> classOfValue = (Optional<Class<?>>) classOfResult.supply();

assertTrue(classOfValue.isPresent(), "Class<?> type should be present");
assertEquals(Class.class, classOfValue.get(), "Should return Class.class");
```

### Building Expression Contexts with DSL

From `ExpressionContextBuilderTest.testBuildWithAutoDetection()`:

```java
import com.garganttua.core.expression.dsl.ExpressionContextBuilder;
import com.garganttua.core.expression.context.IExpressionContext;

// Create context with auto-detection
IExpressionContextBuilder builder = ExpressionContextBuilder.builder()
    .withPackage("com.garganttua.core.expression.dsl")
    .autoDetect(true);
builder.build();
```

From `ExpressionContextBuilderTest.testWithPackage()`:

```java
// Test adding a single package
ExpressionContextBuilder builder = ExpressionContextBuilder.builder();

builder.withPackage("com.example.test");

String[] packages = builder.getPackages();
assertEquals(1, packages.length);
assertEquals("com.example.test", packages[0]);
```

From `ExpressionContextBuilderTest.testWithPackages()`:

```java
// Test adding multiple packages
ExpressionContextBuilder builder = ExpressionContextBuilder.builder();

String[] packagesToAdd = {
        "com.example.test1",
        "com.example.test2",
        "com.example.test3"
};

builder.withPackages(packagesToAdd);

String[] packages = builder.getPackages();
assertEquals(3, packages.length);
```

From `ExpressionContextBuilderTest.testwithExpressionNodeStaticMethod()`:

```java
// Test binding a static method
ExpressionContextBuilder builder = ExpressionContextBuilder.builder();

IExpressionMethodBinderBuilder<String> methodBuilder = builder
        .withExpressionNode(TestExpressions.class, String.class)
        .method("getString");
assertNotNull(methodBuilder);
```

From `ExpressionContextBuilderTest.testChainedCalls()`:

```java
// Test method chaining
ExpressionContextBuilder builder = ExpressionContextBuilder.builder();

builder
        .withPackage("com.example.test1")
        .withPackage("com.example.test2")
        .autoDetect(true)
        .withExpressionNode(TestExpressions.class, String.class)
        .method("getString");

String[] packages = builder.getPackages();
assertEquals(2, packages.length);
```

### Creating Custom Expression Functions

From `ExpressionContextBuilderTest`:

```java
import com.garganttua.core.expression.annotations.ExpressionLeaf;
import com.garganttua.core.expression.annotations.ExpressionNode;
import lombok.NonNull;

// Example of an ExpressionLeaf annotation
@ExpressionLeaf
public String string(@NonNull String message){
    return message;
}

// Example of an ExpressionNode annotation
@ExpressionNode
public String echo(@NonNull String message){
    return message;
}
```

From `ExpressionContextBuilderTest.TestExpressions`:

```java
// Helper class with static methods for testing
public static class TestExpressions {

    public static String getString() {
        return "test string";
    }

    public static Integer getInteger() {
        return 42;
    }

    public static Double getDouble() {
        return 3.14;
    }

    public static Boolean getBoolean() {
        return true;
    }

    // Non-static method - should fail
    public String getNonStaticString() {
        return "non-static";
    }
}
```

### Creating Expression Node Factories

From `ExpressionNodeFactoryTest.testExpressionNodeFactoryCreation()`:

```java
import com.garganttua.core.expression.context.ExpressionNodeFactory;
import com.garganttua.core.expression.context.ExpressionNodeContext;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.reflection.ObjectAddress;

static class TestService {

    static public String string(String string) {
        return string;
    }

    static public String greet(String name) {
        return "Hello, " + name;
    }
}

// Create a leaf factory
ExpressionNodeFactory<String, ISupplier<String>> leafFactory =
    new ExpressionNodeFactory<String, ISupplier<String>>(
        TestService.class,
        (Class<ISupplier<String>>) (Class<?>) ISupplier.class,
        TestService.class.getMethod("string", String.class),
        new ObjectAddress("string"),
        List.of(false),
        true,  // This is a leaf node
        Optional.of("string"),
        Optional.of("String converter"));

// Create a node factory
ExpressionNodeFactory<String, ISupplier<String>> nodefactory =
    new ExpressionNodeFactory<String, ISupplier<String>>(
        TestService.class,
        (Class<ISupplier<String>>) (Class<?>) ISupplier.class,
        TestService.class.getMethod("greet", String.class),
        new ObjectAddress("greet"),
        List.of(false),
        false,  // This is NOT a leaf node
        Optional.of("greet"),
        Optional.of("Greeting function"));

// Supply the leaf node
Optional<IExpressionNode<String,ISupplier<String>>> leaf =
    leafFactory.supply(new ExpressionNodeContext(List.of("greet"), true));

// Supply the expression node with the leaf as parameter
Optional<IExpressionNode<String,ISupplier<String>>> expression =
    nodefactory.supply(new ExpressionNodeContext(List.of(leaf.get())));

assertEquals("Hello, greet", expression.get().evaluate().supply().get());
```

## Expression Language Syntax

### Literals

```antlr4
STRING       : '"' ... '"'        // "Hello World"
CHAR         : '\'' . '\''        // 'A'
INT_LITERAL  : [0-9]+             // 42, -10
FLOAT_LIT    : [0-9]+ '.' [0-9]+  // 3.14, -2.5
BOOLEAN      : 'true' | 'false'   // true, false
NULL         : 'null'             // null
```

### Types

```antlr4
// Primitive types
int, boolean, byte, short, long, float, double, char

// Class types
java.lang.String
com.myapp.MyClass

// Generic types
List<String>
Map<String, Integer>

// Class references
Class<?>
Class<String>

// Array types (future support)
int[]
String[][]
```

### Function Calls

```antlr4
functionName()                    // No arguments
functionName(arg1)                // Single argument
functionName(arg1, arg2, ...)     // Multiple arguments
functionName(nested())            // Nested calls
```

### Identifiers

Simple identifiers are treated as string literals:
```
myIdentifier → string("myIdentifier")
```

## Standard Expression Leafs

The module provides built-in conversion functions in `StandardExpressionLeafs` (from the actual source code):

### Primitive Converters

All methods are annotated with `@ExpressionLeaf`:

```java
@ExpressionLeaf(name = "string", description = "Converts a value to a String")
public static String String(@Nullable String value) {
    return value;
}

@ExpressionLeaf(name = "int", description = "Parses a string to an Integer")
public static Integer Integer(@Nullable String value) {
    // Throws ExpressionException if value cannot be parsed
}

@ExpressionLeaf(name = "long", description = "Parses a string to a Long")
public static Long Long(@Nullable String value) {
    // Throws ExpressionException if value cannot be parsed
}

@ExpressionLeaf(name = "double", description = "Parses a string to a Double")
public static Double Double(@Nullable String value) {
    // Throws ExpressionException if value cannot be parsed
}

@ExpressionLeaf(name = "float", description = "Parses a string to a Float")
public static Float Float(@Nullable String value) {
    // Throws ExpressionException if value cannot be parsed
}

@ExpressionLeaf(name = "boolean", description = "Parses a string to a Boolean (true/false)")
public static Boolean Boolean(@Nullable String value) {
    return java.lang.Boolean.parseBoolean(value);
}

@ExpressionLeaf(name = "byte", description = "Parses a string to a Byte (-128 to 127)")
public static Byte Byte(@Nullable String value) {
    // Throws ExpressionException if value cannot be parsed
}

@ExpressionLeaf(name = "short", description = "Parses a string to a Short")
public static Short Short(@Nullable String value) {
    // Throws ExpressionException if value cannot be parsed
}

@ExpressionLeaf(name = "char", description = "Extracts first character from string as Character")
public static Character Character(@Nullable String value) {
    // Throws ExpressionException if value is empty
}
```

### Type Converters

```java
@ExpressionLeaf(name = "class", description = "Loads a class by fully qualified name or primitive type")
public static Class<?> Class(@Nullable String className) {
    // Supports primitive types: boolean, byte, short, int, long, float, double, char, void
    // Supports fully qualified class names: java.lang.String
    // Throws ExpressionException if class cannot be found
}
```

Usage examples from tests:
- `"int"` returns `int.class`
- `"boolean"` returns `boolean.class`
- `"java.lang.String"` returns `String.class`
- `"Class<?>"` returns `Class.class`

## Architecture

### Module Structure

```
garganttua-expression/
├── src/main/
│   ├── java/com/garganttua/core/expression/
│   │   ├── annotations/          # @ExpressionLeaf, @ExpressionNode, @Expression
│   │   ├── context/              # ExpressionContext, factories, node contexts
│   │   ├── dsl/                  # Builder pattern implementations
│   │   ├── functions/            # StandardExpressionLeafs
│   │   ├── Expression.java       # Main expression implementation
│   │   ├── ExpressionNode.java   # Composite node implementation
│   │   ├── ExpressionLeaf.java   # Leaf node implementation
│   │   └── ContextualExpressionNode.java
│   └── resources/antlr4/
│       └── Expression.g4         # ANTLR4 grammar definition
└── src/test/                     # Comprehensive test suite
```

### Class Hierarchy

```
IExpression<R, S>
  └── Expression<R> implements IExpression<R, ISupplier<R>>

IExpressionNode<R, S>
  ├── ExpressionLeaf<R> implements IExpressionNode<R, ISupplier<R>>
  ├── ExpressionNode<R> implements IExpressionNode<R, ISupplier<R>>
  └── ContextualExpressionNode<R> implements IContextualExpressionNode<R, ISupplier<R>>

IExpressionContext
  └── ExpressionContext implements IExpressionContext

IExpressionNodeFactory<R, S>
  └── ExpressionNodeFactory<R, S> implements IExpressionNodeFactory<R, S>
```

## Advanced Features

### Working with Expression Nodes

From `NodeTest.testSimpleConcatenationExpression()`:

```java
import com.garganttua.core.expression.ExpressionLeaf;
import com.garganttua.core.expression.ExpressionNode;
import com.garganttua.core.expression.Expression;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.ISupplier;

// Create an expression leaf
ExpressionLeaf<String> leaf = new ExpressionLeaf<>("", params -> {
    return new FixedSupplier<String>((String)params[0]);
}, String.class, "Hello world from");

// Create expression nodes that build on each other
ExpressionNode<String> node1 = new ExpressionNode<String>("", params -> {
    ISupplier<String> supplier = (ISupplier<String>) params[0];
    String t = supplier.supply().get() + " node 1";
    return new FixedSupplier<String>(t);
},String.class, List.of(leaf));

ExpressionNode<String> node2 = new ExpressionNode<String>("", params -> {
    ISupplier<String> supplier = (ISupplier<String>) params[0];
    String t = supplier.supply().get() + " node 2";
    return new FixedSupplier<String>(t);
}, String.class, List.of(node1));

ExpressionNode<String> node3 = new ExpressionNode<String>("", params -> {
    ISupplier<String> supplier = (ISupplier<String>) params[0];
    String t = supplier.supply().get() + " node 3";
    return new FixedSupplier<String>(t);
}, String.class, List.of(node2));

// Evaluate the expression
assertEquals("Hello world from node 1 node 2 node 3", node3.evaluate().supply().get());
assertEquals("Hello world from node 1 node 2 node 3", node3.supply().get().supply().get());

// Wrap in an Expression
Expression<String> exp = new Expression<>(node3);

assertEquals("Hello world from node 1 node 2 node 3", exp.evaluate().supply().get());
assertEquals("Hello world from node 1 node 2 node 3", exp.build().supply().get());
```

### Contextual Expression Nodes

From `NodeTest.testContextualEvaluationWithinContextualExpressionNode()`:

```java
import com.garganttua.core.expression.ContextualExpressionNode;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.expression.context.ExpressionContext;
import com.garganttua.core.reflection.binders.ContextualMethodBinder;
import com.garganttua.core.supply.Supplier;

static class StringConcatenator {
    String concatenate(String string, String string2) {
        return string + "" + string2;
    }
}

// Create contextual expression nodes
IExpressionNode<String, ? extends ISupplier<String>> node1 =
    new ContextualExpressionNode<String>("", (c, params) -> {
        ContextualMethodBinder<String, IExpressionContext> mb =
            new ContextualMethodBinder<>(
                new FixedSupplier<>(new StringConcatenator()),
                new ObjectAddress("concatenate"),
                List.of(
                    new FixedSupplier<String>("Hello from node 1"),
                    new FixedSupplier<String>("")),
                String.class);
        return mb;
    }, String.class);

IExpressionNode<String, ? extends ISupplier<String>> node2 =
    new ContextualExpressionNode<String>("", (c, params) -> {
        ContextualMethodBinder<String, IExpressionContext> mb =
            new ContextualMethodBinder<>(
                new FixedSupplier<>(new StringConcatenator()),
                new ObjectAddress("concatenate"),
                List.of(
                    params[0],
                    new FixedSupplier<String>(" node 2")),
                String.class);
        return mb;
    }, String.class, List.of(node1));

IExpressionNode<String, ? extends ISupplier<String>> node3 =
    new ContextualExpressionNode<String>("", (c, params) -> {
        ContextualMethodBinder<String, IExpressionContext> mb =
            new ContextualMethodBinder<>(
                new FixedSupplier<>(new StringConcatenator()),
                new ObjectAddress("concatenate"),
                List.of(
                    params[0],
                    new FixedSupplier<String>(" node 3")),
                String.class);
        return mb;
    }, String.class, List.of(node2));

Expression<String> exp = new Expression<>(node3);

assertEquals("Hello from node 1 node 2 node 3",
    Supplier.contextualRecursiveSupply(exp.build(), new ExpressionContext(Set.of())));
```

### Auto-Detection

From `ExpressionContextBuilderTest.testAutoDetect()`:

```java
// Test auto-detect functionality
ExpressionContextBuilder builder = ExpressionContextBuilder.builder();

builder.autoDetect(true);

builder.autoDetect(false);
```

The auto-detection will find all methods annotated with:
- `@ExpressionLeaf` - for leaf nodes
- `@ExpressionNode` - for composite nodes

## Error Handling

### Type Mismatch Errors

From `ExpressionContextTest.testddExpression_wrongParamType()`:

```java
// This will throw ExpressionException at parse time:
ExpressionException exception = assertThrows(ExpressionException.class,
    () -> expressionContext.expression("add(8,add(toto, 30))"));

assertEquals("Unknown function: add(String,Integer)", exception.getMessage());
```

### Non-Static Method Errors

From `ExpressionContextBuilderTest.testwithExpressionNodeNonStaticMethodFails()`:

```java
// Test that binding a non-static method throws an exception
ExpressionContextBuilder builder = ExpressionContextBuilder.builder();

assertThrows(DslException.class, () -> {
    builder.withExpressionNode(TestExpressions.class, String.class)
            .method("getNonStaticString");
});
```

### Method Resolution Errors

From `ExpressionContextBuilderTest.testMethodResolution()`:

```java
// Test that we can resolve different methods by name
ExpressionContextBuilder builder = ExpressionContextBuilder.builder();

// Should work - correct method name
assertDoesNotThrow(() -> {
    builder.withExpressionNode(TestExpressions.class, String.class)
            .method("getString");
});

// Should fail - method doesn't exist
assertThrows(DslException.class, () -> {
    builder.withExpressionNode(TestExpressions.class, String.class)
            .method("nonExistentMethod");
});
```

## Testing

The module includes comprehensive tests. Here are some examples:

From `ExpressionContextTest.testSimpleBooleanExpression()`:

```java
@Test
public void testSimpleBooleanExpression() throws Exception {
    // Parse a simple integer literal expression
    IExpression<?, ? extends ISupplier<?>> expression = expressionContext.expression("true");

    assertNotNull(expression, "Expression should not be null");

    // Evaluate the expression
    ISupplier<?> result = expression.evaluate();

    assertNotNull(result, "Result should not be null");

    // Get the actual value
    Optional<Boolean> value = (Optional<Boolean>) result.supply();

    assertTrue(value.isPresent(), "Value should be present");
    assertTrue(value.get(), "Value should be true");
}
```

From `ExpressionContextTest` - Setting up test factories:

```java
@BeforeEach
public void setUp() throws Exception {
    // Create factories for StandardExpressionLeafs methods as expression leaves

    ExpressionNodeFactory<String, ISupplier<String>> stringFactory = new ExpressionNodeFactory<>(
            StandardExpressionLeafs.class,
            (Class<ISupplier<String>>) (Class<?>) ISupplier.class,
            StandardExpressionLeafs.class.getMethod("String", String.class),
            new ObjectAddress("String"),
            List.of(false),
            true, // This is a leaf node
            Optional.of("string"),
            Optional.of("Converts a value to a String supplier"));

    ExpressionNodeFactory<Integer, ISupplier<Integer>> intFactory = new ExpressionNodeFactory<>(
            StandardExpressionLeafs.class,
            (Class<ISupplier<Integer>>) (Class<?>) ISupplier.class,
            StandardExpressionLeafs.class.getMethod("Integer", String.class),
            new ObjectAddress("Integer"),
            List.of(false),
            true, // This is a leaf node
            Optional.of("int"),
            Optional.of("Parses a string to an Integer supplier"));

    ExpressionNodeFactory<Integer, ISupplier<Integer>> addFactory = new ExpressionNodeFactory<>(
            TestFunctions.class,
            (Class<ISupplier<Integer>>) (Class<?>) ISupplier.class,
            TestFunctions.class.getMethod("add", Integer.class, Integer.class),
            new ObjectAddress("add"),
            List.of(false, false),
            false, // This is NOT a leaf node - it takes other expression nodes as parameters
            Optional.of("add"),
            Optional.of("Adds two integer suppliers"));

    // Create expression context with leaf and node factories
    Set<IExpressionNodeFactory<?, ? extends ISupplier<?>>> factories = Set.of(
            stringFactory,
            intFactory,
            addFactory
    );

    expressionContext = new ExpressionContext(factories);
}
```

## Tips and Best Practices

### 1. Methods Must Be Static

From the test examples, all expression methods must be static:

```java
// This works - static method
public static String getString() {
    return "test string";
}

// This fails - non-static method
public String getNonStaticString() {
    return "non-static";
}
```

### 2. Provide Type-Safe Factories

From `ExpressionContextTest` setup, always specify correct parameter types:

```java
ExpressionNodeFactory<Integer, ISupplier<Integer>> addFactory = new ExpressionNodeFactory<>(
    TestFunctions.class,
    (Class<ISupplier<Integer>>) (Class<?>) ISupplier.class,
    TestFunctions.class.getMethod("add", Integer.class, Integer.class),
    new ObjectAddress("add"),
    List.of(false, false),  // Neither parameter is nullable
    false,  // Not a leaf (composite node)
    Optional.of("add"),
    Optional.of("Adds two integer suppliers")
);
```

### 3. Distinguish Between Leaf and Node Factories

From `ExpressionNodeFactoryTest`:

```java
// Leaf factory - evaluates directly to a value
ExpressionNodeFactory<String, ISupplier<String>> leafFactory =
    new ExpressionNodeFactory<String, ISupplier<String>>(
        TestService.class,
        (Class<ISupplier<String>>) (Class<?>) ISupplier.class,
        TestService.class.getMethod("string", String.class),
        new ObjectAddress("string"),
        List.of(false),
        true,  // This is a leaf node
        Optional.of("string"),
        Optional.of("String converter"));

// Node factory - combines other nodes
ExpressionNodeFactory<String, ISupplier<String>> nodefactory =
    new ExpressionNodeFactory<String, ISupplier<String>>(
        TestService.class,
        (Class<ISupplier<String>>) (Class<?>) ISupplier.class,
        TestService.class.getMethod("greet", String.class),
        new ObjectAddress("greet"),
        List.of(false),
        false,  // This is NOT a leaf node
        Optional.of("greet"),
        Optional.of("Greeting function"));
```

### 4. Use Auto-Detection for Large Projects

From `ExpressionContextBuilderTest`:

```java
// Enable auto-detection
IExpressionContextBuilder builder = ExpressionContextBuilder.builder()
    .withPackage("com.garganttua.core.expression.dsl")
    .autoDetect(true);
builder.build();
```

### 5. Test Method Resolution Early

From `ExpressionContextBuilderTest.testMethodResolution()`:

```java
// Should work - correct method name
assertDoesNotThrow(() -> {
    builder.withExpressionNode(TestExpressions.class, String.class)
            .method("getString");
});

// Should fail - method doesn't exist
assertThrows(DslException.class, () -> {
    builder.withExpressionNode(TestExpressions.class, String.class)
            .method("nonExistentMethod");
});
```

## Performance Considerations

### Parsing Overhead

- Expression parsing involves ANTLR4 lexing/parsing which has overhead
- Cache parsed expressions when possible
- Consider pre-compiling frequently used expressions

### Factory Lookup

- Factory lookup is O(1) using HashMap by function signature
- Key format: `"functionName(Type1,Type2,...)"`
- Type matching is exact (no coercion)

### Evaluation Cost

- Leaf nodes evaluate directly with minimal overhead
- Composite nodes chain supplier evaluations
- Contextual nodes have additional context resolution cost

## Thread Safety

- `ExpressionContext` is thread-safe and can be shared
- `IExpression` instances are immutable and thread-safe
- `ISupplier` evaluation is thread-safe but depends on implementation
- Expression node factories are immutable and thread-safe

## Limitations and Known Issues

1. **No Operator Support**: Currently only supports function call syntax, no infix operators (`+`, `-`, `*`, `/`)
2. **No Variable Binding**: Expressions cannot reference variables or scopes
3. **No Control Flow**: No support for `if`, `while`, or other control structures
4. **Limited Array Support**: Array literal syntax defined but not fully implemented
5. **No Type Coercion**: Strict type matching required for function calls

## Integration with Other Modules

### garganttua-commons
- Uses `ISupplier` interface for value wrapping
- Relies on reflection utilities from commons
- Uses annotations defined in commons

### garganttua-injection
- `@Expression` annotation for DI field injection
- Expression resolver integration with `DiContextBuilder`
- Seamless injection of expression results into beans

### garganttua-dsl
- Extends `AbstractAutomaticBuilder` for consistent builder pattern
- Uses DSL exception handling
- Follows DSL builder conventions

### garganttua-reflection
- Uses `MethodBinder` for method invocation
- Leverages `ObjectAddress` for method addressing
- Integrates with reflection-based parameter resolution

## License

This module is distributed under the MIT License.

---

**Version**: 2.0.0-ALPHA01
**Last Updated**: December 2024
**Maintainer**: Garganttua Team
