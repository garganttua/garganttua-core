package com.garganttua.core.reflection.fields;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IFieldValue;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.runtime.RuntimeClass;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

public class FieldAccessorTest {

    private static final IReflectionProvider PROVIDER = new RuntimeReflectionProvider();

    // --- Test domain objects ---

    public static class SimpleObject {
        public String publicField = "publicValue";
        private String privateField = "privateValue";
        protected String protectedField = "protectedValue";
        String packageField = "packageValue";

        public SimpleObject() {}
    }

    public static class Inner {
        private String label = "innerLabel";

        public Inner() {}
    }

    public static class Outer {
        private Inner inner = new Inner();

        public Outer() {}
    }

    public static class DeepNested {
        private Outer outer = new Outer();

        public DeepNested() {}
    }

    // ========================================================================
    // getValue - simple fields
    // ========================================================================

    @Test
    public void testGetValuePublicField() throws ReflectionException {
        SimpleObject obj = new SimpleObject();
        obj.publicField = "hello";

        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleObject.class), PROVIDER, "publicField");
        FieldAccessor<String> accessor = new FieldAccessor<>(resolved);

        IFieldValue<String> result = accessor.getValue(obj);

        assertTrue(result.isSingle());
        assertEquals("hello", result.single());
    }

    @Test
    public void testGetValuePrivateField() throws ReflectionException {
        SimpleObject obj = new SimpleObject();

        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleObject.class), PROVIDER, "privateField");
        FieldAccessor<String> accessor = new FieldAccessor<>(resolved);

        IFieldValue<String> result = accessor.getValue(obj);

        assertTrue(result.isSingle());
        assertEquals("privateValue", result.single());
    }

    @Test
    public void testGetValueProtectedField() throws ReflectionException {
        SimpleObject obj = new SimpleObject();

        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleObject.class), PROVIDER, "protectedField");
        FieldAccessor<String> accessor = new FieldAccessor<>(resolved);

        IFieldValue<String> result = accessor.getValue(obj);

        assertTrue(result.isSingle());
        assertEquals("protectedValue", result.single());
    }

    @Test
    public void testGetValuePackagePrivateField() throws ReflectionException {
        SimpleObject obj = new SimpleObject();

        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleObject.class), PROVIDER, "packageField");
        FieldAccessor<String> accessor = new FieldAccessor<>(resolved);

        IFieldValue<String> result = accessor.getValue(obj);

        assertTrue(result.isSingle());
        assertEquals("packageValue", result.single());
    }

    // ========================================================================
    // setValue - simple fields
    // ========================================================================

    @Test
    public void testSetValuePublicField() throws ReflectionException {
        SimpleObject obj = new SimpleObject();

        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleObject.class), PROVIDER, "publicField");
        FieldAccessor<String> accessor = new FieldAccessor<>(resolved);

        IFieldValue<String> newValue = SingleFieldValue.of("updated",
                RuntimeClass.of(String.class));
        accessor.setValue(obj, newValue);

        assertEquals("updated", obj.publicField);
    }

    @Test
    public void testSetValuePrivateField() throws ReflectionException {
        SimpleObject obj = new SimpleObject();

        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleObject.class), PROVIDER, "privateField");
        FieldAccessor<String> accessor = new FieldAccessor<>(resolved);

        IFieldValue<String> newValue = SingleFieldValue.of("secret",
                RuntimeClass.of(String.class));
        accessor.setValue(obj, newValue);

        // Verify via getValue
        IFieldValue<String> result = accessor.getValue(obj);
        assertEquals("secret", result.single());
    }

    @Test
    public void testSetValueProtectedField() throws ReflectionException {
        SimpleObject obj = new SimpleObject();

        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleObject.class), PROVIDER, "protectedField");
        FieldAccessor<String> accessor = new FieldAccessor<>(resolved);

        IFieldValue<String> newValue = SingleFieldValue.of("newProtected",
                RuntimeClass.of(String.class));
        accessor.setValue(obj, newValue);

        // Verify via getValue
        IFieldValue<String> result = accessor.getValue(obj);
        assertEquals("newProtected", result.single());
    }

    // ========================================================================
    // getValue / setValue - nested objects
    // ========================================================================

    @Test
    public void testGetValueNestedField() throws ReflectionException {
        Outer obj = new Outer();

        // fieldByFieldName does deep-search for the leaf field name "label"
        // and automatically builds the address "inner.label"
        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(Outer.class), PROVIDER, "label");
        FieldAccessor<String> accessor = new FieldAccessor<>(resolved);

        IFieldValue<String> result = accessor.getValue(obj);

        assertTrue(result.isSingle());
        assertEquals("innerLabel", result.single());
    }

    @Test
    public void testSetValueNestedField() throws ReflectionException {
        Outer obj = new Outer();

        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(Outer.class), PROVIDER, "label");
        FieldAccessor<String> accessor = new FieldAccessor<>(resolved);

        IFieldValue<String> newValue = SingleFieldValue.of("changed",
                RuntimeClass.of(String.class));
        accessor.setValue(obj, newValue);

        // Verify
        IFieldValue<String> result = accessor.getValue(obj);
        assertEquals("changed", result.single());
    }

    @Test
    public void testGetValueDeepNestedField() throws ReflectionException {
        DeepNested obj = new DeepNested();

        // Deep search: "label" is found through outer -> inner -> label
        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(DeepNested.class), PROVIDER, "label");
        FieldAccessor<String> accessor = new FieldAccessor<>(resolved);

        IFieldValue<String> result = accessor.getValue(obj);

        assertTrue(result.isSingle());
        assertEquals("innerLabel", result.single());
    }

    @Test
    public void testSetValueDeepNestedField() throws ReflectionException {
        DeepNested obj = new DeepNested();

        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(DeepNested.class), PROVIDER, "label");
        FieldAccessor<String> accessor = new FieldAccessor<>(resolved);

        IFieldValue<String> newValue = SingleFieldValue.of("deepChanged",
                RuntimeClass.of(String.class));
        accessor.setValue(obj, newValue);

        IFieldValue<String> result = accessor.getValue(obj);
        assertEquals("deepChanged", result.single());
    }

    @Test
    public void testGetValueNestedNullIntermediate() throws ReflectionException {
        Outer obj = new Outer();
        obj.inner = null;

        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(Outer.class), PROVIDER, "label");
        FieldAccessor<String> accessor = new FieldAccessor<>(resolved);

        IFieldValue<String> result = accessor.getValue(obj);

        assertTrue(result.isSingle());
        assertNull(result.single());
    }

    // ========================================================================
    // Error cases
    // ========================================================================

    @Test
    public void testGetValueNullObjectThrows() throws ReflectionException {
        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleObject.class), PROVIDER, "publicField");
        FieldAccessor<String> accessor = new FieldAccessor<>(resolved);

        assertThrows(ReflectionException.class, () -> accessor.getValue(null));
    }

    @Test
    public void testSetValueNullObjectThrows() throws ReflectionException {
        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleObject.class), PROVIDER, "publicField");
        FieldAccessor<String> accessor = new FieldAccessor<>(resolved);

        IFieldValue<String> newValue = SingleFieldValue.of("value",
                RuntimeClass.of(String.class));

        assertThrows(ReflectionException.class, () -> accessor.setValue(null, newValue));
    }

    @Test
    public void testGetValueWrongObjectTypeThrows() throws ReflectionException {
        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleObject.class), PROVIDER, "publicField");
        FieldAccessor<String> accessor = new FieldAccessor<>(resolved);

        assertThrows(ReflectionException.class, () -> accessor.getValue("not a SimpleObject"));
    }

    @Test
    public void testSetValueWrongObjectTypeThrows() throws ReflectionException {
        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleObject.class), PROVIDER, "publicField");
        FieldAccessor<String> accessor = new FieldAccessor<>(resolved);

        IFieldValue<String> newValue = SingleFieldValue.of("value",
                RuntimeClass.of(String.class));

        assertThrows(ReflectionException.class, () -> accessor.setValue("not a SimpleObject", newValue));
    }

    // ========================================================================
    // setValue that creates a new instance
    // ========================================================================

    @Test
    public void testSetValueCreatesNewInstance() throws ReflectionException {
        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleObject.class), PROVIDER, "publicField");
        FieldAccessor<String> accessor = new FieldAccessor<>(resolved);

        IFieldValue<String> newValue = SingleFieldValue.of("freshValue",
                RuntimeClass.of(String.class));
        IFieldValue<String> result = accessor.setValue(newValue);

        assertEquals("freshValue", result.single());
    }

    // ========================================================================
    // Round-trip: set then get
    // ========================================================================

    @Test
    public void testRoundTripSetThenGet() throws ReflectionException {
        SimpleObject obj = new SimpleObject();

        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleObject.class), PROVIDER, "privateField");
        FieldAccessor<String> accessor = new FieldAccessor<>(resolved);

        IFieldValue<String> toSet = SingleFieldValue.of("roundTrip",
                RuntimeClass.of(String.class));
        accessor.setValue(obj, toSet);

        IFieldValue<String> retrieved = accessor.getValue(obj);
        assertEquals("roundTrip", retrieved.single());
        assertFalse(retrieved.hasException());
    }

    @Test
    public void testGetValueReturnsNullForUninitializedField() throws ReflectionException {
        SimpleObject obj = new SimpleObject();
        obj.publicField = null;

        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleObject.class), PROVIDER, "publicField");
        FieldAccessor<String> accessor = new FieldAccessor<>(resolved);

        IFieldValue<String> result = accessor.getValue(obj);

        assertTrue(result.isSingle());
        assertNull(result.single());
        assertTrue(result.isNull());
    }
}
