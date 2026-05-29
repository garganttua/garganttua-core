package com.garganttua.core.aot.commons;

/**
 * Marker interface implemented by every AOT-generated {@code AOTClass_*}
 * descriptor. Its sole purpose is to make those generated classes
 * discoverable via {@link java.util.ServiceLoader} so loading them at runtime
 * is GraalVM-native-image-friendly out of the box (the closed-world
 * assumption forbids dynamic {@code Class.forName(stringFromFile)}).
 *
 * <p>Each generated descriptor self-registers into {@link AOTRegistry} from
 * its {@code static {}} block; {@code ServiceLoader} only needs to load the
 * class to trigger that initializer. No method on this interface is ever
 * called.
 *
 * <p>SPI descriptor: each generator-produced JAR ships a
 * {@code META-INF/services/com.garganttua.core.aot.commons.IAOTSelfRegistering}
 * listing one descriptor FQN per line. GraalVM native-image picks these up
 * automatically without any reachability-metadata configuration.
 *
 * @since 2.0.0-ALPHA02
 */
public interface IAOTSelfRegistering {
    // intentionally empty — purely a marker for ServiceLoader discovery
}
