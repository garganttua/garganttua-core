package com.garganttua.injection.supplier.binder;

import java.util.List;

import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.supplier.IObjectSupplier;
import com.garganttua.injection.supplier.Supplier;

public class ExecutableBinder<Context> {

    protected Object[] buildArguments(List<IObjectSupplier<?>> parameterSuppliers, Context context) throws DiException {
        if (parameterSuppliers.isEmpty()) {
            return new Object[0];
        }

        Object[] args = new Object[parameterSuppliers.size()];

        for (int i = 0; i < parameterSuppliers.size(); i++) {
            args[i] = Supplier.getObject(parameterSuppliers.get(i), context);
        }

        return args;
    }

}
