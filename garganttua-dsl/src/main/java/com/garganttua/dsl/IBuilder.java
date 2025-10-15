package com.garganttua.dsl;

public interface IBuilder<Built> {

    Built build() throws DslException;

}
