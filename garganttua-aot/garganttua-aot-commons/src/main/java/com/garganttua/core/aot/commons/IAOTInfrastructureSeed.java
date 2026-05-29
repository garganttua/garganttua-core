package com.garganttua.core.aot.commons;

/**
 * SPI for pre-registering framework-public types in the AOT registry on cold
 * start, beyond what {@code CoreInfrastructureSeed} handles for garganttua-core
 * itself.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader} from
 * {@code META-INF/services/com.garganttua.core.aot.commons.IAOTInfrastructureSeed}.
 * They run after the core seed, once per JVM, in
 * {@link jakarta.annotation.Priority} order (higher first; default 0).</p>
 *
 * <h2>Why this SPI exists</h2>
 * <p>The annotation processor only sees types from the current compilation
 * unit — never from JAR dependencies. So in AOT-only mode (no
 * {@code garganttua-runtime-reflection} on the classpath), framework-public
 * types like {@code IAuthenticatorDefinition} from a higher-layer framework
 * (garganttua-api, garganttua-events, etc.) have no descriptor at runtime
 * and the first {@code IClass.getClass(SomeApiIface.class)} call blows up.
 * Each upstream framework ships its own seed so consumers don't have to
 * boilerplate every public type by hand.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * public class ApiInfrastructureSeed implements IAOTInfrastructureSeed {
 *     public void seed(IAOTSeedContext ctx) {
 *         ctx.registerInterface(IApi.class);
 *         ctx.registerInterface(IDomain.class);
 *         ctx.registerInterface(IAuthenticatorDefinition.class);
 *     }
 * }
 * }</pre>
 *
 * <p>Plus a descriptor at
 * {@code META-INF/services/com.garganttua.core.aot.commons.IAOTInfrastructureSeed}
 * listing the implementation FQN. Optionally annotate the class with
 * {@code @jakarta.annotation.Priority(N)} to order it among other seeds.</p>
 *
 * @since 2.0.0-ALPHA02
 */
public interface IAOTInfrastructureSeed {

    /**
     * Called exactly once per JVM, on first AOT registry use. Must be
     * idempotent in case a future caller re-invokes it; the standard helpers
     * on {@link IAOTSeedContext} already skip duplicates.
     */
    void seed(IAOTSeedContext context);
}
