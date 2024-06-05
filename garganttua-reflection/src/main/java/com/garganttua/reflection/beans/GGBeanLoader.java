package com.garganttua.reflection.beans;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GGBeanLoader implements IGGBeanLoader {
	
	private Map<String, IGGBeanSupplier> beanSuppliers;

	protected GGBeanLoader(Collection<IGGBeanSupplier> beanSuppliers) {
		this.beanSuppliers = new HashMap<String, IGGBeanSupplier>();
		beanSuppliers.forEach( bs -> {
			this.beanSuppliers.put(bs.getName(), bs);
		});
	}

	@Override
	public <T> T getBean(String supplier, String name, String type, Class<T> clazz) {
		IGGBeanSupplier beanSupplier = this.beanSuppliers.get(supplier);
		return beanSupplier==null?null:beanSupplier.getBean(name, type, clazz);
	}

}
