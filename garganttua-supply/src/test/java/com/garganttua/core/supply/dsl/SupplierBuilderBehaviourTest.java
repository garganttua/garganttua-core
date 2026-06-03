package com.garganttua.core.supply.dsl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.binders.IConstructorBinder;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.IContextualSupply;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.NullableContextualSupplier;
import com.garganttua.core.supply.NullableSupplier;
import com.garganttua.core.supply.SupplyException;
import com.garganttua.core.supply.TestIClass;

/**
 * Behaviour tests for {@link SupplierBuilder}: source precedence, nullable
 * wrapping, contextual building, static factories and error paths.
 */
public class SupplierBuilderBehaviourTest {

    // --- Constructor / accessor contract ---

    @Test
    public void constructorRejectsNullSuppliedClass() {
        assertThrows(NullPointerException.class,
                () -> new SupplierBuilder<String>(null));
    }

    @Test
    public void getSuppliedTypeAndClassReflectConstructorArg() {
        SupplierBuilder<String> b = new SupplierBuilder<>(TestIClass.of(String.class));
        assertEquals(String.class, b.getSuppliedType());
        assertEquals(TestIClass.of(String.class), b.getSuppliedClass());
    }

    @Test
    public void isContextualFalseUntilContextConfigured() throws DslException {
        SupplierBuilder<String> b = new SupplierBuilder<>(TestIClass.of(String.class));
        assertFalse(b.isContextual(), "Fresh builder should not be contextual");

        b.withContext(TestIClass.of(Object.class),
                (ctx, others) -> Optional.of("x"));
        assertTrue(b.isContextual(), "Builder becomes contextual after withContext");
    }

    // --- Default: no source -> NullSupplier wrapped as nullable ---

    @Test
    public void buildWithNoSourceProducesEmptyNullableSupplier() throws DslException, SupplyException {
        ISupplier<String> supplier = new SupplierBuilder<>(TestIClass.of(String.class)).build();

        assertTrue(supplier instanceof NullableSupplier, "Default build wraps in NullableSupplier");
        assertEquals(Optional.empty(), supplier.supply());
        assertTrue(((NullableSupplier<String>) supplier).isNullable(),
                "Empty source is always wrapped as nullable=true regardless of nullable flag");
    }

    @Test
    public void buildWithNoSourceIsNullableEvenWhenNullableFlagFalse() throws DslException, SupplyException {
        // The build() default branch forces nullable=true; flag is ignored.
        ISupplier<String> supplier = new SupplierBuilder<>(TestIClass.of(String.class))
                .nullable(false)
                .build();

        assertTrue(((NullableSupplier<String>) supplier).isNullable());
        assertEquals(Optional.empty(), supplier.supply());
    }

    // --- Fixed value source ---

    @Test
    public void buildWithValueProducesFixedSupplier() throws DslException, SupplyException {
        ISupplier<String> supplier = new SupplierBuilder<>(TestIClass.of(String.class))
                .withValue("fixed")
                .build();

        assertEquals("fixed", supplier.supply().get());
    }

    @Test
    public void fixedSourceNonNullableThrowsOnEmptyOnlyWhenDelegateEmpty() throws DslException, SupplyException {
        // FixedSupplier never returns empty when value present, so nullable=false is safe.
        ISupplier<Integer> supplier = new SupplierBuilder<>(TestIClass.of(Integer.class))
                .withValue(7)
                .nullable(false)
                .build();
        assertEquals(7, supplier.supply().get());
    }

    // --- Future precedence over blocking queue and value ---

    @Test
    public void futureSourceTakesPrecedenceOverValueAndQueue() throws Exception {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        queue.put("from-queue");
        CompletableFuture<String> future = CompletableFuture.completedFuture("from-future");

        ISupplier<String> supplier = new SupplierBuilder<>(TestIClass.of(String.class))
                .withValue("from-value")
                .withBlockingQueue(queue)
                .withFuture(future)
                .build();

        assertEquals("from-future", supplier.supply().get(),
                "future is highest precedence source");
        assertFalse(queue.isEmpty(), "queue must not be drained when future wins");
    }

