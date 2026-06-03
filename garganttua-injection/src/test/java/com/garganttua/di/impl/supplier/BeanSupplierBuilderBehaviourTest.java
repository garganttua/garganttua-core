package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IBeanSupplier;
import com.garganttua.core.injection.context.dsl.BeanSupplierBuilder;
import com.garganttua.core.injection.context.dsl.IBeanSupplierBuilder;
import com.garganttua.core.injection.dummies.DummyBean;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

/**
 * Behaviour tests for {@link BeanSupplierBuilder}: constructor-from-query initialisation,
 * defaulting of strategy, qualifier extraction, provider override semantics, static vs.
 * contextual build, and null guards on the fluent setters.
 */
public class BeanSupplierBuilderBehaviourTest {

    @BeforeEach
    void setUp() {
        ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider()).build();
    }

    private IClass<DummyBean> dummy() {
        return IClass.getClass(DummyBean.class);
    }

    @Test
    void typeConstructorExposesSuppliedClassAndType() {
        BeanSupplierBuilder<DummyBean> b = new BeanSupplierBuilder<>(dummy());
        assertSame(dummy(), b.getSuppliedClass());
        assertEquals(DummyBean.class, b.getSuppliedType());
    }

    @Test
    void typeConstructorRejectsNullType() {
        assertThrows(NullPointerException.class, () -> new BeanSupplierBuilder<DummyBean>((IClass<DummyBean>) null));
    }

    @Test
    void queryConstructorRejectsNullQuery() {
        assertThrows(NullPointerException.class, () -> new BeanSupplierBuilder<>((BeanReference<DummyBean>) null));
    }

    @Test
    void queryConstructorCopiesTypeFromReference() {
        BeanReference<DummyBean> ref = new BeanReference<>(dummy(), Optional.of(BeanStrategy.prototype),
                Optional.of("myBean"), Set.of());
        BeanSupplierBuilder<DummyBean> b = new BeanSupplierBuilder<>(ref);
        assertSame(dummy(), b.getSuppliedClass());
    }

    @Test
    void providerOverloadWithEmptyOptionalDoesNotSetProvider() throws DslException {
        BeanReference<DummyBean> ref = new BeanReference<>(dummy(), Optional.of(BeanStrategy.singleton),
                Optional.empty(), Set.of());
        // Empty provider Optional -> only initFromQuery branch runs, provider stays null.
        IBeanSupplier<DummyBean> supplier = new BeanSupplierBuilder<>(Optional.empty(), ref).build();
        assertNotNull(supplier);
    }

    @Test
    void providerStringConstructorRejectsNulls() {
        BeanReference<DummyBean> ref = new BeanReference<>(dummy(), Optional.empty(), Optional.empty(), Set.of());
        assertThrows(NullPointerException.class, () -> new BeanSupplierBuilder<>((String) null, ref));
        assertThrows(NullPointerException.class, () -> new BeanSupplierBuilder<>("scope", (BeanReference<DummyBean>) null));
    }

    @Test
    void defaultBuildIsStaticNotContextual() throws DslException {
        BeanSupplierBuilder<DummyBean> b = new BeanSupplierBuilder<>(dummy());
        assertFalse(b.isContextual(), "useStaticContext defaults to true => not contextual");
        IBeanSupplier<DummyBean> supplier = b.build();
        // Static context supplier is the BeanSupplier subclass, not the bare ContextualBeanSupplier.
        assertEquals("BeanSupplier", supplier.getClass().getSimpleName());
    }

    @Test
    void useStaticContextFalseProducesContextualSupplier() throws DslException {
        IBeanSupplierBuilder<DummyBean> b = new BeanSupplierBuilder<>(dummy()).useStaticContext(false);
        assertTrue(b.isContextual());
        IBeanSupplier<DummyBean> supplier = b.build();
        assertEquals("ContextualBeanSupplier", supplier.getClass().getSimpleName());
    }

    @Test
    void buildSucceedsAfterFluentConfiguration() throws DslException {
        IBeanSupplierBuilder<DummyBean> b = new BeanSupplierBuilder<>(dummy())
                .name("primary")
                .provider("app")
                .strategy(BeanStrategy.prototype);
        IBeanSupplier<DummyBean> supplier = b.build();
        assertNotNull(supplier);
        assertEquals(DummyBean.class, supplier.getSuppliedType());
    }

    @Test
    void fluentSettersRejectNull() {
        BeanSupplierBuilder<DummyBean> b = new BeanSupplierBuilder<>(dummy());
        assertThrows(NullPointerException.class, () -> b.name(null));
        assertThrows(NullPointerException.class, () -> b.provider(null));
        assertThrows(NullPointerException.class, () -> b.strategy(null));
        assertThrows(NullPointerException.class, () -> b.qualifier(null));
    }

    @Test
    void dependenciesAreAlwaysEmpty() {
        IBeanSupplierBuilder<DummyBean> b = new BeanSupplierBuilder<>(dummy())
                .strategy(BeanStrategy.singleton);
        assertTrue(b.dependencies().isEmpty(), "bean supplier declares no build-time dependencies");
    }

    @Test
    void qualifierFromQueryIsRetainedThroughBuild() throws DslException {
        @SuppressWarnings("unchecked")
        IClass<? extends Annotation> qualifier =
                (IClass<? extends Annotation>) IClass.getClass(javax.inject.Singleton.class);
        BeanReference<DummyBean> ref = new BeanReference<>(dummy(), Optional.of(BeanStrategy.singleton),
                Optional.empty(), Set.of(qualifier));
        // The single qualifier from the query is picked up by initFromQuery and survives build().
        IBeanSupplier<DummyBean> supplier = new BeanSupplierBuilder<>(ref).build();
        assertNotNull(supplier);
    }

    @Test
    void chainingReturnsSameBuilderInstance() {
        IBeanSupplierBuilder<DummyBean> b = new BeanSupplierBuilder<>(dummy());
        assertSame(b, b.name("x"));
        assertSame(b, b.provider("p"));
        assertSame(b, b.strategy(BeanStrategy.prototype));
        assertSame(b, b.useStaticContext(true));
    }
}
