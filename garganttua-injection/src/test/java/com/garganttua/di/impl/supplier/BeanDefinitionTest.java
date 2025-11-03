package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.BeanStrategy;

public class BeanDefinitionTest {

    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestQualifier {}

    static class MyBean {}

    static class AnotherBean {}

    @Test
    void testEqualsAndHashCode() {
        BeanDefinition<MyBean> def1 = new BeanDefinition<>(
                MyBean.class,
                Optional.of(BeanStrategy.singleton),
                Optional.of("myBean"),
                Set.of(TestQualifier.class),
                Optional.empty(),
                Set.of(),
                Set.of()
        );

        BeanDefinition<MyBean> def2 = new BeanDefinition<>(
                MyBean.class,
                Optional.of(BeanStrategy.singleton),
                Optional.of("myBean"),
                Set.of(TestQualifier.class),
                Optional.empty(),
                Set.of(),
                Set.of()
        );

        BeanDefinition<MyBean> def3 = new BeanDefinition<>(
                MyBean.class,
                Optional.of(BeanStrategy.prototype),
                Optional.of("myBean"),
                Set.of(TestQualifier.class),
                Optional.empty(),
                Set.of(),
                Set.of()
        );

        assertEquals(def1, def2);
        assertEquals(def1.hashCode(), def2.hashCode());
        assertNotEquals(def1, def3);
        assertNotEquals(def1.hashCode(), def3.hashCode());
    }

    @Test
    void testEffectiveName() {
        BeanDefinition<MyBean> def1 = new BeanDefinition<>(
                MyBean.class,
                Optional.of(BeanStrategy.singleton),
                Optional.empty(),
                Set.of(),
                Optional.empty(),
                Set.of(),
                Set.of()
        );

        assertEquals("MyBean", def1.effectiveName());
    }

    @Test
    void testMatches() {
        BeanDefinition<MyBean> def1 = new BeanDefinition<>(
                MyBean.class,
                Optional.of(BeanStrategy.singleton),
                Optional.of("bean1"),
                Set.of(TestQualifier.class),
                Optional.empty(),
                Set.of(),
                Set.of()
        );

        BeanDefinition<MyBean> def2 = new BeanDefinition<>(
                MyBean.class,
                Optional.of(BeanStrategy.singleton),
                Optional.of("bean1"),
                Set.of(TestQualifier.class),
                Optional.empty(),
                Set.of(),
                Set.of()
        );

        BeanDefinition<MyBean> def3 = new BeanDefinition<>(
                MyBean.class,
                Optional.of(BeanStrategy.singleton),
                Optional.of("bean2"),
                Set.of(),
                Optional.empty(),
                Set.of(),
                Set.of()
        );

        assertTrue(def1.matches(def2));
        assertFalse(def1.matches(def3));
    }
}
