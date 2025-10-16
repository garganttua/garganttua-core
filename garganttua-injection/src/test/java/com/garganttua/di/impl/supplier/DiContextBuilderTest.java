package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.DiContextBuilder;
import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.IBeanScope;
import com.garganttua.injection.spec.IDiChildContextFactory;
import com.garganttua.injection.spec.IDiContext;
import com.garganttua.injection.spec.IPropertyScope;

public class DiContextBuilderTest {
    private DiContextBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new DiContextBuilder();
    }

    @Test
    void testAddBeanScope() throws DiException {
        IBeanScope beans = new DummyBeanScope("scope1");
        IPropertyScope properties = new DummyPropertyScope("scope1");
        builder.beanScope(beans);
        builder.propertyScope(properties);
        IDiContext context = assertDoesNotThrow(builder::build);

        assertEquals(1, context.getBeanScopes().size());
        assertTrue(context.getBeanScopes().stream().anyMatch(s -> s.getName().equals("scope1")));
    }

    @Test
    void testAddDuplicateBeanScopeIgnored() throws DiException {
        IBeanScope scope1 = new DummyBeanScope("scope1");
        IBeanScope scope2 = new DummyBeanScope("scope1");

        builder.beanScope(scope1).beanScope(scope2);
        IDiContext context = assertDoesNotThrow(builder::build);

        assertEquals(1, context.getBeanScopes().size());
    }

    @Test
    void testAddPropertyScope() throws DiException {
        IBeanScope beans = new DummyBeanScope("scope1");
        IPropertyScope properties = new DummyPropertyScope("scope1");
        builder.beanScope(beans);
        builder.propertyScope(properties);
        IDiContext context = assertDoesNotThrow(builder::build);

        assertEquals(1, context.getPropertyScopes().size());
        assertTrue(context.getPropertyScopes().stream().anyMatch(s -> s.getName().equals("scope1")));
    }

    @Test
    void testAddChildContextFactory() throws DiException {
        IDiChildContextFactory<DummyChildContext> factory = new DummyChildContextFactory();
        builder.childContextFactory(factory);
        IBeanScope beans = new DummyBeanScope("scope1");
        IPropertyScope properties = new DummyPropertyScope("scope1");
        builder.beanScope(beans);
        builder.propertyScope(properties);
        IDiContext context = assertDoesNotThrow(builder::build);

        assertEquals(1, context.getChildContextFactories().size());
        assertTrue(context.getChildContextFactories().stream().anyMatch(f -> f.getClass().equals(factory.getClass())));
    }

    @Test
    void testDuplicateChildContextFactoryIgnored() throws DiException {
        IDiChildContextFactory<DummyChildContext> f1 = new DummyChildContextFactory();
        IDiChildContextFactory<DummyChildContext> f2 = new DummyChildContextFactory();

        IBeanScope beans = new DummyBeanScope("scope1");
        IPropertyScope properties = new DummyPropertyScope("scope1");
        builder.beanScope(beans);
        builder.propertyScope(properties);
        builder.childContextFactory(f1).childContextFactory(f2);
        IDiContext context = assertDoesNotThrow(builder::build);

        assertEquals(1, context.getChildContextFactories().size());
    }

    @Test
    void testBuildWithoutScopesThrows() {
        DiContextBuilder emptyBuilder = new DiContextBuilder();
        assertThrows(DslException.class, emptyBuilder::build);
    }

    @Test
    void testListsAreImmutable() throws DslException {
        IBeanScope scope = new DummyBeanScope("scope1");
        builder.beanScope(scope);
        IDiContext context = builder.build();

        assertThrows(UnsupportedOperationException.class,
                () -> context.getBeanScopes().add(new DummyBeanScope("scope2")));
    }

}
