package com.garganttua.reflection.beans;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.garganttua.reflection.GGReflectionException;

public class GGBeanLoader implements IGGBeanLoader {
	
	private Map<String, IGGBeanSupplier> beanSuppliers;

	protected GGBeanLoader(Collection<IGGBeanSupplier> beanSuppliers) {
		this.beanSuppliers = new HashMap<String, IGGBeanSupplier>();
		beanSuppliers.forEach( bs -> {
			this.beanSuppliers.put(bs.getName(), bs);
		});
	}

	@Override
	public Object getBeanNamed(String supplier, String name) throws GGReflectionException {
		IGGBeanSupplier beanSupplier = this.beanSuppliers.get(supplier);
		return beanSupplier==null?null:beanSupplier.getBeanNamed(name);
	}

	@Override
	public <T> T getBeanOfType(String supplier, Class<T> type) throws GGReflectionException{
		IGGBeanSupplier beanSupplier = this.beanSuppliers.get(supplier);
		return beanSupplier==null?null:beanSupplier.getBeanOfType(type);
	}

	@Override
	public <T> List<T> getBeansImplementingInterface(String supplier, Class<T> interfasse) throws GGReflectionException {
		IGGBeanSupplier beanSupplier = this.beanSuppliers.get(supplier);
		return beanSupplier==null?null:beanSupplier.getBeansImplementingInterface(interfasse);
	}

	@Override
	public Object getBeanNamed(String qualifierName) throws GGReflectionException {
		Object bean = null;
		for(IGGBeanSupplier beanSuplier: this.beanSuppliers.values()) {
			try {
				bean = beanSuplier.getBeanNamed(qualifierName);
				if( bean != null ) {
					break;
				}
			} catch(GGReflectionException e) {
				continue;
			}
		}
		
		if( bean == null ) {
			throw new GGReflectionException("Bean "+qualifierName+" not found");
		}
		return bean;
	}

	@Override
	public <T> T getBeanOfType(Class<T> type) throws GGReflectionException {
		T bean = null;
		for(IGGBeanSupplier beanSuplier: this.beanSuppliers.values()) {
			try {
				bean = beanSuplier.getBeanOfType(type);
				if( bean != null ) {
					break;
				}
			} catch(GGReflectionException e) {
				continue;
			}
		}
		
		if( bean == null ) {
			throw new GGReflectionException("Bean "+type+" not found");
		}
		return bean;
	}

}
