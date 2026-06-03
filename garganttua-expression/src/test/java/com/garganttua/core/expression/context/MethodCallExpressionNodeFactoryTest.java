package com.garganttua.core.expression.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.ExpressionNode;
import com.garganttua.core.expression.IEvaluateNode;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.supply.ISupplier;

/**
 * Behaviour tests for {@link MethodCallExpressionNodeFactory}: argument validation, static
 * vs instance method resolution, the deferred (owner type == Object) path, key/description/man
 * generation and error handling.
 */
public class MethodCallExpressionNodeFactoryTest {

    @BeforeEach
    void setUp() {
        IReflection reflection = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider(), 1)
                .build();
        IClass.setReflection(reflection);
    }

    /** Owner node that supplies a fixed value, declaring a chosen final-supplied class. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private IExpressionNode<?, ? extends ISupplier<?>> ownerNode(Object value, Class<?> declaredType) {
        IEvaluateNode evaluate = (params) -> new ISupplier<Object>() {
            @Override
            public Optional<Object> supply() {
                return Optional.ofNullable(value);
            }

            @Override
            public java.lang.reflect.Type getSuppliedType() {
                return declaredType;
            }

            @Override
            public IClass<Object> getSuppliedClass() {
                return (IClass<Object>) (IClass<?>) IClass.getClass(declaredType);
            }
        };
        return new ExpressionNode("owner", evaluate, IClass.getClass(declaredType));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <S extends ISupplier<?>> IExpressionNode<?, S> cast(IExpressionNode<?, ? extends ISupplier<?>> node) {
        return (IExpressionNode) node;
    }

    // ---- argument validation ----

    @Test
    public void constructor_nullOwnerNode_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> new MethodCallExpressionNodeFactory<Object, ISupplier<Object>>(null, "length", new IClass<?>[] {}));
    }

    @Test
    public void constructor_nullMethodName_throwsNPE() {
        IExpressionNode<?, ? extends ISupplier<?>> owner = ownerNode("hello", String.class);
        assertThrows(NullPointerException.class,
                () -> new MethodCallExpressionNodeFactory<Object, ISupplier<Object>>(cast(owner), null, new IClass<?>[] {}));
    }

    @Test
    public void constructor_nullParameterTypes_throwsNPE() {
        IExpressionNode<?, ? extends ISupplier<?>> owner = ownerNode("hello", String.class);
        assertThrows(NullPointerException.class,
                () -> new MethodCallExpressionNodeFactory<Object, ISupplier<Object>>(cast(owner), "length", null));
    }

    // ---- instance method call ----

    @Test
    public void instanceMethodCall_lengthOnString_returnsValue() throws Exception {
        IExpressionNode<?, ? extends ISupplier<?>> owner = ownerNode("hello", String.class);
        MethodCallExpressionNodeFactory<Integer, ISupplier<Integer>> factory =
                new MethodCallExpressionNodeFactory<>(cast(owner), "length", new IClass<?>[] {});

        ExpressionNodeContext ctx = new ExpressionNodeContext(List.of());
        Optional<IMethodReturn<IExpressionNode<Integer, ISupplier<Integer>>>> ret = factory.supply(ctx);
        IExpressionNode<Integer, ISupplier<Integer>> node = ret.flatMap(IMethodReturn::firstOptional).orElseThrow();

        assertEquals(5, node.evaluate().supply().get());
    }

    @Test
    public void instanceMethodCall_key_isNotDeferredForm() throws Exception {
        IExpressionNode<?, ? extends ISupplier<?>> owner = ownerNode("hello", String.class);
        MethodCallExpressionNodeFactory<Integer, ISupplier<Integer>> factory =
                new MethodCallExpressionNodeFactory<>(cast(owner), "length", new IClass<?>[] {});
        // Non-deferred key is delegated to the inner ExpressionNodeFactory and contains the
        // method name with parentheses but no leading ':'.
        String key = factory.key();
        assertTrue(key.startsWith("length("), "key was: " + key);
        assertFalse(key.startsWith(":"), "non-deferred key must not start with ':' : " + key);
    }

    @Test
    public void instanceMethodCall_descriptionDefault() throws Exception {
        IExpressionNode<?, ? extends ISupplier<?>> owner = ownerNode("hello", String.class);
        MethodCallExpressionNodeFactory<Integer, ISupplier<Integer>> factory =
                new MethodCallExpressionNodeFactory<>(cast(owner), "length", new IClass<?>[] {});
        assertEquals("No description available", factory.description());
    }

    @Test
    public void instanceMethodCall_unknownMethod_throws() {
        IExpressionNode<?, ? extends ISupplier<?>> owner = ownerNode("hello", String.class);
        // No such method "thisMethodDoesNotExist" on String -> resolution must fail
        assertThrows(Exception.class,
                () -> new MethodCallExpressionNodeFactory<Object, ISupplier<Object>>(cast(owner), "thisMethodDoesNotExist", new IClass<?>[] {}));
    }

    // ---- static method call (owner supplies a Class / IClass) ----

    @Test
    public void staticMethodCall_ownerIsClass_resolvesStaticTarget() throws Exception {
        // owner node declares type Class.class and supplies the IClass for Integer,
        // so static resolution targets Integer.valueOf(String) -> Integer.
        IClass<Integer> integerIClass = IClass.getClass(Integer.class);
        IExpressionNode<?, ? extends ISupplier<?>> owner = ownerNode(integerIClass, Class.class);

        MethodCallExpressionNodeFactory<Integer, ISupplier<Integer>> factory =
                new MethodCallExpressionNodeFactory<>(cast(owner), "valueOf",
                        new IClass<?>[] { IClass.getClass(String.class) });

        ExpressionNodeContext ctx = new ExpressionNodeContext(List.of("42"));
        Optional<IMethodReturn<IExpressionNode<Integer, ISupplier<Integer>>>> ret = factory.supply(ctx);
        IExpressionNode<Integer, ISupplier<Integer>> node = ret.flatMap(IMethodReturn::firstOptional).orElseThrow();

        assertEquals(42, node.evaluate().supply().get());
    }

    @Test
    public void staticMethodCall_classOwnerSuppliesNonClass_throws() {
        // owner declared as Class.class but supplies a String (not a Class/IClass) -> error
        IExpressionNode<?, ? extends ISupplier<?>> owner = ownerNode("notAClass", Class.class);
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> new MethodCallExpressionNodeFactory<>(cast(owner), "valueOf",
                        new IClass<?>[] { IClass.getClass(int.class) }));
        assertTrue(ex.getMessage().contains("Cannot resolve class for static method call"),
                "message was: " + ex.getMessage());
    }

    @Test
    public void staticMethodCall_classOwnerSuppliesNull_throws() {
        // owner declared as Class.class but supplies null -> orElseThrow path
        IExpressionNode<?, ? extends ISupplier<?>> owner = ownerNode(null, Class.class);
        assertThrows(ExpressionException.class,
                () -> new MethodCallExpressionNodeFactory<>(cast(owner), "valueOf",
                        new IClass<?>[] { IClass.getClass(int.class) }));
    }

    // ---- deferred resolution (owner type is Object) ----

    @Test
    public void deferred_ownerTypeObject_keyHasColonPrefix() throws Exception {
        // Owner declared as Object => deferred. Supplies a String at runtime.
        IExpressionNode<?, ? extends ISupplier<?>> owner = ownerNode("hello", Object.class);
        MethodCallExpressionNodeFactory<Object, ISupplier<Object>> factory =
                new MethodCallExpressionNodeFactory<>(cast(owner), "length", new IClass<?>[] {});

        assertEquals(":length()", factory.key());
    }

    @Test
    public void deferred_ownerTypeObject_descriptionAndMan() throws Exception {
        IExpressionNode<?, ? extends ISupplier<?>> owner = ownerNode("hello", Object.class);
        MethodCallExpressionNodeFactory<Object, ISupplier<Object>> factory =
                new MethodCallExpressionNodeFactory<>(cast(owner), "length", new IClass<?>[] {});

        assertEquals("Deferred method call: length", factory.description());
        assertEquals("Deferred method call: length", factory.man());
    }

    @Test
    public void deferred_ownerTypeObject_executableReferenceIsColonName() throws Exception {
        IExpressionNode<?, ? extends ISupplier<?>> owner = ownerNode("hello", Object.class);
        MethodCallExpressionNodeFactory<Object, ISupplier<Object>> factory =
                new MethodCallExpressionNodeFactory<>(cast(owner), "trim", new IClass<?>[] {});

        assertEquals(":trim", factory.getExecutableReference());
    }

    @Test
    public void deferred_ownerTypeObject_dependenciesEmptyAndSuppliedTypeObject() throws Exception {
        IExpressionNode<?, ? extends ISupplier<?>> owner = ownerNode("hello", Object.class);
        MethodCallExpressionNodeFactory<Object, ISupplier<Object>> factory =
                new MethodCallExpressionNodeFactory<>(cast(owner), "trim", new IClass<?>[] {});

        assertTrue(factory.dependencies().isEmpty());
        assertEquals(Object.class, factory.getSuppliedType());
    }

    @Test
    public void deferred_keyIncludesParameterSimpleNames() throws Exception {
        IExpressionNode<?, ? extends ISupplier<?>> owner = ownerNode("hello", Object.class);
        MethodCallExpressionNodeFactory<Object, ISupplier<Object>> factory =
                new MethodCallExpressionNodeFactory<>(cast(owner), "substring",
                        new IClass<?>[] { IClass.getClass(int.class) });
        assertEquals(":substring(int)", factory.key());
    }

    @Test
    public void deferred_getParametersContextTypes_returnsDeferredTypes() throws Exception {
        IExpressionNode<?, ? extends ISupplier<?>> owner = ownerNode("hello", Object.class);
        IClass<?>[] paramTypes = new IClass<?>[] { IClass.getClass(int.class) };
        MethodCallExpressionNodeFactory<Object, ISupplier<Object>> factory =
                new MethodCallExpressionNodeFactory<>(cast(owner), "substring", paramTypes);

        IClass<?>[] returned = factory.getParametersContextTypes();
        assertEquals(1, returned.length);
        assertEquals(IClass.getClass(int.class), returned[0]);
    }

    @Test
    public void deferred_evaluatesAgainstRuntimeType() throws Exception {
        // Owner is Object at compile time but resolves to a String "  hi  " at runtime.
        IExpressionNode<?, ? extends ISupplier<?>> owner = ownerNode("  hi  ", Object.class);
        MethodCallExpressionNodeFactory<String, ISupplier<String>> factory =
                new MethodCallExpressionNodeFactory<>(cast(owner), "trim", new IClass<?>[] {});

        ExpressionNodeContext ctx = new ExpressionNodeContext(List.of());
        Optional<IMethodReturn<IExpressionNode<String, ISupplier<String>>>> ret = factory.supply(ctx);
        IExpressionNode<String, ISupplier<String>> node = ret.flatMap(IMethodReturn::firstOptional).orElseThrow();

        assertEquals("hi", node.evaluate().supply().get());
    }

    @Test
    public void deferred_evaluatesWithParameters() throws Exception {
        // "hello".substring(2) == "llo", resolved at runtime through the deferred path.
        IExpressionNode<?, ? extends ISupplier<?>> owner = ownerNode("hello", Object.class);
        MethodCallExpressionNodeFactory<String, ISupplier<String>> factory =
                new MethodCallExpressionNodeFactory<>(cast(owner), "substring",
                        new IClass<?>[] { IClass.getClass(int.class) });

        ExpressionNodeContext ctx = new ExpressionNodeContext(List.of(2));
        Optional<IMethodReturn<IExpressionNode<String, ISupplier<String>>>> ret = factory.supply(ctx);
        IExpressionNode<String, ISupplier<String>> node = ret.flatMap(IMethodReturn::firstOptional).orElseThrow();

        assertEquals("llo", node.evaluate().supply().get());
    }

    @Test
    public void deferred_ownerNullAtRuntime_throwsOnSupply() throws Exception {
        // Owner declared Object, supplies null at runtime -> deferred supplier throws.
        IExpressionNode<?, ? extends ISupplier<?>> owner = ownerNode(null, Object.class);
        MethodCallExpressionNodeFactory<String, ISupplier<String>> factory =
                new MethodCallExpressionNodeFactory<>(cast(owner), "trim", new IClass<?>[] {});

        ExpressionNodeContext ctx = new ExpressionNodeContext(List.of());
        IExpressionNode<String, ISupplier<String>> node =
                factory.supply(ctx).flatMap(IMethodReturn::firstOptional).orElseThrow();

        // The owner null is detected lazily when the supplier is evaluated.
        assertThrows(Exception.class, () -> node.evaluate().supply());
    }

    @Test
    public void execute_delegatesToSupply_instanceCall() throws Exception {
        IExpressionNode<?, ? extends ISupplier<?>> owner = ownerNode("  abc  ", String.class);
        MethodCallExpressionNodeFactory<String, ISupplier<String>> factory =
                new MethodCallExpressionNodeFactory<>(cast(owner), "trim", new IClass<?>[] {});

        ExpressionNodeContext ctx = new ExpressionNodeContext(List.of());
        Optional<IMethodReturn<IExpressionNode<String, ISupplier<String>>>> ret = factory.execute(ctx);
        IExpressionNode<String, ISupplier<String>> node = ret.flatMap(IMethodReturn::firstOptional).orElseThrow();

        assertEquals("abc", node.evaluate().supply().get());
    }
}
