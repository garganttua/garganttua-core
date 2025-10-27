package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.dsl.DslException;
import com.garganttua.injection.Beans;
import com.garganttua.injection.DiContextBuilder;
import com.garganttua.injection.DiException;
import com.garganttua.injection.beans.BeanProvider;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

public class DiContextTest {

    @BeforeEach
    void setUp() {
        GGObjectReflectionHelper.annotationScanner = new ReflectionsAnnotationScanner();
    }

    @Test
    public void test() throws DiException, DslException {
        new DiContextBuilder()
        .beanProvider(new BeanProvider(List.of("com.garganttua")))
        .propertyProvider(new DummyPropertyProvider("dummy"))
        .build().onInit().onStart();


        assertNotNull(Beans.bean(DummyBean.class).build().getObject());

    }

}
