package com.garganttua.core.expression.context;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.expression.functions.StandardExpressionLeafs;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.supply.ISupplier;
import static com.garganttua.core.supply.dsl.NullSupplierBuilder.*;

public class ExpressionContextTest {

    // Test helper class for expression functions
    static class TestFunctions {
        public static Integer add(Integer a, Integer b) {
            return a + b;
        }
    }

    private ExpressionContext expressionContext;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setUp() throws Exception {
        // Create factories for StandardExpressionLeafs methods as expression leaves

        ExpressionNodeFactory<String, ISupplier<String>> stringFactory = new ExpressionNodeFactory<>(
                of(StandardExpressionLeafs.class).build(),
                (Class<ISupplier<String>>) (Class<?>) ISupplier.class,
                StandardExpressionLeafs.class.getMethod("String", String.class),
                new ObjectAddress("String"),
                List.of(false),
                Optional.of("string"),
                Optional.of("Converts a value to a String supplier"));

        ExpressionNodeFactory<Integer, ISupplier<Integer>> intFactory = new ExpressionNodeFactory<>(
                of(StandardExpressionLeafs.class).build(),
                (Class<ISupplier<Integer>>) (Class<?>) ISupplier.class,
                StandardExpressionLeafs.class.getMethod("Integer", String.class),
                new ObjectAddress("Integer"),
                List.of(false),
                Optional.of("int"),
                Optional.of("Parses a string to an Integer supplier"));

        ExpressionNodeFactory<Boolean, ISupplier<Boolean>> booleanFactory = new ExpressionNodeFactory<>(
                of(StandardExpressionLeafs.class).build(),
                (Class<ISupplier<Boolean>>) (Class<?>) ISupplier.class,
                StandardExpressionLeafs.class.getMethod("Boolean", String.class),
                new ObjectAddress("Boolean"),
                List.of(false),
                Optional.of("boolean"),
                Optional.of("Parses a string to a Boolean supplier"));

        ExpressionNodeFactory<Integer, ISupplier<Integer>> addFactory = new ExpressionNodeFactory<>(
                of(TestFunctions.class).build(),
                (Class<ISupplier<Integer>>) (Class<?>) ISupplier.class,
                TestFunctions.class.getMethod("add", Integer.class, Integer.class),
                new ObjectAddress("add"),
                List.of(false, false),
                Optional.of("add"),
                Optional.of("Adds two integer suppliers"));

        ExpressionNodeFactory<Integer, ISupplier<Integer>> classFactory = new ExpressionNodeFactory<>(
                of(StandardExpressionLeafs.class).build(),
                (Class<ISupplier<Integer>>) (Class<?>) ISupplier.class,
                StandardExpressionLeafs.class.getMethod("Class", String.class),
                new ObjectAddress("Class"),
                List.of(false),
                Optional.of("class"),
                Optional.of("Parses a string to a Class supplier"));

        // Create expression context with leaf and node factories
        Set<IExpressionNodeFactory<?, ? extends ISupplier<?>>> factories = Set.of(
                stringFactory,
                intFactory,
                booleanFactory,
                addFactory,
                classFactory
        );

        expressionContext = new ExpressionContext(factories);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSimpleStringExpression() throws Exception {
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
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSimpleIntegerExpression() throws Exception {
        // Parse a simple integer literal expression
        IExpression<?, ? extends ISupplier<?>> expression = expressionContext.expression("42");

        assertNotNull(expression, "Expression should not be null");

        // Evaluate the expression
        ISupplier<?> result = expression.evaluate();

        assertNotNull(result, "Result should not be null");

        // Get the actual value
        Optional<Integer> value = (Optional<Integer>) result.supply();

        assertTrue(value.isPresent(), "Value should be present");
        assertEquals(42, value.get(), "Value should be 42");
    }

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    @Test
    public void testAddExpression() throws Exception {
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
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testComplexAddExpression() throws Exception {
        // Parse an expression with a function call: add(42, 30)
        IExpression<?, ? extends ISupplier<?>> expression = expressionContext.expression("add(8,add(42, 30))");

        assertNotNull(expression, "Expression should not be null");

        // Evaluate the expression
        ISupplier<?> result = expression.evaluate();

        assertNotNull(result, "Result should not be null");

        // Get the actual value
        Optional<Integer> value = (Optional<Integer>) result.supply();

        assertTrue(value.isPresent(), "Value should be present");
        assertEquals(80, value.get(), "Value should be 80 ( 8+ (42 + 30))");
    }

    @Test
    public void testddExpression_wrongParamType() throws Exception {
        // Parse an expression with a function call: add(42, 30)
        ExpressionException exception = assertThrows(ExpressionException.class, () -> expressionContext.expression("add(8,add(toto, 30))"));

        assertEquals("Unknown function: add(String,Integer)", exception.getMessage());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrimitiveTypeExpression() throws Exception {

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
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testClassTypeExpression() throws Exception {

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
    }
}
