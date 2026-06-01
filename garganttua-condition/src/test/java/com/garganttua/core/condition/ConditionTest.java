package com.garganttua.core.condition;

import static com.garganttua.core.condition.Conditions.*;
import static com.garganttua.core.supply.dsl.FixedSupplierBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Function;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.supply.dsl.NullSupplierBuilder;

public class ConditionTest {

        private static IReflection reflection;

        @BeforeAll
        static void setUp() throws Exception {
                reflection = ReflectionBuilder.builder()
                                .withProvider(new RuntimeReflectionProvider())
                                .build();
                IClass.setReflection(reflection);
        }

        @AfterAll
        static void tearDown() {
                IClass.setReflection(null);
        }

        @Test
        public void testObjectIsNull() throws ConditionException, DslException {

                assertFalse(isNull(of("null")).build().fullEvaluate());
                assertFalse(isNull("String").build().fullEvaluate());
                assertTrue(isNull(NullSupplierBuilder.of(IClass.getClass(String.class))).build().fullEvaluate());

        }

        @Test
        public void testObjectIsNotNull() throws ConditionException, DslException {

                assertTrue(isNotNull(of("null")).build().fullEvaluate());
                assertTrue(isNotNull("String").build().fullEvaluate());
                assertFalse(isNotNull(NullSupplierBuilder.of(IClass.getClass(String.class))).build().fullEvaluate());

        }

        @Test
        public void testAnd() throws ConditionException, DslException {

                assertFalse(and(isNull(of("null")), isNull(of("null")))
                                .build().fullEvaluate());
                assertFalse(
                                and(isNull(NullSupplierBuilder.of(IClass.getClass(String.class))), isNull(of("null")))
                                                .build().fullEvaluate());
                assertTrue(and(isNull(NullSupplierBuilder.of(IClass.getClass(String.class))),
                                isNull(NullSupplierBuilder.of(IClass.getClass(String.class)))).build().fullEvaluate());

        }

        @Test
        public void testCustom() {

                assertTrue(custom(of("hello"), String::length, len -> len > 3).build().fullEvaluate());
                assertTrue(custom(of(125), val -> val > 3).build().fullEvaluate());

                assertTrue(custom(of("abc"), String::isEmpty, empty -> !empty).build().fullEvaluate());
                assertFalse(custom(of(""), String::isEmpty, empty -> !empty).build().fullEvaluate());

                assertTrue(custom(of(0), val -> val == 0).build().fullEvaluate());
                assertFalse(custom(of(10), val -> val < 5).build().fullEvaluate());

                assertTrue(custom(of(true), val -> val).build().fullEvaluate());
                assertFalse(custom(of(false), val -> val).build().fullEvaluate());

                assertTrue(custom(of("abc123"), String::length, len -> len == 6).build().fullEvaluate());
                assertTrue(custom(of(3.14), val -> val > 3).build().fullEvaluate());

                assertTrue(custom(of("identity"), Function.identity(), s -> s.startsWith("i")).build().fullEvaluate());

                assertTrue(custom(of("hello"), str -> str.chars().sum(), sum -> sum > 500).build().fullEvaluate());

                assertTrue(and(custom(of(10), val -> val > 5), custom(of(20), val -> val < 30)).build().fullEvaluate());

        }

        @Test
        public void testOrOperator() {

                assertTrue(
                                or(
                                                custom(of(5), v -> v > 3),
                                                custom(of(2), v -> v > 10)).build().fullEvaluate());

                assertFalse(
                                or(
                                                custom(of(1), v -> v > 3),
                                                custom(of(2), v -> v > 10)).build().fullEvaluate());

                assertTrue(
                                or(
                                                custom(of("test"), String::isEmpty, e -> !e),
                                                custom(of(99), v -> v < 100)).build().fullEvaluate());

        }

        @Test
        public void testNorOperator() {

                assertFalse(
                                nor(
                                                custom(of(5), v -> v > 3),
                                                custom(of(8), v -> v > 3)).build().fullEvaluate());

                assertFalse(
                                nor(
                                                custom(of(5), v -> v > 3),
                                                custom(of(2), v -> v > 3)).build().fullEvaluate());
                assertTrue(
                                nor(
                                                custom(of(1), v -> v > 3),
                                                custom(of(2), v -> v > 3)).build().fullEvaluate());

        }

        @Test
        public void testNandOperator() {

                assertFalse(
                                nand(
                                                custom(of(10), v -> v > 5),
                                                custom(of(20), v -> v < 30)).build().fullEvaluate());

                assertTrue(
                                nand(
                                                custom(of(10), v -> v > 5),
                                                custom(of(50), v -> v < 30)).build().fullEvaluate());

                assertTrue(
                                nand(
                                                custom(of(1), v -> v > 5),
                                                custom(of(2), v -> v > 10)).build().fullEvaluate());

        }

        @Test
        public void testXorOperator() {

                assertFalse(
                                xor(
                                                custom(of(10), v -> v > 5),
                                                custom(of(20), v -> v < 30)).build().fullEvaluate());

                assertTrue(
                                xor(
                                                custom(of(10), v -> v > 5),
                                                custom(of(50), v -> v < 30)).build().fullEvaluate());

                assertTrue(
                                xor(
                                                custom(of(1), v -> v > 5),
                                                custom(of(20), v -> v < 30)).build().fullEvaluate());

                assertFalse(
                                xor(
                                                custom(of(1), v -> v > 5),
                                                custom(of(2), v -> v > 10)).build().fullEvaluate());

                assertFalse(
                                xor(
                                                custom(of(10), v -> v > 5),
                                                custom(of(5), v -> v < 0),
                                                custom(of(20), v -> v < 30)).build().fullEvaluate());

                assertTrue(
                                xor(
                                                custom(of(10), v -> v > 5),
                                                custom(of(5), v -> v > 0),
                                                custom(of(20), v -> v < 30)).build().fullEvaluate());

                assertFalse(
                                xor(
                                                custom(of(10), v -> v > 5),
                                                custom(of(20), v -> v < 30),
                                                custom(of(0), v -> v < 0),
                                                custom(of(1), v -> v > 10)).build().fullEvaluate());
        }

