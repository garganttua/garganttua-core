package com.garganttua.core.reflection.binders;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.dsl.AbstractConstructorBinderBuilder;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
import com.garganttua.core.supply.dsl.NullSupplierBuilder;

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
        protected void doAutoDetection() throws DslException {

        }

    }

    private ConcreteConstructorBinderBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ConcreteConstructorBinderBuilder(TargetClass.class);
    }

    @Test
    void testBuildWithMatchingConstructorUsingRawValues() throws DslException, ReflectionException {
        builder
                .withParam("Hello")
                .withParam(123);

        IConstructorBinder<TargetClass> binder = builder.build();
        assertNotNull(binder, "Binder should not be null");

        Optional<? extends TargetClass> obj = binder.execute();
        assertTrue(obj.isPresent(), "Object should be created");
        assertInstanceOf(TargetClass.class, obj.get());
        TargetClass tc = (TargetClass) obj.get();
        assertEquals("Hello", tc.name);
        assertEquals(123, tc.value);
    }

    @Test
    void testBuildWithMatchingConstructorUsingSuppliers() throws DslException, ReflectionException {
        builder
                .withParam(new FixedSupplierBuilder<>("Dynamic"))
                .withParam(new FixedSupplierBuilder<>(999));

        IConstructorBinder<TargetClass> binder = builder.build();
        TargetClass tc = binder.execute().get();
        assertEquals("Dynamic", tc.name);
        assertEquals(999, tc.value);
    }

    @Test
    void testBuildWithDefaultConstructor() throws DslException, ReflectionException {
        IConstructorBinder<TargetClass> binder = builder.build();
        TargetClass tc = binder.execute().get();
        assertEquals("default", tc.name);
        assertEquals(-1, tc.value);
    }

    @Test
    void testThrowsIfNoConstructorMatches() throws DslException {
        builder.withParam("abc")
                .withParam("wrongType");

        assertThrows(DslException.class, builder::build, "Should throw if no matching constructor found");
    }

    @Test
    void testNullableParameterAccepted() throws DslException, ReflectionException {
        builder.withParam(0, new NullSupplierBuilder<String>(String.class), true);
        builder.withParam(1, 77);

        IConstructorBinder<TargetClass> binder = builder.build();
        TargetClass tc = binder.execute().get();
        assertNull(tc.name);
        assertEquals(77, tc.value);
    }

}