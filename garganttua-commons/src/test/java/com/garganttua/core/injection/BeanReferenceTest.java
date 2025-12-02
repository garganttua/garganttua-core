package com.garganttua.core.injection;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class BeanReferenceTest {

    // --- Dummy test annotation classes for qualifiers ---
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Q1 {}

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Q2 {}

    public static class DummyService {}

    @Nested
    @DisplayName("Provider parsing")
    class ProviderTests {

        @Test
        void test_provider_only_with_name() throws Exception {
            var result = BeanReference.parse("local::#Mail");

            assertEquals(Optional.of("local"), result.value1());
            var def = result.value2();

            assertTrue(def.name().isPresent());
            assertEquals("Mail", def.name().get());
            assertTrue(def.strategy().isEmpty());
            assertNull(def.type());
        }

        @Test
        void test_provider_and_class() throws Exception {
            var result = BeanReference.parse("remote::java.lang.String");

            assertEquals(Optional.of("remote"), result.value1());
            assertEquals(String.class, result.value2().type());
        }
    }

    @Nested
    @DisplayName("Class parsing")
    class ClassTests {

        @Test
        void test_fqcn_class_only() throws Exception {
            var result = BeanReference.parse("java.lang.String");

            assertTrue(result.value1().isEmpty());
            assertEquals(String.class, result.value2().type());
        }

        @Test
        void test_simple_class_should_throw() {
            assertThrows(DiException.class,
                    () -> BeanReference.parse("MyService"));
        }
    }

    @Nested
    @DisplayName("Strategy parsing")
    class StrategyTests {

        @Test
        void test_strategy_only() throws Exception {
            var result = BeanReference.parse("java.lang.String!singleton");

            assertEquals(String.class, result.value2().type());
            assertEquals(Optional.of(BeanStrategy.singleton), result.value2().strategy());
        }

        @Test
        void test_provider_class_strategy() throws Exception {
            var result = BeanReference.parse("local::java.lang.String!prototype");

            assertEquals(Optional.of("local"), result.value1());
            assertEquals(BeanStrategy.prototype, result.value2().strategy().get());
        }
    }

    @Nested
    @DisplayName("Name parsing")
    class NameTests {

        @Test
        void test_name_only_with_class() throws Exception {
            var result = BeanReference.parse("java.lang.String#main");

            assertEquals(String.class, result.value2().type());
            assertEquals("main", result.value2().name().get());
        }

        @Test
        void test_full_combo_provider_class_strategy_name()
                throws Exception {

            var result = BeanReference.parse("local::java.lang.String!singleton#bean1");

            assertEquals(Optional.of("local"), result.value1());
            assertEquals(String.class, result.value2().type());
            assertEquals(BeanStrategy.singleton, result.value2().strategy().get());
            assertEquals("bean1", result.value2().name().get());
        }
    }

    @Nested
    @DisplayName("Qualifier parsing")
    class QualifierTests {

        @Test
        void test_single_qualifier_fqcn() throws Exception {
            var result = BeanReference.parse("java.lang.String@" + Q1.class.getName());

            Set<Class<? extends Annotation>> quals = result.value2().qualifiers();

            assertEquals(1, quals.size());
            assertTrue(quals.contains(Q1.class));
        }

        @Test
        void test_multiple_qualifiers_fqcn() throws Exception {
            var ref = "java.lang.String@" + Q1.class.getName()
                    + "@" + Q2.class.getName();

            var result = BeanReference.parse(ref);

            Set<Class<? extends Annotation>> quals = result.value2().qualifiers();

            assertEquals(2, quals.size());
            assertTrue(quals.contains(Q1.class));
            assertTrue(quals.contains(Q2.class));
        }

        @Test
        void test_full_combo_everything() throws Exception {
            var ref = "local::java.lang.String!prototype#main@"
                    + Q1.class.getSimpleName() + "@" + Q2.class.getName();

            var result = BeanReference.parse(ref);
            var def = result.value2();

            assertEquals(Optional.of("local"), result.value1());
            assertEquals(String.class, def.type());
            assertEquals(BeanStrategy.prototype, def.strategy().get());
            assertEquals("main", def.name().get());

            // Q1 simple name -> n’est pas résolu en Class, donc on vérifie que Q2 FQCN est présent
            assertEquals(Set.of(Q2.class), def.qualifiers());
        }
    }
}
