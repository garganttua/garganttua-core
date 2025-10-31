package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.DiException;
import com.garganttua.injection.IBeanSupplier;
import com.garganttua.injection.beans.BeanFactoryBuilder;
import com.garganttua.injection.beans.BeanStrategy;
import com.garganttua.injection.beans.IBeanFactoryBuilder;
import com.garganttua.injection.supplier.builder.supplier.FixedObjectSupplierBuilder;
import com.garganttua.reflection.GGReflectionException;

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
