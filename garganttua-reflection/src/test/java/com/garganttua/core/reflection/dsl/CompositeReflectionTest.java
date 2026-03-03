package com.garganttua.core.reflection.dsl;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IObjectQuery;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

public class CompositeReflectionTest {

    private static IReflection reflection;

    @BeforeAll
    static void setUp() throws DslException {
        reflection = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .build();
    }

    // ========================================================================
    // Test domain classes
    // ========================================================================

    public static class SampleClass {
        public String name;
        private int value;

        public SampleClass() {}

        public SampleClass(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setValue(int v) {
            this.value = v;
        }

        public int getValue() {
            return value;
        }
    }

    public static class NoDefaultConstructor {
        private final String data;

        public NoDefaultConstructor(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }
    }

    // ========================================================================
    // 1. getClass(Class) -- returns a valid IClass wrapper
    // ========================================================================

    @Test
    void getClass_returnsValidIClassWrapper() {
        IClass<SampleClass> clazz = reflection.getClass(SampleClass.class);

        assertNotNull(clazz);
        assertEquals(SampleClass.class.getName(), clazz.getName());
        assertEquals("SampleClass", clazz.getSimpleName());
        assertFalse(clazz.isPrimitive());
        assertFalse(clazz.isInterface());
    }

    @Test
    void getClass_forPrimitive_returnsIClass() {
        IClass<Integer> clazz = reflection.getClass(int.class);

        assertNotNull(clazz);
        assertTrue(clazz.isPrimitive());
    }

    // ========================================================================
    // 2. forName(String) -- resolves a known class
    // ========================================================================

    @Test
    void forName_withKnownClass_returnsIClass() throws ClassNotFoundException {
        IClass<?> clazz = reflection.forName("java.lang.String");

        assertNotNull(clazz);
        assertEquals("java.lang.String", clazz.getName());
        assertEquals("String", clazz.getSimpleName());
    }

    @Test
    void forName_withTestClass_returnsIClass() throws ClassNotFoundException {
        IClass<?> clazz = reflection.forName(SampleClass.class.getName());

        assertNotNull(clazz);
        assertEquals(SampleClass.class.getName(), clazz.getName());
    }

    // ========================================================================
    // 3. forName(String) with unknown class -- throws ClassNotFoundException
    // ========================================================================

    @Test
    void forName_withUnknownClass_throwsClassNotFoundException() {
        assertThrows(ClassNotFoundException.class, () ->
                reflection.forName("com.nonexistent.NoSuchClass"));
    }

    // ========================================================================
    // 4. findConstructor(IClass) -- finds no-arg constructor
    // ========================================================================

    @Test
    void findConstructor_noArg_returnsConstructor() {
        IClass<SampleClass> clazz = reflection.getClass(SampleClass.class);

        Optional<IConstructor<?>> ctor = reflection.findConstructor(clazz);

        assertTrue(ctor.isPresent());
        assertEquals(0, ctor.get().getParameterCount());
    }

    @Test
    void findConstructor_withParams_returnsMatchingConstructor() {
        IClass<SampleClass> clazz = reflection.getClass(SampleClass.class);
        IClass<String> stringClass = reflection.getClass(String.class);

        Optional<IConstructor<?>> ctor = reflection.findConstructor(clazz, stringClass);

        assertTrue(ctor.isPresent());
        assertEquals(1, ctor.get().getParameterCount());
    }

    @Test
    void findConstructor_noArgOnClassWithoutDefault_returnsEmpty() {
        IClass<NoDefaultConstructor> clazz = reflection.getClass(NoDefaultConstructor.class);

        Optional<IConstructor<?>> ctor = reflection.findConstructor(clazz);

        assertTrue(ctor.isEmpty());
    }

    // ========================================================================
    // 5. newInstance(IClass) -- creates a new instance
    // ========================================================================

    @Test
    void newInstance_noArg_createsInstance() throws ReflectionException {
        IClass<SampleClass> clazz = reflection.getClass(SampleClass.class);

        SampleClass instance = reflection.newInstance(clazz);

        assertNotNull(instance);
        assertNull(instance.name);
        assertEquals(0, instance.getValue());
    }

    @Test
    void newInstance_withArgs_createsInstance() throws ReflectionException {
        IClass<SampleClass> clazz = reflection.getClass(SampleClass.class);

        SampleClass instance = reflection.newInstance(clazz, "hello");

        assertNotNull(instance);
        assertEquals("hello", instance.getName());
    }

    @Test
    void newInstance_noArgOnClassWithoutDefault_throwsReflectionException() {
        IClass<NoDefaultConstructor> clazz = reflection.getClass(NoDefaultConstructor.class);

        assertThrows(ReflectionException.class, () -> reflection.newInstance(clazz));
    }

