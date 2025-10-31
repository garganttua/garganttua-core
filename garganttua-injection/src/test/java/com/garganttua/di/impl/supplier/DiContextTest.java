package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.dsl.DslException;
import com.garganttua.injection.Beans;
import com.garganttua.injection.DiContextBuilder;
import com.garganttua.injection.DiException;
import com.garganttua.injection.Properties;
import com.garganttua.injection.beans.BeanProvider;
import com.garganttua.injection.properties.PropertyProvider;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

public class DiContextTest {

    private PropertyProvider provider = new PropertyProvider();
    String propertyValue = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() throws DiException, DslException {
        GGObjectReflectionHelper.annotationScanner = new ReflectionsAnnotationScanner();
        new DiContextBuilder()
                .beanProvider(new BeanProvider(List.of("com.garganttua")))
                .propertyProvider(this.provider)
                .build().onInit().onStart();

        this.provider.setProperty("com.garganttua.dummyPropertyInConstructor", propertyValue);
    }

    @Test
    public void testPropertiesAreLoaded() throws DiException, DslException {
        Optional<String> property = Properties.property(String.class).key("com.garganttua.dummyPropertyInConstructor")
                .build().getObject();

        assertNotNull(property);
        assertTrue(property.isPresent());

        assertEquals(propertyValue, property.get());
    }

    @Test
    public void testDummyBeanIsLoaded() throws DiException, DslException {
        Optional<DummyBean> bean = Beans.bean(DummyBean.class).build().getObject();
        assertNotNull(bean);
        assertTrue(bean.isPresent());

        assertEquals(propertyValue, bean.get().getValue());
        assertNotNull(bean.get().getAnotherBean());
        assertTrue(bean.get().isPostConstructCalled());
        assertNotNull(bean.get().getOtherBean());
    }

}
