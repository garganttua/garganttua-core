package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.DiException;
import com.garganttua.injection.IBeanSupplier;
import com.garganttua.injection.beans.BeanFactoryBuilder;
import com.garganttua.injection.beans.BeanStrategy;
import com.garganttua.injection.beans.IBeanFactoryBuilder;
import com.garganttua.injection.supplier.builder.supplier.FixedObjectSupplierBuilder;

public class BeanFactoryBuilderTest {

    @Test
    public void test() throws DslException, DiException{
        IBeanFactoryBuilder<DummyBean> builder = new BeanFactoryBuilder<>(DummyBean.class);

        IBeanSupplier<DummyBean> beanSupplier = builder
        .strategy(BeanStrategy.singleton)
        .name("aBean")
        .qualifier(DummyBeanQualifier.class)
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
    }

}
