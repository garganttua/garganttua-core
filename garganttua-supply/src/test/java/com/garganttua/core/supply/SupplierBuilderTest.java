package com.garganttua.core.supply;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.IConstructorBinder;
import com.garganttua.core.reflection.binders.IContextualConstructorBinder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.supply.dsl.SupplierBuilder;

class SupplierBuilderTest {

    // --- Fake implementations for testing -------------------------

    static class FakeConstructorBinder<T> implements IConstructorBinder<T> {

        @Override
        public String getExecutableReference() {
            throw new UnsupportedOperationException("Unimplemented method 'getExecutableReference'");
        }

        @Override
        public Optional<T> execute() throws ReflectionException {
            throw new UnsupportedOperationException("Unimplemented method 'execute'");
        }

        @Override
        public Set<Class<?>> getDependencies() {
            throw new UnsupportedOperationException("Unimplemented method 'getDependencies'");
        }

        @Override
        public Class<T> getConstructedType() {
            throw new UnsupportedOperationException("Unimplemented method 'getConstructedType'");
        }
    }

    static class FakeContextualConstructorBinder<T>
            implements IContextualConstructorBinder<T> {

        @Override
        public Class<T> getConstructedType() {
            throw new UnsupportedOperationException("Unimplemented method 'getConstructedType'");
        }

        @Override
        public String getExecutableReference() {
            throw new UnsupportedOperationException("Unimplemented method 'getExecutableReference'");
        }

        @Override
        public Set<Class<?>> getDependencies() {
            throw new UnsupportedOperationException("Unimplemented method 'getDependencies'");
        }

        @Override
        public Class<?>[] getParametersContextTypes() {
            throw new UnsupportedOperationException("Unimplemented method 'getParametersContextTypes'");
        }

        @Override
        public Optional<T> execute(Void ownerContext, Object... contexts) throws ReflectionException {
            throw new UnsupportedOperationException("Unimplemented method 'execute'");
        }
    }

    static class FakeContextualSupply<T, C>
            implements IContextualObjectSupply<T, C> {

        @Override
        public Optional<T> supplyObject(C context, Object... otherContexts) {
            throw new UnsupportedOperationException("Unimplemented method 'supplyObject'");
        }
    }

    // Helpers to build SupplierBuilder
    private <T> ISupplierBuilder<T> builder(Class<T> type) {
        return new SupplierBuilder<>(type);
    }

    // ----------------------------------------------------------------------
    // 1) VALUE PRESENT
    // ----------------------------------------------------------------------

    @Test
    void testValueSupplier() throws DslException {
        var b = builder(String.class).withValue("hello");
        var s = b.build();
        assertTrue(s instanceof NullableObjectSupplier);
        assertTrue(((NullableObjectSupplier<?>) s).getDelegate() instanceof FixedObjectSupplier);
    }

    @Test
    void testValueNullable() throws DslException {
        var b = builder(String.class).withValue("hello").nullable(true);
        var s = b.build();
        assertTrue(s instanceof NullableObjectSupplier);
        assertTrue(((NullableObjectSupplier<?>) s).isNullable());
    }

    // ----------------------------------------------------------------------
    // 2) CONTEXT + CTOR
    // ----------------------------------------------------------------------

    @Test
    void testContextWithContextualCtor() throws DslException {
        var b = builder(String.class)
                .withContext(Integer.class, new FakeContextualSupply<>())
                .withConstructor(new FakeContextualConstructorBinder<>());

        var s = b.build();
        assertTrue(s instanceof NullableContextualObjectSupplier);
        assertTrue(((NullableContextualObjectSupplier<?, ?>) s).getDelegate() instanceof NewContextualObjectSupplier);
    }

    @Test
    void testContextWithContextualCtorNullable() throws DslException {
        var b = builder(String.class)
                .withContext(Integer.class, new FakeContextualSupply<>())
                .withConstructor(new FakeContextualConstructorBinder<>())
                .nullable(true);

        var s = b.build();
        assertTrue(s instanceof NullableContextualObjectSupplier);
        assertTrue(((NullableContextualObjectSupplier<?, ?>) s).isNullable());
    }

    // ----------------------------------------------------------------------
    // 3) CONTEXT + NO CTOR
    // ----------------------------------------------------------------------

    @Test
    void testContextWithoutCtor() throws DslException {
        var b = builder(String.class)
                .withContext(Integer.class, new FakeContextualSupply<>());

        var s = b.build();
        assertTrue(s instanceof NullableContextualObjectSupplier);
        assertTrue(((NullableContextualObjectSupplier<?, ?>) s).getDelegate() instanceof ContextualObjectSupplier);
    }

    @Test
    void testContextWithoutCtorNullable() throws DslException {
        var b = builder(String.class)
                .withContext(Integer.class, new FakeContextualSupply<>())
                .nullable(true);

        var s = b.build();
        assertTrue(s instanceof NullableContextualObjectSupplier);
        assertTrue(((NullableContextualObjectSupplier<?, ?>) s).isNullable());
    }

    // ----------------------------------------------------------------------
    // 4) CTOR ONLY
    // ----------------------------------------------------------------------

    @Test
    void testCtorOnly() throws DslException {
        var b = builder(String.class)
                .withConstructor(new FakeConstructorBinder<>());

        var s = b.build();
        assertTrue(s instanceof NullableObjectSupplier);
        assertTrue(((NullableObjectSupplier<?>) s).getDelegate() instanceof NewObjectSupplier);
    }

    @Test
    void testCtorOnlyNullable() throws DslException {
        var b = builder(String.class)
                .withConstructor(new FakeConstructorBinder<>())
                .nullable(true);

        var s = b.build();
        assertTrue(s instanceof NullableObjectSupplier);
        assertTrue(((NullableObjectSupplier<?>) s).isNullable());
    }

    // ----------------------------------------------------------------------
    // 5) NOTHING DEFINED → NullObjectSupplier
    // ----------------------------------------------------------------------

    @Test
    void testDefaultNullSupplier() throws DslException {
        var b = builder(String.class);
        var s = b.build();
        assertTrue(s instanceof NullableObjectSupplier);
        assertTrue(((NullableObjectSupplier<?>) s).getDelegate() instanceof NullObjectSupplier);
    }

    @Test
    void testDefaultNullSupplierNullable() throws DslException {
        var b = builder(String.class).nullable(true);
        var s = b.build();

        assertTrue(s instanceof NullableObjectSupplier);
        assertTrue(((NullableObjectSupplier<?>) s).isNullable());
    }

    // ----------------------------------------------------------------------
    // 6) ERROR CASE: context + non-contextual ctor
    // ----------------------------------------------------------------------

    @Test
    void testInvalidContextConstructor() {
        var b = builder(String.class)
                .withContext(Integer.class, new FakeContextualSupply<>());

        assertThrows(DslException.class, () -> b.withConstructor(new FakeConstructorBinder<>()).build());
    }

}