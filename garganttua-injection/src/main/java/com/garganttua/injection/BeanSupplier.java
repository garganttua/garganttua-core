package com.garganttua.injection;

import java.util.Objects;
import java.util.Optional;

public class BeanSupplier<Bean> implements IBeanSupplier<Bean> {

    private String name;
    private Class<Bean> type;
    private String scope;

    public BeanSupplier(String name, Class<Bean> type, String scope) {
        this.name = name;
        this.type = Objects.requireNonNull(type, "Type cannot be null");;
        this.scope = scope;
    }

    @Override
    public Optional<Bean> getObject() throws DiException {

        boolean nameProvided = this.name != null && !this.name.isEmpty();
        boolean scopeProvided = this.scope != null && !this.scope.isEmpty();

        if( nameProvided && scopeProvided ) {
            return DiContext.context.getBeanFromScope(this.scope, this.name, this.type);
        } else if( nameProvided && !scopeProvided) {
            return DiContext.context.getBean(this.name, this.type);
        }  else if( !nameProvided && scopeProvided)  {
            return DiContext.context.getBeanFromScope(this.scope, this.type);
        } else {
            return DiContext.context.getBean(this.type);
        }
    }

    @Override
    public Class<Bean> getObjectClass() {
        return this.type;
    }


}
