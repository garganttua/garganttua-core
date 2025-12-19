package com.garganttua.core.expression.context;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.IExpressionNode;
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

        Optional<IExpressionNode<String,ISupplier<String>>> leaf = leafFactory.supply(new ExpressionNodeContext(List.of("greet")));

        Optional<IExpressionNode<String,ISupplier<String>>> expression = nodefactory.supply(new ExpressionNodeContext(List.of(leaf.get())));

        assertEquals("Hello, greet", expression.get().evaluate().supply().get());
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

        Optional<IExpressionNode<String,ISupplier<String>>> expression = nodefactory.supply(new ExpressionNodeContext(List.of("greet")));

        assertEquals("Hello, greet", expression.get().evaluate().supply().get());
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
        assertTrue(manual.contains("arg0"), "Manual should contain parameter names");
        assertTrue(manual.contains("(required)"), "Manual should indicate required parameters");

        // Print the manual for visual verification
        System.out.println("Generated Manual Page:");
        System.out.println("=====================");
        System.out.println(manual);
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
}
