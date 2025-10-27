package com.garganttua.di.impl.supplier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.garganttua.injection.spec.beans.annotation.Property;
import com.garganttua.injection.spec.beans.annotation.Provider;

@Singleton
@Named("dummyBean")
public class DummyBean {

    private String value = "default";
    private boolean postConstructCalled = false;

    @Inject
    @Provider("garganttua")
    @Named("emailService")
    @DummyBeanQualifier
    private DummyOtherBean otherBean;

    private AnotherDummyBean anotherBean;

    public DummyBean() {
    }

    @Inject
    public DummyBean(@Provider("dummy") @Property("com.garganttua.dummyPropertyInConstructor") String value) {
        this.value = value;
    }

    @Inject
    public DummyBean(String value, @Singleton AnotherDummyBean anotherBean) {
        this.value = value;
        this.anotherBean = anotherBean;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String v) {
        this.value = v;
    }

    public boolean isPostConstructCalled() {
        return postConstructCalled;
    }

    @Inject
    public void markPostConstruct() {
        this.postConstructCalled = true;
    }
}
