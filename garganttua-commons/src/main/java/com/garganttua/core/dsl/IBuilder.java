package com.garganttua.core.dsl;

public interface IBuilder<Built> {

    Built build() throws DslException;

}
