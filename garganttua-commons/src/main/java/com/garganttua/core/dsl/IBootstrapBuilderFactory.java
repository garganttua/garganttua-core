package com.garganttua.core.dsl;

/**
 * SPI factory for {@link IBuilder} instances that Bootstrap auto-discovers and
 * registers during cold-start.
 *
 * <p>Each garganttua-core module that ships a "standard" builder (e.g.
 * {@code InjectionContextBuilder}, {@code ExpressionContextBuilder}) provides
 * an implementation of this interface and declares it in
 * {@code META-INF/services/com.garganttua.core.dsl.IBootstrapBuilderFactory}.
 * {@code Bootstrap.builder().autoDetect(true).build()} then instantiates them
 * automatically — no need for the user to call
 * {@code .withBuilder(new InjectionContextBuilder())} explicitly.
 *
 * <p>The factory layer (rather than registering builder classes directly via
 * ServiceLoader) exists because:
 * <ul>
 *   <li>Some builder constructors are protected or throw checked exceptions
 *       — incompatible with {@link java.util.ServiceLoader} requirements.</li>
 *   <li>It lets each module's factory call its own static {@code builder()}
 *       method, preserving any setup logic that the public factory does.</li>
 * </ul>
 *
 * <p>If the user explicitly registers a builder of the same concrete class via
 * {@code Bootstrap.withBuilder(...)}, the SPI-loaded one is skipped (the user's
 * explicit registration always wins).
 *
 * @since 2.0.0-ALPHA02
 */
@FunctionalInterface
public interface IBootstrapBuilderFactory {

    /**
     * Create a fresh builder instance to register with the current Bootstrap.
     *
     * @return a freshly constructed builder
     * @throws DslException if the underlying builder constructor fails
     */
    IBuilder<?> create() throws DslException;
}