        @Test
        public void testEqualsOperator() {

                assertTrue(Conditions.equals(of(10), of(10)).build().fullEvaluate());
                assertFalse(Conditions.equals(of(10), of(20)).build().fullEvaluate());

                assertTrue(Conditions.equals(of("abc"), of("abc")).build().fullEvaluate());
                assertFalse(Conditions.equals(of("abc"), of("ABC")).build().fullEvaluate());

                assertTrue(Conditions.equals(of(10.0), of(10.0)).build().fullEvaluate());
                assertEquals("Type mismatch Integer VS Double",
                                assertThrows(DslException.class,
                                                () -> Conditions.equals(of(10), of(10.0)).build().fullEvaluate())
                                                .getMessage());

                Object o = new Object();
                assertTrue(Conditions.equals(of(o), of(o)).build().fullEvaluate());
                assertFalse(Conditions.equals(of(o), of(new Object())).build().fullEvaluate());

        }

        @Test
        public void testIsNotEqualsOperator() {

                assertFalse(notEquals(of(10), of(10)).build().fullEvaluate());
                assertTrue(notEquals(of(10), of(20)).build().fullEvaluate());

                assertFalse(notEquals(of("abc"), of("abc")).build().fullEvaluate());
                assertTrue(notEquals(of("abc"), of("ABC")).build().fullEvaluate());

                assertFalse(notEquals(of(10.0), of(10.0)).build().fullEvaluate());
                assertEquals("Type mismatch Integer VS Double",
                                assertThrows(DslException.class, () -> notEquals(of(10), of(10.0)).build().fullEvaluate())
                                                .getMessage());

                Object o = new Object();
                assertFalse(notEquals(of(o), of(o)).build().fullEvaluate());
                assertTrue(notEquals(of(o), of(new Object())).build().fullEvaluate());

        }

        /**
         * Regression: the {@code notNull} / {@code null} built-in @Expression
         * functions must be Optional-aware. A supplier such as a request-arg
         * lookup returns {@code Optional.ofNullable(...)}; an absent key yields
         * {@code Optional.empty()} — a non-null reference. Before the fix,
         * {@code notNull(Optional.empty())} returned true, so a stage guarded by
         * {@code when("notNull(:arg(@0,\"k\"))")} ran even when the key was absent.
         */
        @Test
        public void testNotNullAndNullAreOptionalAwareAndStrictInverses() {
                // null reference
                assertFalse(NotNullCondition.notNull(null));
                assertTrue(NullCondition.Null(null));
                // empty Optional reads as null
                assertFalse(NotNullCondition.notNull(java.util.Optional.empty()));
                assertTrue(NullCondition.Null(java.util.Optional.empty()));
                // present Optional reads as a value
                assertTrue(NotNullCondition.notNull(java.util.Optional.of("v")));
                assertFalse(NullCondition.Null(java.util.Optional.of("v")));
                // plain non-null object reads as a value
                assertTrue(NotNullCondition.notNull("string"));
                assertFalse(NullCondition.Null("string"));

                // strict-inverse invariant across all four cases
                for (Object x : new Object[] { null, java.util.Optional.empty(),
                                java.util.Optional.of("v"), "string" }) {
                        assertEquals(NotNullCondition.notNull(x), !NullCondition.Null(x));
                }
        }

        /**
         * Follow-up regression: every value-inspecting primitive condition must
         * unwrap Optional operands (a present Optional becomes its value, an empty
         * one becomes null) — otherwise a guard like {@code equals(:arg(@0,"mode"),
         * "uuid")} compares {@code Optional.of("uuid")} against {@code "uuid"} and
         * logs "Type mismatch: Optional VS String" then returns false.
         */
        @Test
        public void testEqualsAndComparisonsAreOptionalAware() {
                // equals — acceptance criterion of the follow-up ticket
                assertTrue(EqualsCondition.equals(java.util.Optional.of("uuid"), "uuid"));
                assertTrue(EqualsCondition.equals("uuid", java.util.Optional.of("uuid")));
                assertTrue(EqualsCondition.equals(java.util.Optional.of("uuid"), java.util.Optional.of("uuid")));
                assertFalse(EqualsCondition.equals(java.util.Optional.empty(), "uuid")); // empty -> null
                assertFalse(EqualsCondition.equals(java.util.Optional.of("id"), "uuid"));

                // notEquals — symmetric (null operand stays false per existing semantics)
                assertTrue(NotEqualsCondition.notEquals(java.util.Optional.of("id"), "uuid"));
                assertFalse(NotEqualsCondition.notEquals(java.util.Optional.of("uuid"), "uuid"));

                // ordering comparisons — Object/Object and the int-mixed overloads
                assertTrue(GreaterCondition.greater(java.util.Optional.of(5), 3));
                assertFalse(GreaterCondition.greater(java.util.Optional.empty(), 3)); // empty -> null -> false
                assertTrue(LowerCondition.lower(3, java.util.Optional.of(5)));
                assertTrue(GreaterOrEqualsCondition.greaterOrEquals(java.util.Optional.of(5), java.util.Optional.of(5)));
                assertTrue(LowerOrEqualsCondition.lowerOrEquals(java.util.Optional.of(2), java.util.Optional.of(5)));
        }

}
