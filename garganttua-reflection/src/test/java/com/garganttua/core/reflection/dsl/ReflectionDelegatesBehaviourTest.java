package com.garganttua.core.reflection.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.runtime.RuntimeClass;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflection.utils.WildcardTypeImpl;

/**
 * Behaviour tests covering branches of {@code FieldDelegate} and {@code TypeDelegate}
 * exposed through the public {@link IReflection} facade that are not exercised by
 * {@code CompositeReflectionTest} (annotation discovery, address resolution,
 * generic type arguments, collection detection, type-variable extraction).
 */
public class ReflectionDelegatesBehaviourTest {

    private static IReflection reflection;

    @BeforeAll
    static void setUp() throws DslException {
        reflection = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .build();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Marked {
    }

    public static class Leaf {
        @Marked
        public String tagged;
        public String plain;
    }

    public static class Root {
        public Leaf leaf;
        @Marked
        public int directlyTagged;
        public List<String> names;
        public java.util.Map<String, Integer> mapping;
        public String[] array;
        public String scalar;
    }

    public static class NumberBox extends java.util.ArrayList<Number> {
    }

    // ===== findFieldAnnotatedWith (recursive into nested complex types) =====

    @Test
    public void findFieldAnnotatedWith_directField() {
        Optional<IField> found = reflection.findFieldAnnotatedWith(
                RuntimeClass.of(Root.class), RuntimeClass.of(Marked.class));
        assertTrue(found.isPresent());
        // 'directlyTagged' is declared after 'leaf'; recursion into leaf.tagged comes first.
        assertNotNull(found.get());
    }

    @Test
    public void findFieldAnnotatedWith_nestedField() {
        Optional<IField> found = reflection.findFieldAnnotatedWith(
                RuntimeClass.of(Leaf.class), RuntimeClass.of(Marked.class));
        assertTrue(found.isPresent());
        assertEquals("tagged", found.get().getName());
    }

    @Test
    public void findFieldAnnotatedWith_absentReturnsEmpty() {
        Optional<IField> found = reflection.findFieldAnnotatedWith(
                RuntimeClass.of(Leaf.class), RuntimeClass.of(Deprecated.class));
        assertTrue(found.isEmpty());
    }

    // ===== findFieldAddressesWithAnnotation =====

    @Test
    public void findFieldAddresses_unlinkedFindsNestedAndDirect() {
        List<String> addresses = reflection.findFieldAddressesWithAnnotation(
                RuntimeClass.of(Root.class), RuntimeClass.of(Marked.class), false);
        // nested leaf.tagged + the direct directlyTagged
        assertTrue(addresses.contains("tagged"));
        assertTrue(addresses.contains("directlyTagged"));
    }

    @Test
    public void findFieldAddresses_emptyWhenNoAnnotation() {
        List<String> addresses = reflection.findFieldAddressesWithAnnotation(
                RuntimeClass.of(Root.class), RuntimeClass.of(Deprecated.class), false);
        assertTrue(addresses.isEmpty());
    }

    // ===== resolveFieldAddress =====

    @Test
    public void resolveFieldAddress_byName() {
        Optional<ObjectAddress> addr = reflection.resolveFieldAddress("scalar", RuntimeClass.of(Root.class));
        assertTrue(addr.isPresent());
        assertEquals("scalar", addr.get().toString());
    }

    @Test
    public void resolveFieldAddress_byNameUnknownReturnsEmpty() {
        Optional<ObjectAddress> addr = reflection.resolveFieldAddress("ghost", RuntimeClass.of(Root.class));
        assertTrue(addr.isEmpty());
    }

    @Test
    public void resolveFieldAddress_byNameWithType() {
        Optional<ObjectAddress> addr = reflection.resolveFieldAddress(
                "scalar", RuntimeClass.of(Root.class), RuntimeClass.of(String.class));
        assertTrue(addr.isPresent());
    }

    @Test
    public void resolveFieldAddress_byNameWithWrongTypeReturnsEmpty() {
        Optional<ObjectAddress> addr = reflection.resolveFieldAddress(
                "scalar", RuntimeClass.of(Root.class), RuntimeClass.of(Integer.class));
        assertTrue(addr.isEmpty());
    }

    @Test
    public void resolveFieldAddress_byAddress() {
        Optional<ObjectAddress> addr = reflection.resolveFieldAddress(
                new ObjectAddress("scalar", true), RuntimeClass.of(Root.class));
        assertTrue(addr.isPresent());
    }

    // ===== address-based get/set field value =====

    @Test
    public void addressGetAndSetRoundTrip() throws ReflectionException {
        Root root = new Root();
        root.scalar = "before";
        ObjectAddress address = new ObjectAddress("scalar", true);

        reflection.setFieldValue(root, address, "after");
        assertEquals("after", root.scalar);
        assertEquals("after", reflection.getFieldValue(root, address));
    }

    // ===== isCollectionOrMapOrArray =====

    @Test
    public void isCollectionOrMapOrArray_listTrue() throws Exception {
        IField f = RuntimeClass.of(Root.class).getDeclaredField("names");
        assertTrue(reflection.isCollectionOrMapOrArray(f));
    }

    @Test
    public void isCollectionOrMapOrArray_mapTrue() throws Exception {
        IField f = RuntimeClass.of(Root.class).getDeclaredField("mapping");
        assertTrue(reflection.isCollectionOrMapOrArray(f));
    }

    @Test
    public void isCollectionOrMapOrArray_arrayTrue() throws Exception {
        IField f = RuntimeClass.of(Root.class).getDeclaredField("array");
        assertTrue(reflection.isCollectionOrMapOrArray(f));
    }

    @Test
    public void isCollectionOrMapOrArray_scalarFalse() throws Exception {
        IField f = RuntimeClass.of(Root.class).getDeclaredField("scalar");
        assertFalse(reflection.isCollectionOrMapOrArray(f));
    }

    // ===== getGenericTypeArgument (from generic superclass) =====

    @Test
    public void getGenericTypeArgument_fromSuperclass() {
        IClass<?> arg = reflection.getGenericTypeArgument(RuntimeClass.of(NumberBox.class), 0);
        assertNotNull(arg);
        assertEquals(Number.class.getName(), arg.getName());
    }

    @Test
    public void getGenericTypeArgument_nonGenericSuperclassReturnsNull() {
        assertNull(reflection.getGenericTypeArgument(RuntimeClass.of(Root.class), 0));
    }

    // ===== extractClass branches =====

    @Test
    public void extractClass_fromPlainClass() {
        IClass<?> c = reflection.extractClass(String.class);
        assertEquals(String.class.getName(), c.getName());
    }

    @Test
    public void extractClass_fromWildcardUsesUpperBound() {
        Type wildcard = WildcardTypeImpl.extends_(Number.class);
        IClass<?> c = reflection.extractClass(wildcard);
        assertEquals(Number.class.getName(), c.getName());
    }

    @Test
    public void extractClass_fromTypeVariableUsesFirstBound() throws Exception {
        // List<E>'s element type is a TypeVariable bounded by Object
        Type tv = java.util.List.class.getTypeParameters()[0];
        IClass<?> c = reflection.extractClass(tv);
        assertEquals(Object.class.getName(), c.getName());
    }

    @Test
    public void typeEquals_assignableHierarchyMatches() {
        // Number is assignable from Integer -> typeEquals returns true
        assertTrue(reflection.typeEquals(Number.class, Integer.class));
    }

    @Test
    public void typeEquals_unrelatedClassesFalse() {
        assertFalse(reflection.typeEquals(String.class, Integer.class));
    }

    @Test
    public void typeEquals_nullSecondFalse() {
        assertFalse(reflection.typeEquals(String.class, null));
    }

    @Test
    public void parameterTypes_nullArgIsObject() {
        IClass<?>[] types = reflection.parameterTypes(new Object[] { null, "x" });
        assertEquals(Object.class.getName(), types[0].getName());
        assertEquals(String.class.getName(), types[1].getName());
    }

    @Test
    public void extractClass_unsupportedTypeThrows() {
        Type fake = new Type() {
            @Override
            public String getTypeName() {
                return "fake";
            }
        };
        assertThrows(IllegalArgumentException.class, () -> reflection.extractClass(fake));
    }
}
