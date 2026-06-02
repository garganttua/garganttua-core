package com.garganttua.core.dsl;

import com.garganttua.core.observability.Logger;
import java.util.Objects;

/**
 * Base class for automatic builders that also hold a link back to a parent builder.
 *
 * <p>
 * Combines {@link AbstractAutomaticBuilder}'s build lifecycle with {@link #up()}
 * parent navigation. Auto-detection is disabled by default for linked builders, as
 * they are typically driven by their parent. The link is mandatory.
 * </p>
 *
 * @param <Builder> the concrete builder type for method chaining
 * @param <Link> the type of the parent builder reachable via {@link #up()}
 * @param <Built> the type of object this builder constructs
 * @see AbstractAutomaticBuilder
 * @see IAutomaticLinkedBuilder
 */
public abstract class AbstractAutomaticLinkedBuilder<Builder, Link, Built> extends AbstractAutomaticBuilder<Builder, Built>
        implements IAutomaticLinkedBuilder<Builder, Link, Built> {
    private static final Logger log = Logger.getLogger(AbstractAutomaticLinkedBuilder.class);

    private Link link;

    /**
     * Creates a linked automatic builder bound to the given parent, with
     * auto-detection initially disabled.
     *
     * @param link the parent builder this builder navigates back to
     * @throws NullPointerException if {@code link} is {@code null}
     */
    protected AbstractAutomaticLinkedBuilder(Link link){
        log.trace("Entering AbstractAutomaticLinkedBuilder constructor with link: {}", link);
        this.link = Objects.requireNonNull(link, "Up cannot be null");
        this.autoDetect = false;
        log.debug("Link set to: {}, autoDetect initialized to false", this.link);
        log.trace("Exiting constructor");
    }

    /**
     * Returns the parent builder this builder is linked to.
     *
     * @return the parent (link) builder
     */
    @Override
    public Link up() {
        log.trace("Entering up() method");
        log.debug("Returning link: {}", this.link);
        log.trace("Exiting up() method");
        return this.link;
    }

    /**
     * Re-parents this builder to a new link.
     *
     * @param up the new parent builder
     * @return this builder for method chaining
     * @throws NullPointerException if {@code up} is {@code null}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Builder setUp(Link up){
        log.trace("Entering setUp() with link: {}", up);
        try {
            this.link = Objects.requireNonNull(up, "Up cannot be null");
            log.debug("Link updated to: {}", this.link);
            log.trace("Exiting setUp()");
            return (Builder) this;
        } catch (NullPointerException e) {
            log.error("setUp() parameter cannot be null", e);
            throw e;
        }
    }
}