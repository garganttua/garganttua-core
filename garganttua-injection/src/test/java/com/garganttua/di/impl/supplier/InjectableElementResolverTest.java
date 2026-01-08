package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.Predefined;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.beans.resolver.PrototypeElementResolver;
import com.garganttua.core.injection.context.beans.resolver.SingletonElementResolver;
import com.garganttua.core.injection.context.properties.resolver.PropertyElementResolver;
import com.garganttua.core.injection.dummies.AnotherDummyBean;
import com.garganttua.core.injection.dummies.DummyBean;
import com.garganttua.core.injection.dummies.DummyOtherBean;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.supply.SupplyException;

public class InjectableElementResolverTest {

    @BeforeEach
    void setUp() throws DiException, DslException, LifecycleException {
        ObjectReflectionHelper.setAnnotationScanner(new ReflectionsAnnotationScanner());
        InjectionContext.builder().withPackage("com.garganttua")
                .propertyProvider(Predefined.PropertyProviders.garganttua.toString())
                .withProperty(String.class, "com.garganttua.dummyPropertyInConstructor", "propertyValue")
                .up()
                .autoDetect(true)
                .build().onInit().onStart();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSingleton() throws NoSuchMethodException, SecurityException, DiException, DslException, SupplyException {
        Constructor<DummyBean> ctor = DummyBean.class.getConstructor(String.class, AnotherDummyBean.class, DummyOtherBean.class);
        Parameter[] params = ctor.getParameters();

        SingletonElementResolver singletonConstructor = new SingletonElementResolver(
                new HashSet<Class<? extends Annotation>>());

        Resolved resolved = singletonConstructor.resolve(params[2].getType(), params[2]);

        assertNotNull(resolved);
        assertTrue(resolved.resolved());
        assertEquals(DummyOtherBean.class, resolved.elementSupplier().getSuppliedClass());
        Optional<DummyOtherBean> bean = (Optional<DummyOtherBean>) resolved.elementSupplier().build().supply();
        assertNotNull(bean);
        assertTrue(bean.isPresent());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrototype() throws NoSuchMethodException, SecurityException, DiException, DslException, SupplyException {
        Constructor<DummyBean> ctor = DummyBean.class.getConstructor(String.class, AnotherDummyBean.class, DummyOtherBean.class);
        Parameter[] params = ctor.getParameters();

        PrototypeElementResolver prototypeConstructor = new PrototypeElementResolver(
                new HashSet<Class<? extends Annotation>>());

        Resolved resolved = prototypeConstructor.resolve(params[1].getType(), params[1]);

        assertNotNull(resolved);
        assertTrue(resolved.resolved());
        assertEquals(AnotherDummyBean.class, resolved.elementSupplier().getSuppliedClass());
        Optional<AnotherDummyBean> bean = (Optional<AnotherDummyBean>) resolved.elementSupplier().build().supply();
        assertNotNull(bean);
        assertTrue(bean.isPresent());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProperty() throws NoSuchMethodException, SecurityException, DiException, DslException, SupplyException {
        Constructor<DummyBean> ctor = DummyBean.class.getConstructor(String.class, AnotherDummyBean.class, DummyOtherBean.class);
        Parameter[] params = ctor.getParameters();

        PropertyElementResolver propertyConstructor = new PropertyElementResolver();

        Resolved resolved = propertyConstructor.resolve(params[0].getType(), params[0]);

        assertNotNull(resolved);
        assertTrue(resolved.resolved());
        assertEquals(String.class, resolved.elementSupplier().getSuppliedClass());
        Optional<String> property = (Optional<String>) resolved.elementSupplier().build().supply();
        assertNotNull(property);
        assertTrue(property.isPresent());
        assertEquals("propertyValue", property.get());
    }

}
