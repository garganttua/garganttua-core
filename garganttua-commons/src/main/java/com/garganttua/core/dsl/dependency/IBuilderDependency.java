package com.garganttua.core.dsl.dependency;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.garganttua.core.dsl.IBuilderObserver;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.reflection.IClass;

/**
 * Runtime handle to a single declared dependency, observing the upstream
 * builder and exposing its built result once available.
 *
 * <p>An {@code IBuilderDependency} is the resolved counterpart of a
 * {@link DependencySpec}: it tracks whether the upstream builder has produced
 * its object yet and gives the consumer conditional access to that object.
 *
 * @param <Builder> the upstream observable builder type
 * @param <Built>   the type the upstream builder produces
 * @see DependencySpec
 * @see IDependentBuilder
 */
public interface IBuilderDependency<Builder extends IObservableBuilder<Builder, Built>, Built> extends IBuilderObserver<Builder, Built> {

    /**
     * @return {@code true} once the upstream builder has produced its built
     *         object and it is available via {@link #get()}
     */
    boolean isReady();

    /**
     * @return {@code true} if no upstream builder has been provided for this
     *         dependency
     */
    boolean isEmpty();

    /**
     * @return the {@link IClass} of the upstream builder this dependency tracks
     */
    IClass<Builder> getDependency();

    /**
     * @return the built object produced by the upstream builder
     */
    Built get();

    /**
     * @return the upstream builder instance backing this dependency
     */
    Builder builder();

    /**
     * Asserts that an upstream builder has been provided for this dependency.
     *
     * @throws com.garganttua.core.dsl.DslException if the dependency is empty
     */
    void requireNotEmpty();

    /**
     * Runs the given consumer with the built object only if the dependency is
     * {@linkplain #isReady() ready}.
     *
     * @param consumer action to apply to the built object
     */
    void ifReady(Consumer<Built> consumer);

    /**
     * Runs {@code consumer} with the built object if ready, otherwise runs
     * {@code fallbackAction}.
     *
     * @param consumer       action applied when the dependency is ready
     * @param fallbackAction action run when the dependency is not ready
     */
    void ifReadyOrElse(Consumer<Built> consumer, Runnable fallbackAction);

    /**
     * Runs {@code consumer} with the built object if ready, otherwise throws.
     *
     * @param consumer action applied when the dependency is ready
     */
    void ifReadyOrElseThrow(Consumer<Built> consumer);

    /**
     * Runs {@code consumer} with the built object if ready, otherwise throws
     * the exception produced by {@code exceptionSupplier}.
     *
     * @param consumer          action applied when the dependency is ready
     * @param exceptionSupplier supplies the exception to throw when not ready
     * @param <X>               the type of exception thrown when not ready
     * @throws X if the dependency is not ready
     */
    <X extends Throwable> void ifReadyOrElseThrow(
            Consumer<Built> consumer,
            Supplier<? extends X> exceptionSupplier) throws X;

    /**
     * Pushes the scan packages discovered from the upstream context to the
     * given consumer, allowing the dependent builder to share scan scope.
     *
     * @param packageConsumer receives the set of package names from the context
     */
    void synchronizePackagesFromContext(Consumer<Set<String>> packageConsumer);

}
