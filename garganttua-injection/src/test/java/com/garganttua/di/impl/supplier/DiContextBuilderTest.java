package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.dsl.DslException;
import com.garganttua.injection.DiContext;
import com.garganttua.injection.DiException;
import com.garganttua.injection.beans.Predefined;
import com.garganttua.injection.spec.IDiChildContextFactory;
import com.garganttua.injection.spec.IDiContext;
import com.garganttua.injection.spec.IDiContextBuilder;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

public class DiContextBuilderTest {

    @BeforeEach
    void setUp() throws DiException, DslException {
        GGObjectReflectionHelper.annotationScanner = new ReflectionsAnnotationScanner();
    }

    @Test
    void testBuiltInBeanProviderIsPresent() throws DiException, DslException {
        IDiContext context = (IDiContext) DiContext.builder().withPackage("com.garganttua").build().onInit().onStart();
        assertEquals(1, context.getBeanProviders().size());
        assertTrue(context.getBeanProvider(Predefined.BeanProviders.garganttua.toString()).isPresent());
    }

    @Test
    void testBuiltInPropertyProviderIsPresent() throws DiException, DslException {
        IDiContext context = (IDiContext) DiContext.builder().withPackage("com.garganttua").build().onInit().onStart();
        assertEquals(1, context.getPropertyProviders().size());
        assertTrue(context.getBeanProvider(Predefined.PropertyProviders.garganttua.toString()).isPresent());
    }

    @Test
    void testAddDuplicateBeanProviderIgnored() throws DiException, DslException {
        IDiContext context = (IDiContext) DiContext.builder().withPackage("com.garganttua")
                .beanProvider(Predefined.BeanProviders.garganttua.toString(), new DummyBeanProviderBuilder()).up()
                .beanProvider("dummy", new DummyBeanProviderBuilder()).up()
                .build().onInit().onStart();

        assertEquals(2, context.getBeanProviders().size());
    }

    @Test
    void testDuplicatePropertyProviderIgnored() throws DiException, DslException {
        IDiContext context = (IDiContext) DiContext.builder().withPackage("com.garganttua")
                .propertyProvider(Predefined.PropertyProviders.garganttua.toString(), new DummyPropertyProviderBuilder()).up()
                .propertyProvider("dummy", new DummyPropertyProviderBuilder()).up()
                .build().onInit().onStart();

        assertEquals(2, context.getPropertyProviders().size());
    }
}
