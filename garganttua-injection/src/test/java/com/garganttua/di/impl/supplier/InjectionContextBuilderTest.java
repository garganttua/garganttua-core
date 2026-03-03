package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.Predefined;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.dummies.DummyBeanProviderBuilder;
import com.garganttua.core.injection.dummies.DummyPropertyProviderBuilder;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

public class InjectionContextBuilderTest {

    private IReflectionBuilder rb;

    @BeforeEach
    void setUp() throws DiException, DslException {
        rb = ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider()).withScanner(new ReflectionsAnnotationScanner());
        rb.build();
    }

    @Test
    void testBuiltInBeanProviderIsPresent() throws DiException, DslException, LifecycleException {
        IInjectionContext context = (IInjectionContext) InjectionContext.builder().provide(rb).withPackage("com.garganttua").build().onInit().onStart();
        assertEquals(1, context.getBeanProviders().size());
        assertTrue(context.getBeanProvider(Predefined.BeanProviders.garganttua.toString()).isPresent());
    }

    @Test
    void testBuiltInPropertyProviderIsPresent() throws DiException, DslException, LifecycleException {
        IInjectionContext context = (IInjectionContext) InjectionContext.builder().provide(rb).withPackage("com.garganttua").build().onInit().onStart();
        assertEquals(1, context.getPropertyProviders().size());
        assertTrue(context.getBeanProvider(Predefined.PropertyProviders.garganttua.toString()).isPresent());
    }

    @Test
    void testAddDuplicateBeanProviderIgnored() throws DiException, DslException, LifecycleException {
        IInjectionContext context = (IInjectionContext) InjectionContext.builder().provide(rb).withPackage("com.garganttua")
                .beanProvider(Predefined.BeanProviders.garganttua.toString(), new DummyBeanProviderBuilder()).up()
                .beanProvider("dummy", new DummyBeanProviderBuilder()).up()
                .build().onInit().onStart();

        assertEquals(2, context.getBeanProviders().size());
    }

    @Test
    void testDuplicatePropertyProviderIgnored() throws DiException, DslException, LifecycleException {
        IInjectionContext context = (IInjectionContext) InjectionContext.builder().provide(rb).withPackage("com.garganttua")
                .propertyProvider(Predefined.PropertyProviders.garganttua.toString(), new DummyPropertyProviderBuilder()).up()
                .propertyProvider("dummy", new DummyPropertyProviderBuilder()).up()
                .build().onInit().onStart();

        assertEquals(2, context.getPropertyProviders().size());
    }
}
