package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.Predefined;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.dummies.DummyBean;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

/**
 * Behaviour tests for the {@link InjectionContext} implementation: property
 * lookups across overloads, provider lookups, query routing/errors, copy, and the
 * bootstrap summary contributor.
 */
public class InjectionContextBehaviourTest {

    private static final String GARG = Predefined.BeanProviders.garganttua.toString();
    private String propertyValue = UUID.randomUUID().toString();
    private IInjectionContext ctx;

    @BeforeEach
    void setUp() throws DslException, LifecycleException {
        IReflectionBuilder rb = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .withScanner(new ReflectionsAnnotationScanner());
        rb.build();
        ctx = InjectionContext.builder().provide(rb).withPackage("com.garganttua")
                .propertyProvider(Predefined.PropertyProviders.garganttua.toString())
                .withProperty(IClass.getClass(String.class), "com.garganttua.dummyPropertyInConstructor", propertyValue)
                .up()
                .autoDetect(true)
                .build();
        ctx.onInit().onStart();
    }

    // ---------- properties ----------

    @Test
    void getPropertyByKeyReturnsValue() throws DiException {
        Optional<String> v = ctx.getProperty("com.garganttua.dummyPropertyInConstructor", IClass.getClass(String.class));
        assertTrue(v.isPresent());
        assertEquals(propertyValue, v.get());
    }

    @Test
    void getPropertyOptionalProviderPresentRoutesToNamedProvider() throws DiException {
        Optional<String> v = ctx.getProperty(Optional.of(Predefined.PropertyProviders.garganttua.toString()),
                "com.garganttua.dummyPropertyInConstructor", IClass.getClass(String.class));
        assertEquals(propertyValue, v.orElse(null));
    }

    @Test
    void getPropertyOptionalProviderEmptyFallsBackToGlobal() throws DiException {
        Optional<String> v = ctx.getProperty(Optional.empty(),
                "com.garganttua.dummyPropertyInConstructor", IClass.getClass(String.class));
        assertEquals(propertyValue, v.orElse(null));
    }

    @Test
    void getPropertyFromUnknownProviderReturnsEmpty() throws DiException {
        Optional<String> v = ctx.getProperty("no-such-provider",
                "com.garganttua.dummyPropertyInConstructor", IClass.getClass(String.class));
        assertTrue(v.isEmpty());
    }

    @Test
    void getPropertyNullKeyThrowsNpe() {
        assertThrows(NullPointerException.class, () -> ctx.getProperty((String) null, IClass.getClass(String.class)));
    }

    @Test
    void getPropertyNullTypeThrowsNpe() {
        assertThrows(NullPointerException.class, () -> ctx.getProperty("k", null));
    }

    @Test
    void setPropertyThenReadBack() throws DiException {
        String val = UUID.randomUUID().toString();
        ctx.setProperty(Predefined.PropertyProviders.garganttua.toString(), "ctx.runtime.key", val);
        assertEquals(val, ctx.getProperty("ctx.runtime.key", IClass.getClass(String.class)).orElse(null));
    }

    @Test
    void setPropertyOnUnknownProviderThrowsDiException() {
        DiException ex = assertThrows(DiException.class, () -> ctx.setProperty("no-such", "k", "v"));
        assertTrue(ex.getMessage().contains("not found or immutable"));
    }

    @Test
    void setPropertyNullValueThrowsNpe() {
        assertThrows(NullPointerException.class,
                () -> ctx.setProperty(Predefined.PropertyProviders.garganttua.toString(), "k", null));
    }

    // ---------- providers ----------

    @Test
    void getBeanProviderKnownPresentUnknownEmpty() {
        assertTrue(ctx.getBeanProvider(GARG).isPresent());
        assertTrue(ctx.getBeanProvider("no-such").isEmpty());
    }

    @Test
    void getPropertyProviderKnownPresentUnknownEmpty() {
        assertTrue(ctx.getPropertyProvider(Predefined.PropertyProviders.garganttua.toString()).isPresent());
        assertTrue(ctx.getPropertyProvider("no-such").isEmpty());
    }

