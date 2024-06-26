package com.garganttua.reflection.beans;

import java.util.Objects;

import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.beans.annotation.GGBean;
import com.garganttua.reflection.beans.annotation.GGBeanLoadingStrategy;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

import lombok.Getter;

public class GGBeanFactory {

	public GGBeanFactory(GGBean annotation, Class<?> type) {
		this.name = annotation.name().isEmpty() ? type.getSimpleName() : annotation.name() ;
		this.strategy = annotation.strategy();
		this.type = type;
	}

	@Getter
	private Class<?> type;
	
	@Getter
	private String name;
	
	@Getter
	private GGBeanLoadingStrategy strategy;

	private Object bean;
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GGBeanFactory that = (GGBeanFactory) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(name, that.name) &&
                strategy == that.strategy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, strategy);
    }

	public Object getBean() throws GGReflectionException {
		if( this.strategy == GGBeanLoadingStrategy.newInstance ) {
			return GGObjectReflectionHelper.instanciateNewObject(this.type);
		} else {
			if( this.bean == null ) {
				this.bean = GGObjectReflectionHelper.instanciateNewObject(this.type);
			}
			return this.bean;
		}
	}
}
