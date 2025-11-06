package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IInjectableBuilderRegistry;
import com.garganttua.core.injection.annotations.Property;
import com.garganttua.core.injection.annotations.Prototype;
import com.garganttua.core.injection.dummies.AnotherDummyBean;
import com.garganttua.core.injection.dummies.DummyBean;
import com.garganttua.core.injection.dummies.DummyOtherBean;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.SupplyException;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;
import com.garganttua.injection.DiContext;
import com.garganttua.injection.InjectBuilderFactory;
import com.garganttua.injection.InjectableBuilderRegistry;
import com.garganttua.injection.PropertyBuilderFactory;
import com.garganttua.injection.PrototypeBuilderFactory;
import com.garganttua.injection.SingletonBuilderFactory;
import com.garganttua.injection.beans.Predefined;

public class InjectableBuilderRegistryTest {

        private IInjectableBuilderRegistry reg;

        @BeforeEach
        void setUp() throws DiException, DslException, LifecycleException {
                ObjectReflectionHelper.annotationScanner = new ReflectionsAnnotationScanner();
                DiContext.builder().withPackage("com.garganttua")
                                .propertyProvider(Predefined.PropertyProviders.garganttua.toString())
                                .withProperty(String.class, "com.garganttua.dummyPropertyInConstructor",
                                                "propertyValue")
                                .up()
                                .build().onInit().onStart();

                this.reg = new InjectableBuilderRegistry();
                this.reg.registerFactory(Prototype.class, new PrototypeBuilderFactory(new HashSet<>()));
                this.reg.registerFactory(Singleton.class, new SingletonBuilderFactory(new HashSet<>()));
                this.reg.registerFactory(Inject.class, new InjectBuilderFactory(new HashSet<>()));
                this.reg.registerFactory(Property.class, new PropertyBuilderFactory());

        }

        @SuppressWarnings("unchecked")
        @Test
        public void testSingleton() throws NoSuchMethodException, SecurityException, DiException, DslException, SupplyException {
                Constructor<DummyBean> ctor = DummyBean.class.getConstructor(String.class, AnotherDummyBean.class,
                                DummyOtherBean.class);
                Parameter[] params = ctor.getParameters();

                Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> builder = this.reg.createBuilder(
                                params[2].getType(),
                                params[2]);

                assertNotNull(builder);
                assertTrue(builder.isPresent());
                assertEquals(DummyOtherBean.class, builder.get().getSuppliedType());
                Optional<DummyOtherBean> bean = (Optional<DummyOtherBean>) builder.get().build().supply();
                assertNotNull(bean);
                assertTrue(bean.isPresent());
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testPrototype() throws NoSuchMethodException, SecurityException, DiException, DslException, SupplyException {
                Constructor<DummyBean> ctor = DummyBean.class.getConstructor(String.class, AnotherDummyBean.class,
                                DummyOtherBean.class);
                Parameter[] params = ctor.getParameters();

                Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> builder = this.reg.createBuilder(
                                params[1].getType(),
                                params[1]);

                assertNotNull(builder);
                assertTrue(builder.isPresent());
                assertEquals(AnotherDummyBean.class, builder.get().getSuppliedType());
                Optional<AnotherDummyBean> bean = (Optional<AnotherDummyBean>) builder.get().build().supply();
                assertNotNull(bean);
                assertTrue(bean.isPresent());
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testProperty() throws NoSuchMethodException, SecurityException, DiException, DslException, SupplyException {
                Constructor<DummyBean> ctor = DummyBean.class.getConstructor(String.class, AnotherDummyBean.class,
                                DummyOtherBean.class);
                Parameter[] params = ctor.getParameters();

                Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> builder = this.reg.createBuilder(
                                params[0].getType(),
                                params[0]);

                assertNotNull(builder);
                assertTrue(builder.isPresent());
                assertEquals(String.class, builder.get().getSuppliedType());
                Optional<String> property = (Optional<String>) builder.get().build().supply();
                assertNotNull(property);
                assertTrue(property.isPresent());
                assertEquals("propertyValue", property.get());
        }
}
