package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanSupplier;
import com.garganttua.core.injection.context.dsl.IBeanFactoryBuilder;
import com.garganttua.core.reflection.GGReflectionException;
import com.garganttua.core.supplying.dsl.FixedObjectSupplierBuilder;
import com.garganttua.injection.beans.BeanFactoryBuilder;

public class BeanFactoryBuilderTest {

    @Test
    public void test() throws DslException, DiException, GGReflectionException {
        String random = UUID.randomUUID().toString();
        IBeanFactoryBuilder<DummyBean> builder = new BeanFactoryBuilder<>(DummyBean.class);

        IBeanSupplier<DummyBean> beanSupplier = builder
                .strategy(BeanStrategy.singleton)
                .name("aBean")
                .qualifier(DummyBeanQualifier.class)
                .field(String.class).field("anotherValue").withValue(FixedObjectSupplierBuilder.of(random)).up()
                .constructor()
                .withParam(FixedObjectSupplierBuilder.of("constructedWithParameter"))
                .up()
                .postConstruction().method("markPostConstruct").withReturn(Void.class)
                .up()
                .build();

        assertNotNull(beanSupplier);

        Optional<DummyBean> bean = beanSupplier.getObject();

        assertNotNull(bean);
        assertEquals("constructedWithParameter", bean.get().getValue());
        assertTrue(bean.get().isPostConstructCalled());
        assertEquals(random, bean.get().getAnotherValue());

        assertEquals(1, beanSupplier.getDependencies().size());
    }

}
