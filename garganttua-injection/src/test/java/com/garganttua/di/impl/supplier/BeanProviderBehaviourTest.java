package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanFactory;
import com.garganttua.core.injection.context.beans.BeanFactory;
import com.garganttua.core.injection.context.beans.BeanProvider;
import com.garganttua.core.injection.dummies.AnotherDummyBean;
import com.garganttua.core.injection.dummies.DummyBean;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

/**
 * Behaviour tests for {@link BeanProvider}: query/get resolution, mutability gating,
 * the add() validation matrix, copy semantics, size and lifecycle gating.
 */
public class BeanProviderBehaviourTest {

    private IReflectionBuilder rb;

    private BeanFactory<DummyBean> singletonFactory() {
        return new BeanFactory<>(new BeanDefinition<DummyBean>(
                new BeanReference<>(IClass.getClass(DummyBean.class),
                        Optional.of(BeanStrategy.singleton), Optional.empty(), null),
                Optional.empty(), Set.of(), Set.of()));
    }

    private BeanFactory<AnotherDummyBean> anotherSingletonFactory() {
        return new BeanFactory<>(new BeanDefinition<AnotherDummyBean>(
                new BeanReference<>(IClass.getClass(AnotherDummyBean.class),
                        Optional.of(BeanStrategy.singleton), Optional.empty(), null),
                Optional.empty(), Set.of(), Set.of()));
    }

