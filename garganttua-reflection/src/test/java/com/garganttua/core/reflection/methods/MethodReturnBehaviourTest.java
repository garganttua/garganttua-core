package com.garganttua.core.reflection.methods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.runtime.RuntimeClass;

/**
 * Behaviour tests for {@link SingleMethodReturn} and {@link MultipleMethodReturn}.
 */
public class MethodReturnBehaviourTest {

    private static final IClass<String> STR = RuntimeClass.of(String.class);
    private static final IClass<Integer> INT = RuntimeClass.of(Integer.class);

    // ===== SingleMethodReturn =====

    @Test
    public void single_holdsValue() {
        SingleMethodReturn<String> r = SingleMethodReturn.of("hi", STR);
        assertTrue(r.isSingle());
        assertEquals("hi", r.single());
        assertEquals(List.of("hi"), r.multiple());
        assertEquals(1, r.size());
        assertFalse(r.hasException());
        assertEquals(String.class, r.getSuppliedType());
        assertEquals(STR, r.getSuppliedClass());
    }

    @Test
    public void single_nullValueIsNull() {
        SingleMethodReturn<String> r = SingleMethodReturn.of(null, STR);
        assertTrue(r.isNull());
        assertNull(r.first());
    }

    @Test
    public void single_mapTransforms() {
        SingleMethodReturn<String> r = SingleMethodReturn.of("7", STR);
        IMethodReturn<Integer> mapped = r.map(Integer::valueOf, INT);
        assertEquals(7, mapped.single());
        assertEquals(Integer.class, mapped.getSuppliedType());
    }

    @Test
    public void single_ofExceptionCarriesThrowable() {
        IllegalArgumentException boom = new IllegalArgumentException("nope");
        SingleMethodReturn<String> r = SingleMethodReturn.ofException(boom, STR);
        assertTrue(r.hasException());
        assertSame(boom, r.getException());
        assertNull(r.single());
    }

    @Test
    public void single_ofExceptionNullThrows() {
        assertThrows(NullPointerException.class, () -> SingleMethodReturn.ofException(null, STR));
    }

    @Test
    public void single_nullTypeThrows() {
        assertThrows(NullPointerException.class, () -> SingleMethodReturn.of("x", null));
    }

    @Test
    public void single_equalsAndHashCode() {
        assertEquals(SingleMethodReturn.of("a", STR), SingleMethodReturn.of("a", STR));
        assertNotEquals(SingleMethodReturn.of("a", STR), SingleMethodReturn.of("b", STR));
        assertEquals(SingleMethodReturn.of("a", STR).hashCode(), SingleMethodReturn.of("a", STR).hashCode());
        assertEquals(0, SingleMethodReturn.of(null, STR).hashCode());
    }

    @Test
    public void single_rethrowIfInstanceOfMatching() {
        SingleMethodReturn<String> r = SingleMethodReturn.ofException(new IllegalStateException("s"), STR);
        assertThrows(IllegalStateException.class,
                () -> r.rethrowIfInstanceOf(RuntimeClass.of(IllegalStateException.class)));
    }

    @Test
    public void single_rethrowUncheckedWrapsChecked() {
        SingleMethodReturn<String> r = SingleMethodReturn.ofException(new Exception("checked"), STR);
        assertThrows(RuntimeException.class, r::rethrowUnchecked);
    }

    // ===== MultipleMethodReturn =====

    @Test
    public void multiple_values() {
        MultipleMethodReturn<String> r = MultipleMethodReturn.of(List.of("a", "b"), STR);
        assertFalse(r.isSingle());
        assertTrue(r.isMultiple());
        assertEquals(List.of("a", "b"), r.multiple());
        assertEquals(2, r.size());
        assertEquals("a", r.first());
    }

    @Test
    public void multiple_singleThrows() {
        MultipleMethodReturn<String> r = MultipleMethodReturn.of(List.of("a", "b"), STR);
        assertThrows(IllegalStateException.class, r::single);
    }

    @Test
    public void multiple_mapTransformsAll() {
        MultipleMethodReturn<String> r = MultipleMethodReturn.of(List.of("1", "2"), STR);
        IMethodReturn<Integer> mapped = r.map(Integer::valueOf, INT);
        assertEquals(List.of(1, 2), mapped.multiple());
    }

    @Test
    public void multiple_hasExceptionWhenAnyFails() {
        SingleMethodReturn<String> ok = SingleMethodReturn.of("ok", STR);
        SingleMethodReturn<String> bad = SingleMethodReturn.ofException(new RuntimeException("x"), STR);
        MultipleMethodReturn<String> r = MultipleMethodReturn.ofReturns(STR, List.of(ok, bad));
        assertTrue(r.hasException());
        assertTrue(r.getException() instanceof RuntimeException);
    }

    @Test
    public void multiple_ofMethodReturnsFlattens() {
        IMethodReturn<String> single = SingleMethodReturn.of("s", STR);
        IMethodReturn<String> nested = MultipleMethodReturn.of(List.of("m1", "m2"), STR);
        IMethodReturn<String> failed = SingleMethodReturn.ofException(new RuntimeException("e"), STR);
        MultipleMethodReturn<String> flat = MultipleMethodReturn.ofMethodReturns(
                List.of(single, nested, failed), STR);
        assertEquals(4, flat.getReturns().size());
        assertTrue(flat.hasException());
        assertEquals("s", flat.getReturns().get(0).single());
        assertEquals("m2", flat.getReturns().get(2).single());
    }

    @Test
    public void multiple_getReturnsUnmodifiable() {
        MultipleMethodReturn<String> r = MultipleMethodReturn.of(List.of("a"), STR);
        assertThrows(UnsupportedOperationException.class,
                () -> r.getReturns().add(SingleMethodReturn.of("b", STR)));
    }

    @Test
    public void multiple_equalsAndHashCode() {
        MultipleMethodReturn<String> a = MultipleMethodReturn.of(List.of("a", "b"), STR);
        MultipleMethodReturn<String> b = MultipleMethodReturn.of(List.of("a", "b"), STR);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, MultipleMethodReturn.of(List.of("a", "c"), STR));
    }

    @Test
    public void multiple_nullValuesListThrows() {
        assertThrows(NullPointerException.class, () -> MultipleMethodReturn.of(null, STR));
    }

    @Test
    public void multiple_allNullValuesIsNull() {
        MultipleMethodReturn<String> r = MultipleMethodReturn.of(Arrays.asList(null, null), STR);
        assertTrue(r.isNull());
    }
}
