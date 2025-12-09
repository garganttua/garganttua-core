package com.garganttua.core.supply;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Type;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.dsl.ContextualSupplierBuilder;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

public class SupplierTest {

    @Test
    public void testSimpleFixedStringSupplier() throws DslException, SupplyException {

        ISupplierBuilder<String, ISupplier<String>> b = new ISupplierBuilder<String, ISupplier<String>>() {

            @Override
            public ISupplier<String> build() throws DslException {
                return new ISupplier<String>() {

                    @Override
                    public Optional<String> supply() throws SupplyException {
                        return Optional.of("Hello");
                    }

                    @Override
                    public Type getSuppliedType() {
                        return String.class;
                    }
                };
            }

            @Override
            public Type getSuppliedType() {
                return String.class;
            }

            @Override
            public boolean isContextual() {
                throw new UnsupportedOperationException("Unimplemented method 'isContextual'");
            }

        };

        ISupplier<String> supplier = (ISupplier<String>) b.build();

        assertEquals("Hello", supplier.supply().get());
    }

    @Test
    public void testFixedObjectSupplier() throws SupplyException, DslException {
        FixedSupplierBuilder<String> builder = new FixedSupplierBuilder<String>("hello");

        ISupplier<String> supplier = builder.build();

        assertEquals("hello", supplier.supply().get());
    }

    @Test
    public void testApplicationContextObjectSupplier() throws DslException, SupplyException {

        IContextualSupply<String, Object> supply = new IContextualSupply<String, Object>() {

            @Override
            public Optional<String> supply(Object context, Object... contexts) {
                return Optional.of("hello from context");
            }

        };

        ISupplierBuilder<String, IContextualSupplier<String, Object>> builder = new ContextualSupplierBuilder<String, Object>(
                supply, String.class, Object.class);

        IContextualSupplier<String, Object> supplier = builder.build();

        assertEquals("hello from context", supplier.supply(new Object()).get());

    }

    @Test
    void testApplicationContextObjectLambdaSupplier() throws DslException, SupplyException {

        IContextualSupply<String, Object> supply = (context, contexts) -> Optional.of("hello from context");

        ISupplierBuilder<String, IContextualSupplier<String, Object>> builder = new ContextualSupplierBuilder<String, Object>(
                supply, String.class, Object.class);

        IContextualSupplier<String, Object> supplier = builder.build();

        assertEquals("hello from context", supplier.supply(new Object()).get());
    }

    @Test
    void testCustomContextObjectLambdaSupplier() throws DslException, SupplyException {

        IContextualSupply<String, String> supply = (context, contexts) -> Optional
                .of("hello from context " + context);

        ContextualSupplierBuilder<String, String> builder = new ContextualSupplierBuilder<String, String>(
                supply, String.class, String.class);

        IContextualSupplier<String, String> supplier = builder.build();

        assertEquals("hello from context string context", supplier.supply("string context").get());
    }

}
