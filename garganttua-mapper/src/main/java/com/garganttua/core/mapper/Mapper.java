package com.garganttua.core.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.garganttua.core.mapper.annotations.MappingIgnore;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ReflectionException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Mapper implements IMapper {

	protected final Map<MappingKey, CachedMappingConfiguration> mappingConfigurations = new ConcurrentHashMap<>();

	private final MapperConfiguration configuration = new MapperConfiguration();
	private final IReflection reflection;
	private final MappingRules mappingRules;
	private final List<IMappingListener> listeners = new CopyOnWriteArrayList<>();
	private final MapperMetrics metrics = new MapperMetrics();

	private static final ThreadLocal<Set<Object>> VISITED = new ThreadLocal<>();

	public Mapper(IReflection reflection) {
		this.reflection = Objects.requireNonNull(reflection, "IReflection implementation cannot be null");
		this.mappingRules = new MappingRules(reflection);
	}

	private static record MappingKey(IClass<?> source, IClass<?> destination) {
		private MappingKey {
			Objects.requireNonNull(source, "Source class cannot be null");
			Objects.requireNonNull(destination, "Destination class cannot be null");
		}
	}

	@Override
	public <destination> destination map(Object source, destination destination) throws MapperException {
		if (destination == null)
			throw new MapperException("destination cannot be null");
		return this.map(source, (IClass<destination>) this.reflection.getClass(destination.getClass()), destination);
	}

	@Override
	public <destination> destination map(Object source, IClass<destination> destinationClass) throws MapperException {
		if (destinationClass == null)
			throw new MapperException("destination class cannot be null");
		return this.map(source, destinationClass, null);
	}

	@Override
	public <destination> destination map(Object source, IClass<destination> destinationClass, destination destination)
			throws MapperException {
		// Determine if this is the root call (for metrics/listeners/cycle init)
		boolean isRoot = VISITED.get() == null;

		if (isRoot) {
			VISITED.set(Collections.newSetFromMap(new IdentityHashMap<>()));
			notifyBeforeMapping(source, destinationClass);
		}

		long startNanos = isRoot ? System.nanoTime() : 0;

		try {
			if (destinationClass == null)
				destinationClass = (IClass<destination>) this.reflection.getClass(destination.getClass());

			// Cycle detection
			if (source != null) {
				Set<Object> visited = VISITED.get();
				if (!visited.add(source)) {
					if (this.configuration.failOnCycle()) {
						throw new MapperException("Mapping cycle detected for object of type " + source.getClass().getSimpleName());
					} else {
						log.atWarn().log("Cycle detected for {}, returning null", source.getClass().getSimpleName());
						return destination;
					}
				}
			}

			IClass<?> sourceClass = this.reflection.getClass(source.getClass());
			CachedMappingConfiguration cachedConfig = this.getCachedMappingConfiguration(sourceClass, destinationClass);
			MappingConfiguration mappingConfig = cachedConfig.config();

			log.atDebug().log("Mapping {} -> {} ({})", sourceClass.getSimpleName(), destinationClass.getSimpleName(), mappingConfig.mappingDirection());

			destination result;
			switch (mappingConfig.mappingDirection()) {
				case REGULAR:
					result = this.doMapping(mappingConfig.mappingDirection(), destinationClass, destination,
							source, cachedConfig.destinationExecutors());
					break;
				case REVERSE:
					result = this.doMapping(mappingConfig.mappingDirection(), destinationClass, destination,
							source, cachedConfig.sourceExecutors());
					break;
				default:
					result = this.doMapping(mappingConfig.mappingDirection(), destinationClass, destination,
							source, cachedConfig.destinationExecutors());
			}

			if (isRoot) {
				long durationNanos = System.nanoTime() - startNanos;
				int rulesCount = mappingConfig.mappingDirection() == MappingDirection.REGULAR
						? cachedConfig.destinationExecutors().size()
						: cachedConfig.sourceExecutors().size();
				this.metrics.recordMapping(durationNanos, rulesCount);
				notifyAfterMapping(source, result, durationNanos);
			}
			return result;

		} catch (MapperException e) {
			if (isRoot) {
				this.metrics.recordFailure();
				notifyMappingError(source, destinationClass, e);
			}
			throw new MapperException(e.getMessage(), e);
		} finally {
			if (isRoot) {
				VISITED.remove();
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <destination> destination doMapping(MappingDirection mappingDirection,
			IClass<destination> destinationIClass, destination destObject, Object source,
			List<IMappingRuleExecutor> executors) throws MapperException {

		// Record mapping: build via canonical constructor
		if (destinationIClass.isRecord()) {
			return doRecordMapping(mappingDirection, destinationIClass, destObject, source, executors);
		}

		for (IMappingRuleExecutor executor : executors) {
			try {
				destObject = executor.doMapping(destinationIClass, destObject, source);
			} catch (MapperException e) {
				if (this.configuration.failOnError()) {
					throw new MapperException("Unable to do mapping, aborting", e);
				} else {
					log.atWarn().log("Mapping rule failed, ignoring: {}", e.getMessage());
				}
			}
		}

		return destObject;
	}

	@SuppressWarnings("unchecked")
	private <destination> destination doRecordMapping(MappingDirection mappingDirection,
			IClass<destination> destinationIClass, destination destObject, Object source,
			List<IMappingRuleExecutor> executors) throws MapperException {
		try {
			var components = destinationIClass.getRecordComponents();
			Map<String, Object> values = new LinkedHashMap<>();

			// Initialize with default values for primitives, null for objects
			for (var component : components) {
				values.put(component.getName(), defaultValueForClass(component.getType()));
			}

			// Read source fields directly for each record component
			IClass<?> sourceClass = this.reflection.getClass(source.getClass());
			for (var component : components) {
				String name = component.getName();
				try {
					var sourceQuery = this.reflection.query(sourceClass);
					var sourceFields = sourceQuery.find(new com.garganttua.core.reflection.ObjectAddress(name));
					if (!sourceFields.isEmpty()) {
						IField sourceField = (IField) sourceFields.get(sourceFields.size() - 1);
						com.garganttua.core.reflection.fields.FieldAccessor<Object> accessor =
								new com.garganttua.core.reflection.fields.FieldAccessor<>(
										new com.garganttua.core.reflection.fields.ResolvedField(
												new com.garganttua.core.reflection.ObjectAddress(name, false),
												List.of(sourceField)));
						Object val = accessor.getValue(source).single();
						if (val != null) {
							values.put(name, val);
						}
					}
				} catch (ReflectionException e) {
					// Field not found in source, keep default
				}
			}

			// Build the record via canonical constructor using raw reflection
			Class<?>[] paramTypes = new Class<?>[components.length];
			Object[] args = new Object[components.length];
			for (int i = 0; i < components.length; i++) {
				paramTypes[i] = (Class<?>) components[i].getType().getType();
				args[i] = values.get(components[i].getName());
			}
			java.lang.reflect.Constructor<?> ctor = ((Class<?>) destinationIClass.getType()).getDeclaredConstructor(paramTypes);
			ctor.setAccessible(true);
			return (destination) ctor.newInstance(args);

		} catch (MapperException e) {
			throw e;
		} catch (Exception e) {
			throw new MapperException("Record mapping failed: " + e.getMessage(),
					e instanceof Exception ex ? ex : new RuntimeException(e));
		}
	}

	private static Object defaultValueForClass(IClass<?> type) {
		if (type.isPrimitive()) {
			String name = type.getName();
			return switch (name) {
				case "int" -> 0;
				case "long" -> 0L;
				case "double" -> 0.0;
				case "float" -> 0.0f;
				case "boolean" -> false;
				case "byte" -> (byte) 0;
				case "short" -> (short) 0;
				case "char" -> '\0';
				default -> null;
			};
		}
		return null;
	}

	private MappingDirection determineMapingDirection(List<MappingRule> sourceRules,
			List<MappingRule> destinationRules, boolean conventionGenerated) throws MapperException {
		if (conventionGenerated) {
			return MappingDirection.REGULAR;
		}
		if (sourceRules.isEmpty() && !destinationRules.isEmpty()) {
			return MappingDirection.REGULAR;
		} else if (!sourceRules.isEmpty() && destinationRules.isEmpty()) {
			return MappingDirection.REVERSE;
		} else {
			throw new MapperException(
					"Cannot determine mapping direction as source and destination are annotated with mapping rules, or neither has mapping rule");
		}
	}

	@Override
	public Mapper configure(MapperConfigurationItem element, Object value) {
		this.configuration.configure(element, value);
		return this;
	}

	private CachedMappingConfiguration createCachedMappingConfiguration(IClass<?> source, IClass<?> destination)
			throws MapperException {
		log.atDebug().log("Creating mapping config: {} -> {}", source.getSimpleName(), destination.getSimpleName());

		List<MappingRule> destinationRules = this.mappingRules.parse(destination);
		List<MappingRule> sourceRules = this.mappingRules.parse(source);

		boolean conventionGenerated = false;

		// Convention mapping: if no annotations found on either side
		if (destinationRules.isEmpty() && sourceRules.isEmpty() && this.configuration.autoConventionMapping()) {
			destinationRules = this.mappingRules.generateConventionRules(source, destination);
			conventionGenerated = true;
		} else if (this.configuration.autoConventionMapping()) {
			// Complement annotated rules with convention for unmapped fields
			List<MappingRule> conventionRules = this.mappingRules.generateConventionRules(source, destination);
			if (!destinationRules.isEmpty()) {
				destinationRules = complementRules(destinationRules, conventionRules);
			} else if (!sourceRules.isEmpty()) {
				// For reverse-annotated source, complement source rules with convention on destination side
				List<MappingRule> conventionSourceRules = this.mappingRules.generateConventionRules(destination, source);
				sourceRules = complementRules(sourceRules, conventionSourceRules);
			}
		}

		MappingDirection mappingDirection = this.determineMapingDirection(sourceRules, destinationRules, conventionGenerated);
		MappingConfiguration mappingConfig = new MappingConfiguration(source, destination, sourceRules,
				destinationRules, mappingDirection);

		try {
			if (this.configuration.doValidation()) {
				if (mappingDirection == MappingDirection.REVERSE)
					this.mappingRules.validate(destination, destinationRules);
				if (mappingDirection == MappingDirection.REGULAR)
					this.mappingRules.validate(source, sourceRules);
			}
		} catch (MapperException e) {
			if (this.configuration.failOnError()) {
				throw new MapperException("Unable to validate mapping, aborting", e);
			} else {
				log.atWarn().log("Validation failed, ignoring: {}", e.getMessage());
			}
		}

		// Strict mode: check all destination fields are covered
		if (this.configuration.strictMode()) {
			validateStrictCoverage(destination, mappingDirection == MappingDirection.REGULAR ? destinationRules : sourceRules);
		}

		// Pre-compute executors
		List<IMappingRuleExecutor> destExecutors = precomputeExecutors(MappingDirection.REGULAR,
				destinationRules, source, destination);
		List<IMappingRuleExecutor> srcExecutors = precomputeExecutors(MappingDirection.REVERSE,
				sourceRules, source, destination);

		return new CachedMappingConfiguration(mappingConfig, destExecutors, srcExecutors);
	}

	private List<MappingRule> complementRules(List<MappingRule> annotatedRules, List<MappingRule> conventionRules) {
		List<MappingRule> result = new ArrayList<>(annotatedRules);
		Set<String> coveredDest = ConcurrentHashMap.newKeySet();
		for (MappingRule r : annotatedRules) {
			if (r.destinationFieldAddress() != null) {
				coveredDest.add(r.destinationFieldAddress().toString());
			}
		}
		for (MappingRule conv : conventionRules) {
			if (conv.destinationFieldAddress() != null && !coveredDest.contains(conv.destinationFieldAddress().toString())) {
				result.add(conv);
			}
		}
		return result;
	}

	private void validateStrictCoverage(IClass<?> destination, List<MappingRule> rules) throws MapperException {
		Set<String> coveredFields = ConcurrentHashMap.newKeySet();
		for (MappingRule r : rules) {
			if (r.destinationFieldAddress() != null) {
				coveredFields.add(r.destinationFieldAddress().getLastElement());
			}
		}

		IClass<MappingIgnore> mappingIgnoreClass;
		try {
			mappingIgnoreClass = this.reflection.getClass(MappingIgnore.class);
		} catch (Exception e) {
			mappingIgnoreClass = null;
		}

		List<String> uncovered = new ArrayList<>();
		collectUncoveredFields(destination, coveredFields, uncovered, mappingIgnoreClass);

		if (!uncovered.isEmpty()) {
			throw new MapperException("Strict mode: uncovered destination fields: " + uncovered);
		}
	}

	private void collectUncoveredFields(IClass<?> clazz, Set<String> coveredFields, List<String> uncovered,
			IClass<MappingIgnore> mappingIgnoreClass) {
		for (IField field : clazz.getDeclaredFields()) {
			int modifiers = field.getModifiers();
			if (java.lang.reflect.Modifier.isStatic(modifiers)
					|| java.lang.reflect.Modifier.isTransient(modifiers)
					|| field.isSynthetic()) {
				continue;
			}
			if (mappingIgnoreClass != null && field.isAnnotationPresent(mappingIgnoreClass)) {
				continue;
			}
			if (!coveredFields.contains(field.getName())) {
				uncovered.add(clazz.getSimpleName() + "." + field.getName());
			}
		}
		IClass<?> superclass = clazz.getSuperclass();
		if (superclass != null && !superclass.getName().equals("java.lang.Object")) {
			collectUncoveredFields(superclass, coveredFields, uncovered, mappingIgnoreClass);
		}
	}

	private List<IMappingRuleExecutor> precomputeExecutors(MappingDirection direction,
			List<MappingRule> rules, IClass<?> source, IClass<?> destination) throws MapperException {
		if (rules.isEmpty()) {
			return List.of();
		}

		List<IMappingRuleExecutor> executors = new ArrayList<>();
		// source/destination here are the original map(source, destination) pair
		// getRuleExecutor expects: sourceClass = actual runtime source, destClass = actual runtime dest
		IClass<?> resolvedSource = source;
		IClass<?> resolvedDest = destination;

		for (MappingRule rule : rules) {
			try {
				IMappingRuleExecutor executor = this.mappingRules.getRuleExecutor(this, direction, rule,
						resolvedSource, resolvedDest);
				if (executor != null) {
					executors.add(executor);
				}
			} catch (MapperException e) {
				if (this.configuration.failOnError()) {
					throw e;
				} else {
					log.atWarn().log("Skipping rule during precomputation: {}", e.getMessage());
				}
			}
		}
		return executors;
	}

	@Override
	public MappingConfiguration recordMappingConfiguration(IClass<?> source, IClass<?> destination)
			throws MapperException {
		CachedMappingConfiguration cached = createCachedMappingConfiguration(source, destination);
		MappingKey key = new MappingKey(source, destination);
		this.mappingConfigurations.put(key, cached);
		return cached.config();
	}

	@Override
	public MappingConfiguration getMappingConfiguration(IClass<?> source, IClass<?> destination)
			throws MapperException {
		return getCachedMappingConfiguration(source, destination).config();
	}

	private CachedMappingConfiguration getCachedMappingConfiguration(IClass<?> source, IClass<?> destination)
			throws MapperException {
		MappingKey key = new MappingKey(source, destination);
		try {
			return this.mappingConfigurations.computeIfAbsent(key, k -> {
				try {
					return this.createCachedMappingConfiguration(source, destination);
				} catch (MapperException e) {
					throw new MapperRuntimeException("Failed to create mapping configuration", e);
				}
			});
		} catch (MapperRuntimeException e) {
			throw (MapperException) e.getCause();
		}
	}

	@Override
	public void register(MappingConfiguration config) throws MapperException {
		MappingKey key = new MappingKey(config.source(), config.destination());

		List<IMappingRuleExecutor> destExecutors = precomputeExecutors(MappingDirection.REGULAR,
				config.destinationRules(), config.source(), config.destination());
		List<IMappingRuleExecutor> srcExecutors = precomputeExecutors(MappingDirection.REVERSE,
				config.sourceRules(), config.source(), config.destination());

		this.mappingConfigurations.put(key, new CachedMappingConfiguration(config, destExecutors, srcExecutors));
	}

	@Override
	public void addListener(IMappingListener listener) {
		this.listeners.add(Objects.requireNonNull(listener));
	}

	@Override
	public MapperMetrics getMetrics() {
		return this.metrics;
	}

	private void notifyBeforeMapping(Object source, IClass<?> destClass) {
		for (IMappingListener listener : this.listeners) {
			try {
				listener.onBeforeMapping(source, destClass);
			} catch (Exception e) {
				log.atWarn().log("Listener onBeforeMapping failed: {}", e.getMessage());
			}
		}
	}

	private void notifyAfterMapping(Object source, Object dest, long durationNanos) {
		for (IMappingListener listener : this.listeners) {
			try {
				listener.onAfterMapping(source, dest, durationNanos);
			} catch (Exception e) {
				log.atWarn().log("Listener onAfterMapping failed: {}", e.getMessage());
			}
		}
	}

	private void notifyMappingError(Object source, IClass<?> destClass, Exception error) {
		for (IMappingListener listener : this.listeners) {
			try {
				listener.onMappingError(source, destClass, error);
			} catch (Exception e) {
				log.atWarn().log("Listener onMappingError failed: {}", e.getMessage());
			}
		}
	}

	private static class MapperRuntimeException extends RuntimeException {
		public MapperRuntimeException(String message, MapperException cause) {
			super(message, cause);
		}
	}

}
