package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.dummies.AnotherDummyBean;
import com.garganttua.core.injection.dummies.DummyBean;
import com.garganttua.core.injection.dummies.DummyConstructorBinderBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.binders.IConstructorBinder;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

/**
 * Behaviour tests for {@link BeanDefinition}: reference-only equality/hashCode,
 * the {@code toString} delegation, and {@code dependencies()} aggregation across
 * the constructor binder.
 */
public class BeanDefinitionBehaviourTest {

    private IReflectionBuilder rb;

    @BeforeEach
    void setUp() {
        rb = ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider());
        rb.build();
    }

    private BeanReference<DummyBean> ref(BeanStrategy strategy, String name) {
        return new BeanReference<>(IClass.getClass(DummyBean.class),
                Optional.ofNullable(strategy), Optional.ofNullable(name), Set.of());
    }

    @Test
    void equalsAndHashCodeDependOnReferenceOnly() {
        // Identical reference, but one carries a constructor binder and the other not.
        BeanDefinition<DummyBean> a = new BeanDefinition<>(ref(BeanStrategy.singleton, "b"),
                Optional.empty(), Set.of(), Set.of());
        BeanDefinition<DummyBean> b = new BeanDefinition<>(ref(BeanStrategy.singleton, "b"),
                Optional.empty(), Set.of(), Set.of());
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(a, a);
        assertNotEquals(a, null);
        assertNotEquals(a, "string");
    }

    @Test
    void differentReferenceNameMakesDefinitionsUnequal() {
        BeanDefinition<DummyBean> a = new BeanDefinition<>(ref(BeanStrategy.singleton, "x"),
                Optional.empty(), Set.of(), Set.of());
        BeanDefinition<DummyBean> b = new BeanDefinition<>(ref(BeanStrategy.singleton, "y"),
                Optional.empty(), Set.of(), Set.of());
        assertNotEquals(a, b);
    }

    @Test
    void toStringDelegatesToReference() {
        BeanReference<DummyBean> reference = ref(BeanStrategy.singleton, "b");
        BeanDefinition<DummyBean> def = new BeanDefinition<>(reference, Optional.empty(), Set.of(), Set.of());
        assertEquals(reference.toString(), def.toString());
    }

    @Test
    void dependenciesAreEmptyWithNoBinderFieldsOrMethods() {
        BeanDefinition<DummyBean> def = new BeanDefinition<>(ref(BeanStrategy.singleton, "b"),
                Optional.empty(), Set.of(), Set.of());
        assertTrue(def.dependencies().isEmpty());
    }

    @Test
    void dependenciesCollectFromConstructorBinder() throws DslException {
        IConstructorBinder<DummyBean> binder = new DummyConstructorBinderBuilder<DummyBean>(DummyBean.class)
                .provide(rb)
                .withParam("withParam")
                .build();
        BeanDefinition<DummyBean> def = new BeanDefinition<>(ref(BeanStrategy.singleton, "b"),
                Optional.of(binder), Set.of(), Set.of());

        Set<IClass<?>> deps = def.dependencies();
        assertEquals(1, deps.size(), "single-parameter constructor contributes one dependency");
        assertTrue(deps.contains(IClass.getClass(String.class)),
                "the String parameter type must be a declared dependency");
        assertFalse(deps.contains(IClass.getClass(AnotherDummyBean.class)));
    }
}
