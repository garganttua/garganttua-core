package com.garganttua.core.bootstrap.dsl;

import com.garganttua.core.bootstrap.banner.BannerMode;
import com.garganttua.core.bootstrap.banner.IBanner;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;
import com.garganttua.core.dsl.IRebuildableBuilder;

/**
 * Bootstrap interface for building and rebuilding application components.
 *
 * <p>
 * The bootstrap orchestrates the building of multiple builders in dependency order,
 * manages their lifecycle, and supports rebuilding to integrate dynamically loaded
 * components (e.g., from JARs loaded at runtime).
 * </p>
 *
 * <h2>Banner Display</h2>
 * <p>
 * The bootstrap can display an ASCII art banner at startup, similar to Spring Boot.
 * Use {@link #withBanner(IBanner)} to set a custom banner or {@link #withBannerMode(BannerMode)}
 * to control how/if the banner is displayed.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
public interface IBoostrap extends IRebuildableBuilder<IBoostrap, IBuiltRegistry>, IPackageableBuilder<IBoostrap, IBuiltRegistry> {

    /**
     * Adds a builder to be managed by this bootstrap.
     *
     * @param builder the builder to add
     * @return this bootstrap for method chaining
     */
    IBoostrap withBuilder(IBuilder<?> builder);

    /**
     * Sets the banner to display at startup.
     *
     * <p>
     * If not set, a default Garganttua banner will be used.
     * Use {@link IBanner#OFF} to disable the banner.
     * </p>
     *
     * @param banner the banner to display
     * @return this bootstrap for method chaining
     */
    IBoostrap withBanner(IBanner banner);

    /**
     * Sets the banner display mode.
     *
     * <p>
     * Controls how the banner is displayed:
     * </p>
     * <ul>
     *   <li>{@link BannerMode#CONSOLE} - Print to System.out (default)</li>
     *   <li>{@link BannerMode#LOG} - Log using the logging framework</li>
     *   <li>{@link BannerMode#OFF} - Disable banner display</li>
     * </ul>
     *
     * @param mode the banner display mode
     * @return this bootstrap for method chaining
     */
    IBoostrap withBannerMode(BannerMode mode);

    /**
     * Sets the application name displayed in the banner.
     *
     * @param name the application name
     * @return this bootstrap for method chaining
     */
    IBoostrap withApplicationName(String name);

    /**
     * Sets the application version displayed in the banner.
     *
     * @param version the application version
     * @return this bootstrap for method chaining
     */
    IBoostrap withApplicationVersion(String version);

    /**
     * Rebuilds all managed builders, integrating any new packages or components.
     *
     * <p>
     * The rebuild process:
     * </p>
     * <ol>
     *   <li>Stops all lifecycle-managed objects (in reverse order)</li>
     *   <li>Re-runs auto-detection to discover new @Bootstrap builders</li>
     *   <li>Rebuilds each builder in dependency order</li>
     *   <li>Re-initializes and starts all lifecycle-managed objects</li>
     * </ol>
     *
     * @return the updated built registry
     * @throws DslException if rebuild fails or if called before initial build()
     */
    @Override
    IBuiltRegistry rebuild() throws DslException;

}
