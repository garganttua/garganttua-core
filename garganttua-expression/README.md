# Garganttua Expression

Advanced expression language for dynamic object supplying in the Garganttua Core framework.

## Overview

The `garganttua-expression` module provides a powerful expression language that allows you to parse, build, and evaluate dynamic expressions at runtime. Built on ANTLR4, it offers a type-safe way to create expression trees that produce suppliers of values, enabling complex runtime evaluation scenarios with strongly-typed results.

## Features

- **Expression Parsing**: Parse string expressions into executable expression trees using ANTLR4
- **Type-Safe Evaluation**: Strongly typed expression nodes that return `ISupplier<T>` instances
- **Flexible Node Types**:
  - **Leaf Nodes**: Terminal nodes that convert raw values into suppliers
  - **Composite Nodes**: Internal nodes that compose other expression nodes
- **DSL Builder**: Fluent API for constructing expression contexts programmatically
- **Annotation Support**: Auto-detection of expression functions via `@ExpressionNode` and `@ExpressionLeaf` annotations
- **Standard Functions**: Built-in expression functions for common types (String, Integer, Boolean, etc.)

## Maven Dependency

```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-expression</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

## Key Concepts

### Expression Nodes

Expression nodes are the building blocks of the expression tree:

- **IExpressionNode**: Base interface for all expression nodes
- **ExpressionLeaf**: Leaf nodes that convert raw parameter values to suppliers
- **ExpressionNode**: Composite nodes that evaluate child expression nodes

### Expression Context

The `ExpressionContext` is the runtime environment for parsing and evaluating expressions:

- Maintains a registry of available expression node factories
- Parses expression strings into expression trees
- Resolves function names to their corresponding node factories

### Expression Node Factory

Factories create expression nodes based on method bindings:

- **ExpressionNodeFactory**: Wraps a method and creates corresponding expression nodes
- Distinguishes between leaf factories (for terminal values) and composite factories (for functions)
- Generates unique keys based on function name and parameter types

---

## Quick Start

### Basic Usage

```java
import com.garganttua.core.expression.*;
import com.garganttua.core.expression.context.*;
import com.garganttua.core.expression.functions.StandardExpressionLeafs;
import com.garganttua.core.supply.ISupplier;

// Define expression functions
class MathFunctions {
    public static Integer add(Integer a, Integer b) {
        return a + b;
    }
}

// Create expression context with factories
ExpressionNodeFactory<Integer> intFactory = new ExpressionNodeFactory<>(
    StandardExpressionLeafs.class,
    ISupplier.class,
    StandardExpressionLeafs.class.getMethod("Integer", String.class),
    new ObjectAddress("Integer"),
    List.of(false),
    true, // leaf node
    Optional.of("int"),
    Optional.of("Parses string to Integer")
);

ExpressionNodeFactory<Integer> addFactory = new ExpressionNodeFactory<>(
    MathFunctions.class,
    ISupplier.class,
    MathFunctions.class.getMethod("add", Integer.class, Integer.class),
    new ObjectAddress("add"),
    List.of(false, false),
    false, // composite node
    Optional.of("add"),
    Optional.of("Adds two integers")
);

// Build expression context
IExpressionContext context = new ExpressionContext(Set.of(intFactory, addFactory));

// Parse and evaluate expression
IExpression<?, ?> expression = context.expression("add(42, 30)");
ISupplier<?> result = expression.evaluate();
Optional<Integer> value = (Optional<Integer>) result.supply();

System.out.println(value.get()); // Output: 72
```

### Using the DSL Builder

```java
import com.garganttua.core.expression.dsl.ExpressionContextBuilder;

// Build context using fluent API
IExpressionContext context = ExpressionContextBuilder
    .builder()
    .withExpressionLeaf(StandardExpressionLeafs.class, String.class)
        .method("String")
        .up()
    .withExpressionLeaf(StandardExpressionLeafs.class, Integer.class)
        .method("Integer")
        .up()
    .withExpressionNode(MathFunctions.class, Integer.class)
        .method("add")
        .up()
    .build();

// Parse complex nested expressions
IExpression<?, ?> expr = context.expression("add(8, add(42, 30))");
ISupplier<?> result = expr.evaluate();
Optional<Integer> value = (Optional<Integer>) result.supply();

System.out.println(value.get()); // Output: 80
```

### Using Package Scanning with Annotations

```java
import com.garganttua.core.expression.annotations.ExpressionNode;
import com.garganttua.core.expression.annotations.ExpressionLeaf;

// Define functions with annotations
class AnnotatedFunctions {
    @ExpressionLeaf
    public static String string(String value) {
        return value;
    }

    @ExpressionNode
    public static Integer multiply(Integer a, Integer b) {
        return a * b;
    }
}

// Build context with package scanning
IExpressionContext context = ExpressionContextBuilder
    .builder()
    .withPackage("com.myapp.expressions")
    .autoDetect(true)
    .build();

