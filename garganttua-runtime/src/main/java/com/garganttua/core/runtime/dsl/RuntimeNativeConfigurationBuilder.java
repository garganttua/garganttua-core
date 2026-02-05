package com.garganttua.core.runtime.dsl;

import java.util.HashSet;
import java.util.Set;

import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.nativve.INativeBuilder;
import com.garganttua.core.nativve.INativeReflectionConfiguration;
import com.garganttua.core.nativve.IReflectionConfigurationEntryBuilder;
import com.garganttua.core.nativve.annotations.NativeConfigurationBuilder;
import com.garganttua.core.nativve.image.config.reflection.ReflectConfigEntryBuilder;
import com.garganttua.core.runtime.MethodBinderExpression;
import com.garganttua.core.runtime.Runtime;
import com.garganttua.core.runtime.RuntimeContext;
import com.garganttua.core.runtime.RuntimeContextFactory;
import com.garganttua.core.runtime.RuntimeExpressionContext;
import com.garganttua.core.runtime.RuntimeProcess;
import com.garganttua.core.runtime.RuntimeResult;
import com.garganttua.core.runtime.RuntimeStep;
import com.garganttua.core.runtime.RuntimeStepCatch;
import com.garganttua.core.runtime.RuntimeStepExecutionTools;
import com.garganttua.core.runtime.RuntimeStepFallbackBinder;
import com.garganttua.core.runtime.RuntimeStepMethodBinder;
import com.garganttua.core.runtime.RuntimeStepOnException;
import com.garganttua.core.runtime.RuntimesRegistry;
import com.garganttua.core.runtime.resolver.CodeElementResolver;
import com.garganttua.core.runtime.resolver.ContextElementResolver;
import com.garganttua.core.runtime.resolver.ExceptionElementResolver;
import com.garganttua.core.runtime.resolver.ExceptionMessageElementResolver;
import com.garganttua.core.runtime.resolver.InputElementResolver;
import com.garganttua.core.runtime.resolver.VariableElementResolver;

import lombok.extern.slf4j.Slf4j;

/**
 * Native image configuration builder for the garganttua-runtime module.
 *
 * <p>
 * This builder provides GraalVM native image reflection configuration for all
 * runtime-related classes including:
 * </p>
 * <ul>
 *   <li>Core runtime classes (Runtime, RuntimeContext, RuntimeStep, etc.)</li>
 *   <li>Element resolvers for parameter injection</li>
 *   <li>DSL builder classes</li>
 *   <li>Runtime execution tools</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 */
@Slf4j
@NativeConfigurationBuilder
public class RuntimeNativeConfigurationBuilder
        extends AbstractAutomaticBuilder<RuntimeNativeConfigurationBuilder, INativeReflectionConfiguration>
        implements INativeBuilder<RuntimeNativeConfigurationBuilder, INativeReflectionConfiguration> {

    private final Set<String> packages = new HashSet<>();

    @Override
    public RuntimeNativeConfigurationBuilder withPackages(String[] packageNames) {
        log.atTrace().log("Adding {} packages to runtime native configuration", packageNames.length);
        this.packages.addAll(Set.of(packageNames));
        return this;
    }

    @Override
    public RuntimeNativeConfigurationBuilder withPackage(String packageName) {
        log.atTrace().log("Adding package to runtime native configuration: {}", packageName);
        this.packages.add(packageName);
        return this;
    }

    @Override
    public String[] getPackages() {
        return this.packages.toArray(new String[0]);
    }

    @Override
    protected INativeReflectionConfiguration doBuild() throws DslException {
        log.atTrace().log("Building runtime native configuration");

        Set<IReflectionConfigurationEntryBuilder> entries = new HashSet<>();

        // Core runtime classes
        entries.add(new ReflectConfigEntryBuilder(Runtime.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(RuntimeContext.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(RuntimeContextFactory.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(RuntimeResult.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(RuntimeExpressionContext.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(RuntimeProcess.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(RuntimesRegistry.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        // Step-related classes
        entries.add(new ReflectConfigEntryBuilder(RuntimeStep.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(RuntimeStepMethodBinder.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(RuntimeStepFallbackBinder.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(RuntimeStepExecutionTools.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(RuntimeStepCatch.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(RuntimeStepOnException.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(MethodBinderExpression.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        // Element resolvers (registered via @Resolver annotation)
        entries.add(new ReflectConfigEntryBuilder(InputElementResolver.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(VariableElementResolver.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(ContextElementResolver.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(CodeElementResolver.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(ExceptionElementResolver.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(ExceptionMessageElementResolver.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        // DSL builders
        entries.add(new ReflectConfigEntryBuilder(RuntimeBuilder.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(RuntimesBuilder.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(RuntimeStepBuilder.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(RuntimeStepMethodBuilder.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(RuntimeStepFallbackBuilder.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(RuntimeStepCatchBuilder.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(RuntimeStepOnExceptionBuilder.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        log.atDebug().log("Runtime native configuration built with {} entries", entries.size());

        final Set<IReflectionConfigurationEntryBuilder> finalEntries = entries;
        return () -> finalEntries;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("Auto-detection not required for runtime native configuration");
    }
}
