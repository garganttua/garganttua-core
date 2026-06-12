package com.garganttua.core.aot.annotation.processor;

import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Generates a typed subclass of {@code AOTMethod} for one declared method,
 * with {@code invoke} implemented as a direct call — no {@link java.lang.reflect.Method}
 * involved at runtime.
 */
final class AOTMethodSourceGenerator {

    private final ExecutableElement method;
    private final Types types;
    private final String packageName;
    private final String enclosingSimpleName;
    private final String enclosingQualifiedName;
    private final String generatedSimpleName;
    private final boolean isStatic;
    private final boolean isVoid;

    AOTMethodSourceGenerator(Types types, TypeElement enclosing, ExecutableElement method, String generatedSimpleName) {
        this.types = types;
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
           .append(TypeNames.getTypeName(types, method.getReturnType())).append("\", ")
           .append(buildStringArray(typeNames(params))).append(", ")
           .append(buildStringArray(paramNames(params))).append(", ")
           .append(TypeNames.toReflectModifiers(method.getModifiers())).append(", ")
           .append("new Annotation[0], false, ")
           .append(method.getModifiers().contains(Modifier.DEFAULT)).append(", ")
           .append(method.isVarArgs()).append(", ")
           .append(buildStringArray(exceptionTypeNames())).append(");\n");
        src.append("    }\n\n");

        // invoke(Object, Object...) — wraps the call in a try/catch that
        // sneaky-throws checked exceptions. The signature of invoke does
        // NOT declare them, so we use the generic-erasure trick to bypass
        // javac's checked-exception verification at the call site without
        // wrapping the original throwable (no RuntimeException(t) — the
        // framework expects the original exception type).
        src.append("    @Override\n");
        src.append("    public Object invoke(Object obj, Object... args) {\n");
        String receiver = isStatic
                ? enclosingSimpleName
                : "((" + enclosingSimpleName + ") obj)";
        String call = receiver + "." + method.getSimpleName() + "(" + buildArgCasts(params) + ")";
        boolean hasChecked = !method.getThrownTypes().isEmpty();
        if (hasChecked) {
            src.append("        try {\n");
            if (isVoid) {
                src.append("            ").append(call).append(";\n");
                src.append("            return null;\n");
            } else {
                src.append("            return ").append(call).append(";\n");
            }
            src.append("        } catch (RuntimeException | Error __e) {\n");
            src.append("            throw __e;\n");
            src.append("        } catch (Throwable __t) {\n");
            src.append("            throw ").append(generatedSimpleName).append(".__sneakyThrow(__t);\n");
            src.append("        }\n");
        } else if (isVoid) {
            src.append("        ").append(call).append(";\n");
            src.append("        return null;\n");
        } else {
            src.append("        return ").append(call).append(";\n");
        }
        src.append("    }\n");

        if (hasChecked) {
            src.append("\n");
            src.append("    @SuppressWarnings(\"unchecked\")\n");
            src.append("    private static <E extends Throwable> RuntimeException __sneakyThrow(Throwable t) throws E {\n");
            src.append("        throw (E) t;\n");
            src.append("    }\n");
        }

        src.append("}\n");
        return src.toString();
    }

    private String[] typeNames(List<? extends VariableElement> params) {
        String[] out = new String[params.size()];
        for (int i = 0; i < params.size(); i++) {
            out[i] = TypeNames.getTypeName(types, params.get(i).asType());
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
            out[i] = TypeNames.getTypeName(types, thrown.get(i));
        }
        return out;
    }

    private String buildArgCasts(List<? extends VariableElement> params) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(castArg(types, params.get(i).asType(), i));
        }
        return sb.toString();
    }

    static String castArg(Types types, TypeMirror type, int index) {
        String primitive = TypeNames.primitiveKind(type);
        if (primitive != null) {
            String wrapper = TypeNames.primitiveWrapper(primitive);
            return "(" + wrapper + ") args[" + index + "]";
        }
        return "(" + TypeNames.getTypeName(types, type) + ") args[" + index + "]";
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
