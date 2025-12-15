# Garganttua Expression Module

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

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-expression</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

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

```java
import com.garganttua.core.expression.context.ExpressionContext;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.supply.ISupplier;

// Create an expression context with factories
ExpressionContext context = new ExpressionContext(factories);

// Parse and evaluate a simple expression
IExpression<?, ? extends ISupplier<?>> expr = context.expression("\"Hello World\"");
ISupplier<?> result = expr.evaluate();
Optional<String> value = (Optional<String>) result.supply();

System.out.println(value.get()); // Prints: Hello World
```

### Numeric Expressions

```java
// Integer literal
IExpression<?, ?> intExpr = context.expression("42");
Optional<Integer> intValue = (Optional<Integer>) intExpr.evaluate().supply();
// intValue.get() == 42

// Function call with arguments
IExpression<?, ?> addExpr = context.expression("add(10, 32)");
Optional<Integer> sum = (Optional<Integer>) addExpr.evaluate().supply();
// sum.get() == 42

// Nested function calls
IExpression<?, ?> nestedExpr = context.expression("add(8, add(30, 4))");
Optional<Integer> nestedSum = (Optional<Integer>) nestedExpr.evaluate().supply();
// nestedSum.get() == 42
```

### Type Expressions

```java
// Primitive types
IExpression<?, ?> intType = context.expression("int");
Optional<Class<?>> intClass = (Optional<Class<?>>) intType.evaluate().supply();
// intClass.get() == int.class

IExpression<?, ?> boolType = context.expression("boolean");
Optional<Class<?>> boolClass = (Optional<Class<?>>) boolType.evaluate().supply();
// boolClass.get() == boolean.class

// Class types
IExpression<?, ?> stringType = context.expression("java.lang.String");
Optional<Class<?>> stringClass = (Optional<Class<?>>) stringType.evaluate().supply();
// stringClass.get() == String.class

// Generic types
IExpression<?, ?> classType = context.expression("Class<?>");
Optional<Class<?>> classClass = (Optional<Class<?>>) classType.evaluate().supply();
// classClass.get() == Class.class
```

### Building Expression Contexts with DSL

```java
import com.garganttua.core.expression.dsl.ExpressionContextBuilder;
import com.garganttua.core.expression.context.IExpressionContext;

// Create context with auto-detection
IExpressionContext context = ExpressionContextBuilder.builder()
    .withPackage("com.myapp.expressions")
    .autoDetect(true)
    .build();

// Create context with manual registration
IExpressionContext manualContext = ExpressionContextBuilder.builder()
    .withExpressionLeaf(MyFunctions.class, String.class)
        .method("processString")
        .up()
    .withExpressionNode(Calculator.class, Integer.class)
        .method("add")
        .up()
    .build();
```

### Creating Custom Expression Leafs

```java
import com.garganttua.core.expression.annotations.ExpressionLeaf;

public class CustomExpressionLeafs {

    @ExpressionLeaf(name = "uppercase", description = "Converts string to uppercase")
    public static String toUpperCase(@Nullable String input) {
        return input != null ? input.toUpperCase() : null;
    }

    @ExpressionLeaf(name = "multiply", description = "Multiplies two integers")
    public static Integer multiply(@Nullable Integer a, @Nullable Integer b) {
        if (a == null || b == null) return null;
        return a * b;
    }
}

// Usage:
// context.expression("uppercase(\"hello\")") → "HELLO"
// context.expression("multiply(6, 7)") → 42
```

### Creating Custom Expression Nodes

```java
import com.garganttua.core.expression.annotations.ExpressionNode;
import com.garganttua.core.supply.ISupplier;

public class MathExpressions {

    @ExpressionNode(name = "average", description = "Calculates average of two numbers")
    public static ISupplier<Double> average(
            ISupplier<Integer> a,
            ISupplier<Integer> b) {

        return () -> {
            Optional<Integer> valA = a.supply();
            Optional<Integer> valB = b.supply();

            if (valA.isEmpty() || valB.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of((valA.get() + valB.get()) / 2.0);
        };
    }
}

// Usage:
// context.expression("average(10, 20)") → 15.0
// context.expression("average(add(5, 5), multiply(2, 15))") → 20.0
```

### Integration with Dependency Injection

