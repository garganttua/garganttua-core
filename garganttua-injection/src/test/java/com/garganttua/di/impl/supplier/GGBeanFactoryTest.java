package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.DiException;
import com.garganttua.injection.beans.GGBeanFactory;
import com.garganttua.injection.spec.IDiContext;
import com.garganttua.injection.spec.beans.annotation.GGBeanLoadingStrategy;
import com.garganttua.injection.spec.supplier.binder.IConstructorBinder;

public class GGBeanFactoryTest {

    private GGBeanFactory<DummyBean> singletonWithoutConstructorFactory;
    private GGBeanFactory<DummyBean> newInstanceWithoutConstructorFactory;
    private IConstructorBinder<DummyBean> constructorWithNoParamBinder;
    private IConstructorBinder<DummyBean> constructorWithParamBinder;
    private GGBeanFactory<DummyBean> singletonWithConstructorWithNoParamFactory;
    private GGBeanFactory<DummyBean> singletonWithConstructorWithParamFactory;

    @BeforeEach
    void setup() throws DslException {
        IDiContext context = new DummyDiContext();

        this.constructorWithNoParamBinder = new DummyConstructorBinderBuilder<DummyBean>(DummyBean.class)
                .build();
        this.constructorWithParamBinder = new DummyConstructorBinderBuilder<DummyBean>(DummyBean.class)
                .withParam("constructedWithParameter")
                .build();

        singletonWithoutConstructorFactory = new GGBeanFactory<>(DummyBean.class, GGBeanLoadingStrategy.singleton,
                Optional.empty(),
                Optional.empty(), null, context, context);
        singletonWithConstructorWithNoParamFactory = new GGBeanFactory<>(DummyBean.class,
                GGBeanLoadingStrategy.singleton,
                Optional.empty(),
                Optional.of(constructorWithNoParamBinder), null, context, context);
        singletonWithConstructorWithParamFactory = new GGBeanFactory<>(DummyBean.class, GGBeanLoadingStrategy.singleton,
                Optional.empty(),
                Optional.of(constructorWithParamBinder), null, context, context);
        newInstanceWithoutConstructorFactory = new GGBeanFactory<>(DummyBean.class, GGBeanLoadingStrategy.newInstance,
                Optional.empty(),
                Optional.empty(), null, context, context);
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
