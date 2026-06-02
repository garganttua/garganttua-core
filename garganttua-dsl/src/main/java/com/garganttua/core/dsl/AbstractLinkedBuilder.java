package com.garganttua.core.dsl;

import com.garganttua.core.observability.Logger;
import java.util.Objects;

/**
 * Base class for linked builders that hold a reference to a parent (link) builder.
 *
 * <p>
 * Subclasses gain {@link #up()} navigation back to the parent and {@link #setUp(Object)}
 * re-parenting. The link is mandatory and may never be {@code null}.
 * </p>
 *
 * @param <Link> the type of the parent builder reachable via {@link #up()}
 * @param <Built> the type of object the concrete builder constructs
 * @see ILinkedBuilder
 */
public abstract class AbstractLinkedBuilder<Link, Built>
        implements ILinkedBuilder<Link, Built> {
    private static final Logger log = Logger.getLogger(AbstractLinkedBuilder.class);

    private Link link;

    /**
     * Creates a linked builder bound to the given parent.
     *
     * @param link the parent builder this builder navigates back to
     * @throws NullPointerException if {@code link} is {@code null}
     */
    protected AbstractLinkedBuilder(Link link) {
        log.trace("Entering AbstractLinkedBuilder constructor with link: {}", link);
        this.link = Objects.requireNonNull(link, "Up cannot be null");
        log.debug("Link set to: {}", this.link);
        log.trace("Exiting constructor");
    }

    /**
     * Re-parents this builder to a new link.
     *
     * @param up the new parent builder
     * @throws NullPointerException if {@code up} is {@code null}
     */
    @Override
    public void setUp(Link up){
        log.trace("Entering setUp() with link: {}", up);
        try {
            this.link = Objects.requireNonNull(up, "Up cannot be null");
            log.debug("Link updated to: {}", this.link);
            log.trace("Exiting setUp()");
        } catch (NullPointerException e) {
            log.error("setUp() parameter cannot be null", e);
            throw e;
        }
    }

    /**
     * Returns the parent builder this builder is linked to.
     *
     * @return the parent (link) builder
     */
    @Override
    public Link up() {
        log.trace("Entering up()");
        log.debug("Returning link: {}", this.link);
        log.trace("Exiting up()");
        return this.link;
    }
}
