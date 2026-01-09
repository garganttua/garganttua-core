package com.garganttua.core.dsl;

public interface IObservableBuilder<Builder extends IObservableBuilder<Builder, Built>, Built> extends IBuilder<Built> {

    Builder observer(IBuilderObserver<Builder, Built> observer);

}
