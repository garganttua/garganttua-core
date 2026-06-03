/**
 * Configuration <em>providers</em>: discover configuration sources from a location.
 *
 * <p>{@link com.garganttua.core.configuration.provider.IConfigProvider} abstracts
 * <em>where</em> configuration files live so new locations plug in without touching
 * consumers. Built-in implementations:
 * {@link com.garganttua.core.configuration.provider.ClasspathConfigProvider} (scans a
 * classpath base path, exploded directories and JARs) and
 * {@link com.garganttua.core.configuration.provider.FileSystemConfigProvider} (scans a
 * filesystem directory). A remote provider (sync from a configuration server) can be
 * added the same way.</p>
 *
 * @since 2.0.0-ALPHA02
 */
package com.garganttua.core.configuration.provider;
