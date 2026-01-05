package com.garganttua.core.mutex.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;

/**
 * Marks a type as requiring mutex synchronization support.
 *
 * <p>
 * The {@code @Mutex} annotation indicates that a class uses mutexes for
 * thread-safe operations and should be included in native image configuration
 * when building with GraalVM. It is typically applied to classes that depend
 * on mutex managers or implement mutex-based synchronization.
 * </p>
 *
 * <h2>Usage</h2>
 * <p>
 * Apply this annotation to types that:
 * </p>
 * <ul>
 *   <li>Inject or use {@link com.garganttua.core.mutex.IMutex}</li>
 *   <li>Implement {@link com.garganttua.core.mutex.IMutexManager}</li>
 *   <li>Require mutex resources for GraalVM native image</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @Mutex
 * public class DistributedCacheService {
 *     private final IMutexManager mutexManager;
 *
 *     public void updateCache(String key, Object value) {
 *         IMutex mutex = mutexManager.mutex("cache:" + key);
 *         mutex.acquire(() -> {
 *             // Thread-safe cache update
 *             return null;
 *         });
 *     }
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.mutex.IMutex
 * @see com.garganttua.core.mutex.IMutexManager
 * @see Native
 */
@Native
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Mutex {

}
