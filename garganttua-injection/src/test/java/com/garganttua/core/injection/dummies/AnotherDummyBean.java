package com.garganttua.core.injection.dummies;

import java.util.UUID;

import javax.inject.Named;

import com.garganttua.core.injection.annotations.Prototype;

@Prototype
@Named("AnotherDummyBeanForTest")
public class AnotherDummyBean {

    private String randomValue;

    public AnotherDummyBean() {
        this.randomValue = UUID.randomUUID().toString();
    }

    public String getRandomValue() {
        return this.randomValue;
    }

}
