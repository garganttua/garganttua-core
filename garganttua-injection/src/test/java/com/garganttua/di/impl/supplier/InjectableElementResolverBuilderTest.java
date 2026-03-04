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
import com.garganttua.core.reflection.IAnnotatedElement;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.injection.Predefined;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.InjectionContextBuilder;
import com.garganttua.core.injection.context.dsl.InjectableElementResolverBuilder;
import com.garganttua.core.injection.dummies.AnotherDummyBean;
import com.garganttua.core.injection.dummies.DummyBean;
import com.garganttua.core.injection.dummies.DummyOtherBean;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeParameter;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.supply.SupplyException;

public class InjectableElementResolverBuilderTest {

        private IInjectableElementResolver resolvers;

        @BeforeEach
        void setUp() throws DiException, DslException, LifecycleException {
                IReflectionBuilder rb = ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider()).withScanner(new ReflectionsAnnotationScanner());
                rb.build();
                InjectionContext.builder().provide(rb).withPackage("com.garganttua")
                                .propertyProvider(Predefined.PropertyProviders.garganttua.toString())
                                .withProperty(IClass.getClass(String.class), "com.garganttua.dummyPropertyInConstructor",
                                                "propertyValue")
                                .up()
                                .autoDetect(true)
                                .build().onInit().onStart();

                IInjectableElementResolverBuilder builder = new InjectableElementResolverBuilder(InjectionContext.builder());
                InjectionContextBuilder.setBuiltInResolvers(builder, Set.of(), false);
                this.resolvers = builder.build();

        }

        private static IAnnotatedElement adaptParameter(Parameter param) {
                RuntimeParameter rp = RuntimeParameter.of(param);
                return new IAnnotatedElement() {
                        @Override
                        public <T extends java.lang.annotation.Annotation> T getAnnotation(IClass<T> annotationClass) {
                                return rp.getAnnotation(annotationClass);
                        }
                        @Override
                        public java.lang.annotation.Annotation[] getAnnotations() {
                                return rp.getAnnotations();
                        }
                        @Override
                        public java.lang.annotation.Annotation[] getDeclaredAnnotations() {
                                return rp.getDeclaredAnnotations();
                        }
                        @Override
                        public IReflection reflection() {
                                return IClass.getReflection();
                        }
                };
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testSingleton()
                        throws NoSuchMethodException, SecurityException, DiException, DslException, SupplyException {
                Constructor<DummyBean> ctor = DummyBean.class.getConstructor(String.class, AnotherDummyBean.class,
                                DummyOtherBean.class);
                Parameter[] params = ctor.getParameters();

                IAnnotatedElement adapted2 = adaptParameter(params[2]);
                Resolved resolved = this.resolvers.resolve(
                                IClass.getClass(params[2].getType()),
                                adapted2);

                assertNotNull(resolved);
                assertTrue(resolved.resolved());
                assertEquals(IClass.getClass(DummyOtherBean.class), resolved.elementSupplier().getSuppliedClass());
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

                IAnnotatedElement adapted1 = adaptParameter(params[1]);
                Resolved resolved = this.resolvers.resolve(
                                IClass.getClass(params[1].getType()),
                                adapted1);

                assertNotNull(resolved);
                assertTrue(resolved.resolved());
                assertEquals(IClass.getClass(AnotherDummyBean.class), resolved.elementSupplier().getSuppliedClass());
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

                IAnnotatedElement adapted0 = adaptParameter(params[0]);
                Resolved resolved = this.resolvers.resolve(
                                IClass.getClass(params[0].getType()),
                                adapted0);

                assertNotNull(resolved);
                assertTrue(resolved.resolved());
                assertEquals(IClass.getClass(String.class), resolved.elementSupplier().getSuppliedClass());
                Optional<String> property = (Optional<String>) resolved.elementSupplier().build().supply();
                assertNotNull(property);
                assertTrue(property.isPresent());
                assertEquals("propertyValue", property.get());
        }
}
