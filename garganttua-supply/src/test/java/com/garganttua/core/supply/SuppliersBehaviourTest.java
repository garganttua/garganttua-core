package com.garganttua.core.supply;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Behaviour tests for the concrete {@link ISupplier} / {@link IContextualSupplier}
 * implementations: fixed, null, nullable guards and contextual suppliers.
 */
public class SuppliersBehaviourTest {

    // --- FixedSupplier ---

    @Test
    public void fixedSupplierAlwaysReturnsSameValue() throws SupplyException {
        FixedSupplier<String> s = new FixedSupplier<>("v", TestIClass.of(String.class));
        assertEquals("v", s.supply().get());
        assertSame(s.supply().get(), s.supply().get(), "same instance every call");
        assertEquals(String.class, s.getSuppliedType());
        assertEquals(TestIClass.of(String.class), s.getSuppliedClass());
    }

    @Test
    public void fixedSupplierRejectsNullValue() {
        assertThrows(NullPointerException.class,
                () -> new FixedSupplier<>(null, TestIClass.of(String.class)));
    }

    @Test
    public void fixedSupplierRejectsNullClass() {
        assertThrows(NullPointerException.class,
                () -> new FixedSupplier<>("v", null));
    }

    // --- NullSupplier ---

    @Test
    public void nullSupplierAlwaysEmpty() throws SupplyException {
        NullSupplier<Integer> s = new NullSupplier<>(TestIClass.of(Integer.class));
        assertEquals(Optional.empty(), s.supply());
        assertEquals(Integer.class, s.getSuppliedType());
        assertEquals(TestIClass.of(Integer.class), s.getSuppliedClass());
    }

    // --- NullableSupplier ---

    @Test
    public void nullableSupplierPassesThroughPresentValue() throws SupplyException {
        ISupplier<String> delegate = new FixedSupplier<>("hi", TestIClass.of(String.class));
        NullableSupplier<String> s = new NullableSupplier<>(delegate, false);
        assertEquals("hi", s.supply().get());
        assertFalse(s.isNullable());
        assertSame(delegate, s.getDelegate());
    }

    @Test
    public void nullableSupplierAllowingNullReturnsEmpty() throws SupplyException {
        ISupplier<String> delegate = new NullSupplier<>(TestIClass.of(String.class));
        NullableSupplier<String> s = new NullableSupplier<>(delegate, true);
        assertEquals(Optional.empty(), s.supply());
        assertTrue(s.isNullable());
    }

    @Test
    public void nullableSupplierForbiddingNullThrowsOnEmpty() {
        ISupplier<String> delegate = new NullSupplier<>(TestIClass.of(String.class));
        NullableSupplier<String> s = new NullableSupplier<>(delegate, false);
        SupplyException ex = assertThrows(SupplyException.class, s::supply);
        assertTrue(ex.getMessage().contains("not nullable"));
        assertTrue(ex.getMessage().contains("String"),
                "message should name the supplied type, was: " + ex.getMessage());
    }

    @Test
    public void nullableSupplierTreatsNullOptionalFromDelegateAsEmpty() throws SupplyException {
        // A misbehaving delegate that returns a raw null Optional must be coerced to empty.
        ISupplier<String> nullReturning = new ISupplier<>() {
            @Override
            public Optional<String> supply() {
                return null;
            }

            @Override
            public java.lang.reflect.Type getSuppliedType() {
                return String.class;
            }

            @Override
            public com.garganttua.core.reflection.IClass<String> getSuppliedClass() {
                return TestIClass.of(String.class);
            }
        };
        NullableSupplier<String> permissive = new NullableSupplier<>(nullReturning, true);
        assertEquals(Optional.empty(), permissive.supply(),
                "null Optional from delegate becomes Optional.empty()");

        NullableSupplier<String> strict = new NullableSupplier<>(nullReturning, false);
        assertThrows(SupplyException.class, strict::supply);
    }

    @Test
    public void nullableSupplierRejectsNullDelegate() {
        assertThrows(NullPointerException.class, () -> new NullableSupplier<>(null, true));
    }

    @Test
    public void nullableSupplierDelegatesTypeAccessors() {
        ISupplier<String> delegate = new FixedSupplier<>("x", TestIClass.of(String.class));
        NullableSupplier<String> s = new NullableSupplier<>(delegate, true);
        assertEquals(String.class, s.getSuppliedType());
        assertEquals(TestIClass.of(String.class), s.getSuppliedClass());
    }

    // --- ContextualSupplier ---

