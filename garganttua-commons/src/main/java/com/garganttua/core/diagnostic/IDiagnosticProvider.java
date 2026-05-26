package com.garganttua.core.diagnostic;

/**
 * Factory of {@link IDiagnostic} instances. Installed globally via
 * {@link Diagnostics#setProvider(IDiagnosticProvider)}.
 *
 * <p>Implementations decide:
 * <ul>
 *   <li>Where to write (stderr, file, observability event bus, network…)</li>
 *   <li>Which levels to enable (typically read from a system property or
 *       environment variable)</li>
 *   <li>Whether to cache one {@code IDiagnostic} per source class (recommended)</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA02
 */
public interface IDiagnosticProvider {

    /**
     * Obtain (or compute) a diagnostic instance for the given source class.
     * Most callers will cache one per {@link Class}; consumers are expected
     * to store the result in a {@code static final} field.
     */
    IDiagnostic diagnosticFor(Class<?> source);
}
