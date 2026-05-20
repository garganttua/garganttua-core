package com.garganttua.core.aot.annotation.processor;

import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Generates a typed subclass of {@code AOTMethod} for one declared method,
 * with {@code invoke} implemented as a direct call — no {@link java.lang.reflect.Method}
 * involved at runtime.
 */
final class AOTMethodSourceGenerator {

    private final ExecutableElement method;
    private final String packageName;
    private final String enclosingSimpleName;
    private final String enclosingQualifiedName;
    private final String generatedSimpleName;
    private final boolean isStatic;
    private final boolean isVoid;

    AOTMethodSourceGenerator(TypeElement enclosing, ExecutableElement method, String generatedSimpleName) {
        this.method = method;
        this.generatedSimpleName = generatedSimpleName;
        this.enclosingQualifiedName = enclosing.getQualifiedName().toString();
        this.enclosingSimpleName = enclosing.getSimpleName().toString();
        int lastDot = enclosingQualifiedName.lastIndexOf('.');
        this.packageName = lastDot > 0 ? enclosingQualifiedName.substring(0, lastDot) : "";
        this.isStatic = method.getModifiers().contains(Modifier.STATIC);
        this.isVoid = method.getReturnType().getKind() == TypeKind.VOID;
    }

    String getGeneratedQualifiedName() {
        return packageName.isEmpty() ? generatedSimpleName : packageName + "." + generatedSimpleName;
    }

    String generate() {
        List<? extends VariableElement> params = method.getParameters();
        StringBuilder src = new StringBuilder();
        if (!packageName.isEmpty()) {
            src.append("package ").append(packageName).append(";\n\n");
        }
        src.append("import com.garganttua.core.aot.reflection.AOTMethod;\n");
        src.append("import java.lang.annotation.Annotation;\n\n");

        src.append("/** AOT method descriptor for {@code ").append(enclosingSimpleName)
           .append('.').append(method.getSimpleName()).append("(...)} — generated, do not edit. */\n");
        src.append("@SuppressWarnings(\"all\")\n");
        src.append("public final class ").append(generatedSimpleName).append(" extends AOTMethod {\n\n");
        src.append("    public static final ").append(generatedSimpleName)
           .append(" INSTANCE = new ").append(generatedSimpleName).append("();\n\n");

        // private no-arg ctor → super(...)
        src.append("    private ").append(generatedSimpleName).append("() {\n");
        src.append("        super(\"").append(method.getSimpleName()).append("\", \"")
           .append(enclosingQualifiedName).append("\", \"")
           .append(TypeNames.getTypeName(method.getReturnType())).append("\", ")
           .append(buildStringArray(typeNames(params))).append(", ")
           .append(buildStringArray(paramNames(params))).append(", ")
           .append(TypeNames.toReflectModifiers(method.getModifiers())).append(", ")
           .append("new Annotation[0], false, ")
           .append(method.getModifiers().contains(Modifier.DEFAULT)).append(", ")
           .append(method.isVarArgs()).append(", ")
           .append(buildStringArray(exceptionTypeNames())).append(");\n");
        src.append("    }\n\n");

        // invoke(Object, Object...)
        src.append("    @Override\n");
        src.append("    public Object invoke(Object obj, Object... args) {\n");
        String receiver = isStatic
                ? enclosingSimpleName
                : "((" + enclosingSimpleName + ") obj)";
        String call = receiver + "." + method.getSimpleName() + "(" + buildArgCasts(params) + ")";
        if (isVoid) {
            src.append("        ").append(call).append(";\n");
            src.append("        return null;\n");
        } else {
            src.append("        return ").append(call).append(";\n");
        }
        src.append("    }\n");

        src.append("}\n");
        return src.toString();
    }

    private String[] typeNames(List<? extends VariableElement> params) {
        String[] out = new String[params.size()];
        for (int i = 0; i < params.size(); i++) {
            out[i] = TypeNames.getTypeName(params.get(i).asType());
        }
        return out;
    }

    private String[] paramNames(List<? extends VariableElement> params) {
        String[] out = new String[params.size()];
        for (int i = 0; i < params.size(); i++) {
            out[i] = params.get(i).getSimpleName().toString();
        }
        return out;
    }

    private String[] exceptionTypeNames() {
        List<? extends TypeMirror> thrown = method.getThrownTypes();
        String[] out = new String[thrown.size()];
        for (int i = 0; i < thrown.size(); i++) {
            out[i] = TypeNames.getTypeName(thrown.get(i));
        }
        return out;
    }

    private String buildArgCasts(List<? extends VariableElement> params) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(castArg(params.get(i).asType(), i));
        }
        return sb.toString();
    }

    static String castArg(TypeMirror type, int index) {
        String primitive = TypeNames.primitiveKind(type);
        if (primitive != null) {
            String wrapper = TypeNames.primitiveWrapper(primitive);
            return "(" + wrapper + ") args[" + index + "]";
        }
        return "(" + TypeNames.getTypeName(type) + ") args[" + index + "]";
    }

    static String buildStringArray(String[] values) {
        if (values.length == 0) return "new String[0]";
        StringBuilder sb = new StringBuilder("new String[]{");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append('"').append(values[i]).append('"');
        }
        sb.append('}');
        return sb.toString();
    }
}