    // ========================================================================
    // 6. findField(IClass, String) -- finds a field
    // ========================================================================

    @Test
    void findField_existingPublicField_returnsField() {
        IClass<SampleClass> clazz = reflection.getClass(SampleClass.class);

        Optional<IField> field = reflection.findField(clazz, "name");

        assertTrue(field.isPresent());
        assertEquals("name", field.get().getName());
    }

    @Test
    void findField_existingPrivateField_returnsField() {
        IClass<SampleClass> clazz = reflection.getClass(SampleClass.class);

        Optional<IField> field = reflection.findField(clazz, "value");

        assertTrue(field.isPresent());
        assertEquals("value", field.get().getName());
    }

    @Test
    void findField_nonExistentField_returnsEmpty() {
        IClass<SampleClass> clazz = reflection.getClass(SampleClass.class);

        Optional<IField> field = reflection.findField(clazz, "nonExistent");

        assertTrue(field.isEmpty());
    }

    // ========================================================================
    // 7. findMethod(IClass, String) -- finds a method
    // ========================================================================

    @Test
    void findMethod_existingMethod_returnsMethod() {
        IClass<SampleClass> clazz = reflection.getClass(SampleClass.class);

        Optional<IMethod> method = reflection.findMethod(clazz, "getName");

        assertTrue(method.isPresent());
        assertEquals("getName", method.get().getName());
    }

    @Test
    void findMethod_nonExistentMethod_returnsEmpty() {
        IClass<SampleClass> clazz = reflection.getClass(SampleClass.class);

        Optional<IMethod> method = reflection.findMethod(clazz, "nonExistent");

        assertTrue(method.isEmpty());
    }

    @Test
    void findMethods_returnsAllOverloads() {
        IClass<SampleClass> clazz = reflection.getClass(SampleClass.class);

        // SampleClass has getName() and getValue() but no overloads of getName
        List<IMethod> methods = reflection.findMethods(clazz, "getName");

        assertFalse(methods.isEmpty());
        assertTrue(methods.stream().allMatch(m -> "getName".equals(m.getName())));
    }

    // ========================================================================
    // 8. invokeMethod(Object, IMethod, IClass, Object...) -- invokes a method
    // ========================================================================

    @Test
    void invokeMethod_withIMethod_returnsResult() throws ReflectionException {
        IClass<SampleClass> clazz = reflection.getClass(SampleClass.class);
        SampleClass instance = new SampleClass("test");

        Optional<IMethod> method = reflection.findMethod(clazz, "getName");
        assertTrue(method.isPresent());

        IClass<String> returnType = reflection.getClass(String.class);
        String result = reflection.invokeMethod(instance, method.get(), returnType);

        assertEquals("test", result);
    }

    @Test
    void invokeMethod_byName_returnsResult() throws ReflectionException {
        SampleClass instance = new SampleClass("byName");

        IClass<String> returnType = reflection.getClass(String.class);
        String result = reflection.invokeMethod(instance, "getName", returnType);

        assertEquals("byName", result);
    }

    @Test
    void invokeMethod_voidMethod_executesSuccessfully() throws ReflectionException {
        IClass<SampleClass> clazz = reflection.getClass(SampleClass.class);
        SampleClass instance = new SampleClass();

        Optional<IMethod> method = reflection.findMethod(clazz, "setValue");
        assertTrue(method.isPresent());

        IClass<Void> returnType = reflection.getClass(void.class);
        reflection.invokeMethod(instance, method.get(), returnType, 42);

        assertEquals(42, instance.getValue());
    }

    // ========================================================================
    // 9. query(IClass) -- creates an object query
    // ========================================================================

    @Test
    void query_byClass_returnsObjectQuery() throws ReflectionException {
        IClass<SampleClass> clazz = reflection.getClass(SampleClass.class);

        IObjectQuery<SampleClass> query = reflection.query(clazz);

        assertNotNull(query);
    }

    @Test
    void query_byObject_returnsObjectQuery() throws ReflectionException {
        SampleClass instance = new SampleClass("query");

        IObjectQuery<SampleClass> query = reflection.query(instance);

        assertNotNull(query);
    }

    @Test
    void query_withNullObject_throwsReflectionException() {
        assertThrows(ReflectionException.class, () -> reflection.query((Object) null));
    }

    @Test
    void query_canFindFieldByName() throws ReflectionException {
        IClass<SampleClass> clazz = reflection.getClass(SampleClass.class);
        IObjectQuery<SampleClass> query = reflection.query(clazz);

        List<Object> result = query.find("name");

        assertFalse(result.isEmpty());
    }

