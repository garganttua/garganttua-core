package com.garganttua.core.reflection.fields;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IFieldValue;
import com.garganttua.core.reflection.runtime.RuntimeClass;

/**
 * Behaviour tests for {@link SingleFieldValue} and {@link MultipleFieldValue}.
 */
public class FieldValueBehaviourTest {

    private static final IClass<String> STR = RuntimeClass.of(String.class);
    private static final IClass<Integer> INT = RuntimeClass.of(Integer.class);

    // ===== SingleFieldValue =====

    @Test
    public void single_holdsValueAndCardinality() {
        SingleFieldValue<String> v = SingleFieldValue.of("hello", STR);
        assertTrue(v.isSingle());
        assertFalse(v.isMultiple());
        assertEquals("hello", v.single());
        assertEquals(1, v.size());
        assertFalse(v.isEmpty());
        assertFalse(v.hasException());
        assertNull(v.getException());
    }

    @Test
    public void single_multipleReturnsSingletonList() {
        SingleFieldValue<String> v = SingleFieldValue.of("x", STR);
        assertEquals(List.of("x"), v.multiple());
        assertEquals("x", v.first());
    }

    @Test
    public void single_nullValueIsNull() {
        SingleFieldValue<String> v = SingleFieldValue.of(null, STR);
        assertTrue(v.isNull());
        assertNull(v.single());
        assertFalse(v.hasException());
    }

    @Test
    public void single_suppliedTypeMatchesDeclared() {
        SingleFieldValue<String> v = SingleFieldValue.of("a", STR);
        assertEquals(String.class, v.getSuppliedType());
        assertEquals(STR, v.getSuppliedClass());
    }

    @Test
    public void single_mapTransformsValueAndType() {
        SingleFieldValue<String> v = SingleFieldValue.of("42", STR);
        IFieldValue<Integer> mapped = v.map(Integer::valueOf, INT);
        assertTrue(mapped.isSingle());
        assertEquals(42, mapped.single());
        assertEquals(Integer.class, mapped.getSuppliedType());
    }

    @Test
    public void single_ofExceptionCarriesThrowable() {
        RuntimeException boom = new RuntimeException("boom");
        SingleFieldValue<String> v = SingleFieldValue.ofException(boom, STR);
        assertTrue(v.hasException());
        assertSame(boom, v.getException());
        assertNull(v.single());
    }

    @Test
    public void single_ofExceptionNullExceptionThrows() {
        assertThrows(NullPointerException.class, () -> SingleFieldValue.ofException(null, STR));
    }

    @Test
    public void single_nullTypeThrows() {
        assertThrows(NullPointerException.class, () -> SingleFieldValue.of("x", null));
    }

    @Test
    public void single_equalsBasedOnValue() {
        assertEquals(SingleFieldValue.of("a", STR), SingleFieldValue.of("a", STR));
        assertNotEquals(SingleFieldValue.of("a", STR), SingleFieldValue.of("b", STR));
        assertEquals(SingleFieldValue.of("a", STR).hashCode(), SingleFieldValue.of("a", STR).hashCode());
        // nulls are equal
        assertEquals(SingleFieldValue.of(null, STR), SingleFieldValue.of(null, STR));
        assertEquals(0, SingleFieldValue.of(null, STR).hashCode());
    }

    @Test
    public void single_toStringMentionsValue() {
        assertTrue(SingleFieldValue.of("zz", STR).toString().contains("zz"));
    }

    @Test
    public void single_singleOptionalPresentAndEmptyForNull() {
        assertTrue(SingleFieldValue.of("a", STR).singleOptional().isPresent());
        assertTrue(SingleFieldValue.of(null, STR).singleOptional().isEmpty());
    }

    // ===== MultipleFieldValue =====

    @Test
    public void multiple_cardinalityAndValues() {
        MultipleFieldValue<String> v = MultipleFieldValue.of(List.of("a", "b", "c"), STR);
        assertFalse(v.isSingle());
        assertTrue(v.isMultiple());
        assertEquals(List.of("a", "b", "c"), v.multiple());
        assertEquals(3, v.size());
        assertEquals("a", v.first());
    }

