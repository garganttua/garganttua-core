package com.garganttua.core.aot.nativve;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import com.garganttua.core.aot.commons.AOTRegistry;
import com.garganttua.core.aot.reflection.AOTReflectionProvider;

/**
 * GraalVM native-image {@link Feature} that mirrors every AOT descriptor
 * shipped by the framework / consumer into the closed-world reflection
 * configuration at analysis time. Removes the need for hand-written or
 * mojo-generated {@code reflect-config.json} on top of the consumer-side AOT
 * pipeline — the same source of truth ({@link AOTRegistry}) drives both the
 * runtime IClass lookup and the native-image reflection registration.
 *
 * <p>Auto-activated through
 * {@code META-INF/native-image/com.garganttua.core/garganttua-aot-native-feature/native-image.properties}
 * so consumers only need to add {@code garganttua-starter-native} to the
 * classpath plus the GraalVM build plugin in their pom.
 *
 * @since 2.0.0-ALPHA02
 */
public class GarganttuaAotFeature implements Feature {

    @Override
    public String getDescription() {
        return "Registers garganttua-core AOT descriptors with RuntimeReflection";
    }

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        // Trigger AOTReflectionProvider's <clinit>, which runs
        // CoreInfrastructureSeed.bootstrap(). The seed both pre-registers
        // the framework infrastructure interfaces AND walks the classpath
        // (via ServiceLoader on IAOTSelfRegistering) to fire each
        // AOTClass_*'s static initialiser, populating the registry.
        try {
            Class.forName(AOTReflectionProvider.class.getName(), true,
                    Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "AOTReflectionProvider must be on the native-image build classpath", e);
        }

        int classCount = 0;
        int memberCount = 0;
        for (String fqn : AOTRegistry.getInstance().registeredClasses()) {
            try {
                Class<?> clazz = access.findClassByName(fqn);
                if (clazz == null) {
                    // Could not be resolved by name lookup — skip rather than
                    // fail the whole build, the user's reflect-config.json
                    // can still cover the gap if needed.
                    continue;
                }
                RuntimeReflection.register(clazz);
                classCount++;
                memberCount += registerMembers(clazz);
            } catch (RuntimeException e) {
                System.err.println("[GarganttuaAotFeature] Failed to register " + fqn + ": " + e.getMessage());
            }
        }
        System.out.println("[GarganttuaAotFeature] Registered " + classCount
                + " AOT descriptor classes (" + memberCount + " members) with RuntimeReflection.");
    }

    /**
     * Eagerly register every declared constructor / method / field for
     * reflective access. Conservative on purpose: native-image is happiest
     * with explicit registration even when a constructor is never invoked
     * reflectively. The cost is a slight image-size increase, never a
     * correctness issue.
     */
    private static int registerMembers(Class<?> clazz) {
        int count = 0;
        try {
            Constructor<?>[] ctors = clazz.getDeclaredConstructors();
            RuntimeReflection.register(ctors);
            count += ctors.length;
        } catch (LinkageError | RuntimeException ignored) {
            // primitive types, sealed-hidden classes, etc.
        }
        try {
            Method[] methods = clazz.getDeclaredMethods();
            RuntimeReflection.register(methods);
            count += methods.length;
        } catch (LinkageError | RuntimeException ignored) {
            // ignored
        }
        try {
            Field[] fields = clazz.getDeclaredFields();
            RuntimeReflection.register(fields);
            count += fields.length;
        } catch (LinkageError | RuntimeException ignored) {
            // ignored
        }
        return count;
    }
}
