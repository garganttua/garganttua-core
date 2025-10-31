package com.garganttua.injection.spec.supplier.builder.binder;

import java.lang.reflect.Field;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.spec.supplier.binder.IInjectableField;
import com.garganttua.injection.spec.supplier.builder.IFieldBuilder;
import com.garganttua.reflection.GGObjectAddress;

public interface IInjectableFieldBuilder<FieldType, OwnerType, Link>
        extends IFieldBuilder<Link, IInjectableField<OwnerType>> {

    IInjectableFieldBuilder<FieldType, OwnerType, Link> field(String fieldName)
            throws DslException;

    IInjectableFieldBuilder<FieldType, OwnerType, Link> field(Field field)
            throws DslException;

    IInjectableFieldBuilder<FieldType, OwnerType, Link> field(GGObjectAddress address)
            throws DslException;

}
