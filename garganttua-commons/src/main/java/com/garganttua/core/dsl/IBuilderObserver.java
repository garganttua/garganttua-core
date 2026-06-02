package com.garganttua.core.dsl;

/**
 * Callback notified with the object produced by an {@link IObservableBuilder}
 * once it has been built.
 *
 * @param <Builder> the observable builder type
 * @param <Built> the type of object produced by the builder
 * @since 2.0.0-ALPHA01
 * @see IObservableBuilder
 */
@FunctionalInterface
public interface IBuilderObserver<Builder extends IObservableBuilder<Builder, Built>, Built> {

    /**
     * Handles the built object.
     *
     * @param observable the object produced by the builder
     */
    void handle(Built observable);

}
