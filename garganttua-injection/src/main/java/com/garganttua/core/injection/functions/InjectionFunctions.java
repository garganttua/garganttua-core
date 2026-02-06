package com.garganttua.core.injection.functions;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.Pair;
import com.garganttua.core.injection.context.InjectionContext;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

/**
 * Expression functions for interacting with the dependency injection context.
 *
 * <p>
 * This class provides @Expression annotated functions for querying beans,
 * properties, and managing the injection context from Garganttua scripts.
 * </p>
 *
 * <h2>Usage Examples (in .gs script)</h2>
 * <pre>
 * # Get a bean by type
 * service <- getBean(com.example.MyService.class)
 *
 * # Get a bean by reference string
 * service <- getBean("com.example.MyService#primary")
 *
 * # Check if a bean exists
 * exists <- hasBean("com.example.MyService")
 *
 * # Get a property
 * dbUrl <- getProperty("db.url", String.class)
 *
 * # List all bean providers
 * providers <- beanProviders()
 *
 * # Count total beans
 * count <- beanCount()
 * </pre>
 *
 * <h2>Bean Reference Format</h2>
 * <pre>
 * [provider::][class][!strategy][#name][@qualifier]
 * </pre>
 * Examples:
 * <ul>
 *   <li>{@code com.example.MyService} - by class</li>
 *   <li>{@code MyService#primary} - by class and name</li>
 *   <li>{@code MyService!singleton} - by class and strategy</li>
 *   <li>{@code provider::MyService@qualifier} - full reference</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see InjectionContext
 * @see IInjectionContext
 * @see BeanReference
 */
@Slf4j
public final class InjectionFunctions {

