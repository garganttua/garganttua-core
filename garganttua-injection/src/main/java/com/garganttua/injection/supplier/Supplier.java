package com.garganttua.injection.supplier;

import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.supplier.IContextualObjectSupplier;
import com.garganttua.injection.spec.supplier.IObjectSupplier;
import com.garganttua.injection.supplier.builder.supplier.NullableEnforcingObjectSupplier;

public class Supplier {

    @SuppressWarnings("unchecked")
    public static <Supplied> Supplied getObject(IObjectSupplier<Supplied> supplier, Object... contexts)
            throws DiException {

        if (supplier == null) {
            throw new DiException("Supplier cannot be null");
        }

        Supplied obj = null;

        if (supplier instanceof NullableEnforcingObjectSupplier<Supplied> nes) {
            obj = nes.getObject().orElse(null);
        } else if (supplier instanceof IContextualObjectSupplier<?, ?> contextualRaw) {
            IContextualObjectSupplier<Supplied, Object> contextual = (IContextualObjectSupplier<Supplied, Object>) contextualRaw;

            Object matchingContext = null;
            if (contexts != null) {
                for (Object ctx : contexts) {
                    if (ctx != null && contextual.getContextClass().isAssignableFrom(ctx.getClass())) {
                        matchingContext = ctx;
                        break;
                    }
                }
            }

            if (matchingContext == null) {
                throw new DiException(
                        "No compatible context found for supplier expecting " + contextual.getContextClass().getName());
            }

            obj = contextual.getObject(matchingContext).orElse(null);
        } else {
            obj = supplier.getObject().orElse(null);
        }

        return obj;
    }

}
