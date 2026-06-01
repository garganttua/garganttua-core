package com.garganttua.core.aot.reflection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Regression: the AOT field source generator emitted {@code genericType = null}
 * for parameterized fields, so {@code AOTField.getGenericType()} fell back to
 * the erased raw type. Downstream {@code instanceof ParameterizedType} checks
 * (e.g. {@code MappingRules.getFieldGenericType}) then failed and the mapper
 * dropped a {@code List<String>}→{@code List<String>} copy from the direct
 * executor to per-element re-mapping. The generator now emits
 * {@link AOTParameterizedType#of} for parameterized fields.
 */
class AOTFieldGenericTypeTest {

    @Test
    void parameterizedTypeReportsItsActualArguments() {
        AOTParameterizedType type = AOTParameterizedType.of(List.class, String.class);

        assertSame(List.class, type.getRawType());
        assertEquals(1, type.getActualTypeArguments().length);
        assertSame(String.class, type.getActualTypeArguments()[0]);

        AOTParameterizedType map = AOTParameterizedType.of(Map.class, String.class, Integer.class);
        assertSame(String.class, map.getActualTypeArguments()[0]);
        assertSame(Integer.class, map.getActualTypeArguments()[1]);
    }

    @Test
    void aotFieldExposesParameterizedGenericType() {
        // Mirrors what the source generator now emits for `List<String> field;`:
        // a real ParameterizedType instead of the erased raw type.
        AOTField field = new AOTField(
                "items",
                "com.example.Holder",
                "java.util.List",
                Modifier.PRIVATE,
                new Annotation[0],
                AOTParameterizedType.of(List.class, String.class));

        Type generic = field.getGenericType();
        ParameterizedType pt = assertInstanceOf(ParameterizedType.class, generic,
                "AOTField.getGenericType() must report a ParameterizedType for a parameterized field");
        assertSame(List.class, pt.getRawType());
        assertSame(String.class, pt.getActualTypeArguments()[0]);
    }
}
