package com.garganttua.core.reflection.binders.dsl;

import java.lang.reflect.Field;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.binders.IFieldBinder;

public interface IFieldBinderBuilder<FieldType, OwnerType, Builder, Link>
                extends IValuableBuilder<Builder, Link, IFieldBinder<OwnerType, FieldType>> {

        IFieldBinderBuilder<FieldType, OwnerType, Builder, Link> field(String fieldName)
                        throws DslException;

        IFieldBinderBuilder<FieldType, OwnerType, Builder, Link> field(Field field)
                        throws DslException;

        IFieldBinderBuilder<FieldType, OwnerType, Builder, Link> field(ObjectAddress address)
                        throws DslException;

}
