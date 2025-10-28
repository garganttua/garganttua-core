package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.dsl.DslException;
import com.garganttua.injection.DiContextBuilder;
import com.garganttua.injection.DiException;
import com.garganttua.injection.beans.BeanProvider;
import com.garganttua.injection.beans.BeanQuery;
import com.garganttua.injection.beans.IBeanQueryBuilder;
import com.garganttua.injection.properties.PropertyProvider;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

public class BeanQueryTest {

    @BeforeEach
    void setUp() throws DiException, DslException {
        GGObjectReflectionHelper.annotationScanner = new ReflectionsAnnotationScanner();
        PropertyProvider provider = new PropertyProvider();
        provider.setProperty("com.garganttua.dummyPropertyInConstructor", "propertyValue");

        new DiContextBuilder()
                .beanProvider(new BeanProvider(List.of("com.garganttua")))
                .propertyProvider(provider)
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
