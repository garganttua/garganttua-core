package com.garganttua.injection;

import java.util.Objects;
import java.util.Optional;

public class BeanSupplier<Bean> implements IBeanSupplier<Bean> {

    private String name;
    private Class<Bean> type;
    private String provider;

    public BeanSupplier(String name, Class<Bean> type, String provider) {
        this.name = name;
        this.type = Objects.requireNonNull(type, "Type cannot be null");;
        this.provider = provider;
    }

    @Override
    public Optional<Bean> getObject() throws DiException {

        boolean nameProvided = this.name != null && !this.name.isEmpty();
        boolean providerProvided = this.provider != null && !this.provider.isEmpty();

        if( DiContext.context == null ){
            throw new DiException("Context not built");
        }

        if( nameProvided && providerProvided ) {
            return DiContext.context.getBeanFromProvider(this.provider, this.name, this.type);
        } else if( nameProvided && !providerProvided) {
            return DiContext.context.getBean(this.name, this.type);
        }  else if( !nameProvided && providerProvided)  {
            return DiContext.context.getBeanFromProvider(this.provider, this.type);
        } else {
            return DiContext.context.getBean(this.type);
        }
    }

    @Override
    public Class<Bean> getObjectClass() {
        return this.type;
    }


}
