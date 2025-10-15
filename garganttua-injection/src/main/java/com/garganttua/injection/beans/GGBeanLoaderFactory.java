package com.garganttua.injection.beans;

import java.util.Collection;
import java.util.HashSet;

import com.garganttua.injection.spec.beans.IGGBeanLoader;
import com.garganttua.injection.spec.beans.IGGBeanSupplier;
import com.garganttua.reflection.properties.IGGPropertyLoader;

public class GGBeanLoaderFactory {

	public static IGGBeanLoader getLoader(IGGPropertyLoader propLoader, Collection<String> packages) {
		Collection<IGGBeanSupplier> beanSuppliers = new HashSet<IGGBeanSupplier>();
		GGBeanSupplier ggBeanSupplier = new GGBeanSupplier(packages, propLoader);
		beanSuppliers.add(ggBeanSupplier);
		GGBeanLoader ggBeanLoader = new GGBeanLoader(beanSuppliers);
		ggBeanSupplier.setBeanLoader(ggBeanLoader);
		return ggBeanLoader;
	}
	
	public static IGGBeanLoader getLoader(IGGPropertyLoader propLoader, Collection<String> packages, Collection<IGGBeanSupplier> suppliers) {
		Collection<IGGBeanSupplier> beanSuppliers = new HashSet<IGGBeanSupplier>();
		GGBeanSupplier ggBeanSupplier = new GGBeanSupplier(packages, propLoader);
		beanSuppliers.add(ggBeanSupplier);
		suppliers.forEach( supplier -> {
			beanSuppliers.add(supplier);
		});
		GGBeanLoader ggBeanLoader = new GGBeanLoader(beanSuppliers);
		ggBeanSupplier.setBeanLoader(ggBeanLoader);
		return ggBeanLoader;
	}

}
