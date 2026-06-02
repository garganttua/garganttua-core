package com.garganttua.core.reflection.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;

/**
 * Class-hierarchy member lookup helpers used by {@link ObjectQuery}.
 *
 * <p>Resolves fields and methods by name walking declared members, implemented
 * interfaces (needed for anonymous classes) and superclasses. Stateless and
 * thread-safe; extracted from {@code ObjectQuery} to keep that type focused on
 * address/path resolution.
 */
final class MemberLookup {

    private MemberLookup() {
    }

    static IField getField(IClass<?> clazz, String name) {
        for (IField f : clazz.getDeclaredFields()) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        IClass<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            return getField(superclass, name);
        }
        return null;
    }

    static IMethod getMethod(IClass<?> clazz, String name) {
        for (IMethod m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        // Traverse interfaces (needed for anonymous classes implementing interfaces)
        for (IClass<?> iface : clazz.getInterfaces()) {
            IMethod m = getMethod(iface, name);
            if (m != null) {
                return m;
            }
        }
        IClass<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            return getMethod(superclass, name);
        }
        return null;
    }

    static List<IMethod> getMethods(IClass<?> clazz, String name) {
        List<IMethod> methods = new ArrayList<>();
        HashSet<String> seenSignatures = new HashSet<>();
        collectMethods(clazz, name, methods, seenSignatures);
        return methods;
    }

    private static void collectMethods(IClass<?> clazz, String name, List<IMethod> methods,
            HashSet<String> seenSignatures) {
        for (IMethod m : clazz.getDeclaredMethods()) {
            addIfNew(m, name, methods, seenSignatures);
        }
        // Traverse interfaces (needed for anonymous classes implementing interfaces)
        for (IClass<?> iface : clazz.getInterfaces()) {
            for (IMethod m : getMethods(iface, name)) {
                addIfNew(m, name, methods, seenSignatures);
            }
        }
        IClass<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            for (IMethod m : getMethods(superclass, name)) {
                addIfNew(m, name, methods, seenSignatures);
            }
        }
    }

    private static void addIfNew(IMethod m, String name, List<IMethod> methods, HashSet<String> seenSignatures) {
        if (!m.getName().equals(name)) {
            return;
        }
        String signature = buildMethodSignature(m);
        if (seenSignatures.add(signature)) {
            methods.add(m);
        }
    }

    static String buildMethodSignature(IMethod method) {
        StringBuilder signature = new StringBuilder(method.getName());
        signature.append("(");
        IClass<?>[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) {
                signature.append(",");
            }
            signature.append(paramTypes[i].getName());
        }
        signature.append(")");
        return signature.toString();
    }
}
