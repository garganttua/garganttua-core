package com.garganttua.core.injection.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;

/**
 * Specifies which bean provider should be used to resolve a dependency.
 *
 * <p>
 * The {@code @Provider} annotation allows fine-grained control over dependency resolution by
 * explicitly specifying the bean provider (scope) from which to retrieve the dependency. This is
 * useful in multi-provider contexts where beans of the same type exist in different providers,
 * such as separating application-level beans from request-level beans or modular subsystems.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Multiple bean providers configured
 * IInjectionContext context = InjectionContextBuilder.create()
 *     .beanProvider("application")
 *         .withBean(DatabaseConnection.class)
 *             .strategy(BeanStrategy.singleton)
 *             .and()
 *         .and()
 *     .beanProvider("request")
 *         .withBean(UserSession.class)
 *             .strategy(BeanStrategy.prototype)
 *             .and()
 *         .and()
 *     .build();
 *
 * // Inject from specific provider
 * public class OrderService {
 *     @Provider("application")
 *     private DatabaseConnection dbConnection;
 *
 *     @Provider("request")
 *     private UserSession userSession;
 * }
 *
 * // Constructor injection with provider specification
 * public class ReportGenerator {
 *     private final DatabaseConnection db;
 *     private final UserSession session;
 *
 *     public ReportGenerator(
 *         @Provider("application") DatabaseConnection db,
 *         @Provider("request") UserSession session) {
 *         this.db = db;
 *         this.session = session;
 *     }
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection.IBeanProvider
 * @see com.garganttua.core.injection.context.dsl.IBeanProviderBuilder
 */
@Native
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Provider {

    /**
     * The name of the bean provider to use for resolving this dependency.
     *
     * <p>
     * Specifies the provider name/scope from which to retrieve the bean. The provider
     * must be registered in the DI context. If the specified provider does not exist
     * or does not contain the requested bean, resolution will fail.
     * </p>
     *
     * @return the provider name
     */
    String value();

}
