package com.garganttua.core.supply.dsl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.IContextualSupply;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;
import com.garganttua.core.supply.TestIClass;

/**
 * Behaviour tests for the small dedicated builders: {@link FixedSupplierBuilder},
 * {@link NullSupplierBuilder} and {@link ContextualSupplierBuilder}.
 */
public class SupplierBuildersBehaviourTest {

    // --- FixedSupplierBuilder ---

    @Test
    public void fixedBuilderBuildsSupplierReturningValue() throws DslException, SupplyException {
        FixedSupplierBuilder<String> b =
                new FixedSupplierBuilder<>("hi", TestIClass.of(String.class));
        assertFalse(b.isContextual());
        assertEquals(String.class, b.getSuppliedType());
        assertEquals(TestIClass.of(String.class), b.getSuppliedClass());
        assertEquals("hi", b.build().supply().get());
    }

    @Test
    public void fixedBuilderRejectsNullObject() {
        assertThrows(NullPointerException.class,
                () -> new FixedSupplierBuilder<>(null, TestIClass.of(String.class)));
    }

    @Test
    public void fixedBuilderRejectsNullClass() {
        assertThrows(NullPointerException.class,
                () -> new FixedSupplierBuilder<>("v", null));
    }

    // Note: the single-arg FixedSupplierBuilder.of(value) overload is intentionally
    // not exercised here because it resolves the type through IClass.getClass(...),
    // which requires an installed IReflection provider not present in this module's
    // test classpath (the tests use the TestIClass stub instead).

    @Test
    public void fixedBuilderOfWithExplicitClass() throws DslException, SupplyException {
        ISupplierBuilder<Integer, ISupplier<Integer>> b =
                FixedSupplierBuilder.of(42, TestIClass.of(Integer.class));
        assertEquals(42, b.build().supply().get());
    }

    @Test
    public void ofNullableReturnsFixedBuilderForNonNull() throws DslException, SupplyException {
        ISupplierBuilder<String, ISupplier<String>> b =
                FixedSupplierBuilder.ofNullable("present", TestIClass.of(String.class));
        assertTrue(b instanceof FixedSupplierBuilder);
        assertEquals("present", b.build().supply().get());
    }

    @Test
    public void ofNullableReturnsNullBuilderForNull() throws DslException, SupplyException {
        ISupplierBuilder<String, ISupplier<String>> b =
                FixedSupplierBuilder.ofNullable(null, TestIClass.of(String.class));
        assertTrue(b instanceof NullSupplierBuilder);
        assertEquals(Optional.empty(), b.build().supply());
    }

    // --- NullSupplierBuilder ---

    @Test
    public void nullBuilderBuildsEmptySupplier() throws DslException, SupplyException {
        NullSupplierBuilder<String> b = new NullSupplierBuilder<>(TestIClass.of(String.class));
        assertFalse(b.isContextual());
        assertEquals(String.class, b.getSuppliedType());
        assertEquals(TestIClass.of(String.class), b.getSuppliedClass());
        assertEquals(Optional.empty(), b.build().supply());
    }

    @Test
    public void nullBuilderStaticOfBuildsEmptySupplier() throws DslException, SupplyException {
        NullSupplierBuilder<Integer> b = NullSupplierBuilder.of(TestIClass.of(Integer.class));
        assertEquals(Optional.empty(), b.build().supply());
        assertEquals(Integer.class, b.getSuppliedType());
    }

    // --- ContextualSupplierBuilder ---

    @Test
    public void contextualBuilderIsContextualAndBuildsWorkingSupplier()
            throws DslException, SupplyException {
        IContextualSupply<String, String> supply = (ctx, others) -> Optional.of("ctx:" + ctx);
        ContextualSupplierBuilder<String, String> b = new ContextualSupplierBuilder<>(
                supply, TestIClass.of(String.class), TestIClass.of(String.class));

        assertTrue(b.isContextual());
        assertEquals(String.class, b.getSuppliedType());
        assertEquals(TestIClass.of(String.class), b.getSuppliedClass());

        IContextualSupplier<String, String> supplier = b.build();
        assertEquals("ctx:hi", supplier.supply("hi").get());
    }

    @Test
    public void contextualBuilderPropagatesContextTypeMismatchAtSupply()
            throws DslException, SupplyException {
        IContextualSupply<String, String> supply = (ctx, others) -> Optional.of("never");
        @SuppressWarnings({ "unchecked", "rawtypes" })
        ContextualSupplierBuilder rawBuilder = new ContextualSupplierBuilder(
                supply, TestIClass.of(String.class), TestIClass.of(String.class));

        @SuppressWarnings("unchecked")
        IContextualSupplier<String, Object> supplier =
                (IContextualSupplier<String, Object>) rawBuilder.build();

        assertThrows(SupplyException.class, () -> supplier.supply(Integer.valueOf(1)));
    }

    @Test
    public void contextualBuilderRejectsNullConstructorArgs() {
        IContextualSupply<String, String> supply = (ctx, others) -> Optional.of("x");
        assertThrows(NullPointerException.class,
                () -> new ContextualSupplierBuilder<>(null, TestIClass.of(String.class), TestIClass.of(String.class)));
        assertThrows(NullPointerException.class,
                () -> new ContextualSupplierBuilder<>(supply, null, TestIClass.of(String.class)));
        assertThrows(NullPointerException.class,
                () -> new ContextualSupplierBuilder<>(supply, TestIClass.of(String.class), null));
    }
}
