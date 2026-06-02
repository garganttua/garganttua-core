package com.garganttua.core.injection.functions;

import java.util.Optional;
import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.reflection.IClass;

import jakarta.annotation.Nullable;

import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Bean-mutation expression functions (addBean / addNamedBean / addSingleton) for
 * Garganttua scripts. Split out of {@code InjectionFunctions} to keep each
 * functions class focused; registered the same way (by FQN in
 * {@code ExpressionContextBuilder.FRAMEWORK_FUNCTION_CLASSES} and via the
 * {@code @Expression} annotation index).
 *
 * @since 2.0.0-ALPHA02
 */
@Reflected(queryAllDeclaredMethods = true)
public final class BeanMutationFunctions {
    private static final Logger log = Logger.getLogger(BeanMutationFunctions.class);

    private BeanMutationFunctions() {
        // Utility class
    }

    private static IInjectionContext getContext() {
        IInjectionContext ctx = InjectionContext.context;
        if (ctx == null) {
            throw new ExpressionException("No InjectionContext available. " +
                    "Ensure the injection context is initialized before using injection expressions.");
        }
        return ctx;
    }

    /**
     * Adds a bean to a specific provider.
     *
     * @param providerName the name of the bean provider
     * @param type the bean type (Class)
     * @param bean the bean instance
     * @throws ExpressionException if context is not available or provider not found
     */
    @Expression(name = "addBean", description = "Adds a bean to a specific provider")
    public static void addBean(@Nullable Object providerName, @Nullable Class<?> type, @Nullable Object bean) {
        String provider = providerName == null ? null : providerName.toString();
        log.trace("Entering addBean(provider={}, type={}, bean={})", provider, type, bean);

        if (provider == null || provider.isBlank()) {
            throw new ExpressionException("addBean: provider name cannot be null or blank");
        }
        if (type == null) {
            throw new ExpressionException("addBean: type cannot be null");
        }

        try {
            IInjectionContext ctx = getContext();
            @SuppressWarnings("unchecked")
            BeanReference<Object> ref = new BeanReference<>((IClass<Object>) IClass.getClass(type), Optional.empty(), Optional.empty(), Set.of());
            ctx.addBean(provider, ref, bean);
            log.debug("addBean: bean of type {} added to provider {}", type, provider);
        } catch (DiException e) {
            log.error("addBean failed for provider={}, type={}", provider, type, e);
            throw new ExpressionException("addBean: failed - " + e.getMessage());
        }
    }

    /**
     * Adds a named bean to a specific provider.
     *
     * @param providerName the name of the bean provider
     * @param type the bean type (Class)
     * @param beanName the name to register the bean under
     * @param bean the bean instance
     * @throws ExpressionException if context is not available or provider not found
     */
    @Expression(name = "addNamedBean", description = "Adds a named bean to a specific provider")
    public static void addNamedBean(@Nullable Object providerName, @Nullable Class<?> type, @Nullable Object beanName, @Nullable Object bean) {
        String provider = providerName == null ? null : providerName.toString();
        String name = beanName == null ? null : beanName.toString();
        log.trace("Entering addNamedBean(provider={}, type={}, name={}, bean={})", provider, type, name, bean);

        if (provider == null || provider.isBlank()) {
            throw new ExpressionException("addNamedBean: provider name cannot be null or blank");
        }
        if (type == null) {
            throw new ExpressionException("addNamedBean: type cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new ExpressionException("addNamedBean: bean name cannot be null or blank");
        }

        try {
            IInjectionContext ctx = getContext();
            @SuppressWarnings("unchecked")
            BeanReference<Object> ref = new BeanReference<>((IClass<Object>) IClass.getClass(type), Optional.empty(), Optional.of(name), Set.of());
            ctx.addBean(provider, ref, bean);
            log.debug("addNamedBean: bean '{}' of type {} added to provider {}", name, type, provider);
        } catch (DiException e) {
            log.error("addNamedBean failed for provider={}, type={}, name={}", provider, type, name, e);
            throw new ExpressionException("addNamedBean: failed - " + e.getMessage());
        }
    }

    /**
     * Adds a singleton bean to a specific provider.
     *
     * @param providerName the name of the bean provider
     * @param type the bean type (Class)
     * @param bean the bean instance
     * @throws ExpressionException if context is not available or provider not found
     */
    @Expression(name = "addSingleton", description = "Adds a singleton bean to a specific provider")
    public static void addSingleton(@Nullable Object providerName, @Nullable Class<?> type, @Nullable Object bean) {
        String provider = providerName == null ? null : providerName.toString();
        log.trace("Entering addSingleton(provider={}, type={}, bean={})", provider, type, bean);

        if (provider == null || provider.isBlank()) {
            throw new ExpressionException("addSingleton: provider name cannot be null or blank");
        }
        if (type == null) {
            throw new ExpressionException("addSingleton: type cannot be null");
        }

        try {
            IInjectionContext ctx = getContext();
            @SuppressWarnings("unchecked")
            BeanReference<Object> ref = new BeanReference<>((IClass<Object>) IClass.getClass(type), Optional.of(BeanStrategy.singleton), Optional.empty(), Set.of());
            ctx.addBean(provider, ref, bean);
            log.debug("addSingleton: singleton bean of type {} added to provider {}", type, provider);
        } catch (DiException e) {
            log.error("addSingleton failed for provider={}, type={}", provider, type, e);
            throw new ExpressionException("addSingleton: failed - " + e.getMessage());
        }
    }
}
