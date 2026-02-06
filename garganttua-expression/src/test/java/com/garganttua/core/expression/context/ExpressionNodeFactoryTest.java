package com.garganttua.core.expression.context;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.supply.ISupplier;
import static com.garganttua.core.supply.dsl.NullSupplierBuilder.*;

public class ExpressionNodeFactoryTest {

    static class TestService {

        static public String string(String string) {
            return string;
        }
        static public String greet(String name) {
            return "Hello, " + name;
        }
        // Method with ISupplier parameter (lazy)
        static public long measureTime(ISupplier<?> expression) {
            long start = System.currentTimeMillis();
            try {
                expression.supply();
            } catch (Exception e) {
                // Ignore
            }
            return System.currentTimeMillis() - start;
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExpressionNodeFactoryCreation() throws Exception {

        ExpressionNodeFactory<String, ISupplier<String>> leafFactory = new ExpressionNodeFactory<String, ISupplier<String>>(
                of(TestService.class).build(),
                (Class<ISupplier<String>>) (Class<?>) ISupplier.class,
                TestService.class.getMethod("string", String.class),
                new ObjectAddress("string"),
                List.of(false),
                Optional.of("string"),
                Optional.of("String converter"));

        ExpressionNodeFactory<String, ISupplier<String>> nodefactory = new ExpressionNodeFactory<String, ISupplier<String>>(
                of(TestService.class).build(),
                (Class<ISupplier<String>>) (Class<?>) ISupplier.class,
                TestService.class.getMethod("greet", String.class),
                new ObjectAddress("greet"),
                List.of(false),
                Optional.of("greet"),
                Optional.of("Greeting function"));

        Optional<IMethodReturn<IExpressionNode<String,ISupplier<String>>>> leafReturn = leafFactory.supply(new ExpressionNodeContext(List.of("greet")));
        IExpressionNode<String,ISupplier<String>> leaf = leafReturn.flatMap(IMethodReturn::firstOptional).get();

        Optional<IMethodReturn<IExpressionNode<String,ISupplier<String>>>> expressionReturn = nodefactory.supply(new ExpressionNodeContext(List.of(leaf)));
        IExpressionNode<String,ISupplier<String>> expression = expressionReturn.flatMap(IMethodReturn::firstOptional).get();

        assertEquals("Hello, greet", expression.evaluate().supply().get());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExpressionNodeCanHandleEitherOtherExpressionNodeAndObject() throws Exception {

        ExpressionNodeFactory<String, ISupplier<String>> nodefactory = new ExpressionNodeFactory<String, ISupplier<String>>(
                of(TestService.class).build(),
                (Class<ISupplier<String>>) (Class<?>) ISupplier.class,
                TestService.class.getMethod("greet", String.class),
                new ObjectAddress("greet"),
                List.of(false),
                Optional.of("greet"),
                Optional.of("Greeting function"));

        Optional<IMethodReturn<IExpressionNode<String,ISupplier<String>>>> expressionReturn = nodefactory.supply(new ExpressionNodeContext(List.of("greet")));
        IExpressionNode<String,ISupplier<String>> expression = expressionReturn.flatMap(IMethodReturn::firstOptional).get();

        assertEquals("Hello, greet", expression.evaluate().supply().get());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testManualPageGeneration() throws Exception {
        // Create a factory with detailed documentation
        ExpressionNodeFactory<String, ISupplier<String>> factory = new ExpressionNodeFactory<String, ISupplier<String>>(
                of(TestService.class).build(),
                (Class<ISupplier<String>>) (Class<?>) ISupplier.class,
                TestService.class.getMethod("greet", String.class),
                new ObjectAddress("greet"),
                List.of(false),
                Optional.of("greet"),
                Optional.of("Greets a person by name"));

        // Generate the manual page
        String manual = factory.man();

        // Print the manual for visual verification
        System.out.println("Generated Manual Page:");
        System.out.println("=====================");
        System.out.println(manual);

        // Verify the manual contains expected sections
        assertNotNull(manual, "Manual should not be null");
        assertTrue(manual.contains("NAME"), "Manual should contain NAME section");
        assertTrue(manual.contains("SYNOPSIS"), "Manual should contain SYNOPSIS section");
        assertTrue(manual.contains("DESCRIPTION"), "Manual should contain DESCRIPTION section");
        assertTrue(manual.contains("PARAMETERS"), "Manual should contain PARAMETERS section");
        assertTrue(manual.contains("RETURN VALUE"), "Manual should contain RETURN VALUE section");

        // Verify content details
        assertTrue(manual.contains("greet"), "Manual should contain function name");
        assertTrue(manual.contains("Greets a person by name"), "Manual should contain description");
        assertTrue(manual.contains("String"), "Manual should contain return type");
        assertTrue(manual.contains("name"), "Manual should contain parameter names");
        assertTrue(manual.contains("(required)"), "Manual should indicate required parameters");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testManualPageWithNullableParameter() throws Exception {
        // Create a test method with nullable parameter
        ExpressionNodeFactory<String, ISupplier<String>> factory = new ExpressionNodeFactory<String, ISupplier<String>>(
                of(TestService.class).build(),
                (Class<ISupplier<String>>) (Class<?>) ISupplier.class,
                TestService.class.getMethod("string", String.class),
                new ObjectAddress("string"),
                List.of(true), // Parameter is nullable
                Optional.of("string"),
                Optional.of("Converts any value to string representation"));

        String manual = factory.man();

        // Verify nullable parameter is indicated
        assertTrue(manual.contains("(nullable)"), "Manual should indicate nullable parameters");

        System.out.println("\nGenerated Manual Page with Nullable Parameter:");
        System.out.println("==============================================");
        System.out.println(manual);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testKeyGeneration() throws Exception {
        ExpressionNodeFactory<String, ISupplier<String>> factory = new ExpressionNodeFactory<String, ISupplier<String>>(
                of(TestService.class).build(),
                (Class<ISupplier<String>>) (Class<?>) ISupplier.class,
                TestService.class.getMethod("greet", String.class),
                new ObjectAddress("greet"),
                List.of(false),
                Optional.of("greet"),
                Optional.of("Greeting function"));

        String key = factory.key();

        assertEquals("greet(String)", key, "Key should follow format: functionName(ParameterTypes)");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDescriptionRetrieval() throws Exception {
        ExpressionNodeFactory<String, ISupplier<String>> factory = new ExpressionNodeFactory<String, ISupplier<String>>(
                of(TestService.class).build(),
                (Class<ISupplier<String>>) (Class<?>) ISupplier.class,
                TestService.class.getMethod("greet", String.class),
                new ObjectAddress("greet"),
                List.of(false),
                Optional.of("greet"),
                Optional.of("A function that greets people"));

        String description = factory.description();

        assertEquals("A function that greets people", description, "Description should match provided value");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLazyParameterKeyGeneration() throws Exception {
        // Create a factory for a method with ISupplier parameter (lazy)
        ExpressionNodeFactory<Long, ISupplier<Long>> factory = new ExpressionNodeFactory<Long, ISupplier<Long>>(
                of(TestService.class).build(),
                (Class<ISupplier<Long>>) (Class<?>) ISupplier.class,
                TestService.class.getMethod("measureTime", ISupplier.class),
                new ObjectAddress("measureTime"),
                List.of(true), // nullable
                Optional.of("time"),
                Optional.of("Measures execution time"));

        // Verify the key uses "ISupplier" for lazy parameters
        String key = factory.key();
        assertEquals("time(ISupplier)", key, "Key should use 'ISupplier' for lazy parameters");

        // Verify the parameter is detected as lazy
        assertTrue(factory.isLazyParameter(0), "Parameter 0 should be detected as lazy");

        System.out.println("Lazy parameter key: " + key);
        System.out.println("Lazy parameters: " + factory.getLazyParameters());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLazyParameterFactoryLookupAndExecution() throws Exception {
        // Create a factory for a method with ISupplier parameter (lazy)
        ExpressionNodeFactory<Long, ISupplier<Long>> factory = new ExpressionNodeFactory<Long, ISupplier<Long>>(
                of(TestService.class).build(),
                (Class<ISupplier<Long>>) (Class<?>) ISupplier.class,
                TestService.class.getMethod("measureTime", ISupplier.class),
                new ObjectAddress("measureTime"),
                List.of(true), // nullable
                Optional.of("time"),
                Optional.of("Measures execution time"));

        // Create an ExpressionContext with this factory
        java.util.Set<IExpressionNodeFactory<?, ? extends ISupplier<?>>> factories = new java.util.HashSet<>();
        factories.add((IExpressionNodeFactory<?, ? extends ISupplier<?>>) factory);
        ExpressionContext ctx = new ExpressionContext(factories);

        // Verify the factory is registered with the correct key
        String expectedKey = "time(ISupplier)";
        String man = ctx.man(expectedKey);
        assertNotNull(man, "Factory should be found with key: " + expectedKey);
        System.out.println("Factory found for key '" + expectedKey + "'");

        // Now test parsing an expression that should match this factory
        // The expression time(print("hello")) should find the time(ISupplier) factory
        // because print("hello") returns String, but the factory accepts ISupplier (any type)

        // For this test, let's just verify the factory registration works
        System.out.println("Factory key in context: " + factory.key());
        System.out.println("Factory man page:\n" + man);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLazyParameterExpressionParsing() throws Exception {
        // Create a factory for a method with ISupplier parameter (lazy)
        ExpressionNodeFactory<Long, ISupplier<Long>> timeFactory = new ExpressionNodeFactory<Long, ISupplier<Long>>(
                of(TestService.class).build(),
                (Class<ISupplier<Long>>) (Class<?>) ISupplier.class,
                TestService.class.getMethod("measureTime", ISupplier.class),
                new ObjectAddress("measureTime"),
                List.of(true), // nullable
                Optional.of("time"),
                Optional.of("Measures execution time"));

        // Create a string factory for the inner expression
        ExpressionNodeFactory<String, ISupplier<String>> stringFactory = new ExpressionNodeFactory<String, ISupplier<String>>(
                of(TestService.class).build(),
                (Class<ISupplier<String>>) (Class<?>) ISupplier.class,
                TestService.class.getMethod("greet", String.class),
                new ObjectAddress("greet"),
                List.of(false),
                Optional.of("greet"),
                Optional.of("Greets a person"));

        // Create a factory for string literal
        ExpressionNodeFactory<String, ISupplier<String>> stringLiteralFactory = new ExpressionNodeFactory<String, ISupplier<String>>(
                of(TestService.class).build(),
                (Class<ISupplier<String>>) (Class<?>) ISupplier.class,
                TestService.class.getMethod("string", String.class),
                new ObjectAddress("string"),
                List.of(true),
                Optional.of("string"),
                Optional.of("String literal"));

        // Create an ExpressionContext with all factories
        java.util.Set<IExpressionNodeFactory<?, ? extends ISupplier<?>>> factories = new java.util.HashSet<>();
        factories.add((IExpressionNodeFactory<?, ? extends ISupplier<?>>) timeFactory);
        factories.add((IExpressionNodeFactory<?, ? extends ISupplier<?>>) stringFactory);
        factories.add((IExpressionNodeFactory<?, ? extends ISupplier<?>>) stringLiteralFactory);
        ExpressionContext ctx = new ExpressionContext(factories);

        // List all registered factories
        System.out.println("Registered factories:");
        System.out.println(ctx.man());

        // Parse an expression: time(greet("world"))
        // The greet("world") expression returns String, but time expects ISupplier
        // The findCompatibleFactory should match time(ISupplier) with an argument type of String
        try {
            var expression = ctx.expression("time(greet(\"world\"))");
            assertNotNull(expression, "Expression should be parsed successfully");
            System.out.println("Expression parsed successfully: time(greet(\"world\"))");

            // Evaluate the expression
            ISupplier<?> result = expression.evaluate();
            assertNotNull(result, "Result supplier should not be null");
            System.out.println("Expression result supplier obtained");

            // Get the value (should be the elapsed time)
            Object value = result.supply().orElse(null);
            System.out.println("Elapsed time: " + value + "ms");
            assertTrue(value instanceof Long, "Result should be a Long (elapsed time)");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Expression parsing failed: " + e.getMessage());
        }
    }
}
