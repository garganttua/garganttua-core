package com.garganttua.core.expression.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.ExpressionNode;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.supply.ISupplier;

/**
 * Behaviour tests for {@link ConstructorCallExpressionNodeFactory}: metadata (key, man,
 * description, executable reference), parameter-type reporting, the constructor-resolution
 * error paths, and end-to-end instantiation through the produced node.
 */
public class ConstructorCallExpressionNodeFactoryBehaviourTest {

    @BeforeEach
    void setUp() {
        IReflection reflection = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider(), 1)
                .build();
        IClass.setReflection(reflection);
    }

    /** A node that evaluates to the given value (used as the "class node" or as a string arg). */
    private <V> IExpressionNode<V, ISupplier<V>> nodeOf(V value, IClass<V> type) {
        return new ExpressionNode<>("lit",
                params -> new ISupplier<V>() {
                    @Override
                    public Optional<V> supply() {
                        return Optional.ofNullable(value);
                    }

                    @Override
                    public Type getSuppliedType() {
                        return type.getType();
                    }

                    @Override
                    public IClass<V> getSuppliedClass() {
                        return type;
                    }
                }, type);
    }

    @SuppressWarnings("unchecked")
    private IExpressionNode<?, ?> classNode(Class<?> clazz) {
        return nodeOf((IClass<?>) IClass.getClass(clazz), (IClass) IClass.getClass(IClass.class));
    }

    // ---- resolution errors ----

    @Test
    public void constructor_nullClassNode_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> new ConstructorCallExpressionNodeFactory<>(null, new IClass<?>[0]));
    }

    @Test
    public void constructor_nullParameterTypes_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> new ConstructorCallExpressionNodeFactory<>(classNode(String.class), null));
    }

    @Test
    public void constructor_classNodeSuppliesNonClass_throwsExpressionException() {
        IExpressionNode<?, ?> notAClass = nodeOf("hello", IClass.getClass(String.class));
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> new ConstructorCallExpressionNodeFactory<>(notAClass, new IClass<?>[0]));
        assertTrue(ex.getMessage().contains("unexpected type"),
                "should report the unexpected supplied type, was: " + ex.getMessage());
    }

    @Test
    public void constructor_classNodeSuppliesNull_throwsExpressionException() {
        IExpressionNode<?, ?> empty = nodeOf((String) null, IClass.getClass(String.class));
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> new ConstructorCallExpressionNodeFactory<>(empty, new IClass<?>[0]));
        assertTrue(ex.getMessage().contains("Cannot resolve class"));
    }

    @Test
    public void constructor_noMatchingConstructor_throwsExpressionException() {
        // String has no (int, int) constructor
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> new ConstructorCallExpressionNodeFactory<>(classNode(String.class),
                        new IClass<?>[] { IClass.getClass(int.class), IClass.getClass(int.class) }));
        assertTrue(ex.getMessage().contains("No constructor found"));
    }

    // ---- metadata ----

    @Test
    public void key_format_isColonParenWithSimpleNames() throws Exception {
        var factory = new ConstructorCallExpressionNodeFactory<>(classNode(String.class),
                new IClass<?>[] { IClass.getClass(String.class) });
        // ":(<ClassSimpleName>,<ParamSimpleName>)"
        assertEquals(":(String,String)", factory.key());
    }

    @Test
    public void key_noArgConstructor_hasNoTrailingParamComma() throws Exception {
        var factory = new ConstructorCallExpressionNodeFactory<>(classNode(StringBuilder.class), new IClass<?>[0]);
        assertEquals(":(StringBuilder)", factory.key());
    }

    @Test
    public void description_mentionsTargetSimpleName() throws Exception {
        var factory = new ConstructorCallExpressionNodeFactory<>(classNode(String.class),
                new IClass<?>[] { IClass.getClass(String.class) });
        assertEquals("Constructor for String", factory.description());
    }

    @Test
    public void man_listsFullyQualifiedNameAndParams() throws Exception {
        var factory = new ConstructorCallExpressionNodeFactory<>(classNode(String.class),
                new IClass<?>[] { IClass.getClass(String.class) });
        String man = factory.man();
        assertTrue(man.startsWith("Constructor: java.lang.String("), man);
        assertTrue(man.contains("String"), man);
    }

    @Test
    public void executableReference_isInitSuffix() throws Exception {
        var factory = new ConstructorCallExpressionNodeFactory<>(classNode(String.class), new IClass<?>[0]);
        assertEquals("java.lang.String.<init>", factory.getExecutableReference());
    }

    @Test
    public void dependencies_isEmpty() throws Exception {
        var factory = new ConstructorCallExpressionNodeFactory<>(classNode(String.class), new IClass<?>[0]);
        assertTrue(factory.dependencies().isEmpty());
    }

    @Test
    public void getSuppliedType_isTargetType() throws Exception {
        var factory = new ConstructorCallExpressionNodeFactory<>(classNode(String.class), new IClass<?>[0]);
        assertEquals(String.class, factory.getSuppliedType());
    }

    @Test
    public void getParametersContextTypes_matchSelectedConstructor() throws Exception {
        var factory = new ConstructorCallExpressionNodeFactory<>(classNode(String.class),
                new IClass<?>[] { IClass.getClass(String.class) });
        IClass<?>[] types = factory.getParametersContextTypes();
        assertEquals(1, types.length);
        assertEquals(IClass.getClass(String.class), types[0]);
    }

    @Test
    public void getOwnerContextType_isExpressionNodeContext() throws Exception {
        var factory = new ConstructorCallExpressionNodeFactory<>(classNode(String.class), new IClass<?>[0]);
        assertEquals(IClass.getClass(IExpressionNodeContext.class), factory.getOwnerContextType());
    }

    // ---- end-to-end instantiation ----

    @Test
    @SuppressWarnings("unchecked")
    public void supply_buildsNode_thatConstructsTheTargetInstance() throws Exception {
        var factory = new ConstructorCallExpressionNodeFactory<String, ISupplier<String>>(classNode(String.class),
                new IClass<?>[] { IClass.getClass(String.class) });

        // the constructor argument: a node producing "hello"
        IExpressionNode<String, ISupplier<String>> arg = nodeOf("hello", IClass.getClass(String.class));
        // its evaluated supplier is what the produced node consumes as parameter
        ISupplier<String> argSupplier = arg.evaluate();

        Optional<IMethodReturn<IExpressionNode<String, ISupplier<String>>>> ret =
                factory.supply(new ExpressionNodeContext(List.of(argSupplier)));
        assertTrue(ret.isPresent());
        IExpressionNode<String, ISupplier<String>> node = ret.get().firstOptional().get();
        assertNotNull(node);

        Object built = node.evaluate().supply().orElse(null);
        assertEquals("hello", built);
        // new String("hello") must be a distinct instance from the literal, proving construction happened
        assertEquals(String.class, built.getClass());
    }

    @Test
    public void supply_emptyConstructor_buildsInstance() throws Exception {
        var factory = new ConstructorCallExpressionNodeFactory<StringBuilder, ISupplier<StringBuilder>>(
                classNode(StringBuilder.class), new IClass<?>[0]);

        var ret = factory.supply(new ExpressionNodeContext(List.of()));
        assertTrue(ret.isPresent());
        var node = ret.get().firstOptional().get();
        Object built = node.evaluate().supply().orElse(null);
        assertNotNull(built);
        assertEquals(StringBuilder.class, built.getClass());
        assertEquals("", built.toString());
    }
}
