package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.dsl.DslException;
import com.garganttua.injection.Beans;
import com.garganttua.injection.DiContext;
import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.IDiContext;
import com.garganttua.injection.spec.IDiContextBuilder;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BeansTest {

    private IDiContextBuilder builder;

    @BeforeEach
    void setUp() throws DiException, DslException {
        GGObjectReflectionHelper.annotationScanner = new ReflectionsAnnotationScanner();
        DiContext.context = null;
        builder = DiContext.builder().withPackage("com.garganttua");
    }

    @Test
    @Order(1)
    public void contextNotBuiltShouldThrowException() {
        DiException exception = assertThrows(DiException.class, () -> Beans.bean(DummyBean.class).build().getObject());

        assertEquals("Context not built", exception.getMessage());
    }

    @Test
    @Order(2)
    public void contextNotInitializedShouldThrowException() {

        assertDoesNotThrow(builder::build);

        DiException exception = assertThrows(DiException.class, () -> Beans.bean(DummyBean.class).build().getObject());
        assertEquals("Lifecycle not initialized", exception.getMessage());
    }

    @Test
    @Order(3)
    public void contextNotStartedShouldThrowException() throws DiException {

        IDiContext context = assertDoesNotThrow(builder::build);
        context.onInit();

        DiException exception = assertThrows(DiException.class, () -> Beans.bean(DummyBean.class).build().getObject());
        assertEquals("Lifecycle not started", exception.getMessage());
    }
}
