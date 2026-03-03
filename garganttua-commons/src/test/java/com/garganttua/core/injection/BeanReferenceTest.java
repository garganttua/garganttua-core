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

import com.garganttua.core.reflection.IClass;

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
        void test_provider_and_class_without_other_fields_throws() {
            // parse() currently defers class resolution (needs IReflectionProvider)
            // A reference with only a class string (but type=null) and no strategy/name/qualifier
            // is considered empty and throws
            assertThrows(DiException.class,
                    () -> BeanReference.parse("remote::java.lang.String"));
        }

        @Test
        void test_provider_class_strategy() throws Exception {
            var result = BeanReference.parse("local::java.lang.String!prototype");

            assertEquals(Optional.of("local"), result.value1());
            assertEquals(BeanStrategy.prototype, result.value2().strategy().get());
        }
    }

    @Nested
    @DisplayName("Class parsing")
    class ClassTests {

        @Test
        void test_fqcn_class_only_throws_without_resolver() {
            // parse() defers class resolution — a reference with only class string is empty
            assertThrows(DiException.class,
                    () -> BeanReference.parse("java.lang.String"));
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

            assertNull(result.value2().type());
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

            assertNull(result.value2().type());
            assertEquals("main", result.value2().name().get());
        }

        @Test
        void test_full_combo_provider_class_strategy_name()
                throws Exception {

            var result = BeanReference.parse("local::java.lang.String!singleton#bean1");

            assertEquals(Optional.of("local"), result.value1());
            assertNull(result.value2().type());
            assertEquals(BeanStrategy.singleton, result.value2().strategy().get());
            assertEquals("bean1", result.value2().name().get());
        }
    }

    @Nested
    @DisplayName("Qualifier parsing")
    class QualifierTests {

        @Test
        void test_qualifier_with_strategy() throws Exception {
            // Need at least strategy/name so reference isn't considered empty
            var result = BeanReference.parse("java.lang.String!singleton@" + Q1.class.getName());

            Set<IClass<? extends Annotation>> quals = result.value2().qualifiers();

            // parse() currently defers qualifier resolution (returns empty set)
            assertTrue(quals.isEmpty());
            assertEquals(Optional.of(BeanStrategy.singleton), result.value2().strategy());
        }

        @Test
        void test_full_combo_everything() throws Exception {
            var ref = "local::java.lang.String!prototype#main@"
                    + Q1.class.getSimpleName() + "@" + Q2.class.getName();

            var result = BeanReference.parse(ref);
            var def = result.value2();

            assertEquals(Optional.of("local"), result.value1());
            assertNull(def.type());
            assertEquals(BeanStrategy.prototype, def.strategy().get());
            assertEquals("main", def.name().get());

            // Qualifier resolution deferred in current implementation
            assertTrue(def.qualifiers().isEmpty());
        }
    }
}
