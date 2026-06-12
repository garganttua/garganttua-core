package com.garganttua.core.aot.annotation.processor;

import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Shared helpers for translating javac model artefacts into Java source
 * fragments used by the various AOT source generators.
 */
final class TypeNames {

    private TypeNames() {}

    /**
     * Fully-qualified name suitable for use in generated source (e.g.
     * {@code java.lang.String}, {@code int[]}).
     *
     * <p>Type variables are erased to their bound (or {@code java.lang.Object})
     * before rendering, so a generic method/field never leaks a bare type
     * variable name (e.g. {@code "T"}) into the generated descriptor — that
     * would be an out-of-scope symbol in the generated class and fail to
     * compile. Erasure also drops type arguments from parameterized types
     * ({@code List<String>} → {@code java.util.List}), matching what the raw
     * descriptor strings and {@code invoke}/{@code newInstance} casts need.</p>
     */
    static String getTypeName(Types types, TypeMirror typeMirror) {
        TypeMirror erased = needsErasure(typeMirror) ? types.erasure(typeMirror) : typeMirror;
        return render(erased);
    }

    /**
     * Kinds that may embed type variables (directly or in their bounds /
     * components / type arguments) and must therefore be erased before being
     * rendered into generated source. Primitives, {@code void} and the like
     * are rendered verbatim — {@code Types.erasure} is not defined for them.
     */
    private static boolean needsErasure(TypeMirror typeMirror) {
        return switch (typeMirror.getKind()) {
            case DECLARED, ARRAY, TYPEVAR, INTERSECTION, WILDCARD -> true;
            default -> false;
        };
    }

    private static String render(TypeMirror typeMirror) {
        return switch (typeMirror.getKind()) {
            case DECLARED -> {
                DeclaredType declaredType = (DeclaredType) typeMirror;
                javax.lang.model.element.TypeElement element =
                        (javax.lang.model.element.TypeElement) declaredType.asElement();
                yield element.getQualifiedName().toString();
            }
            case ARRAY -> {
                ArrayType arrayType = (ArrayType) typeMirror;
                yield render(arrayType.getComponentType()) + "[]";
            }
            case VOID -> "void";
            default -> typeMirror.toString();
        };
    }

    /** Translates javac modifiers into the {@link java.lang.reflect.Modifier} bitmask. */
    static int toReflectModifiers(Set<Modifier> modifiers) {
        int flags = 0;
        for (Modifier mod : modifiers) {
            flags |= switch (mod) {
                case PUBLIC -> java.lang.reflect.Modifier.PUBLIC;
                case PROTECTED -> java.lang.reflect.Modifier.PROTECTED;
                case PRIVATE -> java.lang.reflect.Modifier.PRIVATE;
                case ABSTRACT -> java.lang.reflect.Modifier.ABSTRACT;
                case STATIC -> java.lang.reflect.Modifier.STATIC;
                case FINAL -> java.lang.reflect.Modifier.FINAL;
                case TRANSIENT -> java.lang.reflect.Modifier.TRANSIENT;
                case VOLATILE -> java.lang.reflect.Modifier.VOLATILE;
                case SYNCHRONIZED -> java.lang.reflect.Modifier.SYNCHRONIZED;
                case NATIVE -> java.lang.reflect.Modifier.NATIVE;
                case STRICTFP -> java.lang.reflect.Modifier.STRICT;
                default -> 0;
            };
        }
        return flags;
    }

    /** Java keyword for primitive type names, or {@code null} if not a primitive. */
    static String primitiveKind(TypeMirror type) {
        return switch (type.getKind()) {
            case BOOLEAN -> "boolean";
            case BYTE -> "byte";
            case CHAR -> "char";
            case SHORT -> "short";
            case INT -> "int";
            case LONG -> "long";
            case FLOAT -> "float";
            case DOUBLE -> "double";
            default -> null;
        };
    }

    /** Wrapper class for a primitive type name, or {@code null} if not a primitive name. */
    static String primitiveWrapper(String primitive) {
        return switch (primitive) {
            case "boolean" -> "Boolean";
            case "byte" -> "Byte";
            case "char" -> "Character";
            case "short" -> "Short";
            case "int" -> "Integer";
            case "long" -> "Long";
            case "float" -> "Float";
            case "double" -> "Double";
            default -> null;
        };
    }
}
