package com.garganttua.core.nativve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.garganttua.core.CoreException;

/**
 * Behaviour tests for {@link NativeException}: it is a {@link CoreException}
 * carrying the {@code NATIVE_ERROR} code and preserving the wrapped cause and
 * its message.
 */
public class NativeExceptionBehaviourTest {

    @Test
    public void carriesTheNativeErrorCode() {
        NativeException ex = new NativeException(new IOException("boom"));
        assertEquals(CoreException.NATIVE_ERROR, ex.getCode());
    }

    @Test
    public void preservesTheWrappedCause() {
        IOException cause = new IOException("disk full");
        NativeException ex = new NativeException(cause);
        assertSame(cause, ex.getCause());
    }

    @Test
    public void messageIsDerivedFromCauseMessage() {
        NativeException ex = new NativeException(new IllegalStateException("bad state"));
        assertEquals("bad state", ex.getMessage());
    }

    @Test
    public void isACoreException() {
        org.junit.jupiter.api.Assertions.assertInstanceOf(CoreException.class,
                new NativeException(new RuntimeException("x")));
    }

    @Test
    public void nativeErrorCodeIsTen() {
        // pins the documented mapping (CoreException.NATIVE_ERROR == 10)
        assertEquals(10, CoreException.NATIVE_ERROR);
    }
}
