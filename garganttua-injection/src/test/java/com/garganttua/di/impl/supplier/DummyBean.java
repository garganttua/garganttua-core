package com.garganttua.di.impl.supplier;

import com.garganttua.injection.spec.beans.annotation.GGBean;
import com.garganttua.injection.spec.beans.annotation.GGBeanLoadingStrategy;

@GGBean(name = "dummy", strategy = GGBeanLoadingStrategy.newInstance)
public class DummyBean {

    private String value = "default";
    private boolean postConstructCalled = false;

    public DummyBean() {
    }

    public DummyBean(String value) {
        this.value = value;
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

    public void markPostConstruct() {
        this.postConstructCalled = true;
    }
}
