package com.garganttua.dsl;

public interface IAutomaticBuilder<Builder, Built> extends IBuilder<Built> {

    Builder autoDetect(boolean b) throws DslException;

}
