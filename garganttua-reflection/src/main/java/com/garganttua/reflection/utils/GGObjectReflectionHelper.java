package com.garganttua.reflection.utils;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import com.garganttua.reflection.GGReflectionException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GGObjectReflectionHelper {
	
	public static Constructor<?> getConstructorWithNoParams(Class<?> classs){
		try {
			return classs.getDeclaredConstructor();
		} catch (NoSuchMethodException | SecurityException e) {
			return null;
		}		
	}
	
	public static Field getField(Class<?> objectClass, String fieldName) {
		for( Field f: objectClass.getDeclaredFields() ) {
			if( f.getName().equals(fieldName) ) {
				return f;
			}
		}
		if( objectClass.getSuperclass() != null ) {
			return GGObjectReflectionHelper.getField(objectClass.getSuperclass(), fieldName);
		}
		return null;
	}

	public static Method getMethod(Class<?> objectClass, String methodName) {
		for( Method f: objectClass.getDeclaredMethods() ) {
			if( f.getName().equals(methodName) ) {
				return f;
			}
		}
		if( objectClass.getSuperclass() != null ) {
			return getMethod(objectClass.getSuperclass(), methodName);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <destination> destination instanciateNewObject(Class<?> clazz) throws GGReflectionException {
		Constructor<?> ctor = GGObjectReflectionHelper.getConstructorWithNoParams(clazz);
		if( ctor != null ) {
			try( GGConstructorAccessManager accessor = new GGConstructorAccessManager(ctor) ){
				return (destination) ctor.newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				throw new GGReflectionException(e);
			}
		}
		throw new GGReflectionException("Class "+clazz.getSimpleName()+" does not have constructor with no params");
	}
	
	static public void setObjectFieldValue(Object entity, Field field, Object value) throws GGReflectionException {
		if( field == null ) {
			throw new GGReflectionException("Cannot set null field of object "+entity.getClass().getName()+" with value "+value);
		}
		
		try( GGFieldAccessManager manager = new GGFieldAccessManager(field) ){
			field.set(entity, value);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new GGReflectionException("Cannot set field "+field.getName()+" of object "+entity.getClass().getName()+" with value "+value, e);
		}
	}
	
	public static Object getObjectFieldValue(Object entity, String fieldName) throws GGReflectionException {
		Field field = GGObjectReflectionHelper.getField(entity.getClass(), fieldName);
		if( field == null ) {
			throw new GGReflectionException("Cannot get field "+fieldName+" of object "+entity.getClass().getName());
		}
		
		return getObjectFieldValue(entity, fieldName, field);
	}

	public static Object getObjectFieldValue(Object entity, String fieldName, Field field)
			throws GGReflectionException {
		try( GGFieldAccessManager manager = new GGFieldAccessManager(field) ){
			return field.get(entity);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new GGReflectionException("Cannot get field "+fieldName+" of object "+entity.getClass().getName(), e);
		}
	}

	public static Object invokeMethod(Object object, String methodName, Method method, Object ...args) throws GGReflectionException {
		GGObjectReflectionHelper.checkMethodAndParams(method, args);
		
		try( GGMethodAccessManager manager = new GGMethodAccessManager(method) ){
			return method.invoke(object, args);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new GGReflectionException("Cannot invoke method "+methodName+" of object "+object.getClass().getName(), e);
		} 
	}
	
	public static void checkMethodAndParams(Method method, Object ...args) throws GGReflectionException {
		if( method.getParameterCount() != args.length ) {
			throw new GGReflectionException("Method "+method.getName()+" needs "+method.getParameterCount()+" "+method.getParameterTypes()+" but "+args.length+" have been provided : "+args);
		}
		
		Class<?>[] params = method.getParameterTypes();
		for( int i = 0; i < args.length; i++ ) {
			if( args[i] == null )
				continue;
			if( !params[i].isAssignableFrom(args[i].getClass()) ) {
				throw new GGReflectionException("Method "+method.getName()+" needs parameter "+i+" to be of type "+params[i]+", not "+args[i].getClass());
			}
		}
	}

	public static <K, V> Map<K, V> newHashMapOf(Class<K> keyType, Class<V> valueType) {
		return new HashMap<K, V>();
	}

	public static <K> ArrayList<K> newArrayListOf(Class<K> type) {
		return new ArrayList<K>();
	}

	public static <K> HashSet<K> newHashSetOf(Class<K> type) {
		return new HashSet<K>();
	}

	public static <K> LinkedList<K> newLinkedlistOf(Class<K> type) {
		return new LinkedList<K>();
	}

	public static <K> Vector<K>  newVectorOf(Class<?> type) {
		return new Vector<K>();
	}
	
	public static boolean isImplementingInterface(Class<?> interfaceType, Class<?> objectType) {
		List<Class<?>> interfaces = Arrays.asList(objectType.getInterfaces());
		Optional<Class<?>> found = interfaces.parallelStream().filter(c -> c.equals(interfaceType)).findFirst();
		return found.isPresent();
	}
	
	public static List<Class<?>> getClassesWithAnnotation(String packageName, Class<? extends Annotation> annotation) throws ClassNotFoundException, IOException {
        Reflections reflections = new Reflections(packageName, Scanners.TypesAnnotated);
        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(annotation, true);
        return annotatedClasses.stream().collect(Collectors.toList());
		
		//		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
//        assert classLoader != null;
//        String path = packageName.replace('.', '/');
//        Enumeration<URL> resources = classLoader.getResources(path);
//        List<File> dirs = new ArrayList<>();
//        while (resources.hasMoreElements()) {
//            URL resource = resources.nextElement();
//            dirs.add(new File(resource.getFile()));
//        }
//        List<Class<?>> classes = new ArrayList<>();
//        for (File directory : dirs) {
//            classes.addAll(findClasses(directory, packageName));
//        }
//        List<Class<?>> annotatedClasses = new ArrayList<>();
//        for (Class<?> clazz : classes) {
//            if (clazz.isAnnotationPresent(annotation)) {
//                annotatedClasses.add(clazz);
//            }
//        }
//        return annotatedClasses;
    }

//    private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
//        List<Class<?>> classes = new ArrayList<>();
//        if (!directory.exists()) {
//            return classes;
//        }
//        File[] files = directory.listFiles();
//        if (files != null) {
//            for (File file : files) {
//                if (file.isDirectory()) {
//                    assert !file.getName().contains(".");
//                    classes.addAll(findClasses(file, packageName + "." + file.getName()));
//                } else if (file.getName().endsWith(".class")) {
//                    classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
//                }
//            }
//        }
//        return classes;
//    }

}
