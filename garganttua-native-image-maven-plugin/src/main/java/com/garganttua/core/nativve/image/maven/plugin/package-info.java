/**
 * Maven plugin for GraalVM Native Image configuration management.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides a Maven plugin that automates the generation and management
 * of GraalVM Native Image configuration files. It extracts configuration from
 * dependencies, validates resources, and generates reflect-config.json and
 * resource-config.json files for native image compilation.
 * </p>
 *
 * <h2>Main Class</h2>
 * <ul>
 *   <li>{@code NativeConfigMojo} - Maven Mojo for native image configuration</li>
 * </ul>
 *
 * <h2>Maven Configuration</h2>
 * <pre>{@code
 * <plugin>
 *     <groupId>com.garganttua.core</groupId>
 *     <artifactId>garganttua-native-image-maven-plugin</artifactId>
 *     <version>2.0.0-ALPHA01</version>
 *     <executions>
 *         <execution>
 *             <goals>
 *                 <goal>native-config</goal>
 *             </goals>
 *         </execution>
 *     </executions>
 * </plugin>
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Automatic configuration extraction from dependencies</li>
 *   <li>Resource validation and registration</li>
 *   <li>Reflection configuration aggregation</li>
 *   <li>Multi-module project support</li>
 *   <li>Override and merge modes</li>
 * </ul>
 *
 * <h2>Generated Files</h2>
 * <ul>
 *   <li>{@code META-INF/native-image/reflect-config.json} - Reflection metadata</li>
 *   <li>{@code META-INF/native-image/resource-config.json} - Resource configuration</li>
 * </ul>
 *
 * <h2>Related Modules</h2>
 * <ul>
 *   <li>garganttua-native - Native image configuration API</li>
 *   <li>garganttua-commons - Native annotations</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 */
package com.garganttua.core.nativve.image.maven.plugin;
