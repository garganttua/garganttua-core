package com.garganttua.core.aot.annotation.processor;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Generates a typed subclass of {@code AOTField} for one declared field,
 * with {@code get} / {@code set} (and the typed primitive variants when
 * applicable) implemented as direct field access — no {@link java.lang.reflect.Field}
 * involved at runtime.
 */
final class AOTFieldSourceGenerator {

    private final VariableElement field;
    private final String packageName;
    private final String enclosingSimpleName;
    private final String enclosingQualifiedName;
    private final String generatedSimpleName;
    private final String fieldTypeName;
    private final String primitiveKind;
    private final boolean isStatic;
    private final boolean isFinal;

    AOTFieldSourceGenerator(Types types, TypeElement enclosing, VariableElement field) {
        this.field = field;
        this.enclosingQualifiedName = enclosing.getQualifiedName().toString();
        this.enclosingSimpleName = enclosing.getSimpleName().toString();
        int lastDot = enclosingQualifiedName.lastIndexOf('.');
        this.packageName = lastDot > 0 ? enclosingQualifiedName.substring(0, lastDot) : "";
        this.generatedSimpleName = AOTNaming.fieldDescriptorName(enclosing, field);
        TypeMirror type = field.asType();
        this.fieldTypeName = TypeNames.getTypeName(types, type);
        this.primitiveKind = TypeNames.primitiveKind(type);
        this.isStatic = field.getModifiers().contains(Modifier.STATIC);
        this.isFinal = field.getModifiers().contains(Modifier.FINAL);
    }

    String getGeneratedQualifiedName() {
        return packageName.isEmpty() ? generatedSimpleName : packageName + "." + generatedSimpleName;
    }

    String generate() {
        StringBuilder src = new StringBuilder();
        if (!packageName.isEmpty()) {
            src.append("package ").append(packageName).append(";\n\n");
        }
        src.append("import com.garganttua.core.aot.reflection.AOTField;\n");
        src.append("import java.lang.annotation.Annotation;\n\n");

        src.append("/** AOT field descriptor for {@code ").append(enclosingSimpleName)
           .append('.').append(field.getSimpleName()).append("} — generated, do not edit. */\n");
        src.append("@SuppressWarnings(\"all\")\n");
        src.append("public final class ").append(generatedSimpleName).append(" extends AOTField {\n\n");
        src.append("    public static final ").append(generatedSimpleName)
           .append(" INSTANCE = new ").append(generatedSimpleName).append("();\n\n");

        // private no-arg ctor — pass metadata to the parent
        src.append("    private ").append(generatedSimpleName).append("() {\n");
        src.append("        super(\"").append(field.getSimpleName()).append("\", \"")
           .append(enclosingQualifiedName).append("\", \"")
           .append(fieldTypeName).append("\", ")
           .append(TypeNames.toReflectModifiers(field.getModifiers())).append(", ")
           .append("new Annotation[0], ").append(buildGenericTypeExpr()).append(");\n");
        src.append("    }\n\n");

        // get(Object) — direct read, with autoboxing for primitives
        src.append("    @Override\n");
        src.append("    public Object get(Object obj) {\n");
        src.append("        return ").append(readAccess()).append(";\n");
        src.append("    }\n\n");

        // set(Object, Object) — direct write (UOE for final fields)
        src.append("    @Override\n");
        src.append("    public void set(Object obj, Object value) {\n");
        if (isFinal) {
            src.append("        throw new UnsupportedOperationException(\"Cannot set final field ")
               .append(enclosingQualifiedName).append('.').append(field.getSimpleName())
               .append(" in AOT mode\");\n");
        } else if (primitiveKind != null) {
            String wrapper = TypeNames.primitiveWrapper(primitiveKind);
            src.append("        ").append(writeTarget()).append(" = (").append(wrapper).append(") value;\n");
        } else {
            src.append("        ").append(writeTarget()).append(" = (").append(fieldTypeName).append(") value;\n");
        }
        src.append("    }\n");

        // Typed primitive variants
        if (primitiveKind != null) {
            String capPrim = capitalize(primitiveKind);
            src.append("\n    @Override\n");
            src.append("    public ").append(primitiveKind).append(" get").append(capPrim)
               .append("(Object obj) {\n");
            src.append("        return ").append(readAccess()).append(";\n");
            src.append("    }\n");

            src.append("\n    @Override\n");
            src.append("    public void set").append(capPrim).append("(Object obj, ").append(primitiveKind)
               .append(" v) {\n");
            if (isFinal) {
                src.append("        throw new UnsupportedOperationException(\"Cannot set final field ")
                   .append(enclosingQualifiedName).append('.').append(field.getSimpleName())
                   .append(" in AOT mode\");\n");
            } else {
                src.append("        ").append(writeTarget()).append(" = v;\n");
            }
            src.append("    }\n");
        }

        src.append("}\n");
        return src.toString();
    }

    /**
     * Generated-source expression for the field's generic type. For a
     * parameterized field whose type arguments are all plain classes (e.g.
     * {@code List<String>}, {@code Map<String,Integer>}) emits
     * {@code AOTParameterizedType.of(List.class, String.class)} so that
     * {@code AOTField.getGenericType()} reports a real {@link
     * java.lang.reflect.ParameterizedType} under AOT, just like runtime
     * reflection. Returns {@code "null"} (the previous erased-type behaviour)
     * for non-generic fields, raw types, or arguments that can't be a
     * {@code .class} literal (wildcards, type variables, nested generics).
     */
    private String buildGenericTypeExpr() {
        TypeMirror type = field.asType();
        if (type.getKind() != TypeKind.DECLARED) {
            return "null";
        }
        DeclaredType declared = (DeclaredType) type;
        List<? extends TypeMirror> args = declared.getTypeArguments();
        if (args.isEmpty()) {
            return "null";
        }
        String raw = rawClassLiteral(declared);
        if (raw == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(
                "com.garganttua.core.aot.reflection.AOTParameterizedType.of(").append(raw);
        for (TypeMirror arg : args) {
            String argLiteral = plainClassLiteral(arg);
            if (argLiteral == null) {
                return "null";
            }
            sb.append(", ").append(argLiteral);
        }
        return sb.append(")").toString();
    }

    /** {@code <qualified>.class} for the erasure of a declared type. */
    private static String rawClassLiteral(DeclaredType declared) {
        Element element = declared.asElement();
        return (element instanceof TypeElement typeElement)
                ? typeElement.getQualifiedName().toString() + ".class"
                : null;
    }

    /** Class literal for a plain declared type with no further type arguments. */
    private static String plainClassLiteral(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return null;
        }
        DeclaredType declared = (DeclaredType) type;
        if (!declared.getTypeArguments().isEmpty()) {
            return null;
        }
        return rawClassLiteral(declared);
    }

    private String readAccess() {
        return isStatic
                ? enclosingSimpleName + "." + field.getSimpleName()
                : "((" + enclosingSimpleName + ") obj)." + field.getSimpleName();
    }

    private String writeTarget() {
        return readAccess();
    }

    private static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