    // ========================================================================
    // 10. isComplexType(IClass) -- true for user types, false for primitives
    // ========================================================================

    @Test
    void isComplexType_userClass_returnsTrue() {
        IClass<SampleClass> clazz = reflection.getClass(SampleClass.class);

        assertTrue(reflection.isComplexType(clazz));
    }

    @Test
    void isComplexType_primitiveInt_returnsFalse() {
        IClass<Integer> clazz = reflection.getClass(int.class);

        assertFalse(reflection.isComplexType(clazz));
    }

    @Test
    void isComplexType_string_returnsFalse() {
        IClass<String> clazz = reflection.getClass(String.class);

        assertFalse(reflection.isComplexType(clazz));
    }

    @Test
    void isComplexType_wrapperInteger_returnsFalse() {
        IClass<Integer> clazz = reflection.getClass(Integer.class);

        assertFalse(reflection.isComplexType(clazz));
    }

    // ========================================================================
    // 11. extractClass(Type) -- extracts raw class from Type
    // ========================================================================

    @Test
    void extractClass_fromPlainClass_returnsIClass() {
        IClass<?> result = reflection.extractClass(String.class);

        assertNotNull(result);
        assertEquals("java.lang.String", result.getName());
    }

    @Test
    void extractClass_fromParameterizedType_returnsRawClass() throws NoSuchFieldException {
        // Use a field to get a ParameterizedType at runtime
        Type genericType = GenericFieldHolder.class.getDeclaredField("strings").getGenericType();
        assertTrue(genericType instanceof ParameterizedType);

        IClass<?> result = reflection.extractClass(genericType);

        assertNotNull(result);
        assertEquals("java.util.List", result.getName());
    }

    // ========================================================================
    // Additional field access tests
    // ========================================================================

    @Test
    void getFieldValue_returnsFieldValue() throws ReflectionException {
        SampleClass instance = new SampleClass("fieldAccess");

        Object value = reflection.getFieldValue(instance, "name");

        assertEquals("fieldAccess", value);
    }

    @Test
    void setFieldValue_setsFieldValue() throws ReflectionException {
        SampleClass instance = new SampleClass();

        reflection.setFieldValue(instance, "name", "updated");

        assertEquals("updated", instance.name);
    }

    @Test
    void setFieldValue_privateField_setsValue() throws ReflectionException {
        SampleClass instance = new SampleClass();

        reflection.setFieldValue(instance, "value", 99);

        assertEquals(99, instance.getValue());
    }

    @Test
    void getFieldValue_nonExistentField_throwsReflectionException() {
        SampleClass instance = new SampleClass();

        assertThrows(ReflectionException.class, () ->
                reflection.getFieldValue(instance, "nonExistent"));
    }

    // ========================================================================
    // Additional type utility tests
    // ========================================================================

    @Test
    void supports_returnsTrue() {
        assertTrue(reflection.supports(SampleClass.class));
        assertTrue(reflection.supports(String.class));
    }

    @Test
    void typeEquals_sameType_returnsTrue() {
        assertTrue(reflection.typeEquals(String.class, String.class));
    }

    @Test
    void typeEquals_differentType_returnsFalse() {
        assertFalse(reflection.typeEquals(String.class, Integer.class));
    }

    @Test
    void parameterTypes_fromArgs_returnsCorrectTypes() {
        IClass<?>[] types = reflection.parameterTypes(new Object[]{"hello", 42});

        assertEquals(2, types.length);
        assertEquals("java.lang.String", types[0].getName());
        assertEquals("java.lang.Integer", types[1].getName());
    }

    @Test
    void parameterTypes_nullArgs_returnsEmptyArray() {
        IClass<?>[] types = reflection.parameterTypes(null);

        assertEquals(0, types.length);
    }

    @Test
    void isImplementingInterface_implementsInterface_returnsTrue() {
        IClass<?> serializableClass = reflection.getClass(java.io.Serializable.class);
        IClass<?> stringClass = reflection.getClass(String.class);

        assertTrue(reflection.isImplementingInterface(serializableClass, stringClass));
    }

    @Test
    void isImplementingInterface_doesNotImplement_returnsFalse() {
        IClass<?> runnableClass = reflection.getClass(Runnable.class);
        IClass<?> stringClass = reflection.getClass(String.class);

        assertFalse(reflection.isImplementingInterface(runnableClass, stringClass));
    }

    // ========================================================================
    // Helper classes for generic type testing
    // ========================================================================

    @SuppressWarnings("unused")
    private static class GenericFieldHolder {
        List<String> strings;
    }
}
