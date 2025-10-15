package com.garganttua.dsl;

public interface ILinkedBuilder<Link, Built> extends IBuilder<Built> {

    Link up();

}
