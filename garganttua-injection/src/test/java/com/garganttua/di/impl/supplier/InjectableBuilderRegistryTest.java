package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.dsl.DslException;
import com.garganttua.injection.DiContextBuilder;
import com.garganttua.injection.DiException;
import com.garganttua.injection.IInjectableBuilderRegistry;
import com.garganttua.injection.InjectBuilderFactory;
import com.garganttua.injection.InjectableBuilderRegistry;
import com.garganttua.injection.PropertyBuilderFactory;
import com.garganttua.injection.PrototypeBuilderFactory;
import com.garganttua.injection.SingletonBuilderFactory;
import com.garganttua.injection.beans.BeanProvider;
import com.garganttua.injection.properties.PropertyProvider;
import com.garganttua.injection.spec.beans.annotation.Property;
import com.garganttua.injection.spec.beans.annotation.Prototype;
import com.garganttua.injection.spec.supplier.IObjectSupplier;
import com.garganttua.injection.spec.supplier.builder.supplier.IObjectSupplierBuilder;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

public class InjectableBuilderRegistryTest {

    private IInjectableBuilderRegistry reg;

    @BeforeEach
    void setUp() throws DiException, DslException {
        GGObjectReflectionHelper.annotationScanner = new ReflectionsAnnotationScanner();
        PropertyProvider provider = new PropertyProvider();
        new DiContextBuilder()
                .beanProvider(new BeanProvider(List.of("com.garganttua")))
                .propertyProvider(provider)
                .build().onInit().onStart();

        provider.setProperty("com.garganttua.dummyPropertyInConstructor", "propertyValue");

        this.reg = new InjectableBuilderRegistry();
        this.reg.registerFactory(Prototype.class, new PrototypeBuilderFactory(new HashSet<>()));
        this.reg.registerFactory(Singleton.class, new SingletonBuilderFactory(new HashSet<>()));
        this.reg.registerFactory(Inject.class, new InjectBuilderFactory(new HashSet<>()));
        this.reg.registerFactory(Property.class, new PropertyBuilderFactory());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSingleton() throws NoSuchMethodException, SecurityException, DiException, DslException {
        Constructor<DummyBean> ctor = DummyBean.class.getConstructor(String.class, AnotherDummyBean.class,
                DummyOtherBean.class);
        Parameter[] params = ctor.getParameters();

        Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> builder = this.reg.createBuilder(params[2].getType(),
                params[2]);

        assertNotNull(builder);
        assertTrue(builder.isPresent());
        assertEquals(DummyOtherBean.class, builder.get().getObjectClass());
        Optional<DummyOtherBean> bean = (Optional<DummyOtherBean>) builder.get().build().getObject();
        assertNotNull(bean);
        assertTrue(bean.isPresent());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrototype() throws NoSuchMethodException, SecurityException, DiException, DslException {
        Constructor<DummyBean> ctor = DummyBean.class.getConstructor(String.class, AnotherDummyBean.class,
                DummyOtherBean.class);
        Parameter[] params = ctor.getParameters();

        Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> builder = this.reg.createBuilder(params[1].getType(),
                params[1]);

        assertNotNull(builder);
        assertTrue(builder.isPresent());
        assertEquals(AnotherDummyBean.class, builder.get().getObjectClass());
        Optional<AnotherDummyBean> bean = (Optional<AnotherDummyBean>) builder.get().build().getObject();
        assertNotNull(bean);
        assertTrue(bean.isPresent());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProperty() throws NoSuchMethodException, SecurityException, DiException, DslException {
        Constructor<DummyBean> ctor = DummyBean.class.getConstructor(String.class, AnotherDummyBean.class,
                DummyOtherBean.class);
        Parameter[] params = ctor.getParameters();

        Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> builder = this.reg.createBuilder(params[0].getType(),
                params[0]);

        assertNotNull(builder);
        assertTrue(builder.isPresent());
        assertEquals(String.class, builder.get().getObjectClass());
        Optional<String> property = (Optional<String>) builder.get().build().getObject();
        assertNotNull(property);
        assertTrue(property.isPresent());
        assertEquals("propertyValue", property.get());
    }
}
