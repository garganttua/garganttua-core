package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.beans.Beans;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.injection.dummies.DummyBean;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.supply.SupplyException;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BeansTest {

    private IInjectionContextBuilder builder;

    @BeforeEach
    void setUp() throws DiException, DslException {
        ObjectReflectionHelper.setAnnotationScanner(new ReflectionsAnnotationScanner());
        InjectionContext.context = null;
        builder = InjectionContext.builder().withPackage("com.garganttua");
    }

    @Test
    @Order(1)
    public void contextNotBuiltShouldThrowException() {
        SupplyException exception = assertThrows(SupplyException.class, () -> Beans.bean(DummyBean.class).build().supply());

        assertEquals("Context not built", exception.getMessage());
    }

    @Test
    @Order(2)
    public void contextNotInitializedShouldThrowException() {

        assertDoesNotThrow(builder::build);

        SupplyException exception = assertThrows(SupplyException.class, () -> Beans.bean(DummyBean.class).build().supply());
        assertTrue(exception.getMessage().contains("Lifecycle not initialized"));
    }

    @Test
    @Order(3)
    public void contextNotStartedShouldThrowException() throws DiException, LifecycleException {

        IInjectionContext context = assertDoesNotThrow(builder::build);
        context.onInit();

        SupplyException exception = assertThrows(SupplyException.class, () -> Beans.bean(DummyBean.class).build().supply());
        assertTrue(exception.getMessage().contains("Lifecycle not started"));
    }
}
