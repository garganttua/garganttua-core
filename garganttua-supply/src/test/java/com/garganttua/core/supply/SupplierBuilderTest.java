package com.garganttua.core.supply;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.IConstructorBinder;
import com.garganttua.core.reflection.binders.IContextualConstructorBinder;
import com.garganttua.core.supply.dsl.ICommonSupplierBuilder;
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
        public Set<Class<?>> dependencies() {
            throw new UnsupportedOperationException("Unimplemented method 'getDependencies'");
        }

        @Override
        public Class<T> getConstructedType() {
            throw new UnsupportedOperationException("Unimplemented method 'getConstructedType'");
        }

        @Override
        public Constructor<?> constructor() {
            throw new UnsupportedOperationException("Unimplemented method 'constructor'");
        }

        @Override
        public Optional<T> supply() throws SupplyException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'supply'");
        }

        @Override
        public Type getSuppliedType() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getSuppliedType'");
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
        public Set<Class<?>> dependencies() {
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

        @Override
        public Constructor<?> constructor() {
            throw new UnsupportedOperationException("Unimplemented method 'constructor'");
        }

        @Override
        public Type getSuppliedType() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getSuppliedType'");
        }

        @Override
        public Optional<T> supply(Void ownerContext, Object... otherContexts) throws SupplyException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'supply'");
        }
    }

    static class FakeContextualSupply<T, C>
            implements IContextualSupply<T, C> {

        @Override
        public Optional<T> supply(C context, Object... otherContexts) {
            throw new UnsupportedOperationException("Unimplemented method 'supplyObject'");
        }
    }

    // Helpers to build SupplierBuilder
    private <T> ICommonSupplierBuilder<T> builder(Class<T> type) {
        return new SupplierBuilder<>(type);
    }

    // ----------------------------------------------------------------------
    // 1) VALUE PRESENT
    // ----------------------------------------------------------------------

    @Test
    void testValueSupplier() throws DslException {
        var b = builder(String.class).withValue("hello");
        var s = b.build();
        assertTrue(s instanceof NullableSupplier);
        assertTrue(((NullableSupplier<?>) s).getDelegate() instanceof FixedSupplier);
    }

    @Test
    void testValueNullable() throws DslException {
        var b = builder(String.class).withValue("hello").nullable(true);
        var s = b.build();
        assertTrue(s instanceof NullableSupplier);
        assertTrue(((NullableSupplier<?>) s).isNullable());
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
        assertTrue(s instanceof NullableContextualSupplier);
        assertTrue(((NullableContextualSupplier<?, ?>) s).getDelegate() instanceof NewContextualSupplier);
    }

    @Test
    void testContextWithContextualCtorNullable() throws DslException {
        var b = builder(String.class)
                .withContext(Integer.class, new FakeContextualSupply<>())
                .withConstructor(new FakeContextualConstructorBinder<>())
                .nullable(true);

        var s = b.build();
        assertTrue(s instanceof NullableContextualSupplier);
        assertTrue(((NullableContextualSupplier<?, ?>) s).isNullable());
    }

    // ----------------------------------------------------------------------
    // 3) CONTEXT + NO CTOR
    // ----------------------------------------------------------------------

    @Test
    void testContextWithoutCtor() throws DslException {
        var b = builder(String.class)
                .withContext(Integer.class, new FakeContextualSupply<>());

        var s = b.build();
        assertTrue(s instanceof NullableContextualSupplier);
        assertTrue(((NullableContextualSupplier<?, ?>) s).getDelegate() instanceof ContextualSupplier);
    }

    @Test
    void testContextWithoutCtorNullable() throws DslException {
        var b = builder(String.class)
                .withContext(Integer.class, new FakeContextualSupply<>())
                .nullable(true);

        var s = b.build();
        assertTrue(s instanceof NullableContextualSupplier);
        assertTrue(((NullableContextualSupplier<?, ?>) s).isNullable());
    }

    // ----------------------------------------------------------------------
    // 4) CTOR ONLY
    // ----------------------------------------------------------------------

    @Test
    void testCtorOnly() throws DslException {
        var b = builder(String.class)
                .withConstructor(new FakeConstructorBinder<>());

        var s = b.build();
        assertTrue(s instanceof NullableSupplier);
        assertTrue(((NullableSupplier<?>) s).getDelegate() instanceof NewSupplier);
    }

    @Test
    void testCtorOnlyNullable() throws DslException {
        var b = builder(String.class)
                .withConstructor(new FakeConstructorBinder<>())
                .nullable(true);

        var s = b.build();
        assertTrue(s instanceof NullableSupplier);
        assertTrue(((NullableSupplier<?>) s).isNullable());
    }

    // ----------------------------------------------------------------------
    // 5) NOTHING DEFINED → NullSupplier
    // ----------------------------------------------------------------------

    @Test
    void testDefaultNullSupplier() throws DslException {
        var b = builder(String.class);
        var s = b.build();
        assertTrue(s instanceof NullableSupplier);
        assertTrue(((NullableSupplier<?>) s).getDelegate() instanceof NullSupplier);
    }

    @Test
    void testDefaultNullSupplierNullable() throws DslException {
        var b = builder(String.class).nullable(true);
        var s = b.build();

        assertTrue(s instanceof NullableSupplier);
        assertTrue(((NullableSupplier<?>) s).isNullable());
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