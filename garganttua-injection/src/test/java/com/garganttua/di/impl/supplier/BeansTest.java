package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.dsl.DslException;
import com.garganttua.injection.Beans;
import com.garganttua.injection.DiContextBuilder;
import com.garganttua.injection.DiException;
import com.garganttua.injection.beans.BeanProvider;
import com.garganttua.injection.spec.IDiContext;
import com.garganttua.injection.spec.IDiContextBuilder;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BeansTest {

    private IDiContextBuilder builder;

    @BeforeEach
    void setUp() throws DiException, DslException {
        GGObjectReflectionHelper.annotationScanner = new ReflectionsAnnotationScanner();
        builder = new DiContextBuilder();
    }

    /* @Test
    @Order(1)
    public void contextNotBuiltShouldThrowException() {
        DiException exception = assertThrows(DiException.class, () -> Beans.bean(DummyBean.class).build().getObject());

        assertEquals("Context not built", exception.getMessage());
    } */

    @Test
    @Order(2)
    public void contextNotInitializedShouldThrowException() {
        builder.childContextFactory(new DummyChildContextFactory());
        builder.beanProvider(new BeanProvider(List.of("com.garganttua")));
        builder.propertyProvider(new DummyPropertyProvider("provider1"));

        assertDoesNotThrow(builder::build);

        DiException exception = assertThrows(DiException.class, () -> Beans.bean(DummyBean.class).build().getObject());
        assertEquals("Lifecycle not initialized", exception.getMessage());
    }

    @Test
    @Order(3)
    public void contextNotStartedShouldThrowException() throws DiException {
        builder.childContextFactory(new DummyChildContextFactory());
        builder.beanProvider(new BeanProvider(List.of("com.garganttua")));
        builder.propertyProvider(new DummyPropertyProvider("provider1"));

        IDiContext context = assertDoesNotThrow(builder::build);
        context.onInit();

        DiException exception = assertThrows(DiException.class, () -> Beans.bean(DummyBean.class).build().getObject());
        assertEquals("Lifecycle not started", exception.getMessage());
    }
}
