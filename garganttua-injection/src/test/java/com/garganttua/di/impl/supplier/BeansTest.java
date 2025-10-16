package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.injection.Beans;
import com.garganttua.injection.DiContextBuilder;
import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.IBeanScope;
import com.garganttua.injection.spec.IDiChildContextFactory;
import com.garganttua.injection.spec.IDiContextBuilder;
import com.garganttua.injection.spec.IPropertyScope;

public class BeansTest {

    private IDiContextBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new DiContextBuilder();
    }

    @Test
    public void contextNotBuiltShouldThrowException() {
        assertThrows(NullPointerException.class, () -> Beans.bean(DummyBean.class).build().getObject());
    }

    @Test
    public void contextNotStartedShouldThrowException() {
        IDiChildContextFactory<DummyChildContext> factory = new DummyChildContextFactory();
        IBeanScope beans = new DummyBeanScope("scope1");
        IPropertyScope properties = new DummyPropertyScope("scope1");
        builder.childContextFactory(factory);
        builder.beanScope(beans);
        builder.propertyScope(properties);

        assertDoesNotThrow(builder::build);

        assertThrows(DiException.class, () -> Beans.bean(DummyBean.class).build().getObject());
    }
}
