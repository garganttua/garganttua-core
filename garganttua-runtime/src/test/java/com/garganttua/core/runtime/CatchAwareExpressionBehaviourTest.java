package com.garganttua.core.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.IExpression;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.runtime.CatchAwareExpression.CatchHandler;
import com.garganttua.core.runtime.CatchAwareExpression.CatchResultException;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Behaviour tests for {@link CatchAwareExpression} and its {@link CatchHandler}
 * matcher logic: success pass-through, matching handler producing a
 * {@link CatchResultException}, non-matching propagation, handler-failure
 * wrapping, and match-all/predicate-based handlers.
 */
class CatchAwareExpressionBehaviourTest {

    private static IReflectionBuilder reflectionBuilder;

    @BeforeAll
    static void setup() throws Exception {
        reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .withScanner(new ReflectionsAnnotationScanner());
        reflectionBuilder.build();
    }

    @AfterAll
    static void tearDown() {
        IClass.setReflection(null);
    }

    /** Expression that supplies a fixed value. */
    private static final class ValueExpression implements IExpression<String, ISupplier<String>> {
        private final String value;

        ValueExpression(String value) {
            this.value = value;
        }

        @Override
        public ISupplier<String> evaluate() {
            return new FixedSupplier<>(value, IClass.getClass(String.class));
        }

        @Override
        public IClass<String> getSuppliedClass() {
            return IClass.getClass(String.class);
        }

        @Override
        public Type getSuppliedType() {
            return String.class;
        }

        @Override
        public boolean isContextual() {
            return false;
        }
    }

    /** Expression whose supplier throws the supplied throwable on supply(). */
    private static final class ThrowingExpression implements IExpression<String, ISupplier<String>> {
        private final java.lang.RuntimeException toThrow;

