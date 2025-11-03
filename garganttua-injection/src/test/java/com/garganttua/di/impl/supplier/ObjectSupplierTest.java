package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IContextualObjectSupplier;
import com.garganttua.core.injection.IContextualObjectSupply;
import com.garganttua.core.injection.ICustomContextualObjectSupply;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;
import com.garganttua.injection.supplier.builder.supplier.ContextualObjectSupplierBuilder;
import com.garganttua.injection.supplier.builder.supplier.CustomContextualObjectSupplierBuilder;
import com.garganttua.injection.supplier.builder.supplier.FixedObjectSupplierBuilder;

public class ObjectSupplierTest {

    @Test
    public void testSimpleFixedStringSupplier() throws DiException, DslException {

        IObjectSupplierBuilder<String, IObjectSupplier<String>> b = new IObjectSupplierBuilder<String, IObjectSupplier<String>>() {

            @Override
            public IObjectSupplier<String> build() throws DslException {
                return new IObjectSupplier<String>() {

                    @Override
                    public Optional<String> getObject() throws DiException {
                        return Optional.of("Hello");
                    }

                    @Override
                    public Class<String> getObjectClass() {
                        return String.class;
                    }
                };
            }

            @Override
            public Class<String> getObjectClass() {
                return String.class;
            }

        };

        IObjectSupplier<String> supplier = (IObjectSupplier<String>) b.build();

        assertEquals("Hello", supplier.getObject().get());
    }

    @Test
    public void testFixedObjectSupplier() throws DslException, DiException {
        FixedObjectSupplierBuilder<String> builder = new FixedObjectSupplierBuilder<String>("hello");

        IObjectSupplier<String> supplier = builder.build();

        assertEquals("hello", supplier.getObject().get());
    }

    @Test
    public void testApplicationContextObjectSupplier() throws DiException, DslException {

        IContextualObjectSupply<String> supply = new IContextualObjectSupply<String>() {

            @Override
            public Optional<String> supplyObject(IDiContext context) {
                return Optional.of("hello from context");
            }

        };

        IObjectSupplierBuilder<String, IContextualObjectSupplier<String, IDiContext>> builder = new ContextualObjectSupplierBuilder<String>(
                supply, String.class);

        IContextualObjectSupplier<String, IDiContext> supplier = builder.build();

        assertEquals("hello from context", supplier.getObject(new DummyDiContext() {
        }).get());

    }

    @Test
    void testApplicationContextObjectLambdaSupplier() throws DslException, DiException {

        IContextualObjectSupply<String> supply = (context) -> Optional.of("hello from context");

        IObjectSupplierBuilder<String, IContextualObjectSupplier<String, IDiContext>> builder = new ContextualObjectSupplierBuilder<String>(
                supply, String.class);

        IContextualObjectSupplier<String, IDiContext> supplier = builder.build();

        assertEquals("hello from context", supplier.getObject(new DummyDiContext() {
        }).get());
    }

    @Test
    void testCustomContextObjectLambdaSupplier() throws DslException, DiException {

        ICustomContextualObjectSupply<String, String> supply = (context) -> Optional
                .of("hello from context " + context);

        CustomContextualObjectSupplierBuilder<String, String> builder = new CustomContextualObjectSupplierBuilder<String, String>(
                supply, String.class, String.class);

        IContextualObjectSupplier<String, String> supplier = builder.build();

        assertEquals("hello from context string context", supplier.getObject("string context").get());
    }

}