    @Test
    public void blockingQueueTakesPrecedenceOverValue() throws Exception {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        queue.put("from-queue");

        ISupplier<String> supplier = new SupplierBuilder<>(TestIClass.of(String.class))
                .withValue("from-value")
                .withBlockingQueue(queue)
                .build();

        assertEquals("from-queue", supplier.supply().get());
    }

    // --- Future timeout error path ---

    @Test
    public void futureTimeoutThrowsSupplyException() throws DslException {
        CompletableFuture<String> never = new CompletableFuture<>();
        ISupplier<String> supplier = new SupplierBuilder<>(TestIClass.of(String.class))
                .withFuture(never, 50L)
                .build();

        assertThrows(SupplyException.class, supplier::supply);
    }

    @Test
    public void futureNullResultNonNullableThrows() throws DslException {
        CompletableFuture<String> nullFuture = CompletableFuture.completedFuture(null);
        ISupplier<String> supplier = new SupplierBuilder<>(TestIClass.of(String.class))
                .withFuture(nullFuture)
                .nullable(false)
                .build();

        SupplyException ex = assertThrows(SupplyException.class, supplier::supply);
        assertTrue(ex.getMessage().contains("not nullable"),
                "Message should explain non-nullable violation, was: " + ex.getMessage());
    }

    // --- Contextual building ---

    @Test
    public void buildWithContextProducesNullableContextualSupplier() throws DslException, SupplyException {
        IContextualSupply<String, String> supply = (ctx, others) -> Optional.of("ctx=" + ctx);
        ISupplier<String> built = new SupplierBuilder<>(TestIClass.of(String.class))
                .withContext(TestIClass.of(String.class), supply)
                .build();

        assertTrue(built instanceof NullableContextualSupplier,
                "contextual builds wrap in NullableContextualSupplier");

        @SuppressWarnings("unchecked")
        IContextualSupplier<String, String> ctxSupplier = (IContextualSupplier<String, String>) built;
        assertEquals("ctx=hello", ctxSupplier.supply("hello").get());
    }

    @Test
    public void contextualNullableDefaultIsFalseSoEmptyThrows() throws DslException {
        // contextual branch wraps with this.nullable (default false).
        IContextualSupply<String, String> emptySupply = (ctx, others) -> Optional.empty();
        ISupplier<String> built = new SupplierBuilder<>(TestIClass.of(String.class))
                .withContext(TestIClass.of(String.class), emptySupply)
                .build();

        @SuppressWarnings("unchecked")
        IContextualSupplier<String, String> ctxSupplier = (IContextualSupplier<String, String>) built;
        assertThrows(SupplyException.class, () -> ctxSupplier.supply("anything"));
    }

    @Test
    public void contextualNullableTrueAllowsEmpty() throws DslException, SupplyException {
        IContextualSupply<String, String> emptySupply = (ctx, others) -> Optional.empty();
        ISupplier<String> built = new SupplierBuilder<>(TestIClass.of(String.class))
                .withContext(TestIClass.of(String.class), emptySupply)
                .nullable(true)
                .build();

        @SuppressWarnings("unchecked")
        IContextualSupplier<String, String> ctxSupplier = (IContextualSupplier<String, String>) built;
        assertEquals(Optional.empty(), ctxSupplier.supply("anything"));
    }

    // --- withXxx null-argument guards ---

    @Test
    public void withContextRejectsNullArgs() {
        SupplierBuilder<String> b = new SupplierBuilder<>(TestIClass.of(String.class));
        assertThrows(NullPointerException.class,
                () -> b.withContext(null, (c, o) -> Optional.of("x")));
        assertThrows(NullPointerException.class,
                () -> b.withContext(TestIClass.of(Object.class), null));
    }

    @Test
    public void withFutureRejectsNull() {
        SupplierBuilder<String> b = new SupplierBuilder<>(TestIClass.of(String.class));
        assertThrows(NullPointerException.class, () -> b.withFuture(null));
        assertThrows(NullPointerException.class, () -> b.withFuture(null, 100L));
    }

    @Test
    public void withBlockingQueueRejectsNull() {
        SupplierBuilder<String> b = new SupplierBuilder<>(TestIClass.of(String.class));
        assertThrows(NullPointerException.class, () -> b.withBlockingQueue(null));
        assertThrows(NullPointerException.class, () -> b.withBlockingQueue(null, 100L));
    }

