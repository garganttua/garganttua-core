package com.garganttua.core.console;

import com.garganttua.core.aot.annotation.scanner.AOTAnnotationScanner;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.expression.dsl.ExpressionContextBuilder;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.mutex.IMutexManager;
import com.garganttua.core.mutex.dsl.IMutexManagerBuilder;
import com.garganttua.core.mutex.dsl.MutexManagerBuilder;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

/**
 * Builds the injection, mutex and expression contexts used by {@link ScriptConsole}.
 * Pure construction (no console I/O); extracted from {@code ScriptConsole.initializeContext}.
 */
final class ConsoleContextFactory {

    /** Bundle of the contexts the console wires together at startup. */
    record BuiltContexts(IInjectionContextBuilder injectionContextBuilder,
                         IInjectionContext injectionContext,
                         IExpressionContext expressionContext,
                         IMutexManager mutexManager) {
    }

    private ConsoleContextFactory() {
    }

    static BuiltContexts build(boolean useAOT) {
        // Build reflection provider dynamically (avoids compile-time dependency on runtime-reflection)
        IReflectionBuilder reflectionBuilder;
        try {
            @SuppressWarnings("unchecked")
            Class<? extends IReflectionProvider> providerClass =
                    (Class<? extends IReflectionProvider>) Class.forName(
                            "com.garganttua.core.reflection.runtime.RuntimeReflectionProvider");
            reflectionBuilder = ReflectionBuilder.builder()
                    .withProvider(providerClass.getDeclaredConstructor().newInstance());
            if (useAOT) {
                reflectionBuilder
                        .withScanner(new AOTAnnotationScanner(), 20)
                        .withScanner(new ReflectionsAnnotationScanner(), 10);
            } else {
                reflectionBuilder.withScanner(new ReflectionsAnnotationScanner());
            }
            reflectionBuilder.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize reflection provider", e);
        }

        // Create injection context builder
        IInjectionContextBuilder injectionContextBuilder = InjectionContext.builder()
                .provide(reflectionBuilder)
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime");

        // Create mutex manager builder
        IMutexManagerBuilder mutexManagerBuilder = MutexManagerBuilder.builder()
                .withPackage("com.garganttua.core.mutex")
                .autoDetect(true)
                .provide(injectionContextBuilder);

        // Create expression context builder with specific packages for faster scanning
        ExpressionContextBuilder expressionContextBuilder = ExpressionContextBuilder.builder();
        expressionContextBuilder
                .withPackage("com.garganttua.core.expression.functions")
                .withPackage("com.garganttua.core.console")
                .withPackage("com.garganttua.core.script.functions")
                .withPackage("com.garganttua.core.condition")
                .withPackage("com.garganttua.core.injection.functions")
                .withPackage("com.garganttua.core.mutex.functions")
                .autoDetect(true)
                .provide(injectionContextBuilder);

        // Build contexts manually to ensure proper lifecycle
        IInjectionContext injectionContext = injectionContextBuilder.build();
        injectionContext.onInit().onStart();

        IMutexManager mutexManager = mutexManagerBuilder.build();
        IExpressionContext expressionContext = expressionContextBuilder.build();

        return new BuiltContexts(injectionContextBuilder, injectionContext, expressionContext, mutexManager);
    }
}
