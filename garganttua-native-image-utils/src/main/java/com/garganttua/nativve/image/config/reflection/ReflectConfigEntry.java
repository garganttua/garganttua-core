package com.garganttua.nativve.image.config.reflection;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_DEFAULT) 
public class ReflectConfigEntry {

    private String name;
    private boolean queryAllDeclaredConstructors = false;
    private boolean queryAllPublicConstructors = false;
    private boolean queryAllConstructors = false;
    private boolean queryAllDeclaredMethods = false;
    private boolean queryAllPublicMethods = false;
    private boolean queryAllMethods = false;
    private boolean allDeclaredClasses = false;
    private boolean allDeclaredFields = false;
    private boolean allPublicFields = false;
    private boolean allPublicClasses = false;
    private boolean allDeclaredConstructors = false;
    private boolean allConstructors = false;
    private boolean allDeclaredMethods = false;
    private List<Field> fields;
    private List<Method> methods;
    
    public ReflectConfigEntry(String name) {
    	this.name = name;
    }
    
    public ReflectConfigEntry() {
    }

    @JsonIgnore
    public Class<?> getEntryClass() throws ClassNotFoundException{
    	return Class.forName(this.name);
    }
    
    public String getName() {
        return name;
    }

    public boolean isQueryAllConstructors() {
        return queryAllConstructors;
    }

    public void setQueryAllConstructors(boolean queryAllConstructors) {
        this.queryAllConstructors = queryAllConstructors;
    }
    
    public boolean isQueryAllMethods() {
        return queryAllMethods;
    }

    public void setQueryAllMethods(boolean queryAllMethods) {
        this.queryAllMethods = queryAllMethods;
    }
    
    public boolean isAllDeclaredMethods() {
        return allDeclaredMethods;
    }

    public void setAllDeclaredMethods(boolean allDeclaredMethods) {
        this.allDeclaredMethods = allDeclaredMethods;
    }

    public boolean isQueryAllDeclaredConstructors() {
        return queryAllDeclaredConstructors;
    }

    public void setQueryAllDeclaredConstructors(boolean queryAllDeclaredConstructors) {
        this.queryAllDeclaredConstructors = queryAllDeclaredConstructors;
    }

    public boolean isQueryAllPublicConstructors() {
        return queryAllPublicConstructors;
    }

    public void setQueryAllPublicConstructors(boolean queryAllPublicConstructors) {
        this.queryAllPublicConstructors = queryAllPublicConstructors;
    }

    public boolean isQueryAllDeclaredMethods() {
        return queryAllDeclaredMethods;
    }

    public void setQueryAllDeclaredMethods(boolean queryAllDeclaredMethods) {
        this.queryAllDeclaredMethods = queryAllDeclaredMethods;
    }

    public boolean isQueryAllPublicMethods() {
        return queryAllPublicMethods;
    }

    public void setQueryAllPublicMethods(boolean queryAllPublicMethods) {
        this.queryAllPublicMethods = queryAllPublicMethods;
    }

    public boolean isAllDeclaredClasses() {
        return allDeclaredClasses;
    }

    public void setAllDeclaredClasses(boolean allDeclaredClasses) {
        this.allDeclaredClasses = allDeclaredClasses;
    }

    public boolean isAllPublicClasses() {
        return allPublicClasses;
    }

    public void setAllPublicClasses(boolean allPublicClasses) {
        this.allPublicClasses = allPublicClasses;
    }

    public boolean isAllDeclaredFields() {
        return allDeclaredFields;
    }

    public void setAllDeclaredFields(boolean allDeclaredFields) {
        this.allDeclaredFields = allDeclaredFields;
    }

    public boolean isAllPublicFields() {
        return allPublicFields;
    }

    public void setAllPublicFields(boolean allPublicFields) {
        this.allPublicFields = allPublicFields;
    }

    public boolean isAllConstructors() {
        return allConstructors;
    }

    public void setAllConstructors(boolean allConstructors) {
        this.allConstructors = allConstructors;
    }

    public boolean isAllDeclaredConstructors() {
        return allDeclaredConstructors;
    }

    public void setAllDeclaredConstructors(boolean allDeclaredConstructors) {
        this.allDeclaredConstructors = allDeclaredConstructors;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public List<Method> getMethods() {
        return methods;
    }

    public void setMethods(List<Method> methods) {
        this.methods = methods;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReflectConfigEntry that = (ReflectConfigEntry) o;
//        return queryAllDeclaredConstructors == that.queryAllDeclaredConstructors &&
//                queryAllPublicConstructors == that.queryAllPublicConstructors &&
//                queryAllDeclaredMethods == that.queryAllDeclaredMethods &&
//                queryAllPublicMethods == that.queryAllPublicMethods &&
//                allDeclaredClasses == that.allDeclaredClasses &&
//                allDeclaredFields == that.allDeclaredFields &&
//                allPublicFields == that.allPublicFields &&
//                allPublicClasses == that.allPublicClasses &&
//                allDeclaredConstructors == that.allDeclaredConstructors &&
//                allConstructors == that.allConstructors &&
               return Objects.equals(name, that.name) ;//&&
//                Objects.equals(fields, that.fields) &&
//                Objects.equals(methods, that.methods);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, queryAllDeclaredConstructors, queryAllPublicConstructors, queryAllDeclaredMethods, queryAllPublicMethods,
                allDeclaredClasses, allDeclaredFields, allPublicFields, allPublicClasses, allDeclaredConstructors, allConstructors, fields, methods);
    }

    public static class Field {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Field field = (Field) o;
            return Objects.equals(name, field.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    public static class Method {
        private String name;
        private List<String> parameterTypes;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getParameterTypes() {
            return parameterTypes;
        }

        public void setParameterTypes(List<String> parameterTypes) {
            this.parameterTypes = parameterTypes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Method method = (Method) o;
            return Objects.equals(name, method.name) &&
                    Objects.equals(parameterTypes, method.parameterTypes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, parameterTypes);
        }
    }
}