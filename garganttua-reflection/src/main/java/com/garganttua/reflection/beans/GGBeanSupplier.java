package com.garganttua.reflection.beans;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.beans.annotation.GGBean;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

public class GGBeanSupplier implements IGGBeanSupplier {

	private static final String BEAN_SUPPLIER = "gg";
	
	private List<GGBeanFactory> beans = Collections.synchronizedList(new ArrayList<GGBeanFactory>());
	
	public GGBeanSupplier(Collection<String> packages) {
		packages.parallelStream().forEach(package_ -> {
			try {
				List<Class<?>> annotatedClasses = this.getClassesWithAnnotation(package_, GGBean.class);
				annotatedClasses.forEach( annotatedClass -> {
					GGBeanFactory beanFactory = new GGBeanFactory(annotatedClass.getAnnotation(GGBean.class), annotatedClass);
					this.beans.add(beanFactory);
				});
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		});	
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

    @Override
	public Object getBeanNamed(String name) throws GGReflectionException {
    	Optional<GGBeanFactory> beanFactory = beans.parallelStream().filter(bf -> bf.getName().equals(name)).findFirst();
		return beanFactory.get().getBean();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getBeanOfType(Class<T> type) throws GGReflectionException {
    	Optional<GGBeanFactory> beanFactory = beans.parallelStream().filter(bf -> bf.getType().equals(type)).findFirst();
		return (T) beanFactory.get().getBean();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> getBeansImplementingInterface(Class<T> type) throws GGReflectionException {
		List<T> l = (List<T>) beans.parallelStream().filter(bf -> GGObjectReflectionHelper.isImplementingInterface(type, bf.getType())).collect(Collectors.toList());
		return l;
	}
}
