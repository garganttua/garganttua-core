package com.garganttua.reflection.beans;

import java.util.Collection;
import java.util.HashSet;

public class GGBeanLoaderFactory {

	public static IGGBeanLoader getLoader(Collection<String> packages) {
		Collection<IGGBeanSupplier> beanSuppliers = new HashSet<IGGBeanSupplier>();
		beanSuppliers.add(new GGBeanSupplier(packages));
		return new GGBeanLoader(beanSuppliers);
	}

}
