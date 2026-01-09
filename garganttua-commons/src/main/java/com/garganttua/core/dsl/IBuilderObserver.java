package com.garganttua.core.dsl;

@FunctionalInterface
public interface IBuilderObserver<Builder extends IObservableBuilder<Builder, Built>, Built> {

    void handle(Built observable);

}
