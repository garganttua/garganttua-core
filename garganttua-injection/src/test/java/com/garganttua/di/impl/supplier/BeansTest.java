package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.Beans;
import com.garganttua.injection.DiContextBuilder;
import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.IBeanProvider;
import com.garganttua.injection.spec.IDiChildContextFactory;
import com.garganttua.injection.spec.IDiContext;
import com.garganttua.injection.spec.IDiContextBuilder;
import com.garganttua.injection.spec.IPropertyProvider;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BeansTest {

    private IDiContextBuilder builder;

    @BeforeEach
    void setUp() throws DiException, DslException {
        builder = new DiContextBuilder();
    }

    @Test
    @Order(1)
    public void contextNotBuiltShouldThrowException() {
        DiException exception = assertThrows(DiException.class, () -> Beans.bean(DummyBean.class).build().getObject());

        assertEquals("Context not built", exception.getMessage());
    }

    @Test
    public void contextNotInitializedShouldThrowException() {
        IDiChildContextFactory<DummyChildContext> factory = new DummyChildContextFactory();
        IBeanProvider beans = new DummyBeanProvider("provider1");
        IPropertyProvider properties = new DummyPropertyProvider("provider1");
        builder.childContextFactory(factory);
        builder.beanProvider(beans);
        builder.propertyProvider(properties);

        assertDoesNotThrow(builder::build);

        DiException exception = assertThrows(DiException.class, () -> Beans.bean(DummyBean.class).build().getObject());
        assertEquals("Context not initialized", exception.getMessage());
    }

    @Test
    public void contextNotStartedShouldThrowException() throws DiException {
        IDiChildContextFactory<DummyChildContext> factory = new DummyChildContextFactory();
        IBeanProvider beans = new DummyBeanProvider("provider1");
        IPropertyProvider properties = new DummyPropertyProvider("provider1");
        builder.childContextFactory(factory);
        builder.beanProvider(beans);
        builder.propertyProvider(properties);

        IDiContext context = assertDoesNotThrow(builder::build);
        context.onInit();

        DiException exception = assertThrows(DiException.class, () -> Beans.bean(DummyBean.class).build().getObject());
        assertEquals("Context not started", exception.getMessage());
    }
}
