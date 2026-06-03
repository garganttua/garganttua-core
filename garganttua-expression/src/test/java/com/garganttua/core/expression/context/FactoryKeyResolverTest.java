package com.garganttua.core.expression.context;

import static com.garganttua.core.supply.dsl.NullSupplierBuilder.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.ExpressionNode;
import com.garganttua.core.expression.IEvaluateNode;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.supply.ISupplier;

/**
 * Behaviour-level unit tests for {@link FactoryKeyResolver}: key building, type-name
 * resolution, primitive/wrapper compatibility and compatible-factory selection (scoring).
 */
public class FactoryKeyResolverTest {

    static class Funcs {
        public static int add(int a, int b) {
            return a + b;
        }

        public static String string(Object value) {
            return String.valueOf(value);
        }

        public static int integer(String value) {
            return Integer.parseInt(value);
        }
    }

    private IReflection reflection;

    @BeforeEach
    void setUp() {
        reflection = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider(), 1)
                .build();
        IClass.setReflection(reflection);
    }

    private FactoryKeyResolver resolverWith(IExpressionNodeFactory<?, ? extends ISupplier<?>>... factories) {
        Map<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>> map = new LinkedHashMap<>();
        for (IExpressionNodeFactory<?, ? extends ISupplier<?>> f : factories) {
            map.put(f.key(), f);
        }
        return new FactoryKeyResolver(map);
    }

    private FactoryKeyResolver emptyResolver() {
        return new FactoryKeyResolver(new LinkedHashMap<>());
    }

    @SuppressWarnings("unchecked")
    private ExpressionNodeFactory<Integer, ISupplier<Integer>> addFactory() throws Exception {
        return new ExpressionNodeFactory<>(
                of(IClass.getClass(Funcs.class)).build(),
                (Class<ISupplier<Integer>>) (Class<?>) ISupplier.class,
                IClass.getClass(Funcs.class).getMethod("add", IClass.getClass(int.class), IClass.getClass(int.class)),
                new ObjectAddress("add"),
                List.of(false, false),
                Optional.of("add"),
                Optional.of("Adds two integers"));
    }

    @SuppressWarnings("unchecked")
    private ExpressionNodeFactory<String, ISupplier<String>> stringFactory() throws Exception {
        return new ExpressionNodeFactory<>(
                of(IClass.getClass(Funcs.class)).build(),
                (Class<ISupplier<String>>) (Class<?>) ISupplier.class,
                IClass.getClass(Funcs.class).getMethod("string", IClass.getClass(Object.class)),
                new ObjectAddress("string"),
                List.of(false),
                Optional.of("string"),
                Optional.of("To string"));
    }

    @SuppressWarnings("unchecked")
    private ExpressionNodeFactory<Integer, ISupplier<Integer>> integerFactory() throws Exception {
        return new ExpressionNodeFactory<>(
                of(IClass.getClass(Funcs.class)).build(),
                (Class<ISupplier<Integer>>) (Class<?>) ISupplier.class,
                IClass.getClass(Funcs.class).getMethod("integer", IClass.getClass(String.class)),
                new ObjectAddress("integer"),
                List.of(false),
                Optional.of("integer"),
                Optional.of("Parse int"));
    }

    /** A minimal IExpressionNode reporting a fixed final-supplied class. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private IExpressionNode<?, ? extends ISupplier<?>> nodeOfType(Class<?> type) {
        IEvaluateNode evaluate = (params) -> new ISupplier<Object>() {
            @Override
            public Optional<Object> supply() {
                return Optional.empty();
            }

            @Override
            public java.lang.reflect.Type getSuppliedType() {
                return type;
            }

            @Override
            public IClass<Object> getSuppliedClass() {
                return (IClass<Object>) (IClass<?>) IClass.getClass(type);
            }
        };
        return new ExpressionNode("node", evaluate, IClass.getClass(type));
    }

    // ---- buildKey ----

    @Test
    public void buildKey_singleParam_usesSimpleName() {
        String key = emptyResolver().buildKey("foo", new Class<?>[] { String.class });
        assertEquals("foo(String)", key);
    }

    @Test
    public void buildKey_multipleParams_commaSeparated() {
        String key = emptyResolver().buildKey("add", new Class<?>[] { int.class, int.class });
        assertEquals("add(int,int)", key);
    }

    @Test
    public void buildKey_noParams_emptyParens() {
        String key = emptyResolver().buildKey("now", new Class<?>[] {});
        assertEquals("now()", key);
    }

    // ---- buildNodeKey ----

    @Test
    public void buildNodeKey_usesFinalSuppliedClassSimpleNameForNodes() {
        FactoryKeyResolver r = emptyResolver();
        List<Object> args = List.of(nodeOfType(Integer.class), nodeOfType(String.class));
        assertEquals("combine(Integer,String)", r.buildNodeKey("combine", args));
    }

    @Test
    public void buildNodeKey_usesRuntimeClassForNonNodeArguments() {
        FactoryKeyResolver r = emptyResolver();
        // Raw (non IExpressionNode) values use their own getClass().getSimpleName()
        List<Object> args = List.of("hello", Integer.valueOf(3));
        assertEquals("raw(String,Integer)", r.buildNodeKey("raw", args));
    }

    @Test
    public void buildNodeKey_emptyArguments() {
        assertEquals("foo()", emptyResolver().buildNodeKey("foo", List.of()));
    }

    // ---- resolveSimpleTypeName ----

    @Test
    public void resolveSimpleTypeName_primitivesMapToWrappers() {
        FactoryKeyResolver r = emptyResolver();
        assertEquals(Integer.class, r.resolveSimpleTypeName("int"));
        assertEquals(Integer.class, r.resolveSimpleTypeName("Integer"));
        assertEquals(Long.class, r.resolveSimpleTypeName("long"));
        assertEquals(Boolean.class, r.resolveSimpleTypeName("boolean"));
        assertEquals(Double.class, r.resolveSimpleTypeName("double"));
        assertEquals(Character.class, r.resolveSimpleTypeName("char"));
        assertEquals(Byte.class, r.resolveSimpleTypeName("byte"));
        assertEquals(Short.class, r.resolveSimpleTypeName("short"));
        assertEquals(Float.class, r.resolveSimpleTypeName("float"));
    }

    @Test
    public void resolveSimpleTypeName_knownReferenceTypes() {
        FactoryKeyResolver r = emptyResolver();
        assertEquals(String.class, r.resolveSimpleTypeName("String"));
        assertEquals(Object.class, r.resolveSimpleTypeName("Object"));
        assertEquals(Class.class, r.resolveSimpleTypeName("Class"));
        assertEquals(IClass.class, r.resolveSimpleTypeName("IClass"));
        assertEquals(java.util.Set.class, r.resolveSimpleTypeName("Set"));
        assertEquals(java.util.List.class, r.resolveSimpleTypeName("List"));
        assertEquals(java.util.Map.class, r.resolveSimpleTypeName("Map"));
        assertEquals(java.util.Optional.class, r.resolveSimpleTypeName("Optional"));
        assertEquals(com.garganttua.core.injection.BeanReference.class,
                r.resolveSimpleTypeName("BeanReference"));
    }

    @Test
    public void resolveSimpleTypeName_fallsBackToJavaLangThenJavaUtil() {
        FactoryKeyResolver r = emptyResolver();
        // Not in the switch, resolved via java.lang.* fallback
        assertEquals(Thread.class, r.resolveSimpleTypeName("Thread"));
        // Resolved via java.util.* fallback
        assertEquals(java.util.ArrayList.class, r.resolveSimpleTypeName("ArrayList"));
    }

    @Test
    public void resolveSimpleTypeName_unknownReturnsNull() {
        assertNull(emptyResolver().resolveSimpleTypeName("ThisTypeDoesNotExistAnywhere"));
    }

    // ---- isPrimitiveCompatible ----

    @Test
    public void isPrimitiveCompatible_intAndInteger() {
        FactoryKeyResolver r = emptyResolver();
        assertTrue(r.isPrimitiveCompatible(int.class, Integer.class));
        assertTrue(r.isPrimitiveCompatible(Integer.class, int.class));
        assertTrue(r.isPrimitiveCompatible(int.class, int.class));
    }

    @Test
    public void isPrimitiveCompatible_mismatchedNumericTypesIncompatible() {
        FactoryKeyResolver r = emptyResolver();
        // int vs long must NOT be considered primitive-compatible
        assertFalse(r.isPrimitiveCompatible(int.class, Long.class));
        assertFalse(r.isPrimitiveCompatible(long.class, Integer.class));
        assertFalse(r.isPrimitiveCompatible(double.class, Float.class));
    }

    @Test
    public void isPrimitiveCompatible_nonPrimitiveTypesReturnFalse() {
        FactoryKeyResolver r = emptyResolver();
        assertFalse(r.isPrimitiveCompatible(String.class, String.class));
        assertFalse(r.isPrimitiveCompatible(Object.class, Object.class));
    }

    @Test
    public void isPrimitiveCompatible_allWrapperPairs() {
        FactoryKeyResolver r = emptyResolver();
        assertTrue(r.isPrimitiveCompatible(boolean.class, Boolean.class));
        assertTrue(r.isPrimitiveCompatible(byte.class, Byte.class));
        assertTrue(r.isPrimitiveCompatible(short.class, Short.class));
        assertTrue(r.isPrimitiveCompatible(char.class, Character.class));
        assertTrue(r.isPrimitiveCompatible(long.class, Long.class));
        assertTrue(r.isPrimitiveCompatible(double.class, Double.class));
        assertTrue(r.isPrimitiveCompatible(float.class, Float.class));
    }

    // ---- hasRegisteredFunction ----

    @Test
    public void hasRegisteredFunction_matchesByNamePrefix() throws Exception {
        FactoryKeyResolver r = resolverWith(addFactory(), stringFactory());
        assertTrue(r.hasRegisteredFunction("add"));
        assertTrue(r.hasRegisteredFunction("string"));
    }

    @Test
    public void hasRegisteredFunction_unknownNameReturnsFalse() throws Exception {
        FactoryKeyResolver r = resolverWith(addFactory());
        assertFalse(r.hasRegisteredFunction("nope"));
    }

    @Test
    public void hasRegisteredFunction_doesNotMatchPartialName() throws Exception {
        FactoryKeyResolver r = resolverWith(addFactory());
        // "ad" is a prefix of "add" but should NOT match because the resolver
        // appends "(" before comparing: "ad(" is not a prefix of "add(...)"
        assertFalse(r.hasRegisteredFunction("ad"));
    }

    // ---- findCompatibleFactoryForDirectParams ----

    @Test
    public void findCompatibleFactoryForDirectParams_exactPrimitiveMatch() throws Exception {
        ExpressionNodeFactory<Integer, ISupplier<Integer>> add = addFactory();
        FactoryKeyResolver r = resolverWith(add);
        IExpressionNodeFactory<?, ?> found =
                r.findCompatibleFactoryForDirectParams("add", new Class<?>[] { Integer.class, Integer.class });
        assertSame(add, found);
    }

    @Test
    public void findCompatibleFactoryForDirectParams_arityMismatchReturnsNull() throws Exception {
        FactoryKeyResolver r = resolverWith(addFactory());
        assertNull(r.findCompatibleFactoryForDirectParams("add", new Class<?>[] { Integer.class }));
    }

    @Test
    public void findCompatibleFactoryForDirectParams_incompatibleTypeReturnsNull() throws Exception {
        FactoryKeyResolver r = resolverWith(integerFactory());
        // integer(String) expects String; passing Integer is not assignable nor primitive-compatible
        assertNull(r.findCompatibleFactoryForDirectParams("integer", new Class<?>[] { Integer.class }));
    }

    @Test
    public void findCompatibleFactoryForDirectParams_objectParamAcceptsString() throws Exception {
        ExpressionNodeFactory<String, ISupplier<String>> str = stringFactory();
        FactoryKeyResolver r = resolverWith(str);
        // string(Object) factory accepts a String argument (assignable)
        IExpressionNodeFactory<?, ?> found =
                r.findCompatibleFactoryForDirectParams("string", new Class<?>[] { String.class });
        assertSame(str, found);
    }

    @Test
    public void findCompatibleFactoryForDirectParams_unknownFunctionReturnsNull() throws Exception {
        FactoryKeyResolver r = resolverWith(addFactory());
        assertNull(r.findCompatibleFactoryForDirectParams("ghost", new Class<?>[] {}));
    }

    // ---- findCompatibleFactory (node-based, with scoring) ----

    @Test
    public void findCompatibleFactory_picksMatchingArityAndType() throws Exception {
        ExpressionNodeFactory<Integer, ISupplier<Integer>> add = addFactory();
        FactoryKeyResolver r = resolverWith(add, stringFactory());
        List<Object> args = List.of(nodeOfType(Integer.class), nodeOfType(Integer.class));
        IExpressionNodeFactory<?, ?> found = r.findCompatibleFactory("add", args);
        assertSame(add, found);
    }

    @Test
    public void findCompatibleFactory_noArityMatchReturnsNull() throws Exception {
        FactoryKeyResolver r = resolverWith(addFactory());
        List<Object> args = List.of(nodeOfType(Integer.class));
        assertNull(r.findCompatibleFactory("add", args));
    }

    @Test
    public void findCompatibleFactory_objectArgMatchesAnyByLowestScore() throws Exception {
        ExpressionNodeFactory<String, ISupplier<String>> str = stringFactory();
        FactoryKeyResolver r = resolverWith(str);
        // an Object-typed node arg is compatible with string(Object)
        List<Object> args = List.of(nodeOfType(Object.class));
        assertSame(str, r.findCompatibleFactory("string", args));
    }

    @Test
    public void findCompatibleFactory_unknownNameReturnsNull() throws Exception {
        FactoryKeyResolver r = resolverWith(addFactory());
        assertNull(r.findCompatibleFactory("missing", List.of(nodeOfType(Integer.class))));
    }

    @Test
    public void findCompatibleFactory_incompatibleParamTypeReturnsNull() throws Exception {
        // integer(String) but we pass an Integer node — not assignable, not primitive compatible
        FactoryKeyResolver r = resolverWith(integerFactory());
        List<Object> args = List.of(nodeOfType(Integer.class));
        assertNull(r.findCompatibleFactory("integer", args));
    }
}
