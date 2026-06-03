package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.injection.Predefined;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.dummies.AnotherDummyBean;
import com.garganttua.core.injection.functions.BeanMutationFunctions;
import com.garganttua.core.injection.functions.InjectionFunctions;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

/**
 * Behaviour tests for {@link BeanMutationFunctions} (addBean / addNamedBean /
 * addSingleton) against a built, mutable master {@link InjectionContext}.
 */
public class BeanMutationFunctionsBehaviourTest {

    private static final String GARG = Predefined.BeanProviders.garganttua.toString();

    /** A simple type with no DI annotations, not auto-detected. */
    public static class FreshBean {
        public final String id = UUID.randomUUID().toString();
    }

    @BeforeEach
    void setUp() throws DslException, LifecycleException {
        IReflectionBuilder rb = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .withScanner(new ReflectionsAnnotationScanner());
        rb.build();
        InjectionContext.builder().provide(rb).withPackage("com.garganttua")
                .propertyProvider(Predefined.PropertyProviders.garganttua.toString())
                .withProperty(IClass.getClass(String.class), "com.garganttua.dummyPropertyInConstructor",
                        UUID.randomUUID().toString())
                .up()
                .autoDetect(true)
                .build().onInit().onStart();
    }

    // ---------- addSingleton ----------

    @Test
    void addSingletonRegistersInstanceRetrievableByType() {
        FreshBean instance = new FreshBean();
        BeanMutationFunctions.addSingleton(GARG, FreshBean.class, instance);

        Object resolved = InjectionFunctions.getBean(FreshBean.class);
        assertSame(instance, resolved, "singleton should resolve to the exact instance added");
    }

    @Test
    void addSingletonNullProviderThrows() {
        assertThrows(ExpressionException.class,
                () -> BeanMutationFunctions.addSingleton(null, FreshBean.class, new FreshBean()));
    }

    @Test
    void addSingletonBlankProviderThrows() {
        assertThrows(ExpressionException.class,
                () -> BeanMutationFunctions.addSingleton("   ", FreshBean.class, new FreshBean()));
    }

    @Test
    void addSingletonNullTypeThrows() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> BeanMutationFunctions.addSingleton(GARG, null, new FreshBean()));
        assertTrue(ex.getMessage().contains("type cannot be null"));
    }

    @Test
    void addSingletonUnknownProviderThrows() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> BeanMutationFunctions.addSingleton("no-such", FreshBean.class, new FreshBean()));
        assertTrue(ex.getMessage().contains("addSingleton: failed"));
    }

    // ---------- addBean ----------

    @Test
    void addBeanWithInstanceRegistersSingletonLike() {
        FreshBean instance = new FreshBean();
        BeanMutationFunctions.addBean(GARG, FreshBean.class, instance);
        assertSame(instance, InjectionFunctions.getBean(FreshBean.class));
    }

    @Test
    void addBeanNullProviderThrows() {
        assertThrows(ExpressionException.class,
                () -> BeanMutationFunctions.addBean(null, FreshBean.class, new FreshBean()));
    }

    @Test
    void addBeanNullTypeThrows() {
        assertThrows(ExpressionException.class,
                () -> BeanMutationFunctions.addBean(GARG, null, new FreshBean()));
    }

    @Test
    void addBeanWithoutInstanceIsRejected() {
        // addBean uses empty strategy; with no bean object the provider rejects it
        // (manual addition without an object requires the prototype strategy).
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> BeanMutationFunctions.addBean(GARG, AnotherDummyBean.class, null));
        assertTrue(ex.getMessage().contains("prototype"));
    }

    // ---------- addNamedBean ----------

    @Test
    void addNamedBeanRegistersUnderName() {
        FreshBean instance = new FreshBean();
        BeanMutationFunctions.addNamedBean(GARG, FreshBean.class, "myNamedFresh", instance);

        Object byRef = InjectionFunctions.getBeanByRef(GARG + "::#myNamedFresh");
        assertSame(instance, byRef);
    }

    @Test
    void addNamedBeanNullProviderThrows() {
        assertThrows(ExpressionException.class,
                () -> BeanMutationFunctions.addNamedBean(null, FreshBean.class, "n", new FreshBean()));
    }

    @Test
    void addNamedBeanNullTypeThrows() {
        assertThrows(ExpressionException.class,
                () -> BeanMutationFunctions.addNamedBean(GARG, null, "n", new FreshBean()));
    }

    @Test
    void addNamedBeanNullNameThrows() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> BeanMutationFunctions.addNamedBean(GARG, FreshBean.class, null, new FreshBean()));
        assertTrue(ex.getMessage().contains("bean name cannot be null or blank"));
    }

    @Test
    void addNamedBeanBlankNameThrows() {
        assertThrows(ExpressionException.class,
                () -> BeanMutationFunctions.addNamedBean(GARG, FreshBean.class, "  ", new FreshBean()));
    }
}
