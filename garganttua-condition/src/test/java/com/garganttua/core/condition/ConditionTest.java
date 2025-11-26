package com.garganttua.core.condition;

import static com.garganttua.core.condition.Conditions.*;
import static com.garganttua.core.supply.dsl.FixedObjectSupplierBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.dsl.NullObjectSupplierBuilder;

public class ConditionTest {

        @Test
        public void testObjectIsNull() throws ConditionException, DslException {

                assertFalse(isNull(of("null")).build().evaluate());
                assertFalse(isNull("String").build().evaluate());
                assertTrue(isNull(NullObjectSupplierBuilder.of(String.class)).build().evaluate());

        }

        @Test
        public void testObjectIsNotNull() throws ConditionException, DslException {

                assertTrue(isNotNull(of("null")).build().evaluate());
                assertTrue(isNotNull("String").build().evaluate());
                assertFalse(isNotNull(NullObjectSupplierBuilder.of(String.class)).build().evaluate());

        }

        @Test
        public void testAnd() throws ConditionException, DslException {

                assertFalse(and(isNull(of("null")), isNull(of("null")))
                                .build().evaluate());
                assertFalse(
                                and(isNull(NullObjectSupplierBuilder.of(String.class)), isNull(of("null")))
                                                .build().evaluate());
                assertTrue(and(isNull(NullObjectSupplierBuilder.of(String.class)),
                                isNull(NullObjectSupplierBuilder.of(String.class))).build().evaluate());

        }

        @Test
        public void testCustom() {

                assertTrue(custom(of("hello"), String::length, len -> len > 3).build().evaluate());
                assertTrue(custom(of(125), val -> val > 3).build().evaluate());

                assertTrue(custom(of("abc"), String::isEmpty, empty -> !empty).build().evaluate());
                assertFalse(custom(of(""), String::isEmpty, empty -> !empty).build().evaluate());

                assertTrue(custom(of(0), val -> val == 0).build().evaluate());
                assertFalse(custom(of(10), val -> val < 5).build().evaluate());

                assertTrue(custom(of(true), val -> val).build().evaluate());
                assertFalse(custom(of(false), val -> val).build().evaluate());

                assertTrue(custom(of("abc123"), String::length, len -> len == 6).build().evaluate());
                assertTrue(custom(of(3.14), val -> val > 3).build().evaluate());

                assertTrue(custom(of("identity"), Function.identity(), s -> s.startsWith("i")).build().evaluate());

                assertTrue(custom(of("hello"), str -> str.chars().sum(), sum -> sum > 500).build().evaluate());

                assertTrue(and(custom(of(10), val -> val > 5), custom(of(20), val -> val < 30)).build().evaluate());

        }

        @Test
        public void testOrOperator() {

                assertTrue(
                                or(
                                                custom(of(5), v -> v > 3),
                                                custom(of(2), v -> v > 10)).build().evaluate());

                assertFalse(
                                or(
                                                custom(of(1), v -> v > 3),
                                                custom(of(2), v -> v > 10)).build().evaluate());

                assertTrue(
                                or(
                                                custom(of("test"), String::isEmpty, e -> !e),
                                                custom(of(99), v -> v < 100)).build().evaluate());

        }

        @Test
        public void testNorOperator() {

                assertFalse(
                                nor(
                                                custom(of(5), v -> v > 3),
                                                custom(of(8), v -> v > 3)).build().evaluate());

                assertFalse(
                                nor(
                                                custom(of(5), v -> v > 3),
                                                custom(of(2), v -> v > 3)).build().evaluate());
                assertTrue(
                                nor(
                                                custom(of(1), v -> v > 3),
                                                custom(of(2), v -> v > 3)).build().evaluate());

        }

        @Test
        public void testNandOperator() {

                assertFalse(
                                nand(
                                                custom(of(10), v -> v > 5),
                                                custom(of(20), v -> v < 30)).build().evaluate());

                assertTrue(
                                nand(
                                                custom(of(10), v -> v > 5),
                                                custom(of(50), v -> v < 30)).build().evaluate());

                assertTrue(
                                nand(
                                                custom(of(1), v -> v > 5),
                                                custom(of(2), v -> v > 10)).build().evaluate());

        }

        @Test
        public void testXorOperator() {

                assertFalse(
                                xor(
                                                custom(of(10), v -> v > 5),
                                                custom(of(20), v -> v < 30)).build().evaluate());

                assertTrue(
                                xor(
                                                custom(of(10), v -> v > 5),
                                                custom(of(50), v -> v < 30)).build().evaluate());

                assertTrue(
                                xor(
                                                custom(of(1), v -> v > 5),
                                                custom(of(20), v -> v < 30)).build().evaluate());

                assertFalse(
                                xor(
                                                custom(of(1), v -> v > 5),
                                                custom(of(2), v -> v > 10)).build().evaluate());

                assertFalse(
                                xor(
                                                custom(of(10), v -> v > 5),
                                                custom(of(5), v -> v < 0),
                                                custom(of(20), v -> v < 30)).build().evaluate());

                assertTrue(
                                xor(
                                                custom(of(10), v -> v > 5),
                                                custom(of(5), v -> v > 0),
                                                custom(of(20), v -> v < 30)).build().evaluate());

                assertFalse(
                                xor(
                                                custom(of(10), v -> v > 5),
                                                custom(of(20), v -> v < 30),
                                                custom(of(0), v -> v < 0),
                                                custom(of(1), v -> v > 10)).build().evaluate());
        }

        @Test
        public void testEqualsOperator() {

                assertTrue(Conditions.equals(of(10), of(10)).build().evaluate());
                assertFalse(Conditions.equals(of(10), of(20)).build().evaluate());

                assertTrue(Conditions.equals(of("abc"), of("abc")).build().evaluate());
                assertFalse(Conditions.equals(of("abc"), of("ABC")).build().evaluate());

                assertTrue(Conditions.equals(of(10.0), of(10.0)).build().evaluate());
                assertEquals("Type mismatch Integer VS Double",
                                assertThrows(DslException.class,
                                                () -> Conditions.equals(of(10), of(10.0)).build().evaluate())
                                                .getMessage());

                Object o = new Object();
                assertTrue(Conditions.equals(of(o), of(o)).build().evaluate());
                assertFalse(Conditions.equals(of(o), of(new Object())).build().evaluate());

        }

        @Test
        public void testIsNotEqualsOperator() {

                assertFalse(notEquals(of(10), of(10)).build().evaluate());
                assertTrue(notEquals(of(10), of(20)).build().evaluate());

                assertFalse(notEquals(of("abc"), of("abc")).build().evaluate());
                assertTrue(notEquals(of("abc"), of("ABC")).build().evaluate());

                assertFalse(notEquals(of(10.0), of(10.0)).build().evaluate());
                assertEquals("Type mismatch Integer VS Double",
                                assertThrows(DslException.class, () -> notEquals(of(10), of(10.0)).build().evaluate())
                                                .getMessage());

                Object o = new Object();
                assertFalse(notEquals(of(o), of(o)).build().evaluate());
                assertTrue(notEquals(of(o), of(new Object())).build().evaluate());

        }

}