    private InjectionFunctions() {
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

    // ==================== Bean Query Functions ====================

    /**
     * Gets a bean from the injection context by its type.
     *
     * @param type the class type of the bean to retrieve
     * @return the bean instance, or null if not found
     * @throws ExpressionException if context is not available or query fails
     */
    @Expression(name = "getBean", description = "Gets a bean from the injection context by type")
    public static Object getBean(@Nullable Class<?> type) {
        log.atTrace().log("Entering getBean(type={})", type);

        if (type == null) {
            throw new ExpressionException("getBean: type cannot be null");
        }

        try {
            IInjectionContext ctx = getContext();
            BeanReference<?> ref = new BeanReference<>(type, Optional.empty(), Optional.empty(), Set.of());
            Optional<?> bean = ctx.queryBean(ref);
            Object result = bean.orElse(null);
            log.atDebug().log("getBean result: {}", result);
            return result;
        } catch (DiException e) {
            log.atError().log("getBean failed for type {}", type, e);
            throw new ExpressionException("getBean: failed to query bean - " + e.getMessage());
        }
    }

    /**
     * Gets a bean from the injection context by reference string.
     *
     * <p>Reference format: [provider::][class][!strategy][#name][@qualifier]</p>
     *
     * @param reference the bean reference string
     * @return the bean instance, or null if not found
     * @throws ExpressionException if context is not available or query fails
     */
    @Expression(name = "getBeanByRef", description = "Gets a bean by reference string (e.g., 'MyService#name' or 'provider::MyService!singleton')")
    public static Object getBeanByRef(@Nullable Object reference) {
        String refStr = reference == null ? null : reference.toString();
        log.atTrace().log("Entering getBeanByRef(reference={})", refStr);

        if (refStr == null || refStr.isBlank()) {
            throw new ExpressionException("getBeanByRef: reference cannot be null or blank");
        }

        try {
            IInjectionContext ctx = getContext();
            Pair<Optional<String>, BeanReference<?>> parsed = BeanReference.parse(refStr);
            Optional<String> provider = parsed.value1();
            BeanReference<?> beanRef = parsed.value2();

            Optional<?> bean = ctx.queryBean(provider, beanRef);
            Object result = bean.orElse(null);
            log.atDebug().log("getBeanByRef result: {}", result);
            return result;
        } catch (DiException e) {
            log.atError().log("getBeanByRef failed for reference {}", refStr, e);
            throw new ExpressionException("getBeanByRef: failed to query bean - " + e.getMessage());
        }
    }

    /**
     * Gets all beans matching the specified type.
     *
     * @param type the class type of beans to retrieve
     * @return a list of matching beans (never null, may be empty)
     * @throws ExpressionException if context is not available or query fails
     */
    @Expression(name = "getBeans", description = "Gets all beans of a specific type from the injection context")
    public static List<?> getBeans(@Nullable Class<?> type) {
        log.atTrace().log("Entering getBeans(type={})", type);

        if (type == null) {
            throw new ExpressionException("getBeans: type cannot be null");
        }

        try {
            IInjectionContext ctx = getContext();
            BeanReference<?> ref = new BeanReference<>(type, Optional.empty(), Optional.empty(), Set.of());
            List<?> beans = ctx.queryBeans(ref);
            log.atDebug().log("getBeans found {} beans of type {}", beans.size(), type);
            return beans;
        } catch (DiException e) {
            log.atError().log("getBeans failed for type {}", type, e);
            throw new ExpressionException("getBeans: failed to query beans - " + e.getMessage());
        }
    }

    /**
     * Checks if a bean exists in the injection context.
     *
     * @param reference the bean reference (can be a Class or reference string)
     * @return true if the bean exists, false otherwise
     * @throws ExpressionException if context is not available
     */
    @Expression(name = "hasBean", description = "Checks if a bean exists in the injection context")
    public static boolean hasBean(@Nullable Object reference) {
        log.atTrace().log("Entering hasBean(reference={})", reference);

        if (reference == null) {
            return false;
        }

        try {
            if (reference instanceof Class<?> type) {
                return getBean(type) != null;
            } else {
                return getBeanByRef(reference) != null;
            }
        } catch (ExpressionException e) {
            log.atDebug().log("hasBean returned false due to: {}", e.getMessage());
            return false;
        }
    }

    // ==================== Bean Provider Functions ====================

    /**
     * Returns the number of bean providers in the context.
     *
     * @return the number of bean providers
     * @throws ExpressionException if context is not available
     */
    @Expression(name = "beanProviderCount", description = "Returns the number of bean providers")
    public static int beanProviderCount() {
        log.atTrace().log("Entering beanProviderCount()");

        try {
            IInjectionContext ctx = getContext();
            Set<IBeanProvider> providers = ctx.getBeanProviders();
            int count = providers.size();
            log.atDebug().log("beanProviderCount: {}", count);
            return count;
        } catch (DiException e) {
            log.atError().log("beanProviderCount failed", e);
            throw new ExpressionException("beanProviderCount: failed - " + e.getMessage());
        }
    }

    /**
     * Returns the total number of bean definitions across all providers.
     * Note: This counts bean definitions, not instantiated beans.
     *
     * @return the total bean definition count
     * @throws ExpressionException if context is not available
     */
    @Expression(name = "beanCount", description = "Returns the total number of beans in the injection context")
    public static int beanCount() {
        log.atTrace().log("Entering beanCount()");

        try {
            IInjectionContext ctx = getContext();
            // Use size() to count bean definitions without instantiating them
            int count = ctx.getBeanProviders().stream()
                    .mapToInt(IBeanProvider::size)
                    .sum();
            log.atDebug().log("beanCount: {}", count);
            return count;
        } catch (DiException e) {
            log.atError().log("beanCount failed", e);
            throw new ExpressionException("beanCount: failed - " + e.getMessage());
        }
    }

    /**
     * Returns the number of bean definitions in a specific provider.
     * Note: This counts bean definitions, not instantiated beans.
     *
     * @param providerName the name of the bean provider
     * @return the bean definition count for that provider
     * @throws ExpressionException if context is not available or provider not found
     */
    @Expression(name = "beanCountInProvider", description = "Returns the number of beans in a specific provider")
    public static int beanCountInProvider(@Nullable Object providerName) {
        String name = providerName == null ? null : providerName.toString();
        log.atTrace().log("Entering beanCountInProvider(providerName={})", name);

        if (name == null || name.isBlank()) {
            throw new ExpressionException("beanCountInProvider: provider name cannot be null or blank");
        }

        try {
            IInjectionContext ctx = getContext();
            // Use size() to count bean definitions without instantiating them
            Optional<IBeanProvider> provider = ctx.getBeanProvider(name);
            if (provider.isEmpty()) {
                throw new ExpressionException("beanCountInProvider: provider not found - " + name);
            }
            int count = provider.get().size();
            log.atDebug().log("beanCountInProvider({}): {}", name, count);
            return count;
        } catch (DiException e) {
            log.atError().log("beanCountInProvider failed for provider {}", name, e);
            throw new ExpressionException("beanCountInProvider: failed - " + e.getMessage());
        }
    }

    // ==================== Property Functions ====================

    /**
     * Gets a property value from the injection context.
     *
     * @param key the property key
     * @param type the expected type of the property value
     * @return the property value, or null if not found
     * @throws ExpressionException if context is not available
     */
    @Expression(name = "getProperty", description = "Gets a property value by key and type")
    public static Object getProperty(@Nullable Object key, @Nullable Class<?> type) {
        String keyStr = key == null ? null : key.toString();
        log.atTrace().log("Entering getProperty(key={}, type={})", keyStr, type);

        if (keyStr == null || keyStr.isBlank()) {
            throw new ExpressionException("getProperty: key cannot be null or blank");
        }
        if (type == null) {
            type = String.class;
        }

        try {
            IInjectionContext ctx = getContext();
            Optional<?> value = ctx.getProperty(keyStr, type);
            Object result = value.orElse(null);
            log.atDebug().log("getProperty result: {}", result);
            return result;
        } catch (DiException e) {
            log.atError().log("getProperty failed for key {}", keyStr, e);
            throw new ExpressionException("getProperty: failed - " + e.getMessage());
        }
    }

    /**
     * Gets a property value as String from the injection context.
     *
     * @param key the property key
     * @return the property value as String, or null if not found
     * @throws ExpressionException if context is not available
     */
    @Expression(name = "getPropertyString", description = "Gets a property value as String")
    public static String getPropertyString(@Nullable Object key) {
        Object value = getProperty(key, String.class);
        return value != null ? value.toString() : null;
    }

    /**
     * Sets a property value in a specific provider.
     *
     * @param providerName the name of the property provider
     * @param key the property key
     * @param value the value to set
     * @throws ExpressionException if context is not available or provider is immutable
     */
    @Expression(name = "setProperty", description = "Sets a property value in a specific provider")
    public static void setProperty(@Nullable Object providerName, @Nullable Object key, @Nullable Object value) {
        String provider = providerName == null ? null : providerName.toString();
        String keyStr = key == null ? null : key.toString();
        log.atTrace().log("Entering setProperty(provider={}, key={}, value={})", provider, keyStr, value);

        if (provider == null || provider.isBlank()) {
            throw new ExpressionException("setProperty: provider name cannot be null or blank");
        }
        if (keyStr == null || keyStr.isBlank()) {
            throw new ExpressionException("setProperty: key cannot be null or blank");
        }
        if (value == null) {
            throw new ExpressionException("setProperty: value cannot be null");
        }

        try {
            IInjectionContext ctx = getContext();
            ctx.setProperty(provider, keyStr, value);
            log.atDebug().log("setProperty: {}={} set in provider {}", keyStr, value, provider);
        } catch (DiException e) {
            log.atError().log("setProperty failed for provider={}, key={}", provider, keyStr, e);
            throw new ExpressionException("setProperty: failed - " + e.getMessage());
        }
    }

    /**
     * Checks if a property exists in the injection context.
     *
     * @param key the property key
     * @return true if the property exists, false otherwise
     */
    @Expression(name = "hasProperty", description = "Checks if a property exists")
    public static boolean hasProperty(@Nullable Object key) {
        String keyStr = key == null ? null : key.toString();
        log.atTrace().log("Entering hasProperty(key={})", keyStr);

        if (keyStr == null || keyStr.isBlank()) {
            return false;
        }

        try {
            IInjectionContext ctx = getContext();
            boolean exists = ctx.getProperty(keyStr, Object.class).isPresent();
            log.atDebug().log("hasProperty({}): {}", keyStr, exists);
            return exists;
        } catch (Exception e) {
            log.atDebug().log("hasProperty returned false due to: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Returns the number of property providers in the context.
     *
     * @return the number of property providers
     * @throws ExpressionException if context is not available
     */
    @Expression(name = "propertyProviderCount", description = "Returns the number of property providers")
    public static int propertyProviderCount() {
        log.atTrace().log("Entering propertyProviderCount()");

        try {
            IInjectionContext ctx = getContext();
            Set<IPropertyProvider> providers = ctx.getPropertyProviders();
            int count = providers.size();
            log.atDebug().log("propertyProviderCount: {}", count);
            return count;
        } catch (DiException e) {
            log.atError().log("propertyProviderCount failed", e);
            throw new ExpressionException("propertyProviderCount: failed - " + e.getMessage());
        }
    }

    // ==================== Context Information Functions ====================

    /**
     * Returns information about the injection context.
     *
     * @return a formatted string with context information
     */
    @Expression(name = "injectionInfo", description = "Returns summary information about the injection context")
    public static String injectionInfo() {
        log.atTrace().log("Entering injectionInfo()");

        try {
            IInjectionContext ctx = getContext();
            StringBuilder sb = new StringBuilder();
            sb.append("Injection Context Information\n");
            sb.append("=============================\n");

            Set<IBeanProvider> beanProviders = ctx.getBeanProviders();
            sb.append("Bean Providers: ").append(beanProviders.size()).append("\n");
            int totalBeans = 0;
            for (IBeanProvider provider : beanProviders) {
                BeanReference<Object> ref = new BeanReference<>(Object.class, Optional.empty(), Optional.empty(), Set.of());
                int count = provider.queries(ref).size();
                totalBeans += count;
                sb.append("  - Provider (").append(count).append(" beans, ")
                        .append(provider.isMutable() ? "mutable" : "immutable")
                        .append(")\n");
            }
            sb.append("Total Beans: ").append(totalBeans).append("\n");

            Set<IPropertyProvider> propertyProviders = ctx.getPropertyProviders();
            sb.append("Property Providers: ").append(propertyProviders.size()).append("\n");
            for (IPropertyProvider provider : propertyProviders) {
                sb.append("  - Provider (")
                        .append(provider.keys().size()).append(" properties, ")
                        .append(provider.isMutable() ? "mutable" : "immutable")
                        .append(")\n");
            }

            String info = sb.toString();
            log.atDebug().log("injectionInfo:\n{}", info);
            return info;
        } catch (DiException e) {
            log.atError().log("injectionInfo failed", e);
            throw new ExpressionException("injectionInfo: failed - " + e.getMessage());
        }
    }

    // ==================== Bean Addition Functions ====================

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
        log.atTrace().log("Entering addBean(provider={}, type={}, bean={})", provider, type, bean);

        if (provider == null || provider.isBlank()) {
            throw new ExpressionException("addBean: provider name cannot be null or blank");
        }
        if (type == null) {
            throw new ExpressionException("addBean: type cannot be null");
        }

        try {
            IInjectionContext ctx = getContext();
            @SuppressWarnings("unchecked")
            BeanReference<Object> ref = new BeanReference<>((Class<Object>) type, Optional.empty(), Optional.empty(), Set.of());
            ctx.addBean(provider, ref, bean);
            log.atDebug().log("addBean: bean of type {} added to provider {}", type, provider);
        } catch (DiException e) {
            log.atError().log("addBean failed for provider={}, type={}", provider, type, e);
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
        log.atTrace().log("Entering addNamedBean(provider={}, type={}, name={}, bean={})", provider, type, name, bean);

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
            BeanReference<Object> ref = new BeanReference<>((Class<Object>) type, Optional.empty(), Optional.of(name), Set.of());
            ctx.addBean(provider, ref, bean);
            log.atDebug().log("addNamedBean: bean '{}' of type {} added to provider {}", name, type, provider);
        } catch (DiException e) {
            log.atError().log("addNamedBean failed for provider={}, type={}, name={}", provider, type, name, e);
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
        log.atTrace().log("Entering addSingleton(provider={}, type={}, bean={})", provider, type, bean);

        if (provider == null || provider.isBlank()) {
            throw new ExpressionException("addSingleton: provider name cannot be null or blank");
        }
        if (type == null) {
            throw new ExpressionException("addSingleton: type cannot be null");
        }

        try {
            IInjectionContext ctx = getContext();
            @SuppressWarnings("unchecked")
            BeanReference<Object> ref = new BeanReference<>((Class<Object>) type, Optional.of(BeanStrategy.singleton), Optional.empty(), Set.of());
            ctx.addBean(provider, ref, bean);
            log.atDebug().log("addSingleton: singleton bean of type {} added to provider {}", type, provider);
        } catch (DiException e) {
            log.atError().log("addSingleton failed for provider={}, type={}", provider, type, e);
            throw new ExpressionException("addSingleton: failed - " + e.getMessage());
        }
    }
}
