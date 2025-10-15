package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.supplier.IContextualObjectSupplier;
import com.garganttua.injection.spec.supplier.IObjectSupplier;
import com.garganttua.injection.supplier.Supplier;

class SimpleSupplier implements IObjectSupplier<String> {
    @Override
    public Optional<String> getObject() {
        return Optional.of("Hello World");
    }

    @Override
    public Class<String> getObjectClass() {
        return String.class;
    }
}

class ContextualSupplier implements IContextualObjectSupplier<String, Integer> {
    @Override
    public Class<Integer> getContextClass() {
        return Integer.class;
    }

    @Override
    public Optional<String> getObject(Integer context) {
        return Optional.of("Value is " + context);
    }

    @Override
    public Class<String> getObjectClass() {
        return String.class;
    }
}

class FailingSupplier implements IContextualObjectSupplier<String, Double> {
    @Override
    public Class<Double> getContextClass() {
        return Double.class;
    }

    @Override
    public Optional<String> getObject(Double context) {
        return Optional.of("Double value: " + context);
    }

    @Override
    public Class<String> getObjectClass() {
        return String.class;
    }
}

public class SupplierTest {

    @Test
    @DisplayName("should get object from simple supplier")
    void testSimpleSupplier() throws Exception {
        SimpleSupplier supplier = new SimpleSupplier();

        String result = Supplier.getObject(supplier);

        assertEquals("Hello World", result);
    }

    @Test
    @DisplayName("should get object from contextual supplier with matching context")
    void testContextualSupplierWithContext() throws Exception {
        ContextualSupplier supplier = new ContextualSupplier();

        String result = Supplier.getObject(supplier, "not an int", 42, 3.14);

        assertEquals("Value is 42", result);
    }

    @Test
    @DisplayName("should throw DiException when no compatible context found")
    void testContextualSupplierWithoutContext() {
        FailingSupplier supplier = new FailingSupplier();

        DiException ex = assertThrows(
            DiException.class,
            () -> Supplier.getObject(supplier, "string", 42)
        );

        assertTrue(ex.getMessage().contains("No compatible context found"));
    }

    @Test
    @DisplayName("should throw DiException when supplier is null")
    void testNullSupplier() {
        assertThrows(DiException.class, () -> Supplier.getObject(null));
    }
}

