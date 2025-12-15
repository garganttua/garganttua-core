package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.context.beans.BeanFactory;
import com.garganttua.core.injection.dummies.DummyBean;
import com.garganttua.core.injection.dummies.DummyConstructorBinderBuilder;
import com.garganttua.core.reflection.binders.IConstructorBinder;
import com.garganttua.core.supply.SupplyException;

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
                                new BeanDefinition<DummyBean>(
                                                new BeanReference<>(DummyBean.class,
                                                                Optional.of(BeanStrategy.singleton),
                                                                Optional.empty(),
                                                                null),
                                                Optional.empty(), Set.of(),
                                                Set.of()));
                singletonWithConstructorWithNoParamFactory = new BeanFactory<DummyBean>(
                                new BeanDefinition<DummyBean>(
                                                new BeanReference<>(DummyBean.class,
                                                                Optional.of(BeanStrategy.singleton),
                                                                Optional.empty(),
                                                                null),
                                                Optional.of(this.constructorWithNoParamBinder), Set.of(),
                                                Set.of()));
                singletonWithConstructorWithParamFactory = new BeanFactory<>(
                                new BeanDefinition<DummyBean>(
                                                new BeanReference<>(DummyBean.class,
                                                                Optional.of(BeanStrategy.singleton),
                                                                Optional.empty(),
                                                                null),
                                                Optional.of(this.constructorWithParamBinder), Set.of(),
                                                Set.of()));
                newInstanceWithoutConstructorFactory = new BeanFactory<>(new BeanDefinition<DummyBean>(
                                new BeanReference<>(DummyBean.class,
                                                Optional.of(BeanStrategy.prototype), Optional.empty(), null),
                                Optional.empty(), Set.of(),
                                Set.of()));
        }

        @Test
        void testDenpendencies() {
                assertEquals(1, singletonWithConstructorWithParamFactory.dependencies().size());
        }

        @Test
        void testBeanCreationWithoutConstructorBinder() throws DiException, SupplyException {
                Optional<DummyBean> beanOpt = singletonWithoutConstructorFactory.supply();
                assertTrue(beanOpt.isPresent());
                DummyBean bean = beanOpt.get();

                assertEquals("default", bean.getValue(),
                                "Bean should be created by constructor binder");
                assertTrue(!bean.isPostConstructCalled(),
                                "Post-construct binder should be executed");
        }

        @Test
        void testBeanCreationWithConstructorWithNoParamBinder() throws DiException, SupplyException {
                Optional<DummyBean> beanOpt = singletonWithConstructorWithNoParamFactory.supply();
                assertTrue(beanOpt.isPresent());
                DummyBean bean = beanOpt.get();

                assertEquals("default", bean.getValue(),
                                "Bean should be created by constructor binder");
                assertTrue(!bean.isPostConstructCalled(),
                                "Post-construct binder should be executed");
        }

        @Test
        void testBeanCreationWithConstructorWithParamBinder() throws DiException, SupplyException {
                Optional<DummyBean> beanOpt = singletonWithConstructorWithParamFactory.supply();
                assertTrue(beanOpt.isPresent());
                DummyBean bean = beanOpt.get();

                assertEquals("constructedWithParameter", bean.getValue(),
                                "Bean should be created by constructor binder");
                assertTrue(!bean.isPostConstructCalled(),
                                "Post-construct binder should be executed");
        }

        @Test
        void testSingletonStrategyReturnsSameInstances() throws DiException, SupplyException {
                Optional<DummyBean> bean1 = singletonWithoutConstructorFactory.supply();
                Optional<DummyBean> bean2 = singletonWithoutConstructorFactory.supply();

                assertSame(bean1.get(), bean2.get(),
                                "Singleton strategy should return the same bean instance");
        }

        @Test
        void testNewInstanceStrategyReturnsDifferentInstances() throws DiException, SupplyException {
                Optional<DummyBean> bean1 = newInstanceWithoutConstructorFactory.supply();
                Optional<DummyBean> bean2 = newInstanceWithoutConstructorFactory.supply();

                assertNotSame(bean1.get(), bean2.get(),
                                "Singleton strategy should return the same bean instance");
        }

}
