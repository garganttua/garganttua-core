package com.garganttua.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.JdkClass;
import com.garganttua.core.supply.SupplyException;

/**
 * Behaviour of {@link CoreException}: error-code tagging through public
 * subclasses, message/cause propagation, and the static cause-chain search
 * utilities ({@code findFirstInException} and {@code processException}).
 */
class CoreExceptionBehaviourTest {

	@Test
	void subclass_tagsItsErrorCode() {
		DiException di = new DiException("missing bean");
		assertEquals(CoreException.INJECTION_ERROR, di.getCode());
		assertEquals("missing bean", di.getMessage());
	}

	@Test
	void supplyException_tagsSupplyError() {
		SupplyException ex = new SupplyException("cannot supply");
		assertEquals(CoreException.SUPPLY_ERROR, ex.getCode());
	}

	@Test
	void messageAndCauseConstructor_propagatesCause() {
		IllegalStateException cause = new IllegalStateException("root");
		DiException di = new DiException("wrapped", cause);
		assertEquals("wrapped", di.getMessage());
		assertSame(cause, di.getCause());
	}

	@Test
	void causeOnlyConstructor_derivesMessageFromCause() {
		IllegalArgumentException cause = new IllegalArgumentException("from-cause");
		DiException di = new DiException(cause);
		assertEquals("from-cause", di.getMessage());
		assertSame(cause, di.getCause());
	}

	@Test
	void findFirstInException_findsCoreExceptionDeepInChain() {
		DiException buried = new DiException("buried");
		RuntimeException mid = new RuntimeException("mid", buried);
		IllegalStateException top = new IllegalStateException("top", mid);

		Optional<CoreException> found = CoreException.findFirstInException(top);
		assertTrue(found.isPresent());
		assertSame(buried, found.get());
		assertEquals(CoreException.INJECTION_ERROR, found.get().getCode());
	}

	@Test
	void findFirstInException_returnsEmptyWhenNoCoreExceptionInChain() {
		IllegalStateException top = new IllegalStateException("top",
				new RuntimeException("mid"));
		assertTrue(CoreException.findFirstInException(top).isEmpty());
	}

	@Test
	void findFirstInException_ignoresSelf_searchesOnlyCauses() {
		// The top-level exception is itself a CoreException, but the search starts
		// at getCause() — so a CoreException with a non-CoreException cause yields empty.
		DiException self = new DiException("self", new RuntimeException("plain cause"));
		assertTrue(CoreException.findFirstInException(self).isEmpty(),
				"search starts at the cause, not the exception itself");
	}

	@Test
	void findFirstInException_typed_findsExactType() {
		IOException ioe = new IOException("io");
		RuntimeException top = new RuntimeException("top", ioe);

		IClass<IOException> type = JdkClass.of(IOException.class);
		Optional<IOException> found = CoreException.findFirstInException(top, type);
		assertTrue(found.isPresent());
		assertSame(ioe, found.get());
	}

	@Test
	void findFirstInException_typed_unwrapsInvocationTargetException() {
		IllegalArgumentException target = new IllegalArgumentException("target");
		InvocationTargetException ite = new InvocationTargetException(target);
		RuntimeException top = new RuntimeException("top", ite);

		IClass<IllegalArgumentException> type = JdkClass.of(IllegalArgumentException.class);
		Optional<IllegalArgumentException> found = CoreException.findFirstInException(top, type);
		assertTrue(found.isPresent(),
				"InvocationTargetException's target must be traversed");
		assertSame(target, found.get());
	}

	@Test
	void findFirstInException_typed_returnsEmptyWhenAbsent() {
		RuntimeException top = new RuntimeException("top", new IllegalStateException("mid"));
		IClass<IOException> type = JdkClass.of(IOException.class);
		assertTrue(CoreException.findFirstInException(top, type).isEmpty());
	}

	@Test
	void processException_rethrowsExistingCoreExceptionFromChain() {
		DiException buried = new DiException("buried");
		RuntimeException top = new RuntimeException("top", buried);

		CoreException thrown = assertThrows(CoreException.class,
				() -> CoreException.processException(top));
		assertSame(buried, thrown, "the buried CoreException must be rethrown unchanged");
	}

	@Test
	void processException_wrapsPlainExceptionAsUnknownError() {
		IllegalStateException plain = new IllegalStateException("plain");

		CoreException thrown = assertThrows(CoreException.class,
				() -> CoreException.processException(plain));
		assertEquals(CoreException.UNKNOWN_ERROR, thrown.getCode());
		assertEquals("plain", thrown.getMessage());
		assertSame(plain, thrown.getCause());
	}

	@Test
	void errorCodeConstants_haveDistinctExpectedValues() {
		assertEquals(-1, CoreException.UNKNOWN_ERROR);
		assertEquals(1, CoreException.SUPPLY_ERROR);
		assertEquals(6, CoreException.INJECTION_ERROR);
		assertEquals(15, CoreException.CRYPTO_ERROR);
	}

	@Test
	void coreExceptionCodes_isNotInstantiable() {
		// reflectively invoke the private constructor; it must throw
		// UnsupportedOperationException (wrapped in InvocationTargetException).
		var ctors = CoreExceptionCodes.class.getDeclaredConstructors();
		assertEquals(1, ctors.length);
		ctors[0].setAccessible(true);
		InvocationTargetException ite = assertThrows(InvocationTargetException.class,
				() -> ctors[0].newInstance());
		assertTrue(ite.getCause() instanceof UnsupportedOperationException);
	}

	@Test
	void diException_isACoreExceptionAndRuntimeException() {
		DiException di = new DiException("x");
		assertTrue(di instanceof CoreException);
		assertTrue(di instanceof RuntimeException);
		assertFalse(CoreException.class.equals(DiException.class));
	}
}
