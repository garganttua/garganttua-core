package com.garganttua.core.dsl;

/**
 * Builder that notifies registered observers with the object it produces.
 *
 * @param <Builder> the concrete builder type for method chaining
 * @param <Built> the type of object this builder constructs
 * @since 2.0.0-ALPHA01
 * @see IBuilderObserver
 */
public interface IObservableBuilder<Builder extends IObservableBuilder<Builder, Built>, Built> extends IBuilder<Built> {

    /**
     * Registers an observer to be notified with the built object.
     *
     * @param observer the observer to register
     * @return this builder for method chaining
     */
    Builder observer(IBuilderObserver<Builder, Built> observer);

}
