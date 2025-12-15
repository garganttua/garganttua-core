package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanSupplier;
import com.garganttua.core.injection.context.dsl.BeanFactoryBuilder;
import com.garganttua.core.injection.context.dsl.IBeanFactoryBuilder;
import com.garganttua.core.injection.dummies.DummyBean;
import com.garganttua.core.injection.dummies.DummyBeanQualifier;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.supply.SupplyException;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;

public class BeanFactoryBuilderTest {

    @Test
    public void test() throws DslException, DiException, ReflectionException, SupplyException {
        String random = UUID.randomUUID().toString();
        IBeanFactoryBuilder<DummyBean> builder = new BeanFactoryBuilder<>(DummyBean.class);

        IBeanSupplier<DummyBean> beanSupplier = builder
                .strategy(BeanStrategy.singleton)
                .name("aBean")
                .qualifier(DummyBeanQualifier.class)
                .field(String.class).field("anotherValue").withValue(FixedSupplierBuilder.of(random)).up()
                .constructor()
                .withParam(FixedSupplierBuilder.of("constructedWithParameter"))
                .up()
                .postConstruction().method("markPostConstruct").withReturn(Void.class)
                .up()
                .build();

        assertNotNull(beanSupplier);

        Optional<DummyBean> bean = beanSupplier.supply();

        assertNotNull(bean);
        assertEquals("constructedWithParameter", bean.get().getValue());
        assertTrue(bean.get().isPostConstructCalled());
        assertEquals(random, bean.get().getAnotherValue());

        assertEquals(1, beanSupplier.dependencies().size());
    }

}
