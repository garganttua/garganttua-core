package com.garganttua.core.injection.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;

/**
 * Marks a bean or injection point as using prototype scope strategy.
 *
 * <p>
 * The {@code @Prototype} annotation specifies that a bean should be instantiated anew for each
 * injection or lookup, rather than being shared as a singleton. When applied to a bean class,
 * it defines the default strategy for that bean. When applied to a field or parameter, it
 * requests a fresh instance even if the bean is normally singleton-scoped. This annotation
 * enables fine-grained control over instance lifecycle and sharing.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Mark a bean class as prototype-scoped
 * @Prototype
 * public class RequestHandler {
 *     private final String requestId = UUID.randomUUID().toString();
 *     // Each injection gets a new instance with unique state
 * }
 *
 * // Request prototype instance at injection point
 * public class ServiceCoordinator {
 *     // Gets a new RequestHandler instance
 *     @Prototype
 *     private RequestHandler handler1;
 *
 *     // Gets another new RequestHandler instance
 *     @Prototype
 *     private RequestHandler handler2;
 *
 *     // handler1 != handler2 (different instances)
 * }
 *
 * // Constructor parameter requesting prototype
 * public class TaskExecutor {
 *     private final WorkerThread worker;
 *
 *     public TaskExecutor(@Prototype WorkerThread worker) {
 *         this.worker = worker; // Fresh worker instance
 *     }
 * }
 * }</pre>
 *
 * <h2>Scope Behavior</h2>
 * <ul>
 * <li>On TYPE: Defines the bean's default strategy as prototype</li>
 * <li>On FIELD/PARAMETER: Requests a new instance regardless of bean's default strategy</li>
 * <li>Each injection point receives an independent instance</li>
 * <li>No caching or sharing of instances</li>
 * <li>Suitable for stateful beans or per-request objects</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection.BeanStrategy#prototype
 */
@Native
@Target({ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Prototype {}
