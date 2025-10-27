package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.injection.DiException;
import com.garganttua.injection.beans.BeanProvider;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

public class BeanProviderTest {

    @BeforeEach
    void setUp() {
        GGObjectReflectionHelper.annotationScanner = new ReflectionsAnnotationScanner();
    }

    @Test
    public void test() throws DiException{
        BeanProvider provider = new BeanProvider(List.of("com.garganttua"));
        provider.onInit().onStart();

        Optional<DummyBean> found = provider.getBean(DummyBean.class);

        assertNotNull(found);
        assertTrue(found.isPresent());

        DummyBean bean = found.get();
        assertEquals("default", bean.getValue());

    }

}