// All annotated methods are automatically registered
IExpression<?, ?> expr = context.expression("multiply(6, 7)");
```

---

## Expression Language Syntax

The expression language supports the following constructs defined by the ANTLR4 grammar (`Query.g4`):

### Literals

```java
// String literals
expression("\"Hello World\"")

// Integer literals
expression("42")

// Float literals
expression("3.14")

// Boolean literals
expression("true")
expression("false")

// Null literal
expression("null")
```

### Function Calls

```java
// Simple function call
expression("add(10, 20)")

// Nested function calls
expression("add(multiply(2, 3), 4)")

// Multiple parameters
expression("concat(\"Hello\", \" \", \"World\")")
```

### Arrays (if list factory is registered)

```java
expression("[1, 2, 3, 4, 5]")
expression("[\"a\", \"b\", \"c\"]")
```

### Identifiers

Standalone identifiers are converted to string literals:

```java
expression("myVar")  // Treated as string "myVar"
```

---

## Standard Expression Leafs

The framework provides `StandardExpressionLeafs` with common type conversions:

```java
// String conversion
StandardExpressionLeafs.String(String)  // Returns ISupplier<String>

// Integer parsing
StandardExpressionLeafs.Integer(String) // Returns ISupplier<Integer>

// Boolean parsing
StandardExpressionLeafs.Boolean(String) // Returns ISupplier<Boolean>

// Double parsing
StandardExpressionLeafs.Double(String)  // Returns ISupplier<Double>

// Character parsing
StandardExpressionLeafs.Character(String) // Returns ISupplier<Character>
```

These leafs are designed to be used as terminal nodes in your expression trees, converting string literals from the parsed expression into typed suppliers.

---

## Architecture

### Core Components

1. **IExpression**: Represents a parsed expression ready for evaluation
2. **IExpressionNode**: Node in the expression tree (leaf or composite)
3. **IExpressionContext**: Context holding available functions and parsing logic
4. **IExpressionNodeFactory**: Factory for creating expression nodes from method bindings
5. **ExpressionVisitor**: ANTLR4 visitor that builds expression trees from parsed queries

### Expression Evaluation Flow

```
String Expression → ANTLR4 Parser → QueryParser.QueryContext
                                    ↓
                            ExpressionVisitor
                                    ↓
                          IExpressionNode Tree
                                    ↓
                              Expression.evaluate()
                                    ↓
                            ISupplier<T> Result
```

### Factory Key Generation

Expression node factories are indexed by keys in the format:

```
functionName(Type1,Type2,...)
```

For example:
- `add(Integer,Integer)` - function taking two integers
- `int(String)` - leaf function converting string to integer
- `concat(String,String,String)` - function taking three strings

---

## Advanced Features

### Contextual Expression Nodes

Create expression nodes that are aware of the evaluation context:

```java
public class MyContextualNode<R>
    extends ContextualExpressionNode<R>
    implements IContextualExpressionNode<R, ISupplier<R>> {

    @Override
    public ISupplier<R> evaluate(IExpressionContext context) {
        // Access context during evaluation
        // Useful for variable resolution, scoped functions, etc.
        return () -> Optional.of(result);
    }
}
```

### Custom Node Factories

Create custom node factories for domain-specific functions:

```java
ExpressionNodeFactory<MyType> customFactory = new ExpressionNodeFactory<>(
    MyFunctions.class,
    ISupplier.class,
    MyFunctions.class.getMethod("myFunction", String.class, Integer.class),
    new ObjectAddress("myFunction"),
    List.of(false, true), // second parameter is nullable
    false,
    Optional.of("myFunc"),
    Optional.of("Custom function description")
);
```

### Complex Expression Trees

Build complex nested expressions:

```java
// Expression: add(8, add(42, 30))
// Tree structure:
//        add
//       /   \
//      8    add
//          /   \
//         42   30

