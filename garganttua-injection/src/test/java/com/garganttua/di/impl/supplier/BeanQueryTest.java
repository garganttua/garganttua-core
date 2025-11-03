package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanQueryBuilder;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.injection.DiContext;
import com.garganttua.injection.beans.BeanQuery;
import com.garganttua.injection.beans.Predefined;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

public class BeanQueryTest {

    @BeforeEach
    void setUp() throws DiException, DslException {
        GGObjectReflectionHelper.annotationScanner = new ReflectionsAnnotationScanner();
        DiContext.builder().withPackage("com.garganttua")
                .propertyProvider(Predefined.PropertyProviders.garganttua.toString())
                .withProperty(String.class, "com.garganttua.dummyPropertyInConstructor", "propertyValue")
                .up()
                .build().onInit().onStart();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBeanQueryWithType() throws DiException, DslException {

        IBeanQueryBuilder<DummyBean> builder = (IBeanQueryBuilder<DummyBean>) BeanQuery.builder();
        assertTrue(builder.type(DummyBean.class).build().execute().isPresent());

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
        assertFalse(builder1.type(DummyBean.class).name("toto").build().execute().isPresent());

        IBeanQueryBuilder<DummyBean> builder2 = (IBeanQueryBuilder<DummyBean>) BeanQuery.builder();
        assertTrue(builder2.type(DummyBean.class).name("dummyBeanForTest").build().execute().isPresent());

    }

}
