package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IBeanSupplier;
import com.garganttua.core.injection.context.beans.BeanFactory;
import com.garganttua.core.injection.context.dsl.BeanFactoryBuilder;
import com.garganttua.core.injection.dummies.AnotherDummyBean;
import com.garganttua.core.injection.dummies.DummyBean;
import com.garganttua.core.injection.dummies.DummyBeanQualifier;
import com.garganttua.core.nativve.IReflectionConfigurationEntry;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.supply.SupplyException;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;

/**
 * Additional behaviour tests for {@link BeanFactory} targeting branches not
 * exercised by {@link BeanFactoryTest}: the forced-singleton constructors, the
 * field-injection + post-construct pipeline, prototype re-initialisation,
 * identity (equals/hashCode/getSuppliedType/matches), and native-entry assembly.
 */
public class BeanFactoryMoreBehaviourTest {

    private IReflectionBuilder rb;

    @BeforeEach
    void setUp() {
        rb = ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider());
        rb.build();
    }

    private BeanReference<DummyBean> ref(BeanStrategy strategy, String name,
            Set<IClass<? extends Annotation>> quals) {
        return new BeanReference<>(IClass.getClass(DummyBean.class),
                Optional.ofNullable(strategy), Optional.ofNullable(name),
                quals == null ? Set.of() : quals);
    }

    private BeanDefinition<DummyBean> bareDef(BeanStrategy strategy) {
        return new BeanDefinition<>(ref(strategy, null, null), Optional.empty(), Set.of(), Set.of());
    }

    // ---------- forced-singleton constructors ----------

    @Test
    void seedingWithBeanForcesSingletonEvenWhenDefinitionSaysPrototype() throws SupplyException {
        DummyBean seed = new DummyBean();
        seed.setValue("seeded");
        // Definition declares prototype, but seeding a bean must override to singleton.
        BeanFactory<DummyBean> factory = new BeanFactory<>(bareDef(BeanStrategy.prototype), seed);

        assertEquals(Optional.of(BeanStrategy.singleton), factory.definition().reference().strategy(),
                "seeding a bean must force the singleton strategy on the definition");

        DummyBean a = factory.supply().orElseThrow();
        DummyBean b = factory.supply().orElseThrow();
        assertSame(seed, a, "seeded instance must be returned verbatim");
        assertSame(a, b, "forced singleton must return the same instance");
        assertEquals("seeded", a.getValue());
    }

    @Test
    void optionalConstructorOverloadWithEmptyInstantiatesLazily() throws SupplyException {
        BeanFactory<DummyBean> factory = new BeanFactory<>(bareDef(BeanStrategy.singleton), Optional.empty());
        // strategy stays as declared (singleton) because no bean was seeded
        assertEquals(Optional.of(BeanStrategy.singleton), factory.definition().reference().strategy());
        DummyBean bean = factory.supply().orElseThrow();
        assertEquals("default", bean.getValue());
    }

    @Test
    void optionalConstructorOverloadWithPresentForcesSingleton() throws SupplyException {
        DummyBean seed = new DummyBean();
        BeanFactory<DummyBean> factory = new BeanFactory<>(bareDef(BeanStrategy.prototype), Optional.of(seed));
        assertEquals(Optional.of(BeanStrategy.singleton), factory.definition().reference().strategy());
        assertSame(seed, factory.supply().orElseThrow());
    }

    @Test
    void nullDefinitionRejected() {
        assertThrows(NullPointerException.class, () -> new BeanFactory<>((BeanDefinition<DummyBean>) null));
    }

    // ---------- field injection + post-construct pipeline ----------

    @Test
    void singletonRunsFieldInjectionAndPostConstructExactlyOnce() throws DslException, SupplyException {
        String random = UUID.randomUUID().toString();
        IBeanSupplier<DummyBean> supplier = new BeanFactoryBuilder<>(IClass.getClass(DummyBean.class))
                .provide(rb)
                .strategy(BeanStrategy.singleton)
                .field(IClass.getClass(String.class)).field("anotherValue")
                .withValue(FixedSupplierBuilder.of(random, IClass.getClass(String.class))).up()
                .postConstruction().method("markPostConstruct", IClass.getClass(void.class), new IClass<?>[0]).up()
                .build();

        DummyBean first = supplier.supply().orElseThrow();
        assertTrue(first.isPostConstructCalled(), "post-construct must run on first supply");
        assertEquals(random, first.getAnotherValue(), "field injection must populate anotherValue");

        // Tamper, then ask again: singleton must NOT re-run injection/post-construct.
        first.setValue("tampered");
        DummyBean second = supplier.supply().orElseThrow();
        assertSame(first, second, "singleton returns the same instance");
        assertEquals("tampered", second.getValue(),
                "singleton must not re-initialise the already-initialised bean");
    }

    @Test
    void prototypeReInitialisesEachSuppliedInstance() throws DslException, SupplyException {
        IBeanSupplier<DummyBean> supplier = new BeanFactoryBuilder<>(IClass.getClass(DummyBean.class))
                .provide(rb)
                .strategy(BeanStrategy.prototype)
                .postConstruction().method("markPostConstruct", IClass.getClass(void.class), new IClass<?>[0]).up()
                .build();

        DummyBean a = supplier.supply().orElseThrow();
        DummyBean b = supplier.supply().orElseThrow();
        assertNotSame(a, b, "prototype must yield distinct instances");
        assertTrue(a.isPostConstructCalled());
        assertTrue(b.isPostConstructCalled(), "each prototype instance gets its own post-construct");
    }

    // ---------- identity / metadata ----------

    @Test
    void equalsAndHashCodeKeyedOnDefinitionReferenceOnly() {
        // BeanDefinition equality is reference-only; two factories with same reference are equal.
        BeanFactory<DummyBean> f1 = new BeanFactory<>(bareDef(BeanStrategy.singleton));
        BeanFactory<DummyBean> f2 = new BeanFactory<>(bareDef(BeanStrategy.singleton));
        assertEquals(f1, f2);
        assertEquals(f1.hashCode(), f2.hashCode());
        assertEquals(f1, f1);
        assertNotEquals(f1, null);
        assertNotEquals(f1, "not a factory");

        BeanFactory<DummyBean> differentStrategy = new BeanFactory<>(bareDef(BeanStrategy.prototype));
        assertNotEquals(f1, differentStrategy, "differing strategy in reference => not equal");
    }

    @Test
    void suppliedTypeAndClassMirrorTheDefinitionType() {
        BeanFactory<DummyBean> f = new BeanFactory<>(bareDef(BeanStrategy.singleton));
        assertEquals(DummyBean.class, f.getSuppliedType());
        assertEquals(IClass.getClass(DummyBean.class), f.getSuppliedClass());
    }

    @Test
    void matchesHonoursNameStrategyAndQualifierCriteria() {
        Set<IClass<? extends Annotation>> quals = Set.of(IClass.getClass(DummyBeanQualifier.class));
        BeanDefinition<DummyBean> def = new BeanDefinition<>(
                ref(BeanStrategy.singleton, "theBean", quals), Optional.empty(), Set.of(), Set.of());
        BeanFactory<DummyBean> f = new BeanFactory<>(def);

        // type-only query matches
        assertTrue(f.matches(ref(null, null, null)));
        // matching name matches
        assertTrue(f.matches(ref(null, "theBean", null)));
        // wrong name does not
        assertFalse(f.matches(ref(null, "otherName", null)));
        // wrong strategy does not
        assertFalse(f.matches(ref(BeanStrategy.prototype, null, null)));
        // required qualifier present
        assertTrue(f.matches(ref(null, null, quals)));
        // querying for a type the factory does not produce
        BeanReference<AnotherDummyBean> wrongType = new BeanReference<>(IClass.getClass(AnotherDummyBean.class),
                Optional.empty(), Optional.empty(), Set.of());
        assertFalse(f.matches(wrongType));
    }

    @Test
    void dependenciesEmptyWhenNoConstructorBinderFieldsOrMethods() {
        BeanFactory<DummyBean> f = new BeanFactory<>(bareDef(BeanStrategy.singleton));
        assertTrue(f.dependencies().isEmpty(),
                "a bare definition with no constructor/fields/post-construct has no dependencies");
    }

    // ---------- native entry ----------

    @Test
    void nativeEntryWithoutConstructorBinderUsesDefaultConstructorAndReportsType() throws DslException {
        BeanFactory<DummyBean> f = new BeanFactory<>(bareDef(BeanStrategy.singleton));
        IReflectionConfigurationEntry entry = f.nativeEntry().build();
        // The entry name is the fully-qualified class name; the no-binder branch
        // resolves and registers the default constructor without throwing.
        assertEquals(DummyBean.class.getName(), entry.getName());
    }

    @Test
    void nativeEntryWithConstructorBinderReportsType() throws DslException {
        // A definition with an explicit constructor binder exercises the
        // ifPresent branch of nativeEntry() (constructor taken from the binder).
        IBeanSupplier<DummyBean> supplier = new BeanFactoryBuilder<>(IClass.getClass(DummyBean.class))
                .provide(rb)
                .strategy(BeanStrategy.singleton)
                .constructor()
                .withParam(FixedSupplierBuilder.of("ctor", IClass.getClass(String.class)))
                .up()
                .build();
        BeanFactory<DummyBean> f = (BeanFactory<DummyBean>) supplier;
        IReflectionConfigurationEntry entry = f.nativeEntry().build();
        assertEquals(DummyBean.class.getName(), entry.getName());
    }

    // ---------- supply branch with no strategy declared ----------

    @Test
    void noStrategyDeclaredDefaultsToSingletonBehaviour() throws SupplyException {
        BeanFactory<DummyBean> f = new BeanFactory<>(bareDef(null));
        assertTrue(f.definition().reference().strategy().isEmpty());
        DummyBean a = f.supply().orElseThrow();
        DummyBean b = f.supply().orElseThrow();
        assertSame(a, b, "absent strategy must behave as a singleton");
    }
}
