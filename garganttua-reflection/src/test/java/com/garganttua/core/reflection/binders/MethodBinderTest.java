package com.garganttua.core.reflection.binders;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.binders.dsl.AbstractMethodBinderBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeClass;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.supply.dsl.NullSupplierBuilder;

public class MethodBinderTest {

    @BeforeAll
    static void setUpReflection() throws DslException {
        IReflection reflection = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .build();
        IClass.setReflection(reflection);
    }

    @AfterAll
    static void tearDown() {
        IClass.setReflection(null);
    }

    static class MethodObject {
        public MethodObject() {
        }

        String echo(String message) {
            return message;
        }

        static String staticEcho(String message) {
            return message;
        }
    }

    class ConcreteMethodBinderBuilder
            extends AbstractMethodBinderBuilder<String, ConcreteMethodBinderBuilder, Object, IMethodBinder<String>> {

        protected ConcreteMethodBinderBuilder(Object up, ISupplierBuilder<?, ?> supplier) throws DslException {
            super(up, supplier, Set.of());
        }

        @Override
        protected void doAutoDetection() throws DslException {
            throw new UnsupportedOperationException("Unimplemented method 'doAutoDetection'");
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

    @Test
    public void testEchoMethod() throws DslException {
        ConcreteMethodBinderBuilder b = new ConcreteMethodBinderBuilder(new Object(),
                FixedSupplierBuilder.of(new MethodObject(), RuntimeClass.of(MethodObject.class)));
        b.method("echo", RuntimeClass.of(String.class), RuntimeClass.of(String.class))
                .withParam("Hello");
        b.provide(ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider()));

        IMethodBinder<String> mb = b.build();
        assertEquals("Hello", mb.supply().get().single());
    }

    @Test
    public void testStaticEchoMethod() throws DslException {
        ConcreteMethodBinderBuilder b = new ConcreteMethodBinderBuilder(new Object(),
                new NullSupplierBuilder<>(RuntimeClass.of(MethodObject.class)));
        b.method("staticEcho", RuntimeClass.of(String.class), RuntimeClass.of(String.class))
                .withParam("Hello");
        b.provide(ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider()));

        IMethodBinder<String> mb = b.build();
        assertEquals("Hello", mb.supply().get().single());
    }

}
