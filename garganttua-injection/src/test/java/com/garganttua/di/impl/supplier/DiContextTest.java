package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.injection.Beans;
import com.garganttua.injection.DiContext;
import com.garganttua.injection.Properties;
import com.garganttua.injection.beans.Predefined;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

public class DiContextTest {

    String propertyValue = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() throws DiException, DslException {
        GGObjectReflectionHelper.annotationScanner = new ReflectionsAnnotationScanner();
        DiContext.builder().withPackage("com.garganttua")
        .propertyProvider(Predefined.PropertyProviders.garganttua.toString())
        .withProperty(String.class, "com.garganttua.dummyPropertyInConstructor", propertyValue)
        .up()
                .build().onInit().onStart();
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
