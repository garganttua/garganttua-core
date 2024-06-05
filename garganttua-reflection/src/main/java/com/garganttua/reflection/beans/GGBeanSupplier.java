package com.garganttua.reflection.beans;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import com.garganttua.reflection.beans.annotation.GGBean;

public class GGBeanSupplier implements IGGBeanSupplier {

	private static final String BEAN_SUPPLIER = "gg";
	
	public GGBeanSupplier(Collection<String> packages) {
		packages.parallelStream().forEach(package_ -> {
			try {
				List<Class<?>> annotatedClasses = this.getClassesWithAnnotation(package_, GGBean.class);
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		});	
	}

	@Override
	public <T> T getBean(String name, String type, Class<T> clazz) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		return BEAN_SUPPLIER;
	}
	
	private List<Class<?>> getClassesWithAnnotation(String packageName, Class<? extends Annotation> annotation) throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        List<Class<?>> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        List<Class<?>> annotatedClasses = new ArrayList<>();
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(annotation)) {
                annotatedClasses.add(clazz);
            }
        }
        return annotatedClasses;
    }

    private List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    assert !file.getName().contains(".");
                    classes.addAll(findClasses(file, packageName + "." + file.getName()));
                } else if (file.getName().endsWith(".class")) {
                    classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
                }
            }
        }
        return classes;
    }

}
