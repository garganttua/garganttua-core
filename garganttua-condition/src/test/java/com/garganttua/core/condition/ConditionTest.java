package com.garganttua.core.condition;

import static com.garganttua.core.condition.Conditions.*;
import static com.garganttua.core.supply.dsl.FixedSupplierBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.dsl.NullSupplierBuilder;

public class ConditionTest {

        @Test
        public void testObjectIsNull() throws ConditionException, DslException {

                assertFalse(isNull(of("null")).build().fullEvaluate());
                assertFalse(isNull("String").build().fullEvaluate());
                assertTrue(isNull(NullSupplierBuilder.of(String.class)).build().fullEvaluate());

        }

        @Test
        public void testObjectIsNotNull() throws ConditionException, DslException {

                assertTrue(isNotNull(of("null")).build().fullEvaluate());
                assertTrue(isNotNull("String").build().fullEvaluate());
                assertFalse(isNotNull(NullSupplierBuilder.of(String.class)).build().fullEvaluate());

        }

        @Test
        public void testAnd() throws ConditionException, DslException {

                assertFalse(and(isNull(of("null")), isNull(of("null")))
                                .build().fullEvaluate());
                assertFalse(
                                and(isNull(NullSupplierBuilder.of(String.class)), isNull(of("null")))
                                                .build().fullEvaluate());
                assertTrue(and(isNull(NullSupplierBuilder.of(String.class)),
                                isNull(NullSupplierBuilder.of(String.class))).build().fullEvaluate());

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

}
