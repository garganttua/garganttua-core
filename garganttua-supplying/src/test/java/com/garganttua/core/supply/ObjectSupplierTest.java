package com.garganttua.core.supplying;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.IContextualObjectSupplier;
import com.garganttua.core.supply.IContextualObjectSupply;
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.SupplyException;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;
import com.garganttua.core.supplying.dsl.ContextualObjectSupplierBuilder;
import com.garganttua.core.supplying.dsl.FixedObjectSupplierBuilder;

public class ObjectSupplierTest {

    @Test
    public void testSimpleFixedStringSupplier() throws DslException, SupplyException {

        IObjectSupplierBuilder<String, IObjectSupplier<String>> b = new IObjectSupplierBuilder<String, IObjectSupplier<String>>() {

            @Override
            public IObjectSupplier<String> build() throws DslException {
                return new IObjectSupplier<String>() {

                    @Override
                    public Optional<String> supply() throws SupplyException {
                        return Optional.of("Hello");
                    }

                    @Override
                    public Class<String> getSuppliedType() {
                        return String.class;
                    }
                };
            }

            @Override
            public Class<String> getSuppliedType() {
                return String.class;
            }

            @Override
            public boolean isContextual() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'isContextual'");
            }

        };

        IObjectSupplier<String> supplier = (IObjectSupplier<String>) b.build();

        assertEquals("Hello", supplier.supply().get());
    }

    @Test
    public void testFixedObjectSupplier() throws SupplyException, DslException {
        FixedObjectSupplierBuilder<String> builder = new FixedObjectSupplierBuilder<String>("hello");

        IObjectSupplier<String> supplier = builder.build();

        assertEquals("hello", supplier.supply().get());
    }

    @Test
    public void testApplicationContextObjectSupplier() throws DslException, SupplyException {

        IContextualObjectSupply<String, Object> supply = new IContextualObjectSupply<String, Object>() {

            @Override
            public Optional<String> supplyObject(Object context, Object... contexts) {
                return Optional.of("hello from context");
            }

        };

        IObjectSupplierBuilder<String, IContextualObjectSupplier<String, Object>> builder = new ContextualObjectSupplierBuilder<String, Object>(
                supply, String.class, Object.class);

        IContextualObjectSupplier<String, Object> supplier = builder.build();

        assertEquals("hello from context", supplier.supply(new Object()).get());

    }

    @Test
    void testApplicationContextObjectLambdaSupplier() throws DslException, SupplyException {

        IContextualObjectSupply<String, Object> supply = (context, contexts) -> Optional.of("hello from context");

        IObjectSupplierBuilder<String, IContextualObjectSupplier<String, Object>> builder = new ContextualObjectSupplierBuilder<String, Object>(
                supply, String.class, Object.class);

        IContextualObjectSupplier<String, Object> supplier = builder.build();

        assertEquals("hello from context", supplier.supply(new Object()).get());
    }

    @Test
    void testCustomContextObjectLambdaSupplier() throws DslException, SupplyException {

        IContextualObjectSupply<String, String> supply = (context, contexts) -> Optional
                .of("hello from context " + context);

        ContextualObjectSupplierBuilder<String, String> builder = new ContextualObjectSupplierBuilder<String, String>(
                supply, String.class, String.class);

        IContextualObjectSupplier<String, String> supplier = builder.build();

        assertEquals("hello from context string context", supplier.supply("string context").get());
    }

}
