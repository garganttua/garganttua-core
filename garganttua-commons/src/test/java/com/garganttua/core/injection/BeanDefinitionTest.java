package com.garganttua.core.injection;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class BeanDefinitionTest {

        @Retention(RetentionPolicy.RUNTIME)
        public @interface TestQualifier {
        }

        static class MyBean {
        }

        static class AnotherBean {
        }

        private BeanReference<MyBean> makeReference(Class<MyBean> type, BeanStrategy strategy, String name,
                        Set<Class<? extends java.lang.annotation.Annotation>> qualifiers) {
                return new BeanReference<>(type, Optional.ofNullable(strategy), Optional.ofNullable(name), qualifiers);
        }

        @Test
        void testEqualsAndHashCode() {
                BeanReference<MyBean> ref1 = makeReference(MyBean.class, BeanStrategy.singleton, "myBean",
                                Set.of(TestQualifier.class));
                BeanReference<MyBean> ref2 = makeReference(MyBean.class, BeanStrategy.singleton, "myBean",
                                Set.of(TestQualifier.class));
                BeanReference<MyBean> ref3 = makeReference(MyBean.class, BeanStrategy.prototype, "myBean",
                                Set.of(TestQualifier.class));

                BeanDefinition<MyBean> def1 = new BeanDefinition<>(ref1, Optional.empty(), Set.of(), Set.of());
                BeanDefinition<MyBean> def2 = new BeanDefinition<>(ref2, Optional.empty(), Set.of(), Set.of());
                BeanDefinition<MyBean> def3 = new BeanDefinition<>(ref3, Optional.empty(), Set.of(), Set.of());

                assertEquals(def1, def2);
                assertEquals(def1.hashCode(), def2.hashCode());
                assertNotEquals(def1, def3);
                assertNotEquals(def1.hashCode(), def3.hashCode());
        }

        @Test
        void testEffectiveName() {
                BeanReference<MyBean> ref = makeReference(MyBean.class, BeanStrategy.singleton, null, Set.of());
                BeanDefinition<MyBean> def = new BeanDefinition<>(ref, Optional.empty(), Set.of(), Set.of());

                assertEquals("MyBean", def.reference().effectiveName());
        }

        @Test
        void testMatches() {
                BeanReference<MyBean> ref1 = makeReference(MyBean.class, BeanStrategy.singleton, "bean1",
                                Set.of(TestQualifier.class));
                BeanReference<MyBean> ref2 = makeReference(MyBean.class, BeanStrategy.singleton, "bean1",
                                Set.of(TestQualifier.class));
                BeanReference<MyBean> ref3 = makeReference(MyBean.class, BeanStrategy.singleton, "bean2", Set.of());

                BeanDefinition<MyBean> def1 = new BeanDefinition<>(ref1, Optional.empty(), Set.of(), Set.of());
                BeanDefinition<MyBean> def2 = new BeanDefinition<>(ref2, Optional.empty(), Set.of(), Set.of());
                BeanDefinition<MyBean> def3 = new BeanDefinition<>(ref3, Optional.empty(), Set.of(), Set.of());

                assertTrue(def1.reference().matches(def2.reference()));
                assertFalse(def1.reference().matches(def3.reference()));
        }
}
