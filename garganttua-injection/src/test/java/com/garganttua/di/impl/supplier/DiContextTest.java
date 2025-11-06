package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.dummies.DummyBean;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.supplying.SupplyException;
import com.garganttua.injection.Beans;
import com.garganttua.injection.DiContext;
import com.garganttua.injection.Properties;
import com.garganttua.injection.beans.Predefined;

public class DiContextTest {

    String propertyValue = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() throws DiException, DslException, LifecycleException {
        ObjectReflectionHelper.annotationScanner = new ReflectionsAnnotationScanner();
        DiContext.builder().withPackage("com.garganttua")
        .propertyProvider(Predefined.PropertyProviders.garganttua.toString())
        .withProperty(String.class, "com.garganttua.dummyPropertyInConstructor", propertyValue)
        .up()
                .build().onInit().onStart();
    }

    @Test
    public void testPropertiesAreLoaded() throws DiException, DslException, SupplyException {
        Optional<String> property = Properties.property(String.class).key("com.garganttua.dummyPropertyInConstructor")
                .build().supply();

        assertNotNull(property);
        assertTrue(property.isPresent());

        assertEquals(propertyValue, property.get());
    }

    @Test
    public void testDummyBeanIsLoaded() throws DiException, DslException, SupplyException {
        Optional<DummyBean> bean = Beans.bean(DummyBean.class).build().supply();
        assertNotNull(bean);
        assertTrue(bean.isPresent());

        assertEquals(propertyValue, bean.get().getValue());
        assertNotNull(bean.get().getAnotherBean());
        assertTrue(bean.get().isPostConstructCalled());
        assertNotNull(bean.get().getOtherBean());
    }

}
