package com.garganttua.core.dsl.dependency;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.garganttua.core.dsl.IBuilderObserver;
import com.garganttua.core.dsl.IObservableBuilder;

public interface IBuilderDependency<Builder extends IObservableBuilder<Builder, Built>, Built> extends IBuilderObserver<Builder, Built> {

    boolean isReady();

    boolean isEmpty();

    Class<Builder> getDependency();

    Built get();

    Builder builder();

    void requireNotEmpty();

    void ifReady(Consumer<Built> consumer);

    void ifReadyOrElse(Consumer<Built> consumer, Runnable fallbackAction);

    void ifReadyOrElseThrow(Consumer<Built> consumer);

    <X extends Throwable> void ifReadyOrElseThrow(
            Consumer<Built> consumer,
            Supplier<? extends X> exceptionSupplier) throws X;

    void synchronizePackagesFromContext(Consumer<Set<String>> packageConsumer);

}
