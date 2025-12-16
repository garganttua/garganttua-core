/**
 * Annotations for GraalVM Native Image configuration.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides annotations for declaratively marking classes and methods
 * for inclusion in GraalVM Native Image reflection configuration. These annotations
 * enable automatic generation of reflect-config.json and resource-config.json files.
 * </p>
 *
 * <h2>Annotations</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.nativve.annotations.Native} - Marks a class for native image reflection</li>
 *   <li>{@link com.garganttua.core.nativve.annotations.NativeConfigurationBuilder} - Marks a class as a native configuration builder</li>
 * </ul>
 *
 * <h2>Usage Pattern</h2>
 * <p>
 * Classes annotated with {@code @Native} are automatically included in the
 * native image reflection configuration, enabling reflection access at runtime
 * in GraalVM native images.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @Native
 * public class MyReflectiveClass {
 *     // This class will be included in reflect-config.json
 * }
 * }</pre>
 *
 * <h2>Integration</h2>
 * <p>
 * These annotations are processed by the garganttua-native module and the
 * garganttua-native-image-maven-plugin to generate GraalVM configuration files
 * during the build process.
 * </p>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>garganttua-native - Native image configuration management</li>
 *   <li>garganttua-native-image-maven-plugin - Maven plugin for native image builds</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 */
package com.garganttua.core.nativve.annotations;
