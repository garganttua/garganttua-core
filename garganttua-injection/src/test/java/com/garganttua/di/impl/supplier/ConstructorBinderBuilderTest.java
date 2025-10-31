package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.supplier.binder.IConstructorBinder;
import com.garganttua.injection.supplier.builder.binder.AbstractConstructorBinderBuilder;
import com.garganttua.injection.supplier.builder.supplier.FixedObjectSupplierBuilder;
import com.garganttua.injection.supplier.builder.supplier.NullObjectSupplierBuilder;

public class ConstructorBinderBuilderTest {

    public static class TargetClass {
        public final String name;
        public final int value;

        public TargetClass(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public TargetClass(String name) {
            this(name, 0);
        }

        public TargetClass() {
            this("default", -1);
        }
    }

    static class ConcreteConstructorBinderBuilder
            extends AbstractConstructorBinderBuilder<TargetClass, ConcreteConstructorBinderBuilder, Object> {

        public ConcreteConstructorBinderBuilder(Class<TargetClass> objectClass) {
            super(new Object(), objectClass);
        }

        @Override
        protected ConcreteConstructorBinderBuilder getBuilder() {
            return this;
        }

        @Override
        protected void doAutoDetection() throws DslException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'doAutoDetection'");
        }

        @Override
        public Set<Class<?>> getDependencies() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getDependencies'");
        }

    }

    private ConcreteConstructorBinderBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ConcreteConstructorBinderBuilder(TargetClass.class);
    }

    @Test
    void testBuildWithMatchingConstructorUsingRawValues() throws DslException, DiException {
        builder
                .withParam("Hello")
                .withParam(123);

        IConstructorBinder<TargetClass> binder = builder.build();
        assertNotNull(binder, "Binder should not be null");

        Optional<TargetClass> obj = binder.execute();
        assertTrue(obj.isPresent(), "Object should be created");
        assertInstanceOf(TargetClass.class, obj.get());
        TargetClass tc = (TargetClass) obj.get();
        assertEquals("Hello", tc.name);
        assertEquals(123, tc.value);
    }

    @Test
    void testBuildWithMatchingConstructorUsingSuppliers() throws DslException, DiException {
        builder
                .withParam(new FixedObjectSupplierBuilder<>("Dynamic"))
                .withParam(new FixedObjectSupplierBuilder<>(999));

        IConstructorBinder<TargetClass> binder = builder.build();
        TargetClass tc = binder.execute().get();
        assertEquals("Dynamic", tc.name);
        assertEquals(999, tc.value);
    }

    @Test
    void testBuildWithDefaultConstructor() throws DslException, DiException {

        IConstructorBinder<TargetClass> binder = builder.build();
        TargetClass tc = binder.execute().get();
        assertEquals("default", tc.name);
        assertEquals(-1, tc.value);
    }

    @Test
    void testThrowsIfNoConstructorMatches() throws DslException, DiException {
        builder.withParam("abc")
                .withParam("wrongType");

        assertThrows(DslException.class, builder::build, "Should throw if no matching constructor found");
    }

    @Test
    void testNullableParameterAccepted() throws DslException, DiException {
        builder.withParam(0, new NullObjectSupplierBuilder<String>(String.class), true);
        builder.withParam(1, 77);

        IConstructorBinder<TargetClass> binder = builder.build();
        TargetClass tc = binder.execute().get();
        assertNull(tc.name);
        assertEquals(77, tc.value);
    }

}