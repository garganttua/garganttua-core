package com.garganttua.dsl;

import java.util.Objects;

public abstract class AbstractLinkedBuilder <Link, Built>
        implements ILinkedBuilder<Link, Built> {

    private Link link;

    protected AbstractLinkedBuilder(Link link){
        this.link = Objects.requireNonNull(link, "Up cannot be null");
    }

    @Override
    public Link up() {
        return this.link;
    }

}
