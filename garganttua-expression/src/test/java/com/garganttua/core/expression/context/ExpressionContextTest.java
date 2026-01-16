package com.garganttua.core.expression.context;

import static com.garganttua.core.supply.dsl.NullSupplierBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.expression.dsl.ExpressionContextBuilder;
import com.garganttua.core.expression.functions.Expressions;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.supply.ISupplier;

public class ExpressionContextTest {

    // Test helper class for expression functions
    static class TestFunctions {
        public static Integer add(Integer a, Integer b) {
            return a + b;
        }
    }

    private IExpressionContext expressionContext;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setUp() throws Exception {
        // Create factories for StandardExpressionLeafs methods as expression leaves
        ObjectReflectionHelper.setAnnotationScanner(new ReflectionsAnnotationScanner());

        ExpressionNodeFactory<String, ISupplier<String>> stringFactory = new ExpressionNodeFactory<>(
                of(Expressions.class).build(),
                (Class<ISupplier<String>>) (Class<?>) ISupplier.class,
                Expressions.class.getMethod("String", String.class),
                new ObjectAddress("String"),
                List.of(false),
                Optional.of("string"),
                Optional.of("Converts a value to a String supplier"));

        ExpressionNodeFactory<Integer, ISupplier<Integer>> intFactory = new ExpressionNodeFactory<>(
                of(Expressions.class).build(),
                (Class<ISupplier<Integer>>) (Class<?>) ISupplier.class,
                Expressions.class.getMethod("Integer", String.class),
                new ObjectAddress("Integer"),
                List.of(false),
                Optional.of("int"),
                Optional.of("Parses a string to an Integer supplier"));

        ExpressionNodeFactory<Boolean, ISupplier<Boolean>> booleanFactory = new ExpressionNodeFactory<>(
                of(Expressions.class).build(),
                (Class<ISupplier<Boolean>>) (Class<?>) ISupplier.class,
                Expressions.class.getMethod("Boolean", String.class),
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
                of(Expressions.class).build(),
                (Class<ISupplier<Integer>>) (Class<?>) ISupplier.class,
                Expressions.class.getMethod("Class", String.class),
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
                classFactory);

        expressionContext = new ExpressionContext(factories);
    }

    @Test
    public void test() {

        IInjectionContextBuilder injectionContextBuilder = InjectionContext.builder();
        ExpressionContextBuilder expressionContextBuilder = ExpressionContextBuilder.builder();
        expressionContextBuilder.withPackage("com.garganttua").autoDetect(true).provide(injectionContextBuilder);

        injectionContextBuilder.build().onInit().onStart();
        IExpressionContext expressionContext = expressionContextBuilder.build();

        ISupplier<?> exp = expressionContext.expression("man()").evaluate();

        assertTrue( ((String) exp.supply().get()).contains("AVAILABLE EXPRESSION FUNCTIONS"));
    }

