package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.DiException;
import com.garganttua.injection.beans.BeanDefinition;
import com.garganttua.injection.beans.BeanFactory;
import com.garganttua.injection.beans.BeanStrategy;
import com.garganttua.injection.spec.supplier.binder.IConstructorBinder;

public class BeanFactoryTest {

        private BeanFactory<DummyBean> singletonWithoutConstructorFactory;
        private BeanFactory<DummyBean> newInstanceWithoutConstructorFactory;
        private IConstructorBinder<DummyBean> constructorWithNoParamBinder;
        private IConstructorBinder<DummyBean> constructorWithParamBinder;
        private BeanFactory<DummyBean> singletonWithConstructorWithNoParamFactory;
        private BeanFactory<DummyBean> singletonWithConstructorWithParamFactory;

        @BeforeEach
        void setup() throws DslException {

                this.constructorWithNoParamBinder = new DummyConstructorBinderBuilder<DummyBean>(DummyBean.class)
                                .build();
                this.constructorWithParamBinder = new DummyConstructorBinderBuilder<DummyBean>(DummyBean.class)
                                .withParam("constructedWithParameter")
                                .build();

                singletonWithoutConstructorFactory = new BeanFactory<DummyBean>(
                                new BeanDefinition<DummyBean>(DummyBean.class, Optional.of(BeanStrategy.singleton),
                                                Optional.empty(),
                                                null, Optional.empty(), Set.of(),
                                                Set.of()));
                singletonWithConstructorWithNoParamFactory = new BeanFactory<DummyBean>(
                                new BeanDefinition<DummyBean>(DummyBean.class, Optional.of(BeanStrategy.singleton),
                                                Optional.empty(),
                                                null, Optional.of(this.constructorWithNoParamBinder), Set.of(),
                                                Set.of()));
                singletonWithConstructorWithParamFactory = new BeanFactory<>(
                                new BeanDefinition<DummyBean>(DummyBean.class, Optional.of(BeanStrategy.singleton),
                                                Optional.empty(),
                                                null, Optional.of(this.constructorWithParamBinder), Set.of(),
                                                Set.of()));
                newInstanceWithoutConstructorFactory = new BeanFactory<>(new BeanDefinition<DummyBean>(DummyBean.class,
                                Optional.of(BeanStrategy.prototype), Optional.empty(), null, Optional.empty(), Set.of(),
                                Set.of()));
        }

        @Test
        void testDenpendencies(){
                assertEquals(1, singletonWithConstructorWithParamFactory.getDependencies().size());
        }

        @Test
        void testBeanCreationWithoutConstructorBinder() throws DiException {
                Optional<DummyBean> beanOpt = singletonWithoutConstructorFactory.getObject();
                assertTrue(beanOpt.isPresent());
                DummyBean bean = beanOpt.get();

                assertEquals("default", bean.getValue(),
                                "Bean should be created by constructor binder");
                assertTrue(!bean.isPostConstructCalled(),
                                "Post-construct binder should be executed");
        }

        @Test
        void testBeanCreationWithConstructorWithNoParamBinder() throws DiException {
                Optional<DummyBean> beanOpt = singletonWithConstructorWithNoParamFactory.getObject();
                assertTrue(beanOpt.isPresent());
                DummyBean bean = beanOpt.get();

                assertEquals("default", bean.getValue(),
                                "Bean should be created by constructor binder");
                assertTrue(!bean.isPostConstructCalled(),
                                "Post-construct binder should be executed");
        }

        @Test
        void testBeanCreationWithConstructorWithParamBinder() throws DiException {
                Optional<DummyBean> beanOpt = singletonWithConstructorWithParamFactory.getObject();
                assertTrue(beanOpt.isPresent());
                DummyBean bean = beanOpt.get();

                assertEquals("constructedWithParameter", bean.getValue(),
                                "Bean should be created by constructor binder");
                assertTrue(!bean.isPostConstructCalled(),
                                "Post-construct binder should be executed");
        }

        @Test
        void testSingletonStrategyReturnsSameInstances() throws DiException {
                Optional<DummyBean> bean1 = singletonWithoutConstructorFactory.getObject();
                Optional<DummyBean> bean2 = singletonWithoutConstructorFactory.getObject();

                assertSame(bean1.get(), bean2.get(),
                                "Singleton strategy should return the same bean instance");
        }

        @Test
        void testNewInstanceStrategyReturnsDifferentInstances() throws DiException {
                Optional<DummyBean> bean1 = newInstanceWithoutConstructorFactory.getObject();
                Optional<DummyBean> bean2 = newInstanceWithoutConstructorFactory.getObject();

                assertNotSame(bean1.get(), bean2.get(),
                                "Singleton strategy should return the same bean instance");
        }

}