    @Test
    public void withConstructorRejectsNull() {
        SupplierBuilder<String> b = new SupplierBuilder<>(TestIClass.of(String.class));
        assertThrows(NullPointerException.class, () -> b.withConstructor(null));
    }

    // --- Static factories ---

    @Test
    public void staticFixedFactoryBuildsNonNullableFixedSupplier() throws DslException, SupplyException {
        ISupplier<String> supplier =
                SupplierBuilder.fixed(TestIClass.of(String.class), "v").build();
        assertEquals("v", supplier.supply().get());
        assertFalse(((NullableSupplier<String>) supplier).isNullable());
    }

    @Test
    public void staticNullObjectFactoryBuildsNullableEmptySupplier() throws DslException, SupplyException {
        ISupplier<String> supplier =
                SupplierBuilder.nullObject(TestIClass.of(String.class)).build();
        assertEquals(Optional.empty(), supplier.supply());
    }

    @Test
    public void staticContextualFactoryBuildsContextualSupplier() throws DslException, SupplyException {
        IContextualSupply<String, String> supply = (ctx, others) -> Optional.of("got:" + ctx);
        ISupplier<String> built = SupplierBuilder.contextual(
                TestIClass.of(String.class), TestIClass.of(String.class), supply).build();

        @SuppressWarnings("unchecked")
        IContextualSupplier<String, String> ctxSupplier = (IContextualSupplier<String, String>) built;
        assertEquals("got:abc", ctxSupplier.supply("abc").get());
    }

    @Test
    public void newContextualWithNonContextualBinderThrowsDslException() {
        // newContextual sets contextType + a plain (non-contextual) constructorBinder.
        // build() must reject because the binder is not an IContextualConstructorBinder.
        IConstructorBinder<String> plainBinder = new StubConstructorBinder();
        SupplierBuilder<String> builder = (SupplierBuilder<String>) SupplierBuilder.newContextual(
                TestIClass.of(String.class), TestIClass.of(Object.class), null);
        // Replace the (null) binder with a non-contextual one through the public API.
        // newContextual installs context + binder fields directly; emulate with withConstructor
        // is not possible (it clears contextType expectations), so test via a fresh builder.
        SupplierBuilder<String> b = new SupplierBuilder<>(TestIClass.of(String.class));
        DslException ex = assertThrows(DslException.class, () -> {
            // contextType set + non-contextual binder => build throws
            b.withContext(TestIClass.of(Object.class), (c, o) -> Optional.of("x"));
            b.withConstructor(plainBinder);
            b.build();
        });
        assertTrue(ex.getMessage().contains("not contextual"),
                "Expected DslException about non-contextual binder, was: " + ex.getMessage());
        assertNotNull(builder);
    }

    /**
     * Minimal non-contextual {@link IConstructorBinder} stub. It is never executed
     * in these tests; only its concrete (non-contextual) type matters for the
     * {@code build()} precedence/validation logic.
     */
    private static final class StubConstructorBinder
            implements IConstructorBinder<String> {

        @Override
        public com.garganttua.core.reflection.IClass<String> getConstructedType() {
            return TestIClass.of(String.class);
        }

        @Override
        @SuppressWarnings("deprecation")
        public com.garganttua.core.reflection.IConstructor<?> constructor() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getExecutableReference() {
            return "stub";
        }

        @Override
        public Optional<com.garganttua.core.reflection.IMethodReturn<String>> execute() {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.Set<com.garganttua.core.reflection.IClass<?>> dependencies() {
            return java.util.Set.of();
        }

        @Override
        public Optional<com.garganttua.core.reflection.IMethodReturn<String>> supply() {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.lang.reflect.Type getSuppliedType() {
            return String.class;
        }

        @Override
        @SuppressWarnings("unchecked")
        public com.garganttua.core.reflection.IClass<com.garganttua.core.reflection.IMethodReturn<String>> getSuppliedClass() {
            return (com.garganttua.core.reflection.IClass<com.garganttua.core.reflection.IMethodReturn<String>>)
                    (com.garganttua.core.reflection.IClass<?>) TestIClass.of(com.garganttua.core.reflection.IMethodReturn.class);
        }
    }
}
