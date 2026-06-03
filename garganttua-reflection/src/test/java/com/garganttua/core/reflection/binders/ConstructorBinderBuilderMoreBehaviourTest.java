package com.garganttua.core.reflection.binders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.binders.dsl.AbstractConstructorBinderBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeClass;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.supply.dsl.ContextualSupplierBuilder;

import java.util.Optional;

/**
 * Additional behaviour tests for {@link AbstractConstructorBinderBuilder} covering
 * the branches not exercised by {@code ConstructorBinderBuilderTest}: indexed param
 * binding with gaps, the name-based-binding unsupported overloads, the
 * "parameter not configured" gap path, and the contextual-binder production path.
 */
public class ConstructorBinderBuilderMoreBehaviourTest {

    @BeforeAll
    static void setUp() throws DslException {
        IClass.setReflection(ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider()).build());
    }

    @AfterAll
    static void tearDown() {
        IClass.setReflection(null);
    }

    public static class Pair {
        public final String a;
        public final String b;

        public Pair(String a, String b) {
            this.a = a;
            this.b = b;
        }
    }

    static class ConcreteBuilder
            extends AbstractConstructorBinderBuilder<Pair, ConcreteBuilder, Object> {

        ConcreteBuilder() {
            super(new Object(), RuntimeClass.of(Pair.class), Set.of());
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

    private ConcreteBuilder builder;

    @BeforeEach
    void newBuilder() throws DslException {
        builder = new ConcreteBuilder();
        builder.provide(ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider()));
    }

    @Test
    public void indexedParamsOutOfOrderResolveCorrectly() throws DslException {
        builder.withParam(1, "second");
        builder.withParam(0, "first");

        IConstructorBinder<Pair> binder = builder.build();
        Pair p = binder.execute().get().single();
        assertEquals("first", p.a);
        assertEquals("second", p.b);
    }

    @Test
    public void gapInIndexedParamsThrowsNotConfigured() throws DslException {
        // configure index 1 only -> index 0 stays null -> "Parameter 0 not configured"
        builder.withParam(1, "only");
        DslException ex = assertThrows(DslException.class, builder::build);
        assertTrue(ex.getMessage().contains("not configured"));
    }

    @Test
    public void nameBasedBindingRawValueUnsupported() throws DslException {
        DslException ex = assertThrows(DslException.class, () -> builder.withParam("a", "x"));
        assertTrue(ex.getMessage().contains("not supported"));
    }

    @Test
    public void nameBasedBindingSupplierUnsupported() throws DslException {
        assertThrows(DslException.class,
                () -> builder.withParam("a",
                        com.garganttua.core.supply.dsl.FixedSupplierBuilder.of("x", RuntimeClass.of(String.class))));
    }

    @Test
    public void nameBasedBindingRawValueWithNullableUnsupported() throws DslException {
        assertThrows(DslException.class, () -> builder.withParam("a", "x", true));
    }

    @Test
    public void nameBasedBindingSupplierWithNullableUnsupported() throws DslException {
        assertThrows(DslException.class,
                () -> builder.withParam("a",
                        com.garganttua.core.supply.dsl.FixedSupplierBuilder.of("x", RuntimeClass.of(String.class)),
                        true));
    }

    @Test
    public void contextualParameterProducesContextualBinder() throws DslException {
        // one contextual parameter -> doBuild() takes the contextual branch and returns an
        // IContextualConstructorBinder.
        ContextualSupplierBuilder<String, String> ctxBuilder = new ContextualSupplierBuilder<>(
                (ctx, others) -> Optional.of(ctx),
                RuntimeClass.of(String.class), RuntimeClass.of(String.class));

        builder.withParam(0, ctxBuilder);
        builder.withParam(1, "fixed");

        IConstructorBinder<Pair> binder = builder.build();
        assertTrue(binder instanceof IContextualConstructorBinder,
                "expected a contextual binder, got " + binder.getClass().getSimpleName());

        IContextualConstructorBinder<Pair> ctxBinder = (IContextualConstructorBinder<Pair>) binder;
        Optional<IMethodReturn<Pair>> result = ctxBinder.execute(null, "fromContext");
        Pair p = result.get().single();
        assertEquals("fromContext", p.a);
        assertEquals("fixed", p.b);
    }
}
