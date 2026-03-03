package com.garganttua.core.reflection.fields;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.runtime.RuntimeClass;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

public class FieldResolverTest {

    private static final IReflectionProvider provider = new RuntimeReflectionProvider();

    // --- Test classes ---

    public static class SimpleEntity {
        public String name;
        private int age;
        protected double score;
    }

    public static class ParentEntity {
        public String parentField;
    }

    public static class ChildEntity extends ParentEntity {
        public String childField;
    }

    public static class NestedEntity {
        public SimpleEntity nested;
    }

    // ========================================================================
    // fieldByFieldName tests
    // ========================================================================

    @Test
    public void testFieldByFieldName_publicField() throws ReflectionException {
        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleEntity.class), provider, "name");

        assertNotNull(resolved);
        assertEquals("name", resolved.getName());
        assertEquals(RuntimeClass.of(String.class), resolved.fieldType());
    }

    @Test
    public void testFieldByFieldName_privateField() throws ReflectionException {
        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleEntity.class), provider, "age");

        assertNotNull(resolved);
        assertEquals("age", resolved.getName());
        assertEquals(RuntimeClass.of(int.class), resolved.fieldType());
    }

    @Test
    public void testFieldByFieldName_protectedField() throws ReflectionException {
        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleEntity.class), provider, "score");

        assertNotNull(resolved);
        assertEquals("score", resolved.getName());
        assertEquals(RuntimeClass.of(double.class), resolved.fieldType());
    }

    @Test
    public void testFieldByFieldName_withCorrectType() throws ReflectionException {
        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleEntity.class), provider, "name",
                RuntimeClass.of(String.class));

        assertNotNull(resolved);
        assertEquals("name", resolved.getName());
        assertEquals(RuntimeClass.of(String.class), resolved.fieldType());
    }

    @Test
    public void testFieldByFieldName_withWrongType_throws() {
        assertThrows(ReflectionException.class, () ->
                FieldResolver.fieldByFieldName(
                        RuntimeClass.of(SimpleEntity.class), provider, "name",
                        RuntimeClass.of(Integer.class)));
    }

    @Test
    public void testFieldByFieldName_nonExistentField_throws() {
        assertThrows(ReflectionException.class, () ->
                FieldResolver.fieldByFieldName(
                        RuntimeClass.of(SimpleEntity.class), provider, "nonExistent"));
    }

    @Test
    public void testFieldByFieldName_nullFieldName_throws() {
        assertThrows(NullPointerException.class, () ->
                FieldResolver.fieldByFieldName(
                        RuntimeClass.of(SimpleEntity.class), provider, null));
    }

    @Test
    public void testFieldByFieldName_nullOwnerType_throws() {
        assertThrows(NullPointerException.class, () ->
                FieldResolver.fieldByFieldName(null, provider, "name"));
    }

    @Test
    public void testFieldByFieldName_nullProvider_throws() {
        assertThrows(NullPointerException.class, () ->
                FieldResolver.fieldByFieldName(
                        RuntimeClass.of(SimpleEntity.class), null, "name"));
    }

    // ========================================================================
    // fieldByFieldName - inheritance tests
    // ========================================================================

    @Test
    public void testFieldByFieldName_inheritedField() throws ReflectionException {
        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(ChildEntity.class), provider, "parentField");

        assertNotNull(resolved);
        assertEquals("parentField", resolved.getName());
        assertEquals(RuntimeClass.of(String.class), resolved.fieldType());
    }

    @Test
    public void testFieldByFieldName_ownFieldInChild() throws ReflectionException {
        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(ChildEntity.class), provider, "childField");

        assertNotNull(resolved);
        assertEquals("childField", resolved.getName());
        assertEquals(RuntimeClass.of(String.class), resolved.fieldType());
    }

    @Test
    public void testFieldByFieldName_inheritedFieldWithTypeValidation() throws ReflectionException {
        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(ChildEntity.class), provider, "parentField",
                RuntimeClass.of(String.class));

        assertNotNull(resolved);
        assertEquals("parentField", resolved.getName());
    }

    // ========================================================================
    // fieldByAddress tests
    // ========================================================================

    @Test
    public void testFieldByAddress_simpleAddress() throws ReflectionException {
        ObjectAddress address = new ObjectAddress("name", true);

        ResolvedField resolved = FieldResolver.fieldByAddress(
                RuntimeClass.of(SimpleEntity.class), provider, address);

        assertNotNull(resolved);
        assertEquals("name", resolved.getName());
        assertEquals(RuntimeClass.of(String.class), resolved.fieldType());
    }

    @Test
    public void testFieldByAddress_privateField() throws ReflectionException {
        ObjectAddress address = new ObjectAddress("age", true);

        ResolvedField resolved = FieldResolver.fieldByAddress(
                RuntimeClass.of(SimpleEntity.class), provider, address);

        assertNotNull(resolved);
        assertEquals("age", resolved.getName());
        assertEquals(RuntimeClass.of(int.class), resolved.fieldType());
    }

    @Test
    public void testFieldByAddress_withCorrectType() throws ReflectionException {
        ObjectAddress address = new ObjectAddress("name", true);

        ResolvedField resolved = FieldResolver.fieldByAddress(
                RuntimeClass.of(SimpleEntity.class), provider, address,
                RuntimeClass.of(String.class));

        assertNotNull(resolved);
        assertEquals("name", resolved.getName());
    }

    @Test
    public void testFieldByAddress_withWrongType_throws() {
        assertThrows(ReflectionException.class, () -> {
            ObjectAddress address = new ObjectAddress("name", true);
            FieldResolver.fieldByAddress(
                    RuntimeClass.of(SimpleEntity.class), provider, address,
                    RuntimeClass.of(Integer.class));
        });
    }

    @Test
    public void testFieldByAddress_nestedPath() throws ReflectionException {
        ObjectAddress address = new ObjectAddress("nested.name", false);

        ResolvedField resolved = FieldResolver.fieldByAddress(
                RuntimeClass.of(NestedEntity.class), provider, address);

        assertNotNull(resolved);
        assertEquals("name", resolved.getName());
        assertEquals(RuntimeClass.of(String.class), resolved.fieldType());
    }

    @Test
    public void testFieldByAddress_nullAddress_throws() {
        assertThrows(NullPointerException.class, () ->
                FieldResolver.fieldByAddress(
                        RuntimeClass.of(SimpleEntity.class), provider, null));
    }

    // ========================================================================
    // fieldByField tests
    // ========================================================================

    @Test
    public void testFieldByField_withDeclaredField() throws ReflectionException {
        IField[] declaredFields = RuntimeClass.of(SimpleEntity.class).getDeclaredFields();
        IField nameField = null;
        for (IField f : declaredFields) {
            if ("name".equals(f.getName())) {
                nameField = f;
                break;
            }
        }
        assertNotNull(nameField, "Expected to find 'name' in declared fields");

        ResolvedField resolved = FieldResolver.fieldByField(
                RuntimeClass.of(SimpleEntity.class), provider, nameField);

        assertNotNull(resolved);
        assertEquals("name", resolved.getName());
        assertEquals(RuntimeClass.of(String.class), resolved.fieldType());
    }

    @Test
    public void testFieldByField_privateFieldWithDeclaredField() throws ReflectionException {
        IField[] declaredFields = RuntimeClass.of(SimpleEntity.class).getDeclaredFields();
        IField ageField = null;
        for (IField f : declaredFields) {
            if ("age".equals(f.getName())) {
                ageField = f;
                break;
            }
        }
        assertNotNull(ageField, "Expected to find 'age' in declared fields");

        ResolvedField resolved = FieldResolver.fieldByField(
                RuntimeClass.of(SimpleEntity.class), provider, ageField);

        assertNotNull(resolved);
        assertEquals("age", resolved.getName());
        assertEquals(RuntimeClass.of(int.class), resolved.fieldType());
    }

    @Test
    public void testFieldByField_withTypeValidation() throws ReflectionException {
        IField[] declaredFields = RuntimeClass.of(SimpleEntity.class).getDeclaredFields();
        IField nameField = null;
        for (IField f : declaredFields) {
            if ("name".equals(f.getName())) {
                nameField = f;
                break;
            }
        }
        assertNotNull(nameField, "Expected to find 'name' in declared fields");

        ResolvedField resolved = FieldResolver.fieldByField(
                RuntimeClass.of(SimpleEntity.class), provider, nameField,
                RuntimeClass.of(String.class));

        assertNotNull(resolved);
        assertEquals("name", resolved.getName());
    }

    @Test
    public void testFieldByField_withWrongType_throws() {
        IField[] declaredFields = RuntimeClass.of(SimpleEntity.class).getDeclaredFields();
        IField nameField = null;
        for (IField f : declaredFields) {
            if ("name".equals(f.getName())) {
                nameField = f;
                break;
            }
        }
        assertNotNull(nameField, "Expected to find 'name' in declared fields");

        final IField fieldToResolve = nameField;
        assertThrows(ReflectionException.class, () ->
                FieldResolver.fieldByField(
                        RuntimeClass.of(SimpleEntity.class), provider, fieldToResolve,
                        RuntimeClass.of(Integer.class)));
    }

    @Test
    public void testFieldByField_nullField_throws() {
        assertThrows(NullPointerException.class, () ->
                FieldResolver.fieldByField(
                        RuntimeClass.of(SimpleEntity.class), provider, null));
    }

    // ========================================================================
    // ResolvedField property tests
    // ========================================================================

    @Test
    public void testResolvedField_ownerType() throws ReflectionException {
        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleEntity.class), provider, "name");

        assertEquals(RuntimeClass.of(SimpleEntity.class), resolved.ownerType());
    }

    @Test
    public void testResolvedField_isStatic_false() throws ReflectionException {
        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleEntity.class), provider, "name");

        assertFalse(resolved.isStatic());
    }

    @Test
    public void testResolvedField_address() throws ReflectionException {
        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleEntity.class), provider, "name");

        assertNotNull(resolved.address());
        assertEquals("name", resolved.address().toString());
    }

    @Test
    public void testResolvedField_fieldPath_notEmpty() throws ReflectionException {
        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleEntity.class), provider, "name");

        assertNotNull(resolved.fieldPath());
        assertFalse(resolved.fieldPath().isEmpty());
    }

    // ========================================================================
    // Assignable type validation tests
    // ========================================================================

    @Test
    public void testFieldByFieldName_withAssignableSupertype() throws ReflectionException {
        // Object is assignable from String, so this should succeed
        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleEntity.class), provider, "name",
                RuntimeClass.of(Object.class));

        assertNotNull(resolved);
        assertEquals("name", resolved.getName());
    }

    @Test
    public void testFieldByFieldName_withPrimitiveType() throws ReflectionException {
        ResolvedField resolved = FieldResolver.fieldByFieldName(
                RuntimeClass.of(SimpleEntity.class), provider, "age",
                RuntimeClass.of(int.class));

        assertNotNull(resolved);
        assertEquals("age", resolved.getName());
        assertEquals(RuntimeClass.of(int.class), resolved.fieldType());
    }
}
