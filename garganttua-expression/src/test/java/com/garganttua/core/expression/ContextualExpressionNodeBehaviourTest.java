package com.garganttua.core.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.context.ExpressionContext;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;

/**
 * Behaviour tests for {@link ContextualExpressionNode}: parameter passthrough, eager vs lazy
 * child-node evaluation, the dynamic return-type resolution applied to {@code Object}-typed
 * suppliers, and the constructor null-argument contracts.
 */
public class ContextualExpressionNodeBehaviourTest {

    private final IExpressionContext ctx = new ExpressionContext(Set.of());

    @BeforeEach
    void setUp() {
        IReflection reflection = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider(), 1)
                .build();
        IClass.setReflection(reflection);
    }

    /** A trivial contextual supplier returning a fixed value with a fixed supplied class. */
    private <R> IContextualSupplier<R, IExpressionContext> fixedSupplier(R value, IClass<R> type) {
        return new IContextualSupplier<R, IExpressionContext>() {
            @Override
            public Optional<R> supply(IExpressionContext c, Object... other) {
                return Optional.ofNullable(value);
            }

            @Override
            public IClass<IExpressionContext> getOwnerContextType() {
                return IClass.getClass(IExpressionContext.class);
            }

            @Override
            public Type getSuppliedType() {
                return type.getType();
            }

            @Override
            public IClass<R> getSuppliedClass() {
                return type;
            }
        };
    }

    // ---- constructor null-contracts ----

    @Test
    public void constructor_parameterless_nullEvaluate_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> new ContextualExpressionNode<String>("n", null, IClass.getClass(String.class)));
    }

    @Test
    public void constructor_withParams_nullParams_throwsNPE() {
        IContextualEvaluate<String> ev = (c, p) -> fixedSupplier("x", IClass.getClass(String.class));
        assertThrows(NullPointerException.class,
                () -> new ContextualExpressionNode<>("n", ev, IClass.getClass(String.class), null));
    }

    @Test
    public void constructor_withParams_nullEvaluate_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> new ContextualExpressionNode<String>("n", null, IClass.getClass(String.class), List.of()));
    }

    // ---- getSuppliedType / getFinalSuppliedClass ----

    @Test
    public void getSuppliedType_isIContextualSupplier() {
        ContextualExpressionNode<String> node = new ContextualExpressionNode<>(
                "n", (c, p) -> fixedSupplier("v", IClass.getClass(String.class)), IClass.getClass(String.class));
        assertEquals(IContextualSupplier.class, node.getSuppliedType());
    }

    @Test
    public void getFinalSuppliedClass_returnsDeclaredReturnType() {
        ContextualExpressionNode<String> node = new ContextualExpressionNode<>(
                "n", (c, p) -> fixedSupplier("v", IClass.getClass(String.class)), IClass.getClass(String.class));
        assertEquals(IClass.getClass(String.class), node.getFinalSuppliedClass());
    }

    // ---- parameter passthrough (non-node params are forwarded verbatim) ----

    @Test
    public void evaluate_forwardsRawParametersVerbatim() throws Exception {
        AtomicReference<Object[]> seen = new AtomicReference<>();
        IContextualEvaluate<String> ev = (c, params) -> {
            seen.set(params);
            return fixedSupplier("ok", IClass.getClass(String.class));
        };
        ContextualExpressionNode<String> node = new ContextualExpressionNode<>(
                "n", ev, IClass.getClass(String.class), List.of("a", Integer.valueOf(7)));

        IContextualSupplier<String, IExpressionContext> sup = node.evaluate(ctx);
        assertEquals("ok", sup.supply(ctx).get());
        assertEquals(2, seen.get().length);
        assertEquals("a", seen.get()[0]);
        assertEquals(7, seen.get()[1]);
    }

    @Test
    public void evaluate_parameterlessNode_passesEmptyArray() throws Exception {
        AtomicReference<Object[]> seen = new AtomicReference<>();
        ContextualExpressionNode<String> node = new ContextualExpressionNode<>(
                "n", (c, params) -> {
                    seen.set(params);
                    return fixedSupplier("z", IClass.getClass(String.class));
                }, IClass.getClass(String.class));

        node.evaluate(ctx).supply(ctx);
        assertEquals(0, seen.get().length);
    }

    // ---- eager child-node evaluation ----

    @Test
    public void evaluate_eagerChildNode_isEvaluatedBeforeInvocation() throws Exception {
        // child is a plain (non-contextual) ExpressionNode producing "child-value"
        ExpressionNode<String> child = new ExpressionNode<>("child",
                params -> new ISupplier<String>() {
                    @Override
                    public Optional<String> supply() {
                        return Optional.of("child-value");
                    }

                    @Override
                    public Type getSuppliedType() {
                        return String.class;
                    }

                    @Override
                    public IClass<String> getSuppliedClass() {
                        return IClass.getClass(String.class);
                    }
                }, IClass.getClass(String.class));

        AtomicReference<Object[]> seen = new AtomicReference<>();
        ContextualExpressionNode<String> node = new ContextualExpressionNode<>(
                "n", (c, params) -> {
                    seen.set(params);
                    return fixedSupplier("done", IClass.getClass(String.class));
                }, IClass.getClass(String.class), List.of(child));

        node.evaluate(ctx).supply(ctx);
        // eager: the child node was replaced by its evaluated ISupplier
        Object passed = seen.get()[0];
        assertTrue(passed instanceof ISupplier, "eager child must be an evaluated supplier");
        assertEquals("child-value", ((ISupplier<?>) passed).supply().get());
    }

    // ---- lazy child-node evaluation ----

    @Test
    public void evaluate_lazyChildNode_isNotEvaluatedUntilSupplied() throws Exception {
        AtomicInteger childEvaluations = new AtomicInteger(0);
        ExpressionNode<String> child = new ExpressionNode<>("child",
                params -> {
                    childEvaluations.incrementAndGet();
                    return new ISupplier<String>() {
                        @Override
                        public Optional<String> supply() {
                            return Optional.of("lazy-value");
                        }

                        @Override
                        public Type getSuppliedType() {
                            return String.class;
                        }

                        @Override
                        public IClass<String> getSuppliedClass() {
                            return IClass.getClass(String.class);
                        }
                    };
                }, IClass.getClass(String.class));

        AtomicReference<Object[]> seen = new AtomicReference<>();
        ContextualExpressionNode<String> node = new ContextualExpressionNode<>(
                "n", (c, params) -> {
                    seen.set(params);
                    return fixedSupplier("done", IClass.getClass(String.class));
                }, IClass.getClass(String.class), List.of(child), List.of(Boolean.TRUE));

        node.evaluate(ctx);
        // building the node must NOT have evaluated the lazy child yet
        assertEquals(0, childEvaluations.get(), "lazy child must not be evaluated at build time");

        ISupplier<?> lazy = (ISupplier<?>) seen.get()[0];
        assertNotNull(lazy);
        assertEquals("lazy-value", lazy.supply().get());
        assertEquals(1, childEvaluations.get(), "lazy child must be evaluated exactly once on supply()");
        // supplied type of the wrapper mirrors the child final supplied class
        assertEquals(String.class, lazy.getSuppliedType());
        assertEquals(IClass.getClass(String.class), lazy.getSuppliedClass());
    }

    @Test
    public void evaluate_lazyChildThrowing_wrapsInSupplyException() {
        ExpressionNode<String> child = new ExpressionNode<>("boom",
                params -> {
                    throw new RuntimeException("kaboom");
                }, IClass.getClass(String.class));

        AtomicReference<Object[]> seen = new AtomicReference<>();
        ContextualExpressionNode<String> node = new ContextualExpressionNode<>(
                "n", (c, params) -> {
                    seen.set(params);
                    return fixedSupplier("done", IClass.getClass(String.class));
                }, IClass.getClass(String.class), List.of(child), List.of(Boolean.TRUE));

        node.evaluate(ctx);
        ISupplier<?> lazy = (ISupplier<?>) seen.get()[0];
        // the failure surfaces only when the lazy supplier is actually pulled
        assertThrows(RuntimeException.class, lazy::supply);
    }

    // ---- dynamic return-type resolution for Object-typed suppliers ----

    @Test
    public void evaluate_objectReturnType_resolvesActualTypeFromValue() throws Exception {
        // declared return type is Object -> wrapper resolves concrete type lazily from supplied value
        ContextualExpressionNode<Object> node = new ContextualExpressionNode<>(
                "n", (c, params) -> (IContextualSupplier<Object, IExpressionContext>) (IContextualSupplier<?, IExpressionContext>)
                        fixedSupplier("hello", IClass.getClass(String.class)),
                IClass.getClass(Object.class));

        // before supply, the declared type is still Object
        assertEquals(Object.class, node.getFinalSuppliedClass().getType());

        IContextualSupplier<Object, IExpressionContext> sup = node.evaluate(ctx);
        assertEquals("hello", sup.supply(ctx).get());

        // after producing a non-null value, the node's return type is upgraded to String
        assertEquals(String.class, node.getFinalSuppliedClass().getType());
    }

    @Test
    public void evaluate_objectReturnType_emptyValueLeavesTypeUnresolved() throws Exception {
        IContextualSupplier<Object, IExpressionContext> empty = new IContextualSupplier<Object, IExpressionContext>() {
            @Override
            public Optional<Object> supply(IExpressionContext c, Object... other) {
                return Optional.empty();
            }

            @Override
            public IClass<IExpressionContext> getOwnerContextType() {
                return IClass.getClass(IExpressionContext.class);
            }

            @Override
            public Type getSuppliedType() {
                return Object.class;
            }

            @Override
            public IClass<Object> getSuppliedClass() {
                return IClass.getClass(Object.class);
            }
        };
        ContextualExpressionNode<Object> node = new ContextualExpressionNode<>(
                "n", (c, params) -> empty, IClass.getClass(Object.class));

        IContextualSupplier<Object, IExpressionContext> sup = node.evaluate(ctx);
        assertTrue(sup.supply(ctx).isEmpty());
        // type stays Object because no concrete value was produced
        assertEquals(Object.class, node.getFinalSuppliedClass().getType());
    }

    @Test
    public void evaluate_nonObjectReturnType_returnsOriginalSupplierUnwrapped() throws Exception {
        IContextualSupplier<String, IExpressionContext> original = fixedSupplier("v", IClass.getClass(String.class));
        ContextualExpressionNode<String> node = new ContextualExpressionNode<>(
                "n", (c, params) -> original, IClass.getClass(String.class));

        IContextualSupplier<String, IExpressionContext> sup = node.evaluate(ctx);
        // non-Object return type: the very same supplier instance is returned, no wrapping
        assertSame(original, sup);
    }

    @Test
    public void evaluate_objectReturnType_wrapperDelegatesMetadataToOriginal() throws Exception {
        IContextualSupplier<Object, IExpressionContext> original =
                (IContextualSupplier<Object, IExpressionContext>) (IContextualSupplier<?, IExpressionContext>)
                        fixedSupplier("x", IClass.getClass(String.class));
        ContextualExpressionNode<Object> node = new ContextualExpressionNode<>(
                "n", (c, params) -> original, IClass.getClass(Object.class));

        IContextualSupplier<Object, IExpressionContext> wrapper = node.evaluate(ctx);
        // wrapper is a different instance (the Object-resolving wrapper) but delegates metadata
        assertEquals(original.getSuppliedType(), wrapper.getSuppliedType());
        assertEquals(original.getSuppliedClass(), wrapper.getSuppliedClass());
        assertEquals(original.getOwnerContextType(), wrapper.getOwnerContextType());
    }
}
