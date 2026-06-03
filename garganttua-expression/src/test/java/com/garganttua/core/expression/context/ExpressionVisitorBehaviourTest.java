package com.garganttua.core.expression.context;

import static com.garganttua.core.supply.dsl.NullSupplierBuilder.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.expression.functions.Expressions;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.supply.ISupplier;

/**
 * Behaviour tests for the ANTLR {@code ExpressionVisitor} tree-walking, exercised end-to-end
 * through {@link ExpressionContext#expression(String)}. Covers literal kinds, type expressions,
 * variable references (lazy / eager / positional), method &amp; constructor calls, the
 * {@code for(...)} loop and parse-error paths.
 *
 * <p>Uses a manually-built factory registry (the same idiom as {@code ExpressionContextTest})
 * rather than full auto-detection, because auto-detected instance-method factories use
 * {@code BeanSupplierBuilder} which returns an empty owner for non-bean classes such as
 * {@code Expressions} (documented limitation), breaking literal evaluation.
 */
public class ExpressionVisitorBehaviourTest {

    private IExpressionContext context;

    @SuppressWarnings("unchecked")
    private ExpressionNodeFactory<?, ? extends ISupplier<?>> fn(String name, String methodName,
            IClass<?> paramType) throws Exception {
        IClass<Expressions> ex = IClass.getClass(Expressions.class);
        return new ExpressionNodeFactory<>(
                of(IClass.getClass(Expressions.class)).build(),
                (Class<ISupplier<Object>>) (Class<?>) ISupplier.class,
                ex.getMethod(methodName, paramType),
                new ObjectAddress(methodName),
                List.of(false),
                Optional.of(name),
                Optional.of(name + " function"));
    }

    @BeforeEach
    void setUp() throws Exception {
        IClass.setReflection(ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider(), 1)
                .withScanner(new ReflectionsAnnotationScanner(), 1)
                .build());

        Set<IExpressionNodeFactory<?, ? extends ISupplier<?>>> factories = Set.of(
                fn("string", "string", IClass.getClass(Object.class)),
                fn("int", "integer", IClass.getClass(String.class)),
                fn("boolean", "booleanValue", IClass.getClass(String.class)),
                fn("double", "doublenumber", IClass.getClass(String.class)),
                fn("char", "character", IClass.getClass(String.class)),
                fn("class", "Class", IClass.getClass(String.class)));

