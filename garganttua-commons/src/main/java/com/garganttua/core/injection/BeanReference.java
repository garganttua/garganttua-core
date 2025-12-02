package com.garganttua.core.injection;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*Namming rule [provider::][class(simple or FQDN)][!strategy][#name][@qualifier1(simple or FQDN)][@qualifier2(simple or FQDN),...] */
public record BeanReference<Bean>(Class<Bean> type, Optional<BeanStrategy> strategy, Optional<String> name,
        Set<Class<? extends Annotation>> qualifiers) {

    /**
     * Returns the effective name of the bean.
     *
     * <p>
     * If a name is explicitly specified, it is returned. Otherwise, the simple
     * name of the bean type is used as the effective name.
     * </p>
     *
     * @return the effective bean name
     */
    public String effectiveName() {
        if (name.isPresent())
            return name.get();
        return type.getSimpleName();
    }

    public String toReference() {
        StringBuilder sb = new StringBuilder();

        if (type != null) {
            sb.append(type.getName()); 
        }

        strategy.ifPresent(s -> sb.append("!").append(s.name().toLowerCase()));

        name.ifPresent(n -> sb.append("#").append(n));

        if (qualifiers != null && !qualifiers.isEmpty()) {
            for (Class<? extends Annotation> q : qualifiers) {
                sb.append("@").append(q.getName());
            }
        }
        return sb.toString();
    }

    /**
     * Checks if this bean reference matches the provided query reference.
     *
     * <p>
     * This method performs a partial match where the query reference's criteria
     * are checked against this reference's properties:
     * </p>
     * <ul>
     * <li>Type: The query type must be assignable from this reference's type</li>
     * <li>Name: The effective names must match if a name is specified in the
     * query</li>
     * <li>Strategy: The strategies must match if specified in the query</li>
     * <li>Qualifiers: All query qualifiers must be present in this definition</li>
     * </ul>
     *
     * @param query the query reference to match against
     * @return {@code true} if this reference matches the query, {@code false}
     *         otherwise
     * @throws NullPointerException if query is null
     */
    public boolean matches(BeanReference<?> query) {
        Objects.requireNonNull(query, "Query to match cannot be null");

        if (query.type() != null && !query.type().isAssignableFrom(this.type)) {
            return false;
        }

        if (query.name().isPresent() && !query.effectiveName().equals(this.effectiveName())) {
            return false;
        }

        if (query.strategy().isPresent() && !query.strategy().equals(this.strategy)) {
            return false;
        }

        if (query.qualifiers() != null && !query.qualifiers().isEmpty() && !this.qualifiers.containsAll(query.qualifiers())) {
            return false;
        }

        return true;
    }

    @Override
    public final String toString() {
        return "BeanReference[type=" + (type != null ? type.getName() : "null") +
                ", strategy=" + (strategy.isPresent() ? strategy.get().name() : "default") +
                ", name=" + (name.isPresent() ? name.get() : "type-based") +
                ", qualifiers=" + (qualifiers != null && !qualifiers.isEmpty() ? qualifiers.toString() : "none") + "]";
    }
    
    boolean isEmpty(){
        return type == null && strategy.isEmpty() && name.isEmpty() && qualifiers.isEmpty();
    }

    public static Optional<String> extractProvider(String ref) {
        if (ref.contains("::")) {
            String[] parts = ref.split("::", 2);
            String provider = parts[0].trim();
            return provider.isEmpty() ? Optional.empty() : Optional.of(provider);
        }
        return Optional.empty();
    }

    public static Optional<String> extractClass(String ref) {
        String work = ref;
        if (work.contains("::")) {
            work = work.split("::", 2)[1].trim();
        }
        String pattern = "^[^!#@]+";
        Matcher m = Pattern.compile(pattern).matcher(work);
        if (m.find()) {
            String cls = m.group().trim();
            return cls.isEmpty() ? Optional.empty() : Optional.of(cls);
        }
        return Optional.empty();
    }

    public static Optional<String> extractStrategy(String ref) {
        Matcher m = Pattern.compile("!(\\w+)").matcher(ref);
        if (m.find()) {
            return Optional.of(m.group(1).trim());
        }
        return Optional.empty();
    }

    public static Optional<String> extractName(String ref) {
        Matcher m = Pattern.compile("#([^@]+)").matcher(ref);
        if (m.find()) {
            return Optional.of(m.group(1).trim());
        }
        return Optional.empty();
    }

    public static Set<String> extractQualifiers(String ref) {
        Set<String> qualifiers = new HashSet<>();
        Matcher m = Pattern.compile("@([^!#@]+)").matcher(ref);
        while (m.find()) {
            String q = m.group(1).trim();
            if (!q.isEmpty()) {
                qualifiers.add(q);
            }
        }
        return qualifiers;
    }

    @SuppressWarnings("unchecked")
    public static Pair<Optional<String>, BeanReference<?>> parse(String ref) throws DiException {

        if (ref == null || ref.isBlank()) {
            throw new DiException("Bean reference cannot be null or empty");
        }

        Optional<String> provider = extractProvider(ref);
        Optional<String> clsStr = extractClass(ref);
        Optional<BeanStrategy> strategy = extractStrategy(ref).map(s -> BeanStrategy.valueOf(s.toLowerCase()));
        Optional<String> name = extractName(ref);
        Set<Class<? extends Annotation>> qualifierClasses = new HashSet<>();

        for (String q : extractQualifiers(ref)) {
            try {
                if (q.contains(".")) {
                    qualifierClasses.add((Class<? extends Annotation>) Class.forName(q));
                } 
            } catch (ClassNotFoundException e) {
                throw new DiException("Qualifier class not found: " + q, e);
            }
        }

        Class<?> type = null;
        if (clsStr.isPresent()) {
            String clsName = clsStr.get();
            try {
                if (clsName.contains(".")) {
                    type = Class.forName(clsName);
                } else {
                    type = null;
                }
            } catch (ClassNotFoundException e) {
                throw new DiException("Class not found: " + clsName, e);
            }
        }

        BeanReference<?> reference = new BeanReference<>(type, strategy, name, qualifierClasses);

        if( reference.isEmpty()){
            throw new DiException("Bean reference must specify at least a class, strategy, name, or qualifier");
        }
        return new Pair<>(provider, reference);
    }
}
