package com.garganttua.core.dsl;

public interface IAutomaticLinkedBuilder<Builder, Link, Built> extends IBuilder<Built> {

    Builder autoDetect(boolean b) throws DslException;

    Link up();

    Builder setUp(Link link);

}
