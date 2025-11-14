package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.IInjectableElementResolverBuilder;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.context.DiContext;
import com.garganttua.core.injection.context.Predefined;
import com.garganttua.core.injection.context.dsl.DiContextBuilder;
import com.garganttua.core.injection.context.dsl.InjectableElementResolverBuilder;
import com.garganttua.core.injection.dummies.AnotherDummyBean;
import com.garganttua.core.injection.dummies.DummyBean;
import com.garganttua.core.injection.dummies.DummyOtherBean;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.supplying.SupplyException;

public class InjectableElementResolverBuilderTest {

        private IInjectableElementResolver resolvers;

        @BeforeEach
        void setUp() throws DiException, DslException, LifecycleException {
                ObjectReflectionHelper.annotationScanner = new ReflectionsAnnotationScanner();
                DiContext.builder().withPackage("com.garganttua")
                                .propertyProvider(Predefined.PropertyProviders.garganttua.toString())
                                .withProperty(String.class, "com.garganttua.dummyPropertyInConstructor",
                                                "propertyValue")
                                .up()
                                .autoDetect(true)
                                .build().onInit().onStart();

                IInjectableElementResolverBuilder builder = new InjectableElementResolverBuilder(DiContext.builder());
                DiContextBuilder.setBuiltInResolvers(builder, Set.of());
                this.resolvers = builder.build();

        }

        @SuppressWarnings("unchecked")
        @Test
        public void testSingleton()
                        throws NoSuchMethodException, SecurityException, DiException, DslException, SupplyException {
                Constructor<DummyBean> ctor = DummyBean.class.getConstructor(String.class, AnotherDummyBean.class,
                                DummyOtherBean.class);
                Parameter[] params = ctor.getParameters();

                Resolved resolved = this.resolvers.resolve(
                                params[2].getType(),
                                params[2]);

                assertNotNull(resolved);
                assertTrue(resolved.resolved());
                assertEquals(DummyOtherBean.class, resolved.elementSupplier().getSuppliedType());
                Optional<DummyOtherBean> bean = (Optional<DummyOtherBean>) resolved.elementSupplier().build().supply();
                assertNotNull(bean);
                assertTrue(bean.isPresent());
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testPrototype()
                        throws NoSuchMethodException, SecurityException, DiException, DslException, SupplyException {
                Constructor<DummyBean> ctor = DummyBean.class.getConstructor(String.class, AnotherDummyBean.class,
                                DummyOtherBean.class);
                Parameter[] params = ctor.getParameters();

                Resolved resolved = this.resolvers.resolve(
                                params[1].getType(),
                                params[1]);

                assertNotNull(resolved);
                assertTrue(resolved.resolved());
                assertEquals(AnotherDummyBean.class, resolved.elementSupplier().getSuppliedType());
                Optional<AnotherDummyBean> bean = (Optional<AnotherDummyBean>) resolved.elementSupplier().build().supply();
                assertNotNull(bean);
                assertTrue(bean.isPresent());
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testProperty()
                        throws NoSuchMethodException, SecurityException, DiException, DslException, SupplyException {
                Constructor<DummyBean> ctor = DummyBean.class.getConstructor(String.class, AnotherDummyBean.class,
                                DummyOtherBean.class);
                Parameter[] params = ctor.getParameters();

                Resolved resolved = this.resolvers.resolve(
                                params[0].getType(),
                                params[0]);

                assertNotNull(resolved);
                assertTrue(resolved.resolved());
                assertEquals(String.class, resolved.elementSupplier().getSuppliedType());
                Optional<String> property = (Optional<String>) resolved.elementSupplier().build().supply();
                assertNotNull(property);
                assertTrue(property.isPresent());
                assertEquals("propertyValue", property.get());
        }
}
