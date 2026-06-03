package com.garganttua.core.injection;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;

/**
 * Immutable identity of a bean within the dependency injection system.
 *
 * <p>
 * A reference combines the bean type, optional {@link BeanStrategy}, optional name,
 * and a set of qualifier annotations. References are used both to declare beans and
 * to query for them via {@link #matches(BeanReference)}.
 * </p>
 *
 * <p>
 * References serialize to and parse from the textual naming rule:
 * {@code [provider::][class(simple or FQDN)][!strategy][#name][@qualifier1(simple or FQDN)][@qualifier2(simple or FQDN),...]}
 * </p>
 *
 * @param <Bean>     the bean type this reference identifies
 * @param type       the bean type
 * @param strategy   the optional lifecycle strategy (scope)
 * @param name       the optional bean name
 * @param qualifiers the qualifier annotation types attached to the bean
 * @since 2.0.0-ALPHA01
 * @see BeanStrategy
 * @see BeanDefinition
 */
public record BeanReference<Bean>(IClass<Bean> type, Optional<BeanStrategy> strategy, Optional<String> name,
        Set<IClass<? extends Annotation>> qualifiers) {
    private static final Logger log = Logger.getLogger(BeanReference.class);

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
        log.trace("Entering effectiveName");
        String result;
        if (name.isPresent()) {
            result = name.get();
        } else {
            result = type.getSimpleName();
        }
        log.trace("Exiting effectiveName with result={}", result);
        return result;
    }

    /**
     * Serializes this reference to its textual naming-rule form.
     *
     * @return the reference string in the form
     *         {@code class[!strategy][#name][@qualifier...]}
     */
    public String toReference() {
        StringBuilder sb = new StringBuilder();

        if (type != null) {
            sb.append(type.getName()); 
        }

        strategy.ifPresent(s -> sb.append("!").append(s.name().toLowerCase(java.util.Locale.ROOT)));

        name.ifPresent(n -> sb.append("#").append(n));

        if (qualifiers != null && !qualifiers.isEmpty()) {
            for (IClass<? extends Annotation> q : qualifiers) {
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

    /**
     * Extracts the optional provider segment ({@code provider::}) from a reference string.
     *
     * @param ref the reference string
     * @return the provider name if present, otherwise empty
     */
    public static Optional<String> extractProvider(String ref) {
        if (ref.contains("::")) {
            String[] parts = ref.split("::", 2);
            String provider = parts[0].trim();
            return provider.isEmpty() ? Optional.empty() : Optional.of(provider);
        }
        return Optional.empty();
    }

    /**
     * Extracts the class segment from a reference string (the portion before any
     * {@code !}, {@code #} or {@code @} marker).
     *
     * @param ref the reference string
     * @return the class name if present, otherwise empty
     */
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

    /**
     * Extracts the strategy segment ({@code !strategy}) from a reference string.
     *
     * @param ref the reference string
     * @return the strategy name if present, otherwise empty
     */
    public static Optional<String> extractStrategy(String ref) {
        Matcher m = Pattern.compile("!(\\w+)").matcher(ref);
        if (m.find()) {
            return Optional.of(m.group(1).trim());
        }
        return Optional.empty();
    }

    /**
     * Extracts the name segment ({@code #name}) from a reference string.
     *
     * @param ref the reference string
     * @return the bean name if present, otherwise empty
     */
    public static Optional<String> extractName(String ref) {
        Matcher m = Pattern.compile("#([^@]+)").matcher(ref);
        if (m.find()) {
            return Optional.of(m.group(1).trim());
        }
        return Optional.empty();
    }

    /**
     * Extracts all qualifier segments ({@code @qualifier}) from a reference string.
     *
     * @param ref the reference string
     * @return the set of qualifier names (never {@code null}, may be empty)
     */
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

    /**
     * Parses a textual reference into its provider segment and a {@link BeanReference}.
     *
     * <p>
     * Class and qualifier resolution from their string form is deferred to callers
     * (it requires an {@code IReflectionProvider.forName()}), so the returned
     * reference carries a {@code null} type and empty qualifier set.
     * </p>
     *
     * @param ref the naming rule
     *            {@code [provider::][class(simple or FQDN)][!strategy][#name][@qualifier1][@qualifier2,...]}
     * @return a pair of the optional provider name and the parsed reference
     * @throws DiException if {@code ref} is null/blank or specifies no usable criteria
     */
    public static Pair<Optional<String>, BeanReference<?>> parse(String ref) throws DiException {

        if (ref == null || ref.isBlank()) {
            throw new DiException("Bean reference cannot be null or empty");
        }

        Optional<String> provider = extractProvider(ref);
        Optional<String> clsStr = extractClass(ref);
        Optional<BeanStrategy> strategy = extractStrategy(ref).map(s -> BeanStrategy.valueOf(s.toLowerCase(java.util.Locale.ROOT)));
        Optional<String> name = extractName(ref);
        Set<IClass<? extends Annotation>> qualifierClasses = new HashSet<>();

        // TODO: qualifier resolution requires IReflectionProvider.forName()
        // For now, qualifier resolution from strings is deferred to callers

        IClass<?> type = null;
        // TODO: class resolution requires IReflectionProvider.forName()
        // For now, class resolution from strings is deferred to callers

        BeanReference<?> reference = new BeanReference<>(type, strategy, name, qualifierClasses);

        if( reference.isEmpty()){
            throw new DiException("Bean reference must specify at least a class, strategy, name, or qualifier");
        }
        return new Pair<>(provider, reference);
    }
}
