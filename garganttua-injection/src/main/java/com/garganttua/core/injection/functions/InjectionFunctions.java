package com.garganttua.core.injection.functions;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.Pair;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.reflection.IClass;

import jakarta.annotation.Nullable;

import com.garganttua.core.reflection.annotations.Reflected;
/**
 * Expression functions for interacting with the dependency injection context.
 *
 * <p>
 * This class provides @Expression annotated functions for querying beans,
 * properties, and managing the injection context from Garganttua scripts.
 * </p>
 *
 * <p><b>Usage Examples (in .gs script)</b></p>
 * <pre>{@code
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
 * }</pre>
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
@Reflected(queryAllDeclaredMethods = true)
public final class InjectionFunctions {
    private static final Logger log = Logger.getLogger(InjectionFunctions.class);

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
        log.trace("Entering getBean(type={})", type);

        if (type == null) {
            throw new ExpressionException("getBean: type cannot be null");
        }

        try {
            IInjectionContext ctx = getContext();
            BeanReference<?> ref = new BeanReference<>(IClass.getClass(type), Optional.empty(), Optional.empty(), Set.of());
            Optional<?> bean = ctx.queryBean(ref);
            Object result = bean.orElse(null);
            log.debug("getBean result: {}", result);
            return result;
        } catch (DiException e) {
            log.error("getBean failed for type {}", type, e);
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
        log.trace("Entering getBeanByRef(reference={})", refStr);

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
            log.debug("getBeanByRef result: {}", result);
            return result;
        } catch (DiException e) {
            log.error("getBeanByRef failed for reference {}", refStr, e);
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
        log.trace("Entering getBeans(type={})", type);

        if (type == null) {
            throw new ExpressionException("getBeans: type cannot be null");
        }

        try {
            IInjectionContext ctx = getContext();
            BeanReference<?> ref = new BeanReference<>(IClass.getClass(type), Optional.empty(), Optional.empty(), Set.of());
            List<?> beans = ctx.queryBeans(ref);
            log.debug("getBeans found {} beans of type {}", beans.size(), type);
            return beans;
        } catch (DiException e) {
            log.error("getBeans failed for type {}", type, e);
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
        log.trace("Entering hasBean(reference={})", reference);

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
            log.debug("hasBean returned false due to: {}", e.getMessage());
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
        log.trace("Entering beanProviderCount()");

        try {
            IInjectionContext ctx = getContext();
            Set<IBeanProvider> providers = ctx.getBeanProviders();
            int count = providers.size();
            log.debug("beanProviderCount: {}", count);
            return count;
        } catch (DiException e) {
            log.error("beanProviderCount failed", e);
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
        log.trace("Entering beanCount()");

        try {
            IInjectionContext ctx = getContext();
            // Use size() to count bean definitions without instantiating them
            int count = ctx.getBeanProviders().stream()
                    .mapToInt(IBeanProvider::size)
                    .sum();
            log.debug("beanCount: {}", count);
            return count;
        } catch (DiException e) {
            log.error("beanCount failed", e);
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
        log.trace("Entering beanCountInProvider(providerName={})", name);

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
            log.debug("beanCountInProvider({}): {}", name, count);
            return count;
        } catch (DiException e) {
            log.error("beanCountInProvider failed for provider {}", name, e);
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
        log.trace("Entering getProperty(key={}, type={})", keyStr, type);

        if (keyStr == null || keyStr.isBlank()) {
            throw new ExpressionException("getProperty: key cannot be null or blank");
        }
        if (type == null) {
            type = String.class;
        }

        try {
            IInjectionContext ctx = getContext();
            Optional<?> value = ctx.getProperty(keyStr, IClass.getClass(type));
            Object result = value.orElse(null);
            log.debug("getProperty result: {}", result);
            return result;
        } catch (DiException e) {
            log.error("getProperty failed for key {}", keyStr, e);
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
        log.trace("Entering setProperty(provider={}, key={}, value={})", provider, keyStr, value);

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
            log.debug("setProperty: {}={} set in provider {}", keyStr, value, provider);
        } catch (DiException e) {
            log.error("setProperty failed for provider={}, key={}", provider, keyStr, e);
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
        log.trace("Entering hasProperty(key={})", keyStr);

        if (keyStr == null || keyStr.isBlank()) {
            return false;
        }

        try {
            IInjectionContext ctx = getContext();
            boolean exists = ctx.getProperty(keyStr, IClass.getClass(Object.class)).isPresent();
            log.debug("hasProperty({}): {}", keyStr, exists);
            return exists;
        } catch (Exception e) {
            log.debug("hasProperty returned false due to: {}", e.getMessage());
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
        log.trace("Entering propertyProviderCount()");

        try {
            IInjectionContext ctx = getContext();
            Set<IPropertyProvider> providers = ctx.getPropertyProviders();
            int count = providers.size();
            log.debug("propertyProviderCount: {}", count);
            return count;
        } catch (DiException e) {
            log.error("propertyProviderCount failed", e);
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
        log.trace("Entering injectionInfo()");

        try {
            IInjectionContext ctx = getContext();
            StringBuilder sb = new StringBuilder();
            sb.append("Injection Context Information\n");
            sb.append("=============================\n");

            Set<IBeanProvider> beanProviders = ctx.getBeanProviders();
            sb.append("Bean Providers: ").append(beanProviders.size()).append("\n");
            int totalBeans = 0;
            for (IBeanProvider provider : beanProviders) {
                BeanReference<Object> ref = new BeanReference<>(IClass.getClass(Object.class), Optional.empty(), Optional.empty(), Set.of());
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
            log.debug("injectionInfo:\n{}", info);
            return info;
        } catch (DiException e) {
            log.error("injectionInfo failed", e);
            throw new ExpressionException("injectionInfo: failed - " + e.getMessage());
        }
    }

    // Bean-mutation functions (addBean / addNamedBean / addSingleton) live in
    // BeanMutationFunctions (same package, registered alongside this class).
}
