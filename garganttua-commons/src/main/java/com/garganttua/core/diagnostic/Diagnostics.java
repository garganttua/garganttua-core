package com.garganttua.core.diagnostic;

import java.util.Objects;

/**
 * Global entry point for obtaining {@link IDiagnostic} instances.
 *
 * <p>Initialised lazily with {@link ConsoleDiagnosticProvider} so any code path
 * that calls {@link #of(Class)} works out of the box on a cold JVM. Apps
 * (tests, native-image builds, observability-routed deployments…) can install
 * a different {@link IDiagnosticProvider} via {@link #setProvider}.
 *
 * <h2>Typical class-level usage</h2>
 * <pre>{@code
 * import com.garganttua.core.diagnostic.Diagnostics;
 * import com.garganttua.core.diagnostic.IDiagnostic;
 *
 * public class MyService {
 *     private static final IDiagnostic log = Diagnostics.of(MyService.class);
 *
 *     public void foo() {
 *         log.debug("starting foo with {}", arg);
 *     }
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA02
 */
public final class Diagnostics {

    private static volatile IDiagnosticProvider provider = new ConsoleDiagnosticProvider();

    private Diagnostics() {
    }

    /**
     * @return a diagnostic instance bound to the given source class.
     */
    public static IDiagnostic of(Class<?> source) {
        return provider.diagnosticFor(source);
    }

    /**
     * Install a global diagnostic provider. Call this at startup before any
     * other code obtains an {@link IDiagnostic} — diagnostics held in
     * {@code static final} fields by other classes won't see a later provider
     * swap.
     */
    public static void setProvider(IDiagnosticProvider provider) {
        Diagnostics.provider = Objects.requireNonNull(provider, "provider");
    }

    /**
     * @return the currently-installed provider.
     */
    public static IDiagnosticProvider getProvider() {
        return provider;
    }
}
