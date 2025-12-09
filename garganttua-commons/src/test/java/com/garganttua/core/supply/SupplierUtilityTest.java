package com.garganttua.core.supply;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Type;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Test class for the {@link Supplier} utility class.
 * Tests both contextual and recursive supply methods without using mocks.
 */
public class SupplierUtilityTest {

    // ========== Test Helper Classes ==========

    /**
     * Simple non-contextual supplier that returns a fixed string.
     */
    private static class SimpleStringSupplier implements ISupplier<String> {
        private final String value;

        public SimpleStringSupplier(String value) {
            this.value = value;
        }

        @Override
        public Optional<String> supply() throws SupplyException {
            return Optional.ofNullable(value);
        }

        @Override
        public Type getSuppliedType() {
            return String.class;
        }
    }

    /**
     * Simple non-contextual supplier that returns a fixed integer.
     */
    private static class SimpleIntegerSupplier implements ISupplier<Integer> {
        private final Integer value;

        public SimpleIntegerSupplier(Integer value) {
            this.value = value;
        }

        @Override
        public Optional<Integer> supply() throws SupplyException {
            return Optional.ofNullable(value);
        }

        @Override
        public Type getSuppliedType() {
            return Integer.class;
        }
    }

    /**
     * Contextual supplier that requires a String context.
     */
    private static class ContextualStringSupplier implements IContextualSupplier<String, String> {
        private final String prefix;

        public ContextualStringSupplier(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Optional<String> supply(String context, Object... otherContexts) throws SupplyException {
            return Optional.of(prefix + context);
        }

        @Override
        public Class<String> getOwnerContextType() {
            return String.class;
        }

        @Override
        public Type getSuppliedType() {
            return String.class;
        }
    }

    /**
     * Contextual supplier that requires an Integer context.
     */
    private static class ContextualIntegerSupplier implements IContextualSupplier<Integer, Integer> {
        private final int multiplier;

        public ContextualIntegerSupplier(int multiplier) {
            this.multiplier = multiplier;
        }

        @Override
        public Optional<Integer> supply(Integer context, Object... otherContexts) throws SupplyException {
            return Optional.of(context * multiplier);
        }

        @Override
        public Class<Integer> getOwnerContextType() {
            return Integer.class;
        }

        @Override
        public Type getSuppliedType() {
            return Integer.class;
        }
    }

    /**
     * Supplier that returns another supplier (for testing recursive supply).
     */
    private static class NestedSupplier implements ISupplier<ISupplier<String>> {
        private final ISupplier<String> innerSupplier;

        public NestedSupplier(ISupplier<String> innerSupplier) {
            this.innerSupplier = innerSupplier;
        }

        @Override
        public Optional<ISupplier<String>> supply() throws SupplyException {
            return Optional.ofNullable(innerSupplier);
        }

        @Override
        public Type getSuppliedType() {
            return ISupplier.class;
        }
    }

    /**
     * Deeply nested supplier (3 levels) for testing deep recursion.
     */
    private static class DeeplyNestedSupplier implements ISupplier<ISupplier<ISupplier<String>>> {
        private final String finalValue;

        public DeeplyNestedSupplier(String finalValue) {
            this.finalValue = finalValue;
        }

        @Override
        public Optional<ISupplier<ISupplier<String>>> supply() throws SupplyException {
            return Optional.of(new NestedSupplier(new SimpleStringSupplier(finalValue)));
        }

        @Override
        public Type getSuppliedType() {
            return ISupplier.class;
        }
    }

    /**
     * Custom context class for testing context matching.
     */
    private static class CustomContext {
        private final String data;

        public CustomContext(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }
    }

    /**
     * Contextual supplier that uses CustomContext.
     */
    private static class CustomContextSupplier implements IContextualSupplier<String, CustomContext> {

        @Override
        public Optional<String> supply(CustomContext context, Object... otherContexts) throws SupplyException {
            return Optional.of("Custom: " + context.getData());
        }

        @Override
        public Class<CustomContext> getOwnerContextType() {
            return CustomContext.class;
        }

        @Override
        public Type getSuppliedType() {
            return String.class;
        }
    }

    /**
     * Contextual supplier that accepts Void context (no context needed).
     */
    private static class VoidContextSupplier implements IContextualSupplier<String, Void> {

        @Override
        public Optional<String> supply(Void context, Object... otherContexts) throws SupplyException {
            return Optional.of("No context needed");
        }

        @Override
        public Class<Void> getOwnerContextType() {
            return Void.class;
        }

        @Override
        public Type getSuppliedType() {
            return String.class;
        }
    }

    // ========== Tests for contextualSupply ==========

    @Test
    void testContextualSupply_SimpleNonContextualSupplier() throws SupplyException {
        ISupplier<String> supplier = new SimpleStringSupplier("Hello World");

        String result = Supplier.contextualSupply(supplier);

        assertEquals("Hello World", result);
    }

