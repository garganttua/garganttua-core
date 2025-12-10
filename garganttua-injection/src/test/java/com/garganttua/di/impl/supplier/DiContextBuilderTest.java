package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.injection.context.DiContext;
import com.garganttua.core.injection.context.Predefined;
import com.garganttua.core.injection.dummies.DummyBeanProviderBuilder;
import com.garganttua.core.injection.dummies.DummyPropertyProviderBuilder;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

public class DiContextBuilderTest {

    @BeforeEach
    void setUp() throws DiException, DslException {
        ObjectReflectionHelper.setAnnotationScanner(new ReflectionsAnnotationScanner());
    }

    @Test
    void testBuiltInBeanProviderIsPresent() throws DiException, DslException, LifecycleException {
        IDiContext context = (IDiContext) DiContext.builder().withPackage("com.garganttua").build().onInit().onStart();
        assertEquals(1, context.getBeanProviders().size());
        assertTrue(context.getBeanProvider(Predefined.BeanProviders.garganttua.toString()).isPresent());
    }

    @Test
    void testBuiltInPropertyProviderIsPresent() throws DiException, DslException, LifecycleException {
        IDiContext context = (IDiContext) DiContext.builder().withPackage("com.garganttua").build().onInit().onStart();
        assertEquals(1, context.getPropertyProviders().size());
        assertTrue(context.getBeanProvider(Predefined.PropertyProviders.garganttua.toString()).isPresent());
    }

    @Test
    void testAddDuplicateBeanProviderIgnored() throws DiException, DslException, LifecycleException {
        IDiContext context = (IDiContext) DiContext.builder().withPackage("com.garganttua")
                .beanProvider(Predefined.BeanProviders.garganttua.toString(), new DummyBeanProviderBuilder()).up()
                .beanProvider("dummy", new DummyBeanProviderBuilder()).up()
                .build().onInit().onStart();

        assertEquals(2, context.getBeanProviders().size());
    }

    @Test
    void testDuplicatePropertyProviderIgnored() throws DiException, DslException, LifecycleException {
        IDiContext context = (IDiContext) DiContext.builder().withPackage("com.garganttua")
                .propertyProvider(Predefined.PropertyProviders.garganttua.toString(), new DummyPropertyProviderBuilder()).up()
                .propertyProvider("dummy", new DummyPropertyProviderBuilder()).up()
                .build().onInit().onStart();

        assertEquals(2, context.getPropertyProviders().size());
    }
}
