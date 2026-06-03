package com.garganttua.core.reflection.fields;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeClass;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

/**
 * Behaviour tests for {@link Fields}: generic type extraction, array/map/collection
 * detection, primitive checks, default-value instantiation and the process-wide
 * class blacklist.
 */
public class FieldsBehaviourTest {

    private static final IReflectionProvider PROVIDER = new RuntimeReflectionProvider();

    @BeforeAll
    static void setUpReflection() throws DslException {
        IReflection reflection = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .build();
        IClass.setReflection(reflection);
    }

    @AfterAll
    static void tearDown() {
        IClass.setReflection(null);
    }

    public static class Holder {
        public List<String> stringList;
        public Map<Integer, String> intToString;
        public String[] array;
        public String plain;
        public int primitive;
        public List raw; // no type argument

        // primitive-typed declarations used to drive default instantiation
        public int i;
        public long l;
        public float f;
        public double d;
        public short sh;
        public byte b;
        public char c;
        public boolean bool;
    }

    public static class InterfaceFieldHolder {
        public List<String> list;
        public Set<String> set;
        public Map<String, String> map;
        public Queue<String> queue;
        public SortedSet<String> sortedSet; // Collection but not List/Set/Queue subinterface order
        public Collection<String> collection;
    }

    public static class NoDefaultCtor {
        public NoDefaultCtor(int required) {
        }
    }

    private static IField field(Class<?> owner, String name) throws ReflectionException {
        for (IField f : RuntimeClass.of(owner).getDeclaredFields()) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        throw new IllegalStateException("no field " + name);
    }

    // ========================================================================
    // getGenericType
    // ========================================================================

    @Test
    public void getGenericTypeReturnsListElementType() throws ReflectionException {
        IClass<?> generic = Fields.getGenericType(field(Holder.class, "stringList"), 0);
        assertEquals(RuntimeClass.of(String.class), generic);
    }

    @Test
    public void getGenericTypeWithProviderReturnsListElementType() throws ReflectionException {
        IClass<?> generic = Fields.getGenericType(field(Holder.class, "stringList"), 0, PROVIDER);
        assertEquals(String.class, generic.getType());
    }

    @Test
    public void getGenericTypeSecondArgumentOfMap() throws ReflectionException {
        IClass<?> key = Fields.getGenericType(field(Holder.class, "intToString"), 0);
        IClass<?> value = Fields.getGenericType(field(Holder.class, "intToString"), 1);
        assertEquals(RuntimeClass.of(Integer.class), key);
        assertEquals(RuntimeClass.of(String.class), value);
    }

    @Test
    public void getGenericTypeOutOfBoundsIndexReturnsNull() throws ReflectionException {
        assertNull(Fields.getGenericType(field(Holder.class, "stringList"), 5));
    }

    @Test
    public void getGenericTypeOnRawTypeReturnsNull() throws ReflectionException {
        assertNull(Fields.getGenericType(field(Holder.class, "raw"), 0));
    }

    @Test
    public void getGenericTypeOnNonParameterizedFieldReturnsNull() throws ReflectionException {
        assertNull(Fields.getGenericType(field(Holder.class, "plain"), 0));
    }

    // ========================================================================
    // isArrayOrMapOrCollectionField
    // ========================================================================

    @Test
    public void detectsCollectionField() throws ReflectionException {
        assertTrue(Fields.isArrayOrMapOrCollectionField(field(Holder.class, "stringList")));
        assertTrue(Fields.isArrayOrMapOrCollectionField(field(Holder.class, "stringList"), PROVIDER));
    }

    @Test
    public void detectsMapField() throws ReflectionException {
        assertTrue(Fields.isArrayOrMapOrCollectionField(field(Holder.class, "intToString")));
        assertTrue(Fields.isArrayOrMapOrCollectionField(field(Holder.class, "intToString"), PROVIDER));
    }

    @Test
    public void detectsArrayField() throws ReflectionException {
        assertTrue(Fields.isArrayOrMapOrCollectionField(field(Holder.class, "array")));
        assertTrue(Fields.isArrayOrMapOrCollectionField(field(Holder.class, "array"), PROVIDER));
    }

    @Test
    public void plainAndPrimitiveFieldsAreNotContainers() throws ReflectionException {
        assertFalse(Fields.isArrayOrMapOrCollectionField(field(Holder.class, "plain")));
        assertFalse(Fields.isArrayOrMapOrCollectionField(field(Holder.class, "primitive")));
    }

    // ========================================================================
    // isNotPrimitive / isNotPrimitiveOrInternal
    // ========================================================================

