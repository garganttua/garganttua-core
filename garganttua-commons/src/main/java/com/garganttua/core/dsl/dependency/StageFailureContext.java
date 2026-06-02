package com.garganttua.core.dsl.dependency;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.reflection.IClass;

/**
 * Structured context attached to a {@link com.garganttua.core.dsl.DslException
 * DslException} when a dependency stage fires fails. Replaces the previous
 * opaque {@code "Required dependency X not provided"} message with a rich
 * snapshot of <em>what</em> failed, <em>when</em>, and <em>between whom</em>.
 *
 * <p>Use {@link #describe()} to render a human-readable single-line summary
 * for logs / banners; structured access via the record components allows
 * tools / diagnostics observers to consume the data programmatically.
 *
 * @param consumerClass the class of the dependent builder consuming the
 *                      dependency (may be {@code null} when the failure is
 *                      surfaced outside a hook invocation)
 * @param depClass      the class of the upstream builder that wasn't ready
 * @param stage         the lifecycle stage being processed when the failure
 *                      occurred
 * @param kind          whether the consumer requested the upstream BUILDER
 *                      or BUILT
 * @param reason        short label of the failure
 *                      ({@code "not provided"}, {@code "provided but not built"},
 *                      {@code "hook threw"})
 * @param cause         the underlying throwable if any
 * @since 2.0.0-ALPHA02
 */
public record StageFailureContext(
        IClass<?> consumerClass,
        IClass<? extends IObservableBuilder<?, ?>> depClass,
        DependencyStage stage,
        DependencyKind kind,
        String reason,
        Throwable cause) {

    /**
     * Canonical constructor. Validates that {@code depClass}, {@code stage},
     * {@code kind} and {@code reason} are non-null; {@code consumerClass} and
     * {@code cause} are allowed to be {@code null}.
     */
    public StageFailureContext {
        Objects.requireNonNull(depClass, "depClass");
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(reason, "reason");
    }

    /**
     * One-line human-readable rendering, intended for logs.
     */
    public String describe() {
        StringBuilder sb = new StringBuilder()
                .append("[stage=").append(stage)
                .append(", kind=").append(kind)
                .append(", dep=").append(depClass.getSimpleName());
        if (consumerClass != null) {
            sb.append(", consumer=").append(consumerClass.getSimpleName());
        }
        sb.append(", reason=").append(reason);
        if (cause != null) {
            sb.append(", cause=").append(cause.getClass().getSimpleName())
              .append(": ").append(cause.getMessage());
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * @return the failure {@link #cause()} wrapped in an {@link Optional},
     *         empty when no cause was recorded
     */
    public Optional<Throwable> causeOpt() {
        return Optional.ofNullable(cause);
    }
}
