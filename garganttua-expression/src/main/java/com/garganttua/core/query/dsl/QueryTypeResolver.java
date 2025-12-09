package com.garganttua.core.query.dsl;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

public class QueryTypeResolver {

    private static final Map<String, Class<?>> PRIMITIVES = new HashMap<>();
    private static final Map<String, Class<?>> BOXED = new HashMap<>();

    static {
        PRIMITIVES.put("boolean", boolean.class);
        PRIMITIVES.put("byte", byte.class);
        PRIMITIVES.put("short", short.class);
        PRIMITIVES.put("int", int.class);
        PRIMITIVES.put("long", long.class);
        PRIMITIVES.put("float", float.class);
        PRIMITIVES.put("double", double.class);
        PRIMITIVES.put("char", char.class);

        BOXED.put("Boolean", Boolean.class);
        BOXED.put("Byte", Byte.class);
        BOXED.put("Short", Short.class);
        BOXED.put("Integer", Integer.class);
        BOXED.put("Long", Long.class);
        BOXED.put("Float", Float.class);
        BOXED.put("Double", Double.class);
        BOXED.put("Character", Character.class);
        BOXED.put("String", String.class);
    }

    /**
     * Resolve a TypeNode to a Class<?>.
     * For generic types we return the raw class.
     * For arrays we return the array class.
     * For Class<T> we return Class.class (the meta-type).
     */
    public Class<?> resolve(TypeNode node) throws ClassNotFoundException {
        if (node instanceof PrimitiveTypeNode) {
            String name = ((PrimitiveTypeNode) node).name;
            Class<?> p = PRIMITIVES.get(name);
            if (p == null) throw new ClassNotFoundException("Unknown primitive: " + name);
            return p;
        }
        if (node instanceof ClassTypeNode) {
            ClassTypeNode ct = (ClassTypeNode) node;
            String raw = ct.className;
            // try boxed lookups first
            if (BOXED.containsKey(raw)) return BOXED.get(raw);
            // Try java.lang prefix if single simple name
            try {
                return Class.forName(raw);
            } catch (ClassNotFoundException e) {
                // try java.lang.X
                try {
                    return Class.forName("java.lang." + raw);
                } catch (ClassNotFoundException ex) {
                    // try as primitive wrapper short names (Integer -> int handled via BOXED)
                    throw new ClassNotFoundException("Class not found: " + raw);
                }
            }
        }
        if (node instanceof ArrayTypeNode) {
            ArrayTypeNode at = (ArrayTypeNode) node;
            Class<?> comp = resolve(at.elementType);
            // create array class
            Object array = Array.newInstance(comp, 0);
            return array.getClass();
        }
        if (node instanceof ClassOfTypeNode) {
            // Class<T> or Class<?>
            // we return Class.class (java.lang.Class) since that's the runtime class
            return Class.class;
        }
        // fallback
        throw new ClassNotFoundException("Cannot resolve type node: " + node);
    }
}