    @Test
    void getBeanProvidersSetIsUnmodifiable() throws DiException {
        Set<IBeanProvider> providers = ctx.getBeanProviders();
        assertFalse(providers.isEmpty());
        assertThrows(UnsupportedOperationException.class, providers::clear);
    }

    @Test
    void getPropertyProvidersSetIsUnmodifiable() throws DiException {
        Set<IPropertyProvider> providers = ctx.getPropertyProviders();
        assertThrows(UnsupportedOperationException.class, providers::clear);
    }

    // ---------- bean queries ----------

    @Test
    void queryBeanFromAllProvidersFindsDummyBean() throws DiException {
        BeanReference<DummyBean> ref = new BeanReference<>(IClass.getClass(DummyBean.class),
                Optional.empty(), Optional.empty(), Set.of());
        assertTrue(ctx.queryBean(ref).isPresent());
    }

    @Test
    void queryBeanFromNamedProviderFindsBean() throws DiException {
        BeanReference<DummyBean> ref = new BeanReference<>(IClass.getClass(DummyBean.class),
                Optional.empty(), Optional.empty(), Set.of());
        assertTrue(ctx.queryBean(GARG, ref).isPresent());
    }

    @Test
    void queryBeanInvalidProviderThrows() {
        BeanReference<DummyBean> ref = new BeanReference<>(IClass.getClass(DummyBean.class),
                Optional.empty(), Optional.empty(), Set.of());
        DiException ex = assertThrows(DiException.class, () -> ctx.queryBean("no-such", ref));
        assertTrue(ex.getMessage().contains("Invalid bean provider"));
    }

    @Test
    void queryBeansInvalidProviderThrows() {
        BeanReference<DummyBean> ref = new BeanReference<>(IClass.getClass(DummyBean.class),
                Optional.empty(), Optional.empty(), Set.of());
        assertThrows(DiException.class, () -> ctx.queryBeans("no-such", ref));
    }

    @Test
    void queryBeanNullQueryThrowsNpe() {
        assertThrows(NullPointerException.class, () -> ctx.queryBean((BeanReference<Object>) null));
    }

    @Test
    void queryBeansFromAllProvidersReturnsList() throws DiException {
        BeanReference<DummyBean> ref = new BeanReference<>(IClass.getClass(DummyBean.class),
                Optional.empty(), Optional.empty(), Set.of());
        assertEquals(1, ctx.queryBeans(ref).size());
    }

    @Test
    void addBeanToInvalidProviderThrows() {
        BeanReference<DummyBean> ref = new BeanReference<>(IClass.getClass(DummyBean.class),
                Optional.empty(), Optional.empty(), Set.of());
        assertThrows(DiException.class, () -> ctx.addBean("no-such", ref, new DummyBean()));
    }

    // ---------- copy ----------

    @Test
    void copyProducesIndependentContextWithSameProviders() throws Exception {
        IInjectionContext copy = ctx.copy();
        assertNotSame(ctx, copy);
        copy.onInit().onStart();
        assertEquals(ctx.getBeanProviders().size(), copy.getBeanProviders().size());

        BeanReference<DummyBean> ref = new BeanReference<>(IClass.getClass(DummyBean.class),
                Optional.empty(), Optional.empty(), Set.of());
        assertTrue(copy.queryBean(ref).isPresent(), "copy must still resolve beans");
    }

    // ---------- summary contributor ----------

    @Test
    void summaryCategoryIsInjectionContext() {
        assertEquals("Injection Context", ((InjectionContext) ctx).getSummaryCategory());
    }

    @Test
    void summaryItemsExposeProviderCounts() {
        Map<String, String> items = ((InjectionContext) ctx).getSummaryItems();
        assertTrue(items.containsKey("Bean providers"));
        assertTrue(items.containsKey("Property providers"));
        assertTrue(items.containsKey("Total beans registered"));
        // at least one bean provider and one property provider configured
        assertTrue(Integer.parseInt(items.get("Bean providers")) >= 1);
        assertTrue(Integer.parseInt(items.get("Property providers")) >= 1);
    }
}
