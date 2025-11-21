package com.garganttua.core.reflection;

import java.util.Optional;

import com.garganttua.core.CoreException;
import com.garganttua.core.CoreExceptionCode;

public class ReflectionException extends CoreException {

	public ReflectionException(String string) {
		super(CoreExceptionCode.REFLECTION_ERROR, string);
	}

	public ReflectionException(String string, Exception e) {
		super(CoreExceptionCode.REFLECTION_ERROR, string, e);
	}

	public ReflectionException(Exception e) {
		super(e);
	}

	private static final long serialVersionUID = 2732095843634378815L;

	@SuppressWarnings("unchecked")
    public static <E extends Throwable> Optional<E> findFirstInException(ReflectionException exception,
            Class<E> type) {
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (CoreException.class.isAssignableFrom(type) ) {
                return Optional.of((E) cause);
            }
            cause = cause.getCause();
        }
        return Optional.empty();

    }

}