    @Test
    public void primitiveCheckTreatsPrimitivesAndSimpleTypesAsPrimitive() {
        // int is primitive; String is a "simple type" (wrapper/String/Date family) so also reported as primitive.
        assertFalse(Fields.isNotPrimitive(RuntimeClass.of(int.class)));
        assertFalse(Fields.isNotPrimitive(RuntimeClass.of(String.class)));
        assertFalse(Fields.isNotPrimitive(RuntimeClass.of(Integer.class)));
        // A user-defined type is NOT primitive-like.
        assertTrue(Fields.isNotPrimitive(RuntimeClass.of(Holder.class)));
    }

    @Test
    public void internalCheckExcludesJdkAndPrimitiveTypes() {
        assertFalse(Fields.isNotPrimitiveOrInternal(RuntimeClass.of(int.class)));
        assertFalse(Fields.isNotPrimitiveOrInternal(RuntimeClass.of(String.class)));
        assertTrue(Fields.isNotPrimitiveOrInternal(RuntimeClass.of(Holder.class)));
    }

    // ========================================================================
    // instanciate - primitives
    // ========================================================================

    @Test
    public void instanciatePrimitiveIntReturnsOne() throws ReflectionException {
        assertEquals(1, Fields.instanciate(field(Holder.class, "i")));
    }

    @Test
    public void instanciatePrimitiveLongReturnsZero() throws ReflectionException {
        assertEquals(0L, Fields.instanciate(field(Holder.class, "l")));
    }

    @Test
    public void instanciatePrimitiveBooleanReturnsFalse() throws ReflectionException {
        assertEquals(Boolean.FALSE, Fields.instanciate(field(Holder.class, "bool")));
    }

    @Test
    public void instanciatePrimitiveCharReturnsZeroDigit() throws ReflectionException {
        assertEquals('0', Fields.instanciate(field(Holder.class, "c")));
    }

    @Test
    public void instanciatePrimitiveByteShortFloatDouble() throws ReflectionException {
        assertEquals((byte) 0, Fields.instanciate(field(Holder.class, "b")));
        assertEquals((short) 0, Fields.instanciate(field(Holder.class, "sh")));
        assertEquals(0F, Fields.instanciate(field(Holder.class, "f")));
        assertEquals(0D, Fields.instanciate(field(Holder.class, "d")));
    }

    // ========================================================================
    // instanciate - arrays and collection interfaces (fallback path)
    // ========================================================================

    @Test
    public void instanciateArrayFieldReturnsEmptyArrayOfComponentType() throws ReflectionException {
        Object value = Fields.instanciate(field(Holder.class, "array"));
        assertInstanceOf(String[].class, value);
        assertEquals(0, ((String[]) value).length);
    }

    @Test
    public void instanciateListInterfaceReturnsArrayList() throws ReflectionException {
        Object value = Fields.instanciate(field(InterfaceFieldHolder.class, "list"));
        assertInstanceOf(java.util.ArrayList.class, value);
    }

    @Test
    public void instanciateSetInterfaceReturnsHashSet() throws ReflectionException {
        Object value = Fields.instanciate(field(InterfaceFieldHolder.class, "set"));
        assertInstanceOf(java.util.HashSet.class, value);
    }

    @Test
    public void instanciateMapInterfaceReturnsHashMap() throws ReflectionException {
        Object value = Fields.instanciate(field(InterfaceFieldHolder.class, "map"));
        assertInstanceOf(java.util.HashMap.class, value);
    }

    @Test
    public void instanciateQueueInterfaceReturnsLinkedList() throws ReflectionException {
        Object value = Fields.instanciate(field(InterfaceFieldHolder.class, "queue"));
        assertInstanceOf(java.util.LinkedList.class, value);
    }

    @Test
    public void instanciateGenericCollectionInterfaceReturnsVector() throws ReflectionException {
        Object value = Fields.instanciate(field(InterfaceFieldHolder.class, "collection"));
        assertInstanceOf(java.util.Vector.class, value);
    }

    @Test
    public void instanciateConcreteClassUsesNoArgConstructor() throws ReflectionException {
        Object value = Fields.instanciate(field(Holder.class, "plain"));
        assertInstanceOf(String.class, value);
        // String() no-arg yields empty string
        assertEquals("", value);
    }

    // ========================================================================
    // BlackList
    // ========================================================================

    @Test
    public void blacklistStartsUnlistedThenBecomesListed() {
        IClass<?> target = RuntimeClass.of(FieldsBehaviourTest.class);
        assertFalse(Fields.BlackList.isBlackListed(target));
        Fields.BlackList.addClassToBlackList(target);
        assertTrue(Fields.BlackList.isBlackListed(target));
    }

    // ========================================================================
    // prettyColored
    // ========================================================================

    @Test
    public void prettyColoredContainsOwnerFieldAndType() throws ReflectionException {
        String pretty = Fields.prettyColored(field(Holder.class, "plain"));
        assertTrue(pretty.contains("Holder"));
        assertTrue(pretty.contains("plain"));
        assertTrue(pretty.contains("String"));
    }
}