```java
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.injection.annotations.Singleton;

@Singleton
public class ConfigurableService {

    @Expression("add(10, 32)")
    private ISupplier<Integer> configValue;

    @Expression("java.lang.String")
    private ISupplier<Class<?>> targetType;

    public void process() {
        Optional<Integer> value = configValue.supply();
        value.ifPresent(v -> System.out.println("Config value: " + v));

        Optional<Class<?>> type = targetType.supply();
        type.ifPresent(t -> System.out.println("Target type: " + t.getName()));
    }
}

// Configure in DiContextBuilder:
ExpressionContextBuilder expressionBuilder = ExpressionContextBuilder.builder()
    .withPackage("com.myapp.expressions")
    .autoDetect(true);

IExpressionContext expressionContext = expressionBuilder.build();

DiContextBuilder.builder()
    .withPackage("com.myapp")
    .autoDetect(true)
    .build();

// Register expression resolver
expressionBuilder.context(diContextBuilder);
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

The module provides built-in conversion functions in `StandardExpressionLeafs`:

### Primitive Converters

| Function | Description | Example |
|----------|-------------|---------|
| `string(String)` | String value | `string("hello")` |
| `int(String)` | Parse to Integer | `int("42")` |
| `long(String)` | Parse to Long | `long("1000")` |
| `double(String)` | Parse to Double | `double("3.14")` |
| `float(String)` | Parse to Float | `float("2.5")` |
| `boolean(String)` | Parse to Boolean | `boolean("true")` |
| `byte(String)` | Parse to Byte | `byte("127")` |
| `short(String)` | Parse to Short | `short("1000")` |
| `char(String)` | Extract first char | `char("A")` |

### Type Converters

| Function | Description | Example |
|----------|-------------|---------|
| `class(String)` | Load class by name | `class("java.lang.String")` |
| `class(String)` | Primitive type | `class("int")` → `int.class` |

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

### Contextual Expressions

Contextual expressions can access runtime context during evaluation:

```java
@ExpressionNode(name = "contextualAdd", description = "Adds with context")
public static IContextualSupplier<Integer, IExpressionContext> contextualAdd(
        ISupplier<Integer> a,
        ISupplier<Integer> b) {

    return (context) -> {
        // Access context during evaluation
        Optional<Integer> valA = a.supply();
        Optional<Integer> valB = b.supply();

        if (valA.isEmpty() || valB.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(valA.get() + valB.get());
    };
}
```

### Nullable Parameter Handling

Expression factories support nullable parameters:

```java
ExpressionNodeFactory<String, ISupplier<String>> factory = new ExpressionNodeFactory<>(
    MyClass.class,
    ISupplier.class,
    MyClass.class.getMethod("process", String.class, Integer.class),
    new ObjectAddress("process"),
    List.of(false, true),  // Second parameter is nullable
    true,
    Optional.of("process"),
    Optional.of("Process with optional integer")
);
```

### Auto-Detection

Enable automatic discovery of expression nodes:

```java
IExpressionContext context = ExpressionContextBuilder.builder()
    .withPackage("com.myapp.expressions")
    .withPackage("com.myapp.functions")
    .autoDetect(true)  // Scan for @ExpressionLeaf and @ExpressionNode
    .build();
```

The auto-detection will find all methods annotated with:
- `@ExpressionLeaf` - for leaf nodes
- `@ExpressionNode` - for composite nodes

## Error Handling

### Expression Parsing Errors

```java
try {
    IExpression<?, ?> expr = context.expression("invalid syntax");
} catch (ExpressionException e) {
    // Handle parsing error
    logger.error("Failed to parse expression", e);
}
```

### Type Mismatch Errors

```java
// This will throw ExpressionException at parse time:
context.expression("add(\"string\", 42)");
// Error: Unknown function: add(String,Integer)
// Expected: add(Integer,Integer)
```

### Runtime Evaluation Errors

```java
IExpression<?, ?> expr = context.expression("int(\"not-a-number\")");
try {
    ISupplier<?> result = expr.evaluate();
    result.supply();  // Throws ExpressionException
} catch (ExpressionException e) {
    // Handle conversion error
}
```

## Testing

The module includes comprehensive tests in `ExpressionContextTest`:

```java
@Test
public void testSimpleStringExpression() throws Exception {
    IExpression<?, ?> expression = context.expression("\"Hello World\"");
    ISupplier<?> result = expression.evaluate();
    Optional<String> value = (Optional<String>) result.supply();

    assertTrue(value.isPresent());
    assertEquals("Hello World", value.get());
}

@Test
public void testComplexAddExpression() throws Exception {
    IExpression<?, ?> expression = context.expression("add(8, add(42, 30))");
    ISupplier<?> result = expression.evaluate();
    Optional<Integer> value = (Optional<Integer>) result.supply();

    assertTrue(value.isPresent());
    assertEquals(80, value.get());
}

@Test
public void testTypeExpression() throws Exception {
    IExpression<?, ?> intType = context.expression("int");
    ISupplier<?> result = intType.evaluate();
    Optional<Class<?>> value = (Optional<Class<?>>) result.supply();

    assertTrue(value.isPresent());
    assertEquals(int.class, value.get());
}
```

## Tips and Best Practices

### 1. Use Specific Expression Leaf Names

Choose clear, unambiguous names for your expression functions:

```java
// Good
@ExpressionLeaf(name = "parseDate", description = "...")
@ExpressionLeaf(name = "formatCurrency", description = "...")

// Avoid
@ExpressionLeaf(name = "process", description = "...")
@ExpressionLeaf(name = "convert", description = "...")
```

### 2. Provide Type-Safe Factories

Always specify correct parameter types and return types:

```java
ExpressionNodeFactory<Integer, ISupplier<Integer>> factory = new ExpressionNodeFactory<>(
    Calculator.class,
    ISupplier.class,
    Calculator.class.getMethod("add", Integer.class, Integer.class),
    new ObjectAddress("add"),
    List.of(false, false),  // Neither parameter is nullable
    false,  // Not a leaf (composite node)
    Optional.of("add"),
    Optional.of("Adds two integers")
);
```

### 3. Handle Null Values Appropriately

Use `@Nullable` and nullable parameter lists correctly:

```java
@ExpressionLeaf(name = "processOptional", description = "...")
public static String processOptional(@Nullable String input) {
    if (input == null) {
        return "default";
    }
    return input.toUpperCase();
}
```

### 4. Use Auto-Detection for Large Projects

For projects with many expression functions, use auto-detection:

```java
// Organize expression functions in dedicated packages
com.myapp.expressions.math
com.myapp.expressions.string
com.myapp.expressions.date

// Enable auto-detection
ExpressionContextBuilder.builder()
    .withPackage("com.myapp.expressions")
    .autoDetect(true)
    .build();
```

### 5. Leverage Logging for Debugging

Enable trace-level logging to debug expression parsing:

```xml
<logger name="com.garganttua.core.expression" level="TRACE"/>
```

Logs will show:
- Expression parsing flow
- Node factory lookups
- Parameter type matching
- Expression evaluation steps

### 6. Cache Expression Contexts

Expression contexts are thread-safe and should be reused:

```java
public class ExpressionService {
    private static final IExpressionContext CONTEXT = createContext();

    private static IExpressionContext createContext() {
        return ExpressionContextBuilder.builder()
            .withPackage("com.myapp.expressions")
            .autoDetect(true)
            .build();
    }

    public IExpression<?, ?> parse(String expr) {
        return CONTEXT.expression(expr);
    }
}
```

### 7. Validate Expressions Early

Parse expressions during configuration/startup rather than at runtime:

```java
@Configuration
public class ExpressionConfig {
    @Bean
    public List<IExpression<?, ?>> precompiledExpressions(IExpressionContext context) {
        return List.of(
            context.expression("add(10, 32)"),
            context.expression("java.lang.String"),
            context.expression("multiply(6, 7)")
        );
    }
}
```

### 8. Document Complex Expressions

For complex domain-specific expressions, provide clear documentation:

```java
@ExpressionNode(
    name = "calculateDiscount",
    description = "Calculates discount based on price and percentage. " +
                  "Usage: calculateDiscount(100, 10) → 90.0 (10% off 100)"
)
public static ISupplier<Double> calculateDiscount(
        ISupplier<Double> price,
        ISupplier<Double> percentage) {
    // Implementation
}
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
