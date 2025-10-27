package com.garganttua.di.impl.supplier;

import java.util.UUID;

import com.garganttua.injection.spec.beans.annotation.Prototype;

import lombok.Getter;

@Prototype
public class AnotherDummyBean {

    @Getter
    private String randomValue;

    public AnotherDummyBean() {
        this.randomValue = UUID.randomUUID().toString();;   
    }

}
