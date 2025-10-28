package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.dsl.DslException;
import com.garganttua.injection.Beans;
import com.garganttua.injection.DiContextBuilder;
import com.garganttua.injection.DiException;
import com.garganttua.injection.beans.BeanProvider;
import com.garganttua.injection.properties.PropertyProvider;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

public class DiContextTest {

    private PropertyProvider provider = new PropertyProvider();

    @BeforeEach
    void setUp() {
        GGObjectReflectionHelper.annotationScanner = new ReflectionsAnnotationScanner();
        this.provider.setProperty("com.garganttua.dummyPropertyInConstructor", "propertyValue");
    }

    @Test
    public void test() throws DiException, DslException {
        new DiContextBuilder()
        .beanProvider(new BeanProvider(List.of("com.garganttua")))
        .propertyProvider(this.provider)
        .build().onInit().onStart();

        Optional<DummyBean> bean = Beans.bean(DummyBean.class).build().getObject();
        assertNotNull(bean);
        assertTrue(bean.isPresent());

    }

}