    @BeforeEach
    void setUp() {
        rb = ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider());
        rb.build();
    }

    private BeanProvider started(List<IBeanFactory<?>> factories, boolean mutable) throws LifecycleException {
        BeanProvider p = new BeanProvider(factories, mutable);
        p.onInit().onStart();
        return p;
    }

    @Test
    void constructorRejectsNullFactoryList() {
        assertThrows(NullPointerException.class, () -> new BeanProvider(null));
    }

    @Test
    void defaultConstructorProducesImmutableProvider() {
        BeanProvider p = new BeanProvider(new ArrayList<>());
        assertFalse(p.isMutable(), "single-arg constructor must yield an immutable provider");
    }

    @Test
    void sizeReflectsNumberOfFactories() throws LifecycleException {
        List<IBeanFactory<?>> factories = new ArrayList<>();
        factories.add(singletonFactory());
        factories.add(anotherSingletonFactory());
        BeanProvider p = started(factories, false);
        assertEquals(2, p.size());
    }

    @Test
    void getByTypeReturnsMatchingBean() throws Exception {
        List<IBeanFactory<?>> factories = new ArrayList<>();
        factories.add(singletonFactory());
        BeanProvider p = started(factories, false);

        Optional<DummyBean> bean = p.get(IClass.getClass(DummyBean.class));
        assertTrue(bean.isPresent());
        assertEquals("default", bean.get().getValue());
    }

    @Test
    void getByTypeReturnsEmptyWhenNoFactoryMatches() throws Exception {
        BeanProvider p = started(new ArrayList<>(List.of(anotherSingletonFactory())), false);
        Optional<DummyBean> bean = p.get(IClass.getClass(DummyBean.class));
        assertTrue(bean.isEmpty(), "no DummyBean factory registered -> empty");
    }

    @Test
    void getSingletonReturnsSameInstanceAcrossCalls() throws Exception {
        BeanProvider p = started(new ArrayList<>(List.of(singletonFactory())), false);
        DummyBean a = p.get(IClass.getClass(DummyBean.class)).get();
        DummyBean b = p.get(IClass.getClass(DummyBean.class)).get();
        assertSame(a, b);
    }

    @Test
    void getByNameAndTypeIsUnsupported() throws Exception {
        BeanProvider p = started(new ArrayList<>(List.of(singletonFactory())), false);
        assertThrows(UnsupportedOperationException.class,
                () -> p.get("whatever", IClass.getClass(DummyBean.class)));
    }

    @Test
    void queryReturnsSingleMatchAndEmptyForUnknownType() throws Exception {
        BeanProvider p = started(new ArrayList<>(List.of(singletonFactory())), false);

        BeanReference<DummyBean> match = new BeanReference<>(IClass.getClass(DummyBean.class),
                Optional.empty(), Optional.empty(), Set.of());
        assertTrue(p.query(match).isPresent());

        BeanReference<AnotherDummyBean> miss = new BeanReference<>(IClass.getClass(AnotherDummyBean.class),
                Optional.empty(), Optional.empty(), Set.of());
        assertTrue(p.query(miss).isEmpty());
    }

    @Test
    void queriesReturnsAllMatchingFactories() throws Exception {
        List<IBeanFactory<?>> factories = new ArrayList<>();
        factories.add(singletonFactory());
        factories.add(anotherSingletonFactory());
        BeanProvider p = started(factories, false);

        // Object matches everything
        BeanReference<Object> all = new BeanReference<>(IClass.getClass(Object.class),
                Optional.empty(), Optional.empty(), Set.of());
        assertEquals(2, p.queries(all).size());

        BeanReference<DummyBean> only = new BeanReference<>(IClass.getClass(DummyBean.class),
                Optional.empty(), Optional.empty(), Set.of());
        assertEquals(1, p.queries(only).size());
    }

    @Test
    void getByInterfaceCollectsAssignableBeans() throws Exception {
        List<IBeanFactory<?>> factories = new ArrayList<>();
        factories.add(singletonFactory());
        factories.add(anotherSingletonFactory());
        BeanProvider p = started(factories, false);

        List<Object> objects = p.get(IClass.getClass(Object.class), false);
        assertEquals(2, objects.size());
    }

    @Test
    void copyPreservesFactoriesButImmutable() throws Exception {
        BeanProvider p = started(new ArrayList<>(List.of(singletonFactory())), true);
        assertTrue(p.isMutable());

        BeanProvider copy = (BeanProvider) p.copy();
        assertEquals(1, copy.size());
        assertFalse(copy.isMutable(), "copy() yields an immutable provider");
    }

    @Test
    void addToImmutableProviderThrows() throws Exception {
        BeanProvider p = started(new ArrayList<>(List.of(singletonFactory())), false);
        BeanReference<AnotherDummyBean> ref = new BeanReference<>(IClass.getClass(AnotherDummyBean.class),
                Optional.of(BeanStrategy.singleton), Optional.empty(), Set.of());
        // immutability is checked first in add(ref, bean, autoDetect)
        DiException ex = assertThrows(DiException.class, () -> p.add(ref, new AnotherDummyBean(), false));
        assertTrue(ex.getMessage().contains("not mutable"));
    }

    @Test
    void addNullReferenceThrowsNpe() throws Exception {
        BeanProvider p = started(new ArrayList<>(), true);
        assertThrows(NullPointerException.class,
                () -> p.add((BeanReference<AnotherDummyBean>) null, new AnotherDummyBean(), false));
    }

    @Test
    void addBeanInstanceWithNonSingletonStrategyRejected() throws Exception {
        BeanProvider p = started(new ArrayList<>(), true);
        BeanReference<AnotherDummyBean> ref = new BeanReference<>(IClass.getClass(AnotherDummyBean.class),
                Optional.of(BeanStrategy.prototype), Optional.empty(), Set.of());
        DiException ex = assertThrows(DiException.class,
                () -> p.add(ref, new AnotherDummyBean(), false));
        assertTrue(ex.getMessage().contains("singleton"));
    }

    @Test
    void addNoBeanWithSingletonStrategyRejected() throws Exception {
        BeanProvider p = started(new ArrayList<>(), true);
        // strategy present and == singleton, but no bean object -> must be prototype
        BeanReference<AnotherDummyBean> ref = new BeanReference<>(IClass.getClass(AnotherDummyBean.class),
                Optional.of(BeanStrategy.singleton), Optional.empty(), Set.of());
        DiException ex = assertThrows(DiException.class,
                () -> p.add(ref, (AnotherDummyBean) null, false));
        assertTrue(ex.getMessage().contains("prototype"));
    }

    @Test
    void addNoBeanWithNoStrategyRejected() throws Exception {
        BeanProvider p = started(new ArrayList<>(), true);
        // no strategy at all, no bean -> rejected (needs prototype)
        BeanReference<AnotherDummyBean> ref = new BeanReference<>(IClass.getClass(AnotherDummyBean.class),
                Optional.empty(), Optional.empty(), Set.of());
        DiException ex = assertThrows(DiException.class,
                () -> p.add(ref, (AnotherDummyBean) null, false));
        assertTrue(ex.getMessage().contains("prototype"));
    }

    @Test
    void queryBeforeStartFailsLifecycleGate() {
        BeanProvider p = new BeanProvider(new ArrayList<>(List.of(singletonFactory())), false);
        // not init/started -> wrapLifecycle must surface a DiException
        BeanReference<DummyBean> ref = new BeanReference<>(IClass.getClass(DummyBean.class),
                Optional.empty(), Optional.empty(), Set.of());
        assertThrows(DiException.class, () -> p.query(ref));
    }

    @Test
    void flushClearsFactories() throws Exception {
        BeanProvider p = started(new ArrayList<>(List.of(singletonFactory())), false);
        assertEquals(1, p.size());
        p.onStop().onFlush();
        assertEquals(0, p.size());
    }

    @Test
    void reflectionUsageHasOneEntryPerFactory() throws Exception {
        List<IBeanFactory<?>> factories = new ArrayList<>();
        factories.add(singletonFactory());
        factories.add(anotherSingletonFactory());
        BeanProvider p = started(factories, false);
        assertEquals(2, p.reflectionUsage().size());
    }
}
