package com.garganttua.di.impl.supplier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.garganttua.injection.spec.beans.annotation.Property;
import com.garganttua.injection.spec.beans.annotation.Prototype;
import com.garganttua.injection.spec.beans.annotation.Provider;

import lombok.Getter;

@Singleton
@Named("dummyBeanForTest")
public class DummyBean {

    private String value = "default";

    private boolean postConstructCalled = false;

    @Inject
    @Provider("garganttua")
    @Named("emailService")
    @DummyBeanQualifier
    @Getter
    private DummyOtherBean otherBean;

    @Getter
    private AnotherDummyBean anotherBean;

    @Getter
    private String anotherValue;

    public DummyBean() {
    }

    public DummyBean(@Provider("garganttua") @Property("com.garganttua.dummyPropertyInConstructor") String value) {
        this.value = value;
    }

    @Inject
    public DummyBean(@Provider("garganttua") @Property("com.garganttua.dummyPropertyInConstructor") String value, @Provider("garganttua") @Prototype @Named("AnotherDummyBeanForTest") AnotherDummyBean anotherBean, @Provider("garganttua") @Singleton @Named("emailService") DummyOtherBean otherBean) {
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
