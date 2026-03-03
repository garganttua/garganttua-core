package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanQueryBuilder;
import com.garganttua.core.injection.Predefined;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.beans.BeanQuery;
import com.garganttua.core.injection.dummies.DummyBean;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

public class BeanQueryTest {

    @BeforeEach
    void setUp() throws DiException, DslException, LifecycleException {
        IReflectionBuilder rb = ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider()).withScanner(new ReflectionsAnnotationScanner());
        rb.build();
        InjectionContext.builder().provide(rb).withPackage("com.garganttua").autoDetect(true)
                .propertyProvider(Predefined.PropertyProviders.garganttua.toString())
                .withProperty(IClass.getClass(String.class), "com.garganttua.dummyPropertyInConstructor", "propertyValue")
                .up()
                .build().onInit().onStart();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBeanQueryWithType() throws DiException, DslException {

        IBeanQueryBuilder<DummyBean> builder = (IBeanQueryBuilder<DummyBean>) BeanQuery.builder();
        assertTrue(builder.type(IClass.getClass(DummyBean.class)).build().execute().isPresent());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBeanQueryWithName() throws DiException, DslException {

        IBeanQueryBuilder<DummyBean> builder1 = (IBeanQueryBuilder<DummyBean>) BeanQuery.builder();
        assertFalse(builder1.name("toto").build().execute().isPresent());

        IBeanQueryBuilder<DummyBean> builder2 = (IBeanQueryBuilder<DummyBean>) BeanQuery.builder();
        assertTrue(builder2.name("dummyBeanForTest").build().execute().isPresent());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBeanQueryWithTypeAndName() throws DiException, DslException {

        IBeanQueryBuilder<DummyBean> builder1 = (IBeanQueryBuilder<DummyBean>) BeanQuery.builder();
        assertFalse(builder1.type(IClass.getClass(DummyBean.class)).name("toto").build().execute().isPresent());

        IBeanQueryBuilder<DummyBean> builder2 = (IBeanQueryBuilder<DummyBean>) BeanQuery.builder();
        assertTrue(builder2.type(IClass.getClass(DummyBean.class)).name("dummyBeanForTest").build().execute().isPresent());

    }

}
