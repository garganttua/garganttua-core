package com.garganttua.core.mutex.dsl;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.reflection.IAnnotatedElement;
import com.garganttua.core.reflection.IClass;

/**
 * {@link IElementResolver} for elements annotated with
 * {@link com.garganttua.core.mutex.annotations.Mutex @Mutex}, registered into the
 * injection context by {@link MutexManagerBuilder}.
 *
 * <p>
 * Resolution is not yet implemented; {@link #resolve(IClass, IAnnotatedElement)}
 * currently throws {@link UnsupportedOperationException}.
 * </p>
 */
public class MutexResolver implements IElementResolver {

    /**
     * Resolves a {@code @Mutex}-annotated element to an injectable value.
     *
     * @param elementType the declared type of the element being resolved
     * @param element the annotated element to resolve
     * @return the resolved value
     * @throws DiException if resolution fails
     * @throws UnsupportedOperationException always, as resolution is not yet implemented
     */
    @Override
    public Resolved resolve(IClass<?> elementType, IAnnotatedElement element) throws DiException {
        throw new UnsupportedOperationException("Unimplemented method 'resolve'");
    }

}
