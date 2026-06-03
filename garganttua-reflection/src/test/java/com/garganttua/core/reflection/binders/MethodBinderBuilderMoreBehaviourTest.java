package com.garganttua.core.reflection.binders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.binders.dsl.AbstractMethodBinderBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeClass;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * Additional behaviour tests for {@link AbstractMethodBinderBuilder} focusing on the
 * branches not exercised by {@code MethodBinderTest}: by-name and next-free-slot
 * parameter binding, the "method must be set" guards, method resolution by
 * {@link IMethod} / {@link ObjectAddress}, parameter-count mismatch, collection
 * binding and the metadata accessors.
 */
public class MethodBinderBuilderMoreBehaviourTest {

    private static IReflection reflection;

    @BeforeAll
    static void setUp() throws DslException {
        reflection = ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider()).build();
        IClass.setReflection(reflection);
    }

    @AfterAll
    static void tearDown() {
        IClass.setReflection(null);
    }

    public static class Calc {
        public Calc() {
        }

        public int add(int a, int b) {
            return a + b;
        }

        public String tag(String label) {
            return "[" + label + "]";
        }
    }

    public static class Item {
        public String mark() {
            return "marked";
        }
    }

    static class ConcreteMethodBinderBuilder
            extends AbstractMethodBinderBuilder<Object, ConcreteMethodBinderBuilder, Object, IMethodBinder<Object>> {

        ConcreteMethodBinderBuilder(ISupplierBuilder<?, ?> supplier) throws DslException {
            super(new Object(), supplier, Set.of());
        }

        ConcreteMethodBinderBuilder(ISupplierBuilder<?, ?> supplier, boolean collection) throws DslException {
            super(new Object(), supplier, collection, Set.of());
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

        // Expose the protected setSupplier so a collection owner can be installed
        // after the method has been resolved against the element type.
        void replaceSupplier(ISupplierBuilder<?, ?> supplier) {
            this.setSupplier(supplier);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ConcreteMethodBinderBuilder builderFor(Object owner) throws DslException {
        ConcreteMethodBinderBuilder b = new ConcreteMethodBinderBuilder(
                FixedSupplierBuilder.of(owner, (IClass) RuntimeClass.of(owner.getClass())));
        b.provide(ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider()));
        return b;
    }

    /** Return-type argument as {@code IClass<Object>} so the Object-return builder accepts it. */
    @SuppressWarnings("unchecked")
    private static IClass<Object> ret(Class<?> type) {
        return (IClass<Object>) (IClass<?>) RuntimeClass.of(type);
    }

    // ===== by-name parameter binding =====

    @Test
    public void bindParametersByNameInvokesCorrectly() throws DslException {
        ConcreteMethodBinderBuilder b = builderFor(new Calc());
        b.method("add", ret(int.class), RuntimeClass.of(int.class), RuntimeClass.of(int.class));
        // Parameter names available only if compiled with -parameters; if not, fall back to index.
        IMethod m = b.method();
        String p0 = m.getParameters()[0].getName();
        b.withParam(p0, 3).withParam(1, 4);

        IMethodBinder<Object> binder = b.build();
        assertEquals(7, binder.supply().get().single());
    }

    @Test
    public void unknownParameterNameThrows() throws DslException {
        ConcreteMethodBinderBuilder b = builderFor(new Calc());
        b.method("add", ret(int.class), RuntimeClass.of(int.class), RuntimeClass.of(int.class));
        assertThrows(DslException.class, () -> b.withParam("noSuchParam", 1));
    }

    @Test
    public void unknownParameterNameSupplierThrows() throws DslException {
        ConcreteMethodBinderBuilder b = builderFor(new Calc());
        b.method("add", ret(int.class), RuntimeClass.of(int.class), RuntimeClass.of(int.class));
        ISupplierBuilder<Integer, ?> s = FixedSupplierBuilder.of(1, RuntimeClass.of(Integer.class));
        assertThrows(DslException.class, () -> b.withParam("noSuchParam", s));
    }

    // ===== next-free-slot binding =====

    @Test
    public void nextFreeSlotBindingFillsParametersInOrder() throws DslException {
        ConcreteMethodBinderBuilder b = builderFor(new Calc());
        b.method("add", ret(int.class), RuntimeClass.of(int.class), RuntimeClass.of(int.class));
        b.withParam(10).withParam(5);

        IMethodBinder<Object> binder = b.build();
        assertEquals(15, binder.supply().get().single());
    }

    @Test
    public void nextFreeSlotSupplierBinding() throws DslException {
        ConcreteMethodBinderBuilder b = builderFor(new Calc());
        b.method("add", ret(int.class), RuntimeClass.of(int.class), RuntimeClass.of(int.class));
        b.withParam(FixedSupplierBuilder.of(2, RuntimeClass.of(Integer.class)));
        b.withParam(FixedSupplierBuilder.of(8, RuntimeClass.of(Integer.class)));

        IMethodBinder<Object> binder = b.build();
        assertEquals(10, binder.supply().get().single());
    }

    // ===== "method must be set" guards =====

    @Test
    public void withParamRawBeforeMethodSetThrows() throws DslException {
        ConcreteMethodBinderBuilder b = builderFor(new Calc());
        assertThrows(DslException.class, () -> b.withParam(0, 1));
    }

    @Test
    public void withParamSupplierBeforeMethodSetThrows() throws DslException {
        ConcreteMethodBinderBuilder b = builderFor(new Calc());
        assertThrows(DslException.class,
                () -> b.withParam(0, FixedSupplierBuilder.of(1, RuntimeClass.of(Integer.class))));
    }

    @Test
    public void nextFreeSlotBeforeMethodSetThrows() throws DslException {
        ConcreteMethodBinderBuilder b = builderFor(new Calc());
        assertThrows(DslException.class, () -> b.withParam(1));
    }

    @Test
    public void byNameBeforeMethodSetThrows() throws DslException {
        ConcreteMethodBinderBuilder b = builderFor(new Calc());
        assertThrows(DslException.class, () -> b.withParam("a", 1));
    }

    @Test
    public void buildWithoutMethodThrows() throws DslException {
        ConcreteMethodBinderBuilder b = builderFor(new Calc());
        assertThrows(DslException.class, b::build);
    }

    // ===== method resolution by IMethod / ObjectAddress =====

    @Test
    public void methodByIMethodResolvesAndInvokes() throws DslException {
        Calc calc = new Calc();
        IMethod tag = null;
        for (IMethod m : RuntimeClass.of(Calc.class).getMethods()) {
            if ("tag".equals(m.getName())) {
                tag = m;
            }
        }
        assertNotNull(tag);
        ConcreteMethodBinderBuilder b = builderFor(calc);
        b.method(tag).withParam("X");

        IMethodBinder<Object> binder = b.build();
        assertEquals("[X]", binder.supply().get().single());
    }

    @Test
    public void methodByAddressResolvesAndInvokes() throws DslException {
        ConcreteMethodBinderBuilder b = builderFor(new Calc());
        b.method(new ObjectAddress("tag", true), ret(String.class), RuntimeClass.of(String.class))
                .withParam("Y");

        IMethodBinder<Object> binder = b.build();
        assertEquals("[Y]", binder.supply().get().single());
    }

    @Test
    public void methodByNullAddressThrows() throws DslException {
        ConcreteMethodBinderBuilder b = builderFor(new Calc());
        assertThrows(NullPointerException.class,
                () -> b.method((ObjectAddress) null, ret(String.class)));
    }

    // ===== parameter-count mismatch =====

    @Test
    public void partiallyConfiguredParamsFailOnBuild() throws DslException {
        ConcreteMethodBinderBuilder b = builderFor(new Calc());
        b.method("add", ret(int.class), RuntimeClass.of(int.class), RuntimeClass.of(int.class));
        b.withParam(0, 1); // only one of two params configured; index 1 stays null

        // NOTE: doBuild() calls buildContextual(resolvedParams) before the per-slot
        // "not configured" validation, so the null slot triggers a NullPointerException
        // (param.isContextual() on a null entry) rather than the intended DslException.
        // Asserting the real behaviour here; see SUSPECTED BUG note in the agent report.
        assertThrows(NullPointerException.class, b::build);
    }

    // ===== metadata accessors =====

    @Test
    public void methodNameAndAddressAccessors() throws DslException {
        ConcreteMethodBinderBuilder b = builderFor(new Calc());
        b.method("tag", ret(String.class), RuntimeClass.of(String.class));

        assertEquals("tag", b.getMethodName());
        assertEquals("tag", b.methodAddress().toString());
        assertEquals("tag", b.method().getName());
    }

    @Test
    public void methodNameNullBeforeResolution() throws DslException {
        ConcreteMethodBinderBuilder b = builderFor(new Calc());
        assertNull(b.getMethodName());
    }

    // ===== collection binding =====

    @Test
    public void collectionBinderInvokesMethodPerElement() throws DslException {
        List<Item> items = List.of(new Item(), new Item(), new Item());
        // Build a collection binder: resolve mark() against the Item element type, then
        // swap the owner supplier to the collection itself before building.
        ConcreteMethodBinderBuilder b = new ConcreteMethodBinderBuilder(
                FixedSupplierBuilder.of(new Item(), RuntimeClass.of(Item.class)), true);
        b.provide(ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider()));
        b.method("mark", ret(String.class));
        b.replaceSupplier(FixedSupplierBuilder.of(items, RuntimeClass.of(java.util.List.class)));

        IMethodBinder<Object> binder = b.build();
        var result = binder.supply().get();
        // each element returns "marked" -> a multiple return of size 3
        assertEquals(3, result.multiple().size());
        assertEquals("marked", result.multiple().get(0));
    }
}
