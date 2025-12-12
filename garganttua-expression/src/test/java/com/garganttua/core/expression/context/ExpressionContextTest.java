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

        // Factory for string(String) - expression leaf that converts string to String supplier
        ExpressionNodeFactory<String, ISupplier<String>> stringFactory = new ExpressionNodeFactory<>(
                StandardExpressionLeafs.class,
                (Class<ISupplier<String>>) (Class<?>) ISupplier.class,
                StandardExpressionLeafs.class.getMethod("String", String.class),
                new ObjectAddress("String"),
                List.of(false),
                true, // This is a leaf node
                Optional.of("string"),
                Optional.of("Converts a value to a String supplier"));

        // Factory for int(String) - expression leaf that converts string to Integer supplier
        ExpressionNodeFactory<Integer, ISupplier<Integer>> intFactory = new ExpressionNodeFactory<>(
                StandardExpressionLeafs.class,
                (Class<ISupplier<Integer>>) (Class<?>) ISupplier.class,
                StandardExpressionLeafs.class.getMethod("Integer", String.class),
                new ObjectAddress("Integer"),
                List.of(false),
                true, // This is a leaf node
                Optional.of("int"),
                Optional.of("Parses a string to an Integer supplier"));

        // Factory for add(ISupplier<Integer>, ISupplier<Integer>) - expression node that adds two integers
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
}