        ThrowingExpression(java.lang.RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public ISupplier<String> evaluate() {
            return new ISupplier<String>() {
                @Override
                public Optional<String> supply() {
                    throw toThrow;
                }

                @Override
                public Type getSuppliedType() {
                    return String.class;
                }

                @Override
                public IClass<String> getSuppliedClass() {
                    return IClass.getClass(String.class);
                }
            };
        }

        @Override
        public IClass<String> getSuppliedClass() {
            return IClass.getClass(String.class);
        }

        @Override
        public Type getSuppliedType() {
            return String.class;
        }

        @Override
        public boolean isContextual() {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Constructor validation
    // -------------------------------------------------------------------------

    @Test
    void constructor_nullInner_throwsNpe() {
        var ex = assertThrows(NullPointerException.class,
                () -> new CatchAwareExpression<>(null, List.of()));
        assertEquals("Inner expression cannot be null", ex.getMessage());
    }

    @Test
    void constructor_nullHandlers_throwsNpe() {
        var ex = assertThrows(NullPointerException.class,
                () -> new CatchAwareExpression<>(new ValueExpression("x"), null));
        assertEquals("Handlers cannot be null", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // Delegating metadata
    // -------------------------------------------------------------------------

    @Test
    void metadata_delegatesToInnerExpression() {
        CatchAwareExpression<String> expr = new CatchAwareExpression<>(new ValueExpression("x"), List.of());
        assertEquals(String.class, expr.getSuppliedType());
        assertTrue(expr.getSuppliedClass().represents(String.class));
        assertFalse(expr.isContextual());
    }

    // -------------------------------------------------------------------------
    // Success pass-through
    // -------------------------------------------------------------------------

    @Test
    void supply_innerSucceeds_returnsInnerValue_handlersUntouched() throws Exception {
        CatchAwareExpression<String> expr = new CatchAwareExpression<>(new ValueExpression("ok"),
                List.of(new CatchHandler<>(List.of(IClass.getClass(java.lang.RuntimeException.class)),
                        new ValueExpression("handled"), Optional.empty())));
        assertEquals(Optional.of("ok"), expr.evaluate().supply());
    }

    // -------------------------------------------------------------------------
    // Matching handler => CatchResultException carrying handler result
    // -------------------------------------------------------------------------

    @Test
    void supply_matchingHandler_throwsCatchResultExceptionWithHandlerValueAndVariable() {
        // NOTE: type-based matching resolves against the cause chain
        // (CoreException.findFirstInException starts at getCause()), so the matched
        // type must appear as a CAUSE of the thrown exception.
        CatchHandler<String> handler = new CatchHandler<>(
                List.of(IClass.getClass(IllegalStateException.class)),
                new ValueExpression("recovered"), Optional.of(7), "myVar");
        CatchAwareExpression<String> expr = new CatchAwareExpression<>(
                new ThrowingExpression(new java.lang.RuntimeException("wrap",
                        new IllegalStateException("boom"))), List.of(handler));

        CatchResultException cre = assertThrows(CatchResultException.class,
                () -> expr.evaluate().supply());
        assertEquals("recovered", cre.getResult());
        assertEquals("myVar", cre.getVariableName());
    }

    @Test
    void supply_matchingHandlerWithoutVariable_carriesNullVariableName() {
        CatchHandler<String> handler = new CatchHandler<>(
                List.of(IClass.getClass(IllegalStateException.class)),
                new ValueExpression("recovered"), Optional.empty());
        CatchAwareExpression<String> expr = new CatchAwareExpression<>(
                new ThrowingExpression(new java.lang.RuntimeException("wrap",
                        new IllegalStateException("boom"))), List.of(handler));

        CatchResultException cre = assertThrows(CatchResultException.class,
                () -> expr.evaluate().supply());
        assertEquals("recovered", cre.getResult());
        assertNull(cre.getVariableName());
    }

    // -------------------------------------------------------------------------
    // Non-matching handler => exception propagates (as SupplyException)
    // -------------------------------------------------------------------------

    @Test
    void supply_noMatchingHandler_propagatesAsSupplyException() {
        CatchHandler<String> handler = new CatchHandler<>(
                List.of(IClass.getClass(IllegalArgumentException.class)),
                new ValueExpression("never"), Optional.empty());
        CatchAwareExpression<String> expr = new CatchAwareExpression<>(
                new ThrowingExpression(new IllegalStateException("unmatched")), List.of(handler));

        // IllegalStateException is not an IllegalArgumentException -> no handler -> wrapped.
        assertThrows(SupplyException.class, () -> expr.evaluate().supply());
    }

    @Test
    void supply_emptyHandlers_propagatesAsSupplyException() {
        CatchAwareExpression<String> expr = new CatchAwareExpression<>(
                new ThrowingExpression(new IllegalStateException("boom")), List.of());
        assertThrows(SupplyException.class, () -> expr.evaluate().supply());
    }

    // -------------------------------------------------------------------------
    // Match-all handler (empty exception types)
    // -------------------------------------------------------------------------

    @Test
    void supply_matchAllHandler_catchesAnyException() {
        CatchHandler<String> matchAll = new CatchHandler<>(
                List.<IClass<? extends Throwable>>of(), new ValueExpression("catch-all"), Optional.empty());
        CatchAwareExpression<String> expr = new CatchAwareExpression<>(
                new ThrowingExpression(new ArithmeticException("/0")), List.of(matchAll));

        CatchResultException cre = assertThrows(CatchResultException.class,
                () -> expr.evaluate().supply());
        assertEquals("catch-all", cre.getResult());
    }

    @Test
    void supply_nullExceptionTypes_isTreatedAsMatchAll() {
        CatchHandler<String> matchAll = new CatchHandler<>(
                (List<IClass<? extends Throwable>>) null, new ValueExpression("any"), Optional.empty());
        CatchAwareExpression<String> expr = new CatchAwareExpression<>(
                new ThrowingExpression(new java.lang.RuntimeException("x")), List.of(matchAll));

        CatchResultException cre = assertThrows(CatchResultException.class,
                () -> expr.evaluate().supply());
        assertEquals("any", cre.getResult());
    }

    // -------------------------------------------------------------------------
    // Ordering: first matching handler wins
    // -------------------------------------------------------------------------

    @Test
    void supply_firstMatchingHandlerWins() {
        CatchHandler<String> first = new CatchHandler<>(
                List.of(IClass.getClass(java.lang.RuntimeException.class)), new ValueExpression("first"), Optional.empty());
        CatchHandler<String> second = new CatchHandler<>(
                List.of(IClass.getClass(IllegalStateException.class)), new ValueExpression("second"),
                Optional.empty());
        CatchAwareExpression<String> expr = new CatchAwareExpression<>(
                new ThrowingExpression(new java.lang.RuntimeException("wrap",
                        new IllegalStateException("boom"))), List.of(first, second));

        CatchResultException cre = assertThrows(CatchResultException.class,
                () -> expr.evaluate().supply());
        assertEquals("first", cre.getResult(), "earliest matching handler must win");
    }

    // -------------------------------------------------------------------------
    // Handler itself fails => wrapped in SupplyException
    // -------------------------------------------------------------------------

    @Test
    void supply_handlerThrows_isWrappedInSupplyException() {
        CatchHandler<String> failing = new CatchHandler<>(
                List.of(IClass.getClass(java.lang.RuntimeException.class)),
                new ThrowingExpression(new IllegalArgumentException("handler-failed")), Optional.empty());
        CatchAwareExpression<String> expr = new CatchAwareExpression<>(
                new ThrowingExpression(new java.lang.RuntimeException("wrap",
                        new IllegalStateException("original"))), List.of(failing));

        SupplyException ex = assertThrows(SupplyException.class, () -> expr.evaluate().supply());
        assertEquals("Catch handler failed", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // Predicate-based handler
    // -------------------------------------------------------------------------

    @Test
    void predicateHandler_matchesByCustomPredicate() {
        CatchHandler<String> byMessage = new CatchHandler<>(
                (Throwable t) -> "special".equals(t.getMessage()),
                new ValueExpression("matched-by-predicate"), Optional.empty(), null);
        CatchAwareExpression<String> expr = new CatchAwareExpression<>(
                new ThrowingExpression(new java.lang.RuntimeException("special")), List.of(byMessage));

        CatchResultException cre = assertThrows(CatchResultException.class,
                () -> expr.evaluate().supply());
        assertEquals("matched-by-predicate", cre.getResult());
    }

    @Test
    void predicateHandler_doesNotMatch_propagates() {
        CatchHandler<String> byMessage = new CatchHandler<>(
                (Throwable t) -> "special".equals(t.getMessage()),
                new ValueExpression("nope"), Optional.empty(), null);
        CatchAwareExpression<String> expr = new CatchAwareExpression<>(
                new ThrowingExpression(new java.lang.RuntimeException("ordinary")), List.of(byMessage));

        assertThrows(SupplyException.class, () -> expr.evaluate().supply());
    }

    // -------------------------------------------------------------------------
    // CatchHandler accessors
    // -------------------------------------------------------------------------

    @Test
    void catchHandler_accessors_reflectConstruction() {
        ValueExpression h = new ValueExpression("h");
        CatchHandler<String> handler = new CatchHandler<>(
                List.of(IClass.getClass(IOException.class)), h, Optional.of(42), "v");
        assertSame(h, handler.handler());
        assertEquals(Optional.of(42), handler.code());
        assertEquals("v", handler.variableName());
        // Type-based matching resolves against the CAUSE chain, so the IOException
        // must appear as a cause rather than as the top-level throwable.
        assertTrue(handler.matches(new java.lang.RuntimeException("w", new IOException("io"))));
        assertFalse(handler.matches(new java.lang.RuntimeException("w", new IllegalStateException("not-io"))));
    }

    @Test
    void catchHandler_matchesNestedCause() {
        // CoreException.findFirstInException walks the cause chain.
        CatchHandler<String> handler = new CatchHandler<>(
                List.of(IClass.getClass(IllegalArgumentException.class)),
                new ValueExpression("h"), Optional.empty());
        Throwable wrapped = new java.lang.RuntimeException("outer", new IllegalArgumentException("inner-cause"));
        assertTrue(handler.matches(wrapped), "should match an exception nested as a cause");
    }
}
