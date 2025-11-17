package com.garganttua.core.injection.dummies;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.garganttua.core.injection.annotations.Property;
import com.garganttua.core.injection.annotations.Prototype;
import com.garganttua.core.injection.annotations.Provider;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import lombok.Getter;

@Singleton
@Named("dummyBeanForTest")
public class DummyBean {

    private String value = "default";

    private boolean postConstructCalled = false;

    @Provider("garganttua")
    @Named("emailService")
    @DummyBeanQualifier
    @Getter
    @Nullable
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
    public DummyBean(@Provider("garganttua") @Property("com.garganttua.dummyPropertyInConstructor") String value, @Nullable @Provider("garganttua") @Prototype @Named("AnotherDummyBeanForTest") AnotherDummyBean anotherBean, @Nullable @Provider("garganttua") @Singleton @Named("emailService") DummyOtherBean otherBean) {
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

    @PostConstruct
    public void markPostConstruct() {
        this.postConstructCalled = true;
    }
}
