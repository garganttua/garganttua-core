package com.garganttua.core.dsl;

public interface ILinkedBuilder<Link, Built> extends IBuilder<Built> {

    Link up();

    void setUp(Link up);

}
