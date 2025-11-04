package com.garganttua.core.supplying;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimpleSupplier implements IObjectSupplier<String> {
    @Override
    public Optional<String> supply() {
        return Optional.of("Hello World");
    }

    @Override
    public Class<String> getSuppliedType() {
        return String.class;
    }
}

class ContextualSupplier implements IContextualObjectSupplier<String, Integer> {
    @Override
    public Class<Integer> getOwnerContextClass() {
        return Integer.class;
    }

    @Override
    public Optional<String> supply(Integer context, Object... contexts) {
        return Optional.of("Value is " + context);
    }

    @Override
    public Class<String> getSuppliedType() {
        return String.class;
    }
}

class FailingSupplier implements IContextualObjectSupplier<String, Double> {
    @Override
    public Class<Double> getOwnerContextClass() {
        return Double.class;
    }

    @Override
    public Optional<String> supply(Double context, Object... contexts) {
        return Optional.of("Double value: " + context);
    }

    @Override
    public Class<String> getSuppliedType() {
        return String.class;
    }
}

public class SupplierTest {

    @Test
    @DisplayName("should get object from simple supplier")
    void testSimpleSupplier() throws Exception {
        SimpleSupplier supplier = new SimpleSupplier();

        String result = Supplier.contextualSupply(supplier);

        assertEquals("Hello World", result);
    }

    @Test
    @DisplayName("should get object from contextual supplier with matching context")
    void testContextualSupplierWithContext() throws Exception {
        ContextualSupplier supplier = new ContextualSupplier();

        String result = Supplier.contextualSupply(supplier, "not an int", 42, 3.14);

        assertEquals("Value is 42", result);
    }

    @Test
    @DisplayName("should throw DiException when no compatible context found")
    void testContextualSupplierWithoutContext() {
        FailingSupplier supplier = new FailingSupplier();

        SupplyException ex = assertThrows(
            SupplyException.class,
            () -> Supplier.contextualSupply(supplier, "string", 42)
        );

        assertTrue(ex.getMessage().contains("No compatible context found"));
    }

    @Test
    @DisplayName("should throw DiException when supplier is null")
    void testNullSupplier() {
        assertThrows(SupplyException.class, () -> Supplier.contextualSupply(null));
    }
}

