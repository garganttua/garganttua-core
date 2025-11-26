package com.garganttua.core.supply;

public class Supplier {

    @SuppressWarnings("unchecked")
    public static <Supplied> Supplied contextualSupply(IObjectSupplier<Supplied> supplier, Object... contexts) throws SupplyException{

        if (supplier == null) {
            throw new SupplyException("Supplier cannot be null");
        }

        Supplied obj = null;

        if (supplier instanceof IContextualObjectSupplier<?, ?> contextualRaw) {
            IContextualObjectSupplier<Supplied, Object> contextual = (IContextualObjectSupplier<Supplied, Object>) contextualRaw;

            Object matchingContext = null;
            if (contexts != null) {
                for (Object ctx : contexts) {
                    if (ctx != null && contextual.getOwnerContextType().isAssignableFrom(ctx.getClass())) {
                        matchingContext = ctx;
                        break;
                    }
                }
            }

            if (matchingContext == null) {
                throw new SupplyException(
                        "No compatible context found for supplier expecting " + contextual.getOwnerContextType().getName());
            }

            obj = contextual.supply(matchingContext, contexts).orElse(null);
        } else {
            obj = supplier.supply().orElse(null);
        }

        return obj;
    }

    

}
