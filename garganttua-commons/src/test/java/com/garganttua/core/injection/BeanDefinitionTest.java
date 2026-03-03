package com.garganttua.core.injection;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.JdkClass;

public class BeanDefinitionTest {

        @Retention(RetentionPolicy.RUNTIME)
        public @interface TestQualifier {
        }

        static class MyBean {
        }

        static class AnotherBean {
        }

        private BeanReference<MyBean> makeReference(IClass<MyBean> type, BeanStrategy strategy, String name,
                        Set<IClass<? extends Annotation>> qualifiers) {
                return new BeanReference<>(type, Optional.ofNullable(strategy), Optional.ofNullable(name), qualifiers);
        }

        @Test
        void testEqualsAndHashCode() {
                IClass<MyBean> myBeanClass = JdkClass.of(MyBean.class);
                IClass<? extends Annotation> testQualifier = JdkClass.of(TestQualifier.class);

                BeanReference<MyBean> ref1 = makeReference(myBeanClass, BeanStrategy.singleton, "myBean",
                                Set.of(testQualifier));
                BeanReference<MyBean> ref2 = makeReference(myBeanClass, BeanStrategy.singleton, "myBean",
                                Set.of(testQualifier));
                BeanReference<MyBean> ref3 = makeReference(myBeanClass, BeanStrategy.prototype, "myBean",
                                Set.of(testQualifier));

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
                IClass<MyBean> myBeanClass = JdkClass.of(MyBean.class);
                BeanReference<MyBean> ref = makeReference(myBeanClass, BeanStrategy.singleton, null, Set.of());
                BeanDefinition<MyBean> def = new BeanDefinition<>(ref, Optional.empty(), Set.of(), Set.of());

                assertEquals("MyBean", def.reference().effectiveName());
        }

        @Test
        void testMatches() {
                IClass<MyBean> myBeanClass = JdkClass.of(MyBean.class);
                IClass<? extends Annotation> testQualifier = JdkClass.of(TestQualifier.class);

                BeanReference<MyBean> ref1 = makeReference(myBeanClass, BeanStrategy.singleton, "bean1",
                                Set.of(testQualifier));
                BeanReference<MyBean> ref2 = makeReference(myBeanClass, BeanStrategy.singleton, "bean1",
                                Set.of(testQualifier));
                BeanReference<MyBean> ref3 = makeReference(myBeanClass, BeanStrategy.singleton, "bean2", Set.of());

                BeanDefinition<MyBean> def1 = new BeanDefinition<>(ref1, Optional.empty(), Set.of(), Set.of());
                BeanDefinition<MyBean> def2 = new BeanDefinition<>(ref2, Optional.empty(), Set.of(), Set.of());
                BeanDefinition<MyBean> def3 = new BeanDefinition<>(ref3, Optional.empty(), Set.of(), Set.of());

                assertTrue(def1.reference().matches(def2.reference()));
                assertFalse(def1.reference().matches(def3.reference()));
        }
}
