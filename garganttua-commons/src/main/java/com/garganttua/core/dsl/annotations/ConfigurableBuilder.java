package com.garganttua.core.dsl.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.reflection.annotations.Indexed;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Marks a DSL builder class as configurable from an external configuration file
 * under a short, stable <em>alias</em>.
 *
 * <p>
 * The {@code garganttua-configuration} module discovers configuration files
 * (via {@code IConfigProvider}s), reads each file's target alias from its
 * shebang ({@code #!injection} for text formats, {@code <?garganttua module="injection"?>}
 * for XML, the reserved {@code "$module": "injection"} key for JSON), and resolves
 * the alias to the builder annotated with {@code @ConfigurableBuilder("injection")}.
 * The matching builder instance present in the bootstrap is then populated from the
 * file at the {@code CONFIGURATION} stage, before it is built. Without a bootstrap
 * the user wires the population manually.
 * </p>
 *
 * <p>
 * The annotation is {@link Indexed} (compile-time discovery, no classpath scan) and
 * {@link Reflected} (descriptor available for native-image), mirroring the other
 * discovery annotations such as {@code @Provider}.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @ConfigurableBuilder("injection")
 * public class InjectionContextBuilder implements IInjectionContextBuilder {
 *     // ...
 * }
 * }</pre>
 *
 * <p>A configuration file then targets it with a shebang, e.g. {@code injection.yml}:</p>
 * <pre>{@code
 * #!injection
 * withPackage: com.example.app
 * autoDetect: true
 * }</pre>
 *
 * @since 2.0.0-ALPHA02
 */
@Indexed
@Reflected
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigurableBuilder {

    /**
     * The short, stable alias used by configuration files to target this builder.
     *
     * <p>
     * It must be unique across the classpath; it is the value placed in a
     * configuration file's shebang (e.g. {@code #!injection}). Conventionally the
     * bare module/domain name (e.g. {@code injection}, {@code mutex}, {@code expression}).
     * </p>
     *
     * @return the configuration alias
     */
    String value();

}