    @Test
    void testContextualSupply_NonContextualSupplierReturnsNull() throws SupplyException {
        ISupplier<String> supplier = new SimpleStringSupplier(null);

        String result = Supplier.contextualSupply(supplier);

        assertNull(result);
    }

    @Test
    void testContextualSupply_ContextualSupplierWithMatchingContext() throws SupplyException {
        IContextualSupplier<String, String> supplier = new ContextualStringSupplier("Hello, ");

        String result = Supplier.contextualSupply(supplier, "World");

        assertEquals("Hello, World", result);
    }

    @Test
    void testContextualSupply_ContextualSupplierWithMultipleContexts() throws SupplyException {
        IContextualSupplier<Integer, Integer> supplier = new ContextualIntegerSupplier(10);

        // Pass multiple contexts, should match the Integer one
        Integer result = Supplier.contextualSupply(supplier, "ignored", 5, "also ignored");

        assertEquals(50, result);
    }

    @Test
    void testContextualSupply_ContextualSupplierWithCustomContext() throws SupplyException {
        IContextualSupplier<String, CustomContext> supplier = new CustomContextSupplier();
        CustomContext context = new CustomContext("test data");

        String result = Supplier.contextualSupply(supplier, context);

        assertEquals("Custom: test data", result);
    }

    @Test
    void testContextualSupply_ContextualSupplierWithoutMatchingContext() {
        IContextualSupplier<String, String> supplier = new ContextualStringSupplier("Hello, ");

        // No String context provided
        SupplyException exception = assertThrows(SupplyException.class, () -> {
            Supplier.contextualSupply(supplier, 42, new Object());
        });

        assertTrue(exception.getMessage().contains("No compatible context found"));
    }

    @Test
    void testContextualSupply_VoidContextSupplier() throws SupplyException {
        IContextualSupplier<String, Void> supplier = new VoidContextSupplier();

        String result = Supplier.contextualSupply(supplier);

        assertEquals("No context needed", result);
    }

    @Test
    void testContextualSupply_NullSupplier() {
        SupplyException exception = assertThrows(SupplyException.class, () -> {
            Supplier.contextualSupply(null);
        });

        assertEquals("Supplier cannot be null", exception.getMessage());
    }

    @Test
    void testContextualSupply_ContextualSupplierWithNullContextArray() {
        IContextualSupplier<String, String> supplier = new ContextualStringSupplier("Hello, ");

        SupplyException exception = assertThrows(SupplyException.class, () -> {
            Supplier.contextualSupply(supplier, (Object[]) null);
        });

        assertTrue(exception.getMessage().contains("No compatible context found"));
    }

    // ========== Tests for contextualRecursiveSupply ==========

    @Test
    void testContextualRecursiveSupply_NonNestedSupplier() throws SupplyException {
        ISupplier<String> supplier = new SimpleStringSupplier("Direct value");

        String result = (String) Supplier.contextualRecursiveSupply(supplier);

        assertEquals("Direct value", result);
    }

    @Test
    void testContextualRecursiveSupply_SingleLevelNesting() throws SupplyException {
        ISupplier<String> innerSupplier = new SimpleStringSupplier("Nested value");
        ISupplier<ISupplier<String>> outerSupplier = new NestedSupplier(innerSupplier);

        // contextualRecursiveSupply resolves all nested suppliers to the final value
        String result = (String) Supplier.contextualRecursiveSupply(outerSupplier);

        assertEquals("Nested value", result);
    }

    @Test
    void testContextualRecursiveSupply_DeepNesting() throws SupplyException {
        ISupplier<ISupplier<ISupplier<String>>> deeplyNested = new DeeplyNestedSupplier("Deep value");

        // contextualRecursiveSupply resolves all 3 levels to the final String value
        String result = (String) Supplier.contextualRecursiveSupply(deeplyNested);

        assertEquals("Deep value", result);
    }

    @Test
    void testContextualRecursiveSupply_NestedSupplierReturnsNull() throws SupplyException {
        ISupplier<ISupplier<String>> supplier = new NestedSupplier(null);

        Object result = Supplier.contextualRecursiveSupply(supplier);

        assertNull(result);
    }

    @Test
    void testContextualRecursiveSupply_IntermediateSupplierReturnsNull() throws SupplyException {
        ISupplier<String> nullSupplier = new SimpleStringSupplier(null);
        ISupplier<ISupplier<String>> outerSupplier = new NestedSupplier(nullSupplier);

        // When intermediate supplier returns null, recursion stops and returns null
        Object result = Supplier.contextualRecursiveSupply(outerSupplier);

        assertNull(result);
    }

