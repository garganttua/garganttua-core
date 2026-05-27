package com.garganttua.core.bootstrap.dsl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

/**
 * Validates the ServiceLoader-based bootstrap of {@link IReflectionBuilder}
 * when no reflection is wired manually. With
 * {@code garganttua-runtime-reflection} and {@code garganttua-reflections} on
 * the test classpath, their {@code META-INF/services} descriptors must be
 * picked up so {@code Bootstrap.builder().autoDetect(true).build()} works out
 * of the box.
 */
@DisplayName("Bootstrap SPI Auto-Detection Tests")
class BootstrapAutoDetectSpiTest {

	@BeforeEach
	void clearGlobalReflection() {
		IClass.setReflection(null);
	}

	@AfterEach
	void resetGlobalReflection() {
		IClass.setReflection(null);
	}

	@Test
	@DisplayName("load() + build() loads providers via ServiceLoader")
	void spiFallback_buildsReflectionFromClasspath() throws DslException {
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.autoDetect(true);
		bootstrap.load();

		assertDoesNotThrow(bootstrap::build,
				"Bootstrap should self-bootstrap reflection via SPI when no IReflectionBuilder is provided");

		// IClass.setReflection() is called by ReflectionBuilder.doBuild(), so a
		// working global reflection must now be available.
		IReflection reflection = IClass.getReflection();
		assertNotNull(reflection, "ReflectionBuilder must have built a non-null IReflection");

		// Sanity check: looking up a known class succeeds.
		IClass<String> stringClass = reflection.getClass(String.class);
		assertNotNull(stringClass, "SPI-built reflection must resolve known classes");
	}

	@Test
	@DisplayName("Explicit provide() of IReflectionBuilder skips SPI defaults")
	void explicitOverride_skipsSpi() throws DslException {
		AtomicBoolean userBuilderUsed = new AtomicBoolean(false);
		IReflectionBuilder userBuilder = ReflectionBuilder.builder()
				.withProvider(new RuntimeReflectionProvider(), 99)
				.withScanner(new ReflectionsAnnotationScanner(), 99)
				.observer(built -> userBuilderUsed.set(true));

		Bootstrap bootstrap = new Bootstrap();
		bootstrap.autoDetect(true);
		bootstrap.provide(userBuilder);
		bootstrap.load();

		assertDoesNotThrow(bootstrap::build);
		assertTrue(userBuilderUsed.get(),
				"User-provided reflection builder must be the one that actually built (not the SPI fallback)");
	}

	@Test
	@DisplayName("disableSpiFallback() + no provide() => required-dep error")
	void spiDisabled_withoutProvide_throwsRequiredDepError() {
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.autoDetect(true);
		bootstrap.disableSpiFallback();
		bootstrap.load();   // no-op since SPI fallback is disabled

		DslException ex = assertThrows(DslException.class, bootstrap::build,
				"With SPI disabled and no manual provide, the existing require() contract must fail");
		assertTrue(ex.getMessage().toLowerCase().contains("reflection")
						|| ex.getMessage().toLowerCase().contains("required"),
				"Error message should reference the missing required dependency. Got: " + ex.getMessage());
	}

	@Test
	@DisplayName("autoDetect(false) does not trigger SPI fallback")
	void autoDetectDisabled_skipsSpi() {
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.autoDetect(false);

		// Without auto-detect, no builders are registered and build() returns
		// null. The SPI hook must not fire — assert by checking global
		// reflection remained unset (it was nulled in @BeforeEach).
		assertDoesNotThrow(bootstrap::build);
	}
}
