package com.garganttua.di.impl.supplier;

import java.util.UUID;

import javax.inject.Named;

import com.garganttua.core.injection.annotations.Prototype;

import lombok.Getter;

@Prototype
@Named("AnotherDummyBeanForTest")
public class AnotherDummyBean {

    @Getter
    private String randomValue;

    public AnotherDummyBean() {
        this.randomValue = UUID.randomUUID().toString();
    }

}
