/**
 * Domain-Specific Language (DSL) framework implementation and base builder classes.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the concrete implementation of the DSL framework, including
 * abstract base classes for building fluent, type-safe builder APIs. It implements
 * the builder pattern infrastructure used throughout the Garganttua framework.
 * </p>
 *
 * <h2>Main Implementation Classes</h2>
 * <ul>
 *   <li>{@code AbstractAutomaticBuilder} - Base class for automatic builders with build() method</li>
 *   <li>{@code AbstractAutomaticLinkedBuilder} - Base class for linked automatic builders</li>
 *   <li>{@code AbstractLinkedBuilder} - Base class for linked builders with parent references</li>
 *   <li>{@code OrderedMapBuilder} - Builder for ordered maps</li>
 * </ul>
 *
 * <h2>Usage Example: Custom Automatic Builder</h2>
 * <pre>{@code
 * // Extend AbstractAutomaticBuilder
 * public class ConfigBuilder extends AbstractAutomaticBuilder<ConfigBuilder, Config> {
 *
 *     private String name;
 *     private int timeout;
 *
 *     public ConfigBuilder name(String name) {
 *         this.name = name;
 *         return this;
 *     }
 *
 *     public ConfigBuilder timeout(int timeout) {
 *         this.timeout = timeout;
 *         return this;
 *     }
 *
 *     @Override
 *     protected Config doBuild() {
 *         return new Config(name, timeout);
 *     }
 * }
 *
 * // Usage
 * Config config = new ConfigBuilder()
 *     .name("MyConfig")
 *     .timeout(30)
 *     .build();
 * }</pre>
 *
 * <h2>Usage Example: Linked Builder</h2>
 * <pre>{@code
 * // Parent builder
 * public class ServerBuilder extends AbstractAutomaticBuilder<ServerBuilder, Server> {
 *
 *     private List<RouteConfig> routes = new ArrayList<>();
 *
 *     public RouteBuilder route(String path) {
 *         return new RouteBuilder(this, path);
 *     }
 *
 *     void addRoute(RouteConfig route) {
 *         routes.add(route);
 *     }
 *
 *     @Override
 *     protected Server doBuild() {
 *         return new Server(routes);
 *     }
 * }
 *
 * // Linked child builder
 * public class RouteBuilder extends AbstractLinkedBuilder<RouteBuilder, ServerBuilder> {
 *
 *     private String path;
 *     private String handler;
 *
 *     public RouteBuilder(ServerBuilder parent, String path) {
 *         super(parent);
 *         this.path = path;
 *     }
 *
 *     public RouteBuilder handler(String handler) {
 *         this.handler = handler;
 *         return this;
 *     }
 *
 *     @Override
 *     public ServerBuilder done() {
 *         parent.addRoute(new RouteConfig(path, handler));
 *         return parent;
 *     }
 * }
 *
 * // Usage
 * Server server = new ServerBuilder()
 *     .route("/users")
 *         .handler("UserHandler")
 *         .done()
 *     .route("/orders")
 *         .handler("OrderHandler")
 *         .done()
 *     .build();
 * }</pre>
 *
 * <h2>Usage Example: Ordered Map Builder</h2>
 * <pre>{@code
 * // Build ordered map
 * Map<String, Object> config = new OrderedMapBuilder()
 *     .put("name", "MyApp")
 *     .put("version", "1.0.0")
 *     .put("timeout", 30)
 *     .build();
 *
 * // Preserves insertion order
 * config.forEach((key, value) -> {
 *     System.out.println(key + " = " + value);
 * });
 * }</pre>
 *
 * <h2>Builder Pattern Features</h2>
 * <ul>
 *   <li>Fluent, chainable API</li>
 *   <li>Type-safe configuration</li>
 *   <li>Parent-child builder relationships</li>
 *   <li>Automatic builder implementation</li>
 *   <li>Generic type preservation</li>
 *   <li>{@code done()} for returning to parent builder</li>
 *   <li>{@code build()} for creating final object</li>
 *   <li>Template method pattern for customization</li>
 * </ul>
 *
 * <h2>Integration</h2>
 * <p>
 * These base builder classes are extended by:
 * </p>
 * <ul>
 *   <li>DI context builders</li>
 *   <li>Runtime workflow builders</li>
 *   <li>Condition builders</li>
 *   <li>Reflection binder builders</li>
 *   <li>Supplier builders</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.dsl.IAutomaticBuilder
 * @see com.garganttua.core.dsl.ILinkedBuilder
 */
package com.garganttua.core.dsl;