    @Test
    public void testUnknownMethod() {

        IInjectionContextBuilder injectionContextBuilder = InjectionContext.builder();
        ExpressionContextBuilder expressionContextBuilder = ExpressionContextBuilder.builder();
        expressionContextBuilder.withPackage("com.garganttua").autoDetect(true).provide(injectionContextBuilder);

        injectionContextBuilder.build().onInit().onStart();
        IExpressionContext expressionContext = expressionContextBuilder.build();
        ExpressionException exception = assertThrows(ExpressionException.class, () -> expressionContext.expression("unknown()").evaluate());

        assertEquals("Unknown function: unknown()", exception.getMessage());
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
        ExpressionException exception = assertThrows(ExpressionException.class,
                () -> expressionContext.expression("add(8,add(toto, 30))"));

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

    @Test
    public void testManualPageRetrieval() {
        // Test retrieving manual for the "add" function
        String manual = expressionContext.man("add(Integer,Integer)");

        assertNotNull(manual, "Manual should not be null");
        assertTrue(manual.contains("NAME"), "Manual should contain NAME section");
        assertTrue(manual.contains("add"), "Manual should contain function name");
        assertTrue(manual.contains("Adds two integer suppliers"), "Manual should contain description");
        assertTrue(manual.contains("SYNOPSIS"), "Manual should contain SYNOPSIS section");
        assertTrue(manual.contains("PARAMETERS"), "Manual should contain PARAMETERS section");
        assertTrue(manual.contains("RETURN VALUE"), "Manual should contain RETURN VALUE section");
        assertTrue(manual.contains("Integer"), "Manual should contain Integer type");

        System.out.println("\n=== Manual for add(Integer,Integer) ===");
        System.out.println(manual);
    }

    @Test
    public void testManualPageRetrievalForString() {
        // Test retrieving manual for the "string" function
        String manual = expressionContext.man("string(String)");

        assertNotNull(manual, "Manual should not be null");
        assertTrue(manual.contains("string"), "Manual should contain function name");
        assertTrue(manual.contains("Converts a value to a String supplier"), "Manual should contain description");

        System.out.println("\n=== Manual for string(String) ===");
        System.out.println(manual);
    }

    @Test
    public void testManualPageRetrievalForNonExistentKey() {
        // Test retrieving manual for a non-existent function
        String manual = expressionContext.man("nonExistent(String)");

        assertNull(manual, "Manual should be null for non-existent key");
    }

    @Test
    public void testManualPageRetrievalNullKey() {
        // Test that null key throws NullPointerException
        assertThrows(NullPointerException.class, () -> {
            expressionContext.man(null);
        }, "Should throw NullPointerException for null key");
    }

    @Test
    public void testListFactories() {
        // Test listing all available factories
        String factoryList = expressionContext.man();

        assertNotNull(factoryList, "Factory list should not be null");
        assertTrue(factoryList.contains("AVAILABLE EXPRESSION FUNCTIONS"), "Should contain header");
        assertTrue(factoryList.contains("Total functions:"), "Should contain total count");
        assertTrue(factoryList.contains("add(Integer,Integer)"), "Should contain add function");
        assertTrue(factoryList.contains("string(String)"), "Should contain string function");
        assertTrue(factoryList.contains("int(String)"), "Should contain int function");
        assertTrue(factoryList.contains("boolean(String)"), "Should contain boolean function");
        assertTrue(factoryList.contains("class(String)"), "Should contain class function");
        assertTrue(factoryList.contains("Adds two integer suppliers"), "Should contain add description");
        assertTrue(factoryList.contains("Converts a value to a String supplier"), "Should contain string description");
        assertTrue(factoryList.contains("Use man(\"key\")"), "Should contain usage hint");

        // Verify index format is present
        assertTrue(factoryList.contains("[1]"), "Should contain index [1]");
        assertTrue(factoryList.contains("[2]"), "Should contain index [2]");
        assertTrue(factoryList.contains("[3]"), "Should contain index [3]");

        System.out.println("\n=== List of Available Expression Factories ===");
        System.out.println(factoryList);
    }

    @Test
    public void testListFactoriesIsSorted() {
        // Test that the factory list is sorted alphabetically by key
        String factoryList = expressionContext.man();

        // Extract all function keys from the output
        String[] lines = factoryList.split("\n");
        String previousKey = "";
        int expectedIndex = 1;

        for (String line : lines) {
            // Skip header and footer lines
            if (line.trim().startsWith("AVAILABLE") || line.trim().startsWith("====") ||
                    line.trim().startsWith("Total") || line.trim().startsWith("Use") || line.trim().isEmpty()) {
                continue;
            }

            // Verify index is sequential
            assertTrue(line.contains("[" + expectedIndex + "]"),
                    "Line should contain index [" + expectedIndex + "]: " + line);
            expectedIndex++;

            // Extract key (remove index prefix and everything after '-')
            String withoutIndex = line.trim().replaceFirst("\\[\\d+\\]\\s*", "");
            String currentKey = withoutIndex.split("-")[0].trim();

            // Verify alphabetical order
            assertTrue(currentKey.compareTo(previousKey) >= 0,
                    "Keys should be sorted alphabetically: " + previousKey + " should come before " + currentKey);

            previousKey = currentKey;
        }
    }

    @Test
    public void testManualPageRetrievalByIndex() {
        // Test retrieving manual by index
        // Index 1 should be "add(Integer,Integer)" since it's first alphabetically
        String manual = expressionContext.man(1);

        assertNotNull(manual, "Manual should not be null for valid index");
        assertTrue(manual.contains("NAME"), "Manual should contain NAME section");
        assertTrue(manual.contains("add"), "Manual should contain function name");
        assertTrue(manual.contains("SYNOPSIS"), "Manual should contain SYNOPSIS section");

        System.out.println("\n=== Manual for index 1 ===");
        System.out.println(manual);
    }

    @Test
    public void testManualPageRetrievalByIndexOutOfBounds() {
        // Test with index too high
        String manual = expressionContext.man(999);
        assertNull(manual, "Manual should be null for index out of bounds");

        // Test with index 0
        String manualZero = expressionContext.man(0);
        assertNull(manualZero, "Manual should be null for index 0");

        // Test with negative index
        String manualNegative = expressionContext.man(-1);
        assertNull(manualNegative, "Manual should be null for negative index");
    }

    @Test
    public void testManualPageRetrievalByIndexMatchesKey() {
        // Get the factory list to see which key is at index 1
        String factoryList = expressionContext.man();

        // Get manual by index 1
        String manualByIndex = expressionContext.man(1);

        // The first entry alphabetically should be "add(Integer,Integer)"
        String manualByKey = expressionContext.man("add(Integer,Integer)");

        // They should be the same
        assertEquals(manualByKey, manualByIndex,
                "Manual retrieved by index should match manual retrieved by key");
    }

    @Test
    public void testAllIndicesAccessible() {
        // Test that all indices from 1 to total count are accessible
        String factoryList = expressionContext.man();

        // Parse the total count
        int totalFunctions = 5; // We know there are 5 factories in the test setup

        // Try to get manual for each index
        for (int i = 1; i <= totalFunctions; i++) {
            String manual = expressionContext.man(i);
            assertNotNull(manual, "Manual should not be null for index " + i);
            assertTrue(manual.contains("NAME"), "Manual for index " + i + " should contain NAME section");
        }

        // Index beyond total should return null
        String beyondTotal = expressionContext.man(totalFunctions + 1);
        assertNull(beyondTotal, "Manual should be null for index beyond total");
    }
}
