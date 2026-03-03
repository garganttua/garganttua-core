package com.garganttua.core.reflection.dsl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.garganttua.core.bootstrap.annotations.Bootstrap;
import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilderObserver;
import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.IReflectionProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Bootstrap
public class ReflectionBuilder extends AbstractAutomaticBuilder<IReflectionBuilder, IReflection>
        implements IReflectionBuilder {

    private static final int DEFAULT_PRIORITY = 10;

    private final List<PrioritizedProvider> providers = new ArrayList<>();
    private final List<PrioritizedScanner> scanners = new ArrayList<>();
    private final Set<IBuilderObserver<IReflectionBuilder, IReflection>> observers = new HashSet<>();

    public static IReflectionBuilder builder() {
        return new ReflectionBuilder();
    }

    @Override
    public IReflectionBuilder withProvider(IReflectionProvider provider) {
        return withProvider(provider, DEFAULT_PRIORITY);
    }

    @Override
    public IReflectionBuilder withScanner(IAnnotationScanner scanner) {
        return withScanner(scanner, DEFAULT_PRIORITY);
    }

    @Override
    public IReflectionBuilder withProvider(IReflectionProvider provider, int priority) {
        log.atDebug().log("Adding reflection provider {} with priority {}", provider.getClass().getName(), priority);
        this.providers.add(new PrioritizedProvider(
                Objects.requireNonNull(provider, "Provider cannot be null"), priority));
        return this;
    }

    @Override
    public IReflectionBuilder withScanner(IAnnotationScanner scanner, int priority) {
        log.atDebug().log("Adding annotation scanner {} with priority {}", scanner.getClass().getName(), priority);
        this.scanners.add(new PrioritizedScanner(
                Objects.requireNonNull(scanner, "Scanner cannot be null"), priority));
        return this;
    }

    @Override
    public IReflectionBuilder observer(IBuilderObserver<IReflectionBuilder, IReflection> observer) {
        Objects.requireNonNull(observer, "Observer cannot be null");
        this.observers.add(observer);
        if (this.built != null) {
            observer.handle(this.built);
        }
        return this;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atDebug().log("ReflectionBuilder auto-detection (no-op for now)");
    }

    @Override
    protected IReflection doBuild() throws DslException {
        log.atDebug().log("Building IReflection with {} providers and {} scanners",
                providers.size(), scanners.size());

        List<IReflectionProvider> sortedProviders = providers.stream()
                .sorted(Comparator.comparingInt(PrioritizedProvider::priority).reversed())
                .map(PrioritizedProvider::provider)
                .toList();

        List<IAnnotationScanner> sortedScanners = scanners.stream()
                .sorted(Comparator.comparingInt(PrioritizedScanner::priority).reversed())
                .map(PrioritizedScanner::scanner)
                .toList();

        IReflection reflection = new CompositeReflection(sortedProviders, sortedScanners);
        IClass.setReflection(reflection);
        log.atDebug().log("IReflection built successfully");

        this.observers.forEach(o -> o.handle(reflection));

        return reflection;
    }

    private record PrioritizedProvider(IReflectionProvider provider, int priority) {}
    private record PrioritizedScanner(IAnnotationScanner scanner, int priority) {}
}
