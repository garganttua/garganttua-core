package com.garganttua.core.runtime.resolver;

import static com.garganttua.core.injection.IInjectableElementResolver.*;
import static com.garganttua.core.runtime.RuntimeContext.*;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Resolver;
import com.garganttua.core.reflection.IAnnotatedElement;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.annotations.Code;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * Resolves {@code @Code}-annotated parameters to the runtime exit code supplier.
 *
 * <p>Registered for the {@link Code} annotation; the target parameter must be
 * assignable to {@link Integer}.</p>
 */
@Resolver(annotations = { Code.class })
public class CodeElementResolver implements IElementResolver {
    public CodeElementResolver() {
    }

    private static final Logger log = Logger.getLogger(CodeElementResolver.class);

        /**
         * Resolves the annotated element to a supplier of the current runtime exit code.
         *
         * @param elementType the declared type of the injection target; must be assignable to {@link Integer}
         * @param element     the annotated element being resolved
         * @return a {@link Resolved} wrapping the code supplier
         * @throws DiException if {@code elementType} is not an {@link Integer}
         */
        @Override
        public Resolved resolve(IClass<?> elementType, IAnnotatedElement element) throws DiException {

                log.trace("Resolving code element");

                if (!IClass.getClass(Integer.class).isAssignableFrom(elementType)) {
                        log.error("Injectable is not an Integer, throwing exception");
                        throw new DiException("Injectable is not an Integer : " + elementType.getSimpleName());
                }

                log.debug("Element type is valid Integer, preparing supplier");

                ISupplierBuilder<Integer, IContextualSupplier<Integer, IRuntimeContext<Object, Object>>> s = code();

                boolean nullable = isNullable(element);

                log.debug("Resolved code element successfully");

                return new Resolved(true, elementType, s, nullable);
        }
}