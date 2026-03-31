package com.garganttua.core.reflection.binders;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.dsl.AbstractConstructorBinderBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeClass;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
import com.garganttua.core.supply.dsl.NullSupplierBuilder;

public class ConstructorBinderBuilderTest {

    @BeforeAll
    static void setUpReflection() throws Exception {
        IClass.setReflection(ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .build());
    }

    @AfterAll
    static void tearDownReflection() {
        IClass.setReflection(null);
    }

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
            super(new Object(), RuntimeClass.of(objectClass), Set.of());
        }

        @Override
        protected void doAutoDetection() throws DslException {
        }

        @Override
        protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
        }

        @Override
        protected void doPreBuildWithDependency_(Object dependency) {
        }

        @Override
        protected void doPostBuildWithDependency(Object dependency) {
        }
    }

    private ConcreteConstructorBinderBuilder builder;

    @BeforeEach
    void setUp() throws DslException {
        builder = new ConcreteConstructorBinderBuilder(TargetClass.class);
        builder.provide(ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider()));
    }

    @Test
    void testBuildWithMatchingConstructorUsingRawValues() throws DslException, ReflectionException {
        builder
                .withParam("Hello")
                .withParam(123);

        IConstructorBinder<TargetClass> binder = builder.build();
        assertNotNull(binder, "Binder should not be null");

        Optional<IMethodReturn<TargetClass>> obj = binder.execute();
        assertTrue(obj.isPresent(), "Object should be created");
        IMethodReturn<TargetClass> methodReturn = obj.get();
        assertFalse(methodReturn.hasException(), "Should not have exception");
        TargetClass tc = methodReturn.single();
        assertInstanceOf(TargetClass.class, tc);
        assertEquals("Hello", tc.name);
        assertEquals(123, tc.value);
    }

    @Test
    void testBuildWithMatchingConstructorUsingSuppliers() throws DslException, ReflectionException {
        builder
                .withParam(new FixedSupplierBuilder<>("Dynamic", RuntimeClass.of(String.class)))
                .withParam(new FixedSupplierBuilder<>(999, RuntimeClass.of(Integer.class)));

        IConstructorBinder<TargetClass> binder = builder.build();
        TargetClass tc = binder.execute().get().single();
        assertEquals("Dynamic", tc.name);
        assertEquals(999, tc.value);
    }

    @Test
    void testBuildWithDefaultConstructor() throws DslException, ReflectionException {
        IConstructorBinder<TargetClass> binder = builder.build();
        TargetClass tc = binder.execute().get().single();
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
        builder.withParam(0, new NullSupplierBuilder<String>(RuntimeClass.of(String.class)), true);
        builder.withParam(1, 77);

        IConstructorBinder<TargetClass> binder = builder.build();
        TargetClass tc = binder.execute().get().single();
        assertNull(tc.name);
        assertEquals(77, tc.value);
    }

}
