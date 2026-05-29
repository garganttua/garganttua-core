package com.garganttua.core.aot.commons;

/**
 * Helper API given to {@link IAOTInfrastructureSeed} implementations so they
 * can pre-register framework-public types in the AOT registry without
 * constructing {@code AOTClass} instances themselves.
 *
 * <p>Most seeds only need {@link #registerClass(Class)} /
 * {@link #registerInterface(Class)} — they provide type-identity descriptors
 * for the framework wiring resolved at static-init time (no member metadata
 * required). For advanced cases (plugin types with pre-built descriptors),
 * {@link #registry()} exposes the underlying {@link IAOTRegistry} directly.</p>
 *
 * <p>All methods are idempotent: a type already present in the registry is
 * left untouched (first seed wins).</p>
 *
 * @since 2.0.0-ALPHA02
 */
public interface IAOTSeedContext {

    /**
     * Synthesizes a minimal {@code IClass} descriptor for {@code type} and
     * registers it under {@code type.getName()}. No-op if already registered.
     */
    void registerClass(Class<?> type);

    /**
     * Same as {@link #registerClass(Class)} but forces the
     * {@link java.lang.reflect.Modifier#INTERFACE} bit on the synthesized
     * descriptor. Useful when the JVM class-file modifiers don't already
     * carry the interface bit (e.g. type-by-convention markers).
     */
    void registerInterface(Class<?> type);

    /**
     * Underlying registry, for seeds that already have a pre-built descriptor
     * (e.g. from {@link IAOTClassBuilder}).
     */
    IAOTRegistry registry();
}