IExpression<?, ?> expr = context.expression("add(8, add(42, 30))");
// Result: 80
```

---

## Error Handling

The framework throws `ExpressionException` for:

- Syntax errors during parsing
- Unknown function names
- Type mismatches
- Evaluation errors

```java
try {
    IExpression<?, ?> expr = context.expression("add(\"string\", 42)");
    expr.evaluate();
} catch (ExpressionException e) {
    System.err.println("Expression error: " + e.getMessage());
    // Example: "Unknown function: add(String,Integer)"
}
```

### Error Messages

- **Parsing errors**: "Error parsing expression '...': ..."
- **Unknown function**: "Unknown function: functionName(Type1,Type2)"
- **Function not found**: "Function not found: functionName(Type1,Type2)"
- **Failed node creation**: "Failed to create node for function: ..."

---

## Testing

The module includes comprehensive tests:

- [ExpressionContextTest.java](src/test/java/com/garganttua/core/expression/context/ExpressionContextTest.java): Tests for parsing and evaluating expressions
- [ExpressionNodeFactoryTest.java](src/test/java/com/garganttua/core/expression/context/ExpressionNodeFactoryTest.java): Tests for node factory creation and configuration
- [ExpressionContextBuilderTest.java](src/test/java/com/garganttua/core/expression/dsl/ExpressionContextBuilderTest.java): Tests for DSL builder functionality

### Example Test

```java
@Test
public void testAddExpression() throws Exception {
    // Parse an expression with a function call: add(42, 30)
    IExpression<?, ?> expression = expressionContext.expression("add(42, 30)");

    assertNotNull(expression, "Expression should not be null");

    // Evaluate the expression
    ISupplier<?> result = expression.evaluate();
    Optional<Integer> value = (Optional<Integer>) result.supply();

    assertTrue(value.isPresent(), "Value should be present");
    assertEquals(72, value.get(), "Value should be 72 (42 + 30)");
}
```

Run tests:

```bash
mvn test -pl garganttua-expression
```

---

## Grammar

The expression language is defined using ANTLR4 grammar (`Query.g4`):

### Main Rule

```antlr4
query : expression EOF ;
```

### Expressions

```antlr4
expression
    : functionCall
    | literal
    | type
    | IDENTIFIER
    ;
```

### Function Calls

```antlr4
functionCall
    : IDENTIFIER '(' arguments? ')'
    ;

arguments
    : expression (',' expression)*
    ;
```

### Literals

```antlr4
literal
    : STRING
    | CHAR
    | INT
    | FLOAT
    | BOOLEAN
    | NULL
    | arrayLiteral
    | objectLiteral
    ;
```

### Types

```antlr4
type
    : simpleType arrayDims?
    ;

simpleType
    : primitiveType    // boolean, int, double, etc.
    | classType        // java.lang.String, List<T>
    | classOfType      // Class<String>, Class<?>
    ;
```

---

## Integration with Other Modules

`garganttua-expression` integrates with:

- **garganttua-supply**: Uses `ISupplier` for lazy evaluation and optional results
- **garganttua-reflection**: Uses reflection utilities for method binding and type introspection
- **garganttua-dsl**: Extends DSL builder patterns for fluent API construction
- **garganttua-commons**: Uses core interfaces like `IExpressionContext` and `IExpressionNodeFactory`

---

## Best Practices

1. **Register Leaf Nodes First**: Always register type conversion leafs before composite functions to ensure proper type resolution
2. **Use Type-Safe Factories**: Define factories with proper generic types to maintain compile-time type safety
3. **Handle Nullability**: Explicitly specify which parameters can be null in the `nullableParameters` list
4. **Provide Descriptions**: Add meaningful descriptions to factories for better debugging and documentation
5. **Reuse Contexts**: Create expression contexts once and reuse them for multiple expressions to amortize initialization cost
6. **Cache Parsed Expressions**: If evaluating the same expression multiple times, parse once and evaluate repeatedly
7. **Use DSL Builder**: Prefer the DSL builder API over manual factory construction for cleaner, more maintainable code

---

## Performance Considerations

- **Parsing Overhead**: Expression parsing has overhead; cache parsed `IExpression` instances when possible
- **Lazy Evaluation**: Expression results are `ISupplier` instances, enabling lazy evaluation strategies
- **Factory Lookup**: Factory lookup is O(1) using HashMap-based key resolution
- **Nested Expressions**: Deep nesting creates deep call stacks during evaluation; consider flattening for performance-critical paths

---

## Thread Safety

- **ExpressionContext**: Thread-safe for reading (parsing expressions)
- **Expression Evaluation**: Thread-safe if the underlying functions are thread-safe
- **Node Factories**: Immutable and thread-safe after construction
- **Expression Trees**: Immutable after parsing; safe for concurrent evaluation

---

## Limitations

- **No Operator Overloading**: Use function calls instead of operators (e.g., `add(a, b)` instead of `a + b`)
- **No Control Flow**: No if/else, loops, or other control flow constructs (expressions are purely functional)
- **No Variable Assignments**: Expressions are read-only and cannot modify state
- **Array/Object Literals**: Require appropriate factories to be registered
- **Reserved Keywords**: Keywords like `boolean`, `int`, `true`, `false`, `null`, `Class` cannot be used as function names

---

## Dependencies

```xml
<dependency>
    <groupId>org.antlr</groupId>
    <artifactId>antlr4-runtime</artifactId>
    <version>4.13.0</version>
</dependency>

<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-commons</artifactId>
</dependency>

<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-supply</artifactId>
</dependency>

<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-reflection</artifactId>
</dependency>

<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-dsl</artifactId>
</dependency>
```

---

## License

Part of the Garganttua Core framework. See main project LICENSE file.

---

## Version

Current version: **2.0.0-ALPHA01**

This is an alpha release. APIs may change in future versions.
