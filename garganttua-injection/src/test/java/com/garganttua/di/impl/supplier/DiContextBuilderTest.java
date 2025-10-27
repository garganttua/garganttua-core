package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.dsl.DslException;
import com.garganttua.injection.DiContextBuilder;
import com.garganttua.injection.DiException;
import com.garganttua.injection.beans.BeanProvider;
import com.garganttua.injection.spec.IBeanProvider;
import com.garganttua.injection.spec.IDiChildContextFactory;
import com.garganttua.injection.spec.IDiContext;
import com.garganttua.injection.spec.IPropertyProvider;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

public class DiContextBuilderTest {
    private DiContextBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new DiContextBuilder();
        GGObjectReflectionHelper.annotationScanner = new ReflectionsAnnotationScanner();
    }

    @Test
    void testAddBeanProvider() throws DiException {
        IBeanProvider beans = new BeanProvider(List.of("com.garganttua"));
        IPropertyProvider properties = new DummyPropertyProvider("provider1");
        builder.beanProvider(beans);
        builder.propertyProvider(properties);
        IDiContext context = assertDoesNotThrow(builder::build);
        context.onInit();
        context.onStart();

        assertEquals(1, context.getBeanProviders().size());
        assertTrue(context.getBeanProviders().stream().anyMatch(s -> s.getName().equals("garganttua")));
    }

    @Test
    void testAddDuplicateBeanProviderIgnored() throws DiException {
        IBeanProvider beans = new BeanProvider(List.of("com.garganttua"));
        IBeanProvider provider2 = new DummyBeanProvider("provider1");

        builder.beanProvider(beans).beanProvider(provider2);
        IDiContext context = assertDoesNotThrow(builder::build);
        context.onInit();
        context.onStart();

        assertEquals(2, context.getBeanProviders().size());
    }

    @Test
    void testAddPropertyProvider() throws DiException {
        IBeanProvider beans = new DummyBeanProvider("provider1");
        IPropertyProvider properties = new DummyPropertyProvider("provider1");
        builder.beanProvider(beans);
        builder.propertyProvider(properties);
        IDiContext context = assertDoesNotThrow(builder::build);
                context.onInit();
        context.onStart();

        assertEquals(1, context.getPropertyProviders().size());
        assertTrue(context.getPropertyProviders().stream().anyMatch(s -> s.getName().equals("provider1")));
    }

    @Test
    void testAddChildContextFactory() throws DiException {
        IDiChildContextFactory<DummyChildContext> factory = new DummyChildContextFactory();
        builder.childContextFactory(factory);
        IBeanProvider beans = new DummyBeanProvider("provider1");
        IPropertyProvider properties = new DummyPropertyProvider("provider1");
        builder.beanProvider(beans);
        builder.propertyProvider(properties);
        IDiContext context = assertDoesNotThrow(builder::build);
                context.onInit();
        context.onStart();

        assertEquals(1, context.getChildContextFactories().size());
        assertTrue(context.getChildContextFactories().stream().anyMatch(f -> f.getClass().equals(factory.getClass())));
    }

    @Test
    void testDuplicateChildContextFactoryIgnored() throws DiException {
        IDiChildContextFactory<DummyChildContext> f1 = new DummyChildContextFactory();
        IDiChildContextFactory<DummyChildContext> f2 = new DummyChildContextFactory();

        IBeanProvider beans = new DummyBeanProvider("provider1");
        IPropertyProvider properties = new DummyPropertyProvider("provider1");
        builder.beanProvider(beans);
        builder.propertyProvider(properties);
        builder.childContextFactory(f1).childContextFactory(f2);
        IDiContext context = assertDoesNotThrow(builder::build);
                context.onInit();
        context.onStart();

        assertEquals(1, context.getChildContextFactories().size());
    }

    @Test
    void testBuildWithoutProvidersThrows() {
        DiContextBuilder emptyBuilder = new DiContextBuilder();
        assertThrows(DslException.class, emptyBuilder::build);
    }

    @Test
    void testListsAreImmutable() throws DslException {
        IBeanProvider provider = new DummyBeanProvider("provider1");
        builder.beanProvider(provider);
        IDiContext context = builder.build();

        assertThrows(DiException.class,
                () -> context.getBeanProviders().add(new DummyBeanProvider("provider2")));
    }

}
