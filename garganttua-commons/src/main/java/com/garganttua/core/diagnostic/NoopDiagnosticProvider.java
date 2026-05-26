package com.garganttua.core.diagnostic;

/**
 * {@link IDiagnosticProvider} that silently discards every log call. Useful
 * for tests that want to assert "nothing was logged" or for production
 * deployments where observability events alone are the source of truth.
 *
 * @since 2.0.0-ALPHA02
 */
public final class NoopDiagnosticProvider implements IDiagnosticProvider {

    public static final NoopDiagnosticProvider INSTANCE = new NoopDiagnosticProvider();

    /** Shared {@link IDiagnostic} singleton — stateless. */
    public static final IDiagnostic NOOP = new IDiagnostic() {
        @Override
        public boolean isEnabled(Level level) {
            return false;
        }

        @Override
        public void log(Level level, String format, Object... args) {
            // intentionally empty
        }
    };

    @Override
    public IDiagnostic diagnosticFor(Class<?> source) {
        return NOOP;
    }
}
