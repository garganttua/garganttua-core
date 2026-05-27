package com.garganttua.core.dsl.dependency;

/**
 * Lifecycle stage at which a dependency is consumed.
 *
 * <p>Each stage has a distinct semantic — the framework picks a different
 * hook on the dependent builder to fire, and provides the dependency under
 * a different form ({@link DependencyKind#BUILDER} or
 * {@link DependencyKind#BUILT}).
 *
 * <h2>Stage timeline</h2>
 * <pre>
 *  Bootstrap.build()
 *     ├─ REGISTRATION    : builders discovered (SPI + manual) and topo-sorted
 *     ├─ CONFIGURATION   : every consumer's CONFIGURATION hook fires globally
 *     │                     — no built object exists yet, kind MUST be BUILDER
 *     ├─ for each B in topological order:
 *     │     ├─ AUTO_DETECT phase
 *     │     ├─ B.doBuild() + lifecycle init/start
 *     │     └─ BUILD phase (pre + post hooks)
 *     └─ summary / banner
 * </pre>
 *
 * <p>The CONFIGURATION stage is the one that fires globally before any
 * builder.build() — it gives consumers a chance to mutate an upstream
 * builder (e.g. register a qualifier, add a resolver, plug a child-context
 * factory) before that upstream builds itself.
 *
 * @since 2.0.0-ALPHA02
 * @see DependencyKind
 * @see DependencySpec
 */
public enum DependencyStage {

    /**
     * Pre-build configuration window. Bootstrap runs this stage globally —
     * across all (consumer, dep) pairs — before any builder.build() fires.
     * Mandates {@link DependencyKind#BUILDER}: the consumer receives the
     * upstream builder and may declare configuration on it.
     *
     * <p>Idempotency contract: hooks at this stage <strong>must</strong>
     * be idempotent. Bootstrap fires each (consumer, dep) pair at most
     * once across the lifetime of the Bootstrap instance — including
     * {@code rebuild()} — but the framework cannot enforce side-effect
     * idempotency, so authors must design their CONFIGURATION hooks to
     * tolerate double-invocation.
     */
    CONFIGURATION,

    /**
     * Consumer's own auto-detect phase. Fires inside
     * {@code consumer.build()} after {@code doAutoDetection()}. The
     * dependency must be available in the kind requested (builder or
     * built) at that moment.
     *
     * <p>For {@link DependencyKind#BUILT}, the upstream must have been
     * built earlier in the topological order — Bootstrap guarantees this.
     */
    AUTO_DETECT,

    /**
     * Consumer's pre-build and post-build phases. Fires before and after
     * {@code consumer.doBuild()}. Same kind options as
     * {@link #AUTO_DETECT}.
     */
    BUILD;
}