    @Test
    public void multiple_singleThrows() {
        MultipleFieldValue<String> v = MultipleFieldValue.of(List.of("a", "b"), STR);
        assertThrows(IllegalStateException.class, v::single);
    }

    @Test
    public void multiple_emptyIsEmpty() {
        MultipleFieldValue<String> v = MultipleFieldValue.of(List.of(), STR);
        assertTrue(v.isEmpty());
        assertEquals(0, v.size());
        assertNull(v.first());
    }

    @Test
    public void multiple_mapTransformsAll() {
        MultipleFieldValue<String> v = MultipleFieldValue.of(List.of("1", "2", "3"), STR);
        IFieldValue<Integer> mapped = v.map(Integer::valueOf, INT);
        assertEquals(List.of(1, 2, 3), mapped.multiple());
    }

    @Test
    public void multiple_nullValuesAreAllNull() {
        java.util.List<String> nulls = java.util.Arrays.asList(null, null);
        MultipleFieldValue<String> v = MultipleFieldValue.of(nulls, STR);
        assertTrue(v.isNull());
    }

    @Test
    public void multiple_hasExceptionWhenAnyWrappedFails() {
        SingleFieldValue<String> ok = SingleFieldValue.of("ok", STR);
        SingleFieldValue<String> bad = SingleFieldValue.ofException(new IllegalStateException("x"), STR);
        MultipleFieldValue<String> v = MultipleFieldValue.ofValues(STR, List.of(ok, bad));
        assertTrue(v.hasException());
        assertTrue(v.getException() instanceof IllegalStateException);
    }

    @Test
    public void multiple_noExceptionWhenAllOk() {
        MultipleFieldValue<String> v = MultipleFieldValue.of(List.of("a"), STR);
        assertFalse(v.hasException());
        assertNull(v.getException());
    }

    @Test
    public void multiple_ofFieldValuesFlattensNested() {
        IFieldValue<String> single = SingleFieldValue.of("s", STR);
        IFieldValue<String> nested = MultipleFieldValue.of(List.of("m1", "m2"), STR);
        IFieldValue<String> failed = SingleFieldValue.ofException(new RuntimeException("e"), STR);
        MultipleFieldValue<String> flat = MultipleFieldValue.ofFieldValues(List.of(single, nested, failed), STR);
        // 1 + 2 + 1 (exception entry) = 4 entries
        assertEquals(4, flat.getValues().size());
        assertTrue(flat.hasException());
        // first three non-exception values preserved in order
        assertEquals("s", flat.getValues().get(0).single());
        assertEquals("m1", flat.getValues().get(1).single());
        assertEquals("m2", flat.getValues().get(2).single());
    }

    @Test
    public void multiple_getValuesIsUnmodifiable() {
        MultipleFieldValue<String> v = MultipleFieldValue.of(List.of("a"), STR);
        assertThrows(UnsupportedOperationException.class,
                () -> v.getValues().add(SingleFieldValue.of("b", STR)));
    }

    @Test
    public void multiple_equalsAndHashCode() {
        MultipleFieldValue<String> a = MultipleFieldValue.of(List.of("a", "b"), STR);
        MultipleFieldValue<String> b = MultipleFieldValue.of(List.of("a", "b"), STR);
        MultipleFieldValue<String> c = MultipleFieldValue.of(List.of("a", "c"), STR);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    public void multiple_nullValuesListThrows() {
        assertThrows(NullPointerException.class, () -> MultipleFieldValue.of(null, STR));
    }

    @Test
    public void multiple_streamAndForEach() {
        MultipleFieldValue<String> v = MultipleFieldValue.of(List.of("a", "b"), STR);
        assertEquals(2, v.stream().count());
        StringBuilder sb = new StringBuilder();
        v.forEach(sb::append);
        assertEquals("ab", sb.toString());
    }
}