    @Test
    void testContextualRecursiveSupply_WithContextualSupplier() throws SupplyException {
        // Create a nested structure with contextual supplier at the end
        IContextualSupplier<String, String> contextualSupplier = new ContextualStringSupplier("Prefix: ");

        @SuppressWarnings("unchecked")
        ISupplier<ISupplier<String>> outerSupplier = (ISupplier<ISupplier<String>>) (ISupplier<?>) new NestedSupplier((ISupplier<String>) contextualSupplier);

        // contextualRecursiveSupply resolves the nested structure and applies context
        String result = (String) Supplier.contextualRecursiveSupply(outerSupplier, "context value");

        assertEquals("Prefix: context value", result);
    }

    @Test
    void testContextualRecursiveSupply_MixedContextualAndNonContextual() throws SupplyException {
        // Create a simple nested structure where inner supplier wraps a value that's already computed
        ISupplier<Integer> innerSupplier = new SimpleIntegerSupplier(21);

        // Create a generic nested supplier for Integer
        ISupplier<ISupplier<Integer>> outerSupplier = new ISupplier<ISupplier<Integer>>() {
            @Override
            public Optional<ISupplier<Integer>> supply() throws SupplyException {
                return Optional.of(innerSupplier);
            }

            @Override
            public Type getSuppliedType() {
                return ISupplier.class;
            }
        };

        // contextualRecursiveSupply resolves both levels
        Integer result = (Integer) Supplier.contextualRecursiveSupply(outerSupplier);

        assertEquals(21, result);
    }

    @Test
    void testContextualRecursiveSupply_NullSupplier() {
        SupplyException exception = assertThrows(SupplyException.class, () -> {
            Supplier.contextualRecursiveSupply(null);
        });

        assertEquals("Supplier cannot be null", exception.getMessage());
    }

    @Test
    void testContextualRecursiveSupply_ContextualSupplierWithoutContext() {
        IContextualSupplier<String, String> contextualSupplier = new ContextualStringSupplier("Prefix: ");

        @SuppressWarnings("unchecked")
        ISupplier<ISupplier<String>> outerSupplier = (ISupplier<ISupplier<String>>) (ISupplier<?>) new NestedSupplier((ISupplier<String>) contextualSupplier);

        // No context provided for contextual supplier
        SupplyException exception = assertThrows(SupplyException.class, () -> {
            Supplier.contextualRecursiveSupply(outerSupplier);
        });

        assertTrue(exception.getMessage().contains("No compatible context found"));
    }

    // ========== Edge Cases and Integration Tests ==========

    @Test
    void testContextualSupply_NonContextualWithIgnoredContexts() throws SupplyException {
        ISupplier<Integer> supplier = new SimpleIntegerSupplier(42);

        // Contexts should be ignored for non-contextual suppliers
        Integer result = Supplier.contextualSupply(supplier, "ignored", 999, new Object());

        assertEquals(42, result);
    }

    @Test
    void testContextualRecursiveSupply_VeryDeepNesting() throws SupplyException {
        // Create a 5-level deep nesting manually
        ISupplier<?> level5 = new SimpleStringSupplier("Bottom");
        ISupplier<?> level4 = new NestedSupplier((ISupplier<String>) level5);
        ISupplier<?> level3 = new ISupplier<ISupplier<?>>() {
            @Override
            public Optional<ISupplier<?>> supply() throws SupplyException {
                return Optional.of(level4);
            }

            @Override
            public Type getSuppliedType() {
                return ISupplier.class;
            }
        };
        ISupplier<?> level2 = new ISupplier<ISupplier<?>>() {
            @Override
            public Optional<ISupplier<?>> supply() throws SupplyException {
                return Optional.of(level3);
            }

            @Override
            public Type getSuppliedType() {
                return ISupplier.class;
            }
        };
        ISupplier<?> level1 = new ISupplier<ISupplier<?>>() {
            @Override
            public Optional<ISupplier<?>> supply() throws SupplyException {
                return Optional.of(level2);
            }

            @Override
            public Type getSuppliedType() {
                return ISupplier.class;
            }
        };

        String result = (String) Supplier.contextualRecursiveSupply((ISupplier<String>) level1);

        assertEquals("Bottom", result);
    }

    @Test
    void testContextualSupply_SubtypeContextMatching() throws SupplyException {
        // Test that subtype contexts are matched correctly
        class ParentContext {
            String value = "parent";
        }

        class ChildContext extends ParentContext {
            @SuppressWarnings("unused")
            String childValue = "child";
        }

        IContextualSupplier<String, ParentContext> supplier = new IContextualSupplier<String, ParentContext>() {
            @Override
            public Optional<String> supply(ParentContext context, Object... otherContexts) throws SupplyException {
                return Optional.of("Received: " + context.value);
            }

            @Override
            public Class<ParentContext> getOwnerContextType() {
                return ParentContext.class;
            }

            @Override
            public Type getSuppliedType() {
                return String.class;
            }
        };

        // Pass a child context - should match due to isAssignableFrom
        ChildContext childContext = new ChildContext();
        String result = Supplier.contextualSupply(supplier, childContext);

        assertEquals("Received: parent", result);
    }
}