    @Test
    public void contextualSupplierInvokesUnderlyingSupplyWhenContextMatches() throws SupplyException {
        IContextualSupply<String, String> supply = (ctx, others) -> Optional.of("v=" + ctx);
        ContextualSupplier<String, String> s = new ContextualSupplier<>(
                supply, TestIClass.of(String.class), TestIClass.of(String.class));

        assertEquals("v=abc", s.supply("abc").get());
        assertEquals(String.class, s.getSuppliedType());
        assertEquals(TestIClass.of(String.class), s.getSuppliedClass());
        assertEquals(TestIClass.of(String.class), s.getOwnerContextType());
    }

    @Test
    public void contextualSupplierForwardsOtherContexts() throws SupplyException {
        IContextualSupply<String, String> supply =
                (ctx, others) -> Optional.of(ctx + ":" + others.length);
        ContextualSupplier<String, String> s = new ContextualSupplier<>(
                supply, TestIClass.of(String.class), TestIClass.of(String.class));

        assertEquals("a:2", s.supply("a", "extra1", "extra2").get());
    }

    @Test
    public void contextualSupplierThrowsOnContextTypeMismatch() {
        IContextualSupply<String, String> supply = (ctx, others) -> Optional.of("never");
        // Declared context type is String but we feed an Integer.
        @SuppressWarnings({ "unchecked", "rawtypes" })
        ContextualSupplier rawSupplier = new ContextualSupplier(
                supply, TestIClass.of(String.class), TestIClass.of(String.class));

        SupplyException ex = assertThrows(SupplyException.class,
                () -> rawSupplier.supply(Integer.valueOf(5)));
        assertTrue(ex.getMessage().contains("Context type mismatch"));
        assertTrue(ex.getMessage().contains("String"));
        assertTrue(ex.getMessage().contains("Integer"));
    }

    @Test
    public void contextualSupplierConstructorRejectsNulls() {
        IContextualSupply<String, String> supply = (ctx, others) -> Optional.of("x");
        assertThrows(NullPointerException.class,
                () -> new ContextualSupplier<>(null, TestIClass.of(String.class), TestIClass.of(String.class)));
        assertThrows(NullPointerException.class,
                () -> new ContextualSupplier<>(supply, null, TestIClass.of(String.class)));
        assertThrows(NullPointerException.class,
                () -> new ContextualSupplier<>(supply, TestIClass.of(String.class), null));
    }

    // --- NullableContextualSupplier ---

    @Test
    public void nullableContextualPassesThroughPresentValue() throws SupplyException {
        IContextualSupply<String, String> supply = (ctx, others) -> Optional.of("hi");
        ContextualSupplier<String, String> delegate = new ContextualSupplier<>(
                supply, TestIClass.of(String.class), TestIClass.of(String.class));
        NullableContextualSupplier<String, String> s =
                new NullableContextualSupplier<>(delegate, false);

        assertEquals("hi", s.supply("ctx").get());
        assertFalse(s.isNullable());
        assertSame(delegate, s.getDelegate());
        assertEquals(TestIClass.of(String.class), s.getOwnerContextType());
        assertEquals(String.class, s.getSuppliedType());
        assertEquals(TestIClass.of(String.class), s.getSuppliedClass());
    }

    @Test
    public void nullableContextualForbiddingNullThrowsOnEmpty() {
        IContextualSupply<String, String> empty = (ctx, others) -> Optional.empty();
        ContextualSupplier<String, String> delegate = new ContextualSupplier<>(
                empty, TestIClass.of(String.class), TestIClass.of(String.class));
        NullableContextualSupplier<String, String> s =
                new NullableContextualSupplier<>(delegate, false);

        SupplyException ex = assertThrows(SupplyException.class, () -> s.supply("ctx"));
        assertTrue(ex.getMessage().contains("not nullable"));
    }

    @Test
    public void nullableContextualAllowingNullReturnsEmpty() throws SupplyException {
        IContextualSupply<String, String> empty = (ctx, others) -> Optional.empty();
        ContextualSupplier<String, String> delegate = new ContextualSupplier<>(
                empty, TestIClass.of(String.class), TestIClass.of(String.class));
        NullableContextualSupplier<String, String> s =
                new NullableContextualSupplier<>(delegate, true);

        assertEquals(Optional.empty(), s.supply("ctx"));
        assertTrue(s.isNullable());
    }

    @Test
    public void nullableContextualRejectsNullDelegate() {
        assertThrows(NullPointerException.class,
                () -> new NullableContextualSupplier<>(null, true));
    }
}