        context = new ExpressionContext(factories);
    }

    @SuppressWarnings("unchecked")
    private <T> T eval(String expr) {
        ISupplier<?> s = context.expression(expr).evaluate();
        Object v = s.supply().orElse(null);
        if (v instanceof Optional<?> opt) {
            return (T) opt.orElse(null);
        }
        return (T) v;
    }

    // ---- literals ----

    @Test
    public void visitLiteral_string() {
        assertEquals("Hello World", eval("\"Hello World\""));
    }

    @Test
    public void visitLiteral_int() {
        assertEquals(42, (Integer) eval("42"));
    }

    @Test
    public void visitLiteral_negativeViaArithmeticNotApplicable_boolean() {
        assertEquals(Boolean.TRUE, eval("true"));
        assertEquals(Boolean.FALSE, eval("false"));
    }

    @Test
    public void visitLiteral_double() {
        Object v = eval("3.14");
        assertEquals(3.14d, ((Number) v).doubleValue(), 0.0001);
    }

    @Test
    public void visitLiteral_char() {
        assertEquals('A', (Character) eval("'A'"));
    }

    // ---- standalone identifier becomes a string ----

    @Test
    public void visitExpression_standaloneIdentifier_treatedAsString() {
        // bare identifier is routed through createNode("string", text)
        assertEquals("hello", eval("hello"));
    }

    // ---- type expressions ----

    @Test
    public void visitType_primitive() {
        IClass<?> v = eval("int");
        assertEquals(IClass.getClass(int.class), v);
    }

    @Test
    public void visitType_fullyQualifiedClass() {
        IClass<?> v = eval("java.lang.String");
        assertEquals(IClass.getClass(String.class), v);
    }

    @Test
    public void visitType_classOfWildcard() {
        IClass<?> v = eval("Class<?>");
        assertEquals(IClass.getClass(Class.class), v);
    }

    // ---- method call (static) ----

    @Test
    public void visitMethodCall_staticValueOf() {
        assertEquals("12", eval(":valueOf(String.class, 12)"));
    }

    // ---- constructor call ----

    @Test
    public void visitConstructorCall_stringFromLiteral() {
        assertEquals("hello", eval(":(String.class, \"hello\")"));
    }

    @Test
    public void visitConstructorCall_empty_throws() {
        // ":()" has no class argument -> parse failure surfaced as ExpressionException
        assertThrows(ExpressionException.class, () -> context.expression(":()"));
    }

    // ---- variable references ----

    @Test
    public void visitVariableReference_lazyResolvesFromResolver() throws Exception {
        IExpressionVariableResolver resolver = new IExpressionVariableResolver() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> Optional<T> resolve(String name, IClass<T> type) {
                if ("greeting".equals(name)) {
                    return (Optional<T>) Optional.of("bonjour");
                }
                return Optional.empty();
            }
        };

        IExpression<?, ? extends ISupplier<?>> expr = context.expression("@greeting");
        String result = ExpressionVariableContext.callIn(resolver,
                () -> (String) expr.evaluate().supply().orElse(null));
        assertEquals("bonjour", result);
    }

    @Test
    public void visitVariableReference_eagerEvaluatesStoredSupplier() throws Exception {
        // The resolver returns an ISupplier; eager (.x) must unwrap and supply it.
        ISupplier<Object> stored = new ISupplier<>() {
            @Override
            public Optional<Object> supply() {
                return Optional.of("computed");
            }

            @Override
            public java.lang.reflect.Type getSuppliedType() {
                return String.class;
            }

            @Override
            public IClass<Object> getSuppliedClass() {
                return (IClass<Object>) (IClass<?>) IClass.getClass(String.class);
            }
        };
        IExpressionVariableResolver resolver = new IExpressionVariableResolver() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> Optional<T> resolve(String name, IClass<T> type) {
                if ("v".equals(name)) {
                    return (Optional<T>) Optional.of(stored);
                }
                return Optional.empty();
            }
        };

        IExpression<?, ? extends ISupplier<?>> expr = context.expression(".v");
        Object result = ExpressionVariableContext.callIn(resolver,
                () -> expr.evaluate().supply().orElse(null));
        assertEquals("computed", result);
    }

    @Test
    public void visitVariableReference_noResolverBound_throwsOnSupply() {
        IExpression<?, ? extends ISupplier<?>> expr = context.expression("@missing");
        // No resolver in scope -> SupplyException on evaluation
        assertThrows(Exception.class, () -> expr.evaluate().supply());
    }

    @Test
    public void visitVariableReference_positionalArgument_parses() {
        // @0 is a positional argument reference; parsing must succeed.
        IExpression<?, ? extends ISupplier<?>> expr = context.expression("@0");
        assertFalse(expr == null);
    }

    // ---- for loop ----

    @Test
    public void visitForLoop_wrongArity_throws() {
        // for() requires exactly 4 args
        assertThrows(ExpressionException.class, () -> context.expression("for(\"i\", 1, 2)"));
    }

    @Test
    public void visitForLoop_firstArgNotString_throws() {
        // first argument must evaluate to a String (variable name)
        assertThrows(ExpressionException.class,
                () -> context.expression("for(123, 1, true, 1)"));
    }

    // ---- error paths ----

    @Test
    public void expression_null_throwsNPE() {
        assertThrows(NullPointerException.class, () -> context.expression(null));
    }

    @Test
    public void expression_unknownFunction_throwsWithMessage() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> context.expression("definitelyNotAFunction()").evaluate());
        assertTrue(ex.getMessage().contains("Unknown function"), "message: " + ex.getMessage());
    }

    @Test
    public void expression_typeMismatchOnNestedCall_throws() {
        // add(int,int) cannot accept a String arg -> Unknown function: add(String,int)
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> context.expression("add(toto, 30)"));
        assertTrue(ex.getMessage().contains("add(String,int)")
                        || ex.getMessage().contains("Unknown function"),
                "message: " + ex.getMessage());
    }

    @Test
    public void expression_garbageSyntax_throws() {
        assertThrows(ExpressionException.class, () -> context.expression("@@@!!!###"));
    }
}
