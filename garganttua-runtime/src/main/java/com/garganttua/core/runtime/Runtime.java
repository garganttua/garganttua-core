package com.garganttua.core.runtime;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.execution.ExecutorChain;
import com.garganttua.core.execution.IExecutorChain;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.observability.IObservable;
import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservabilityEmitter;
import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.observability.ObservableRegistry;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;
import com.github.f4b6a3.uuid.UuidCreator;

/**
 * Default {@link IRuntime} implementation: orchestrates an ordered set of
 * {@link IRuntimeStep steps} over a dedicated child {@link IRuntimeContext}.
 *
 * <p>Each execution creates its own context (seeded with preset variables),
 * builds an {@link IExecutorChain} from the steps, runs it, and collects an
 * {@link IRuntimeResult}. The runtime is {@link IObservable}: start/end/error
 * events are emitted under the {@code runtime:<name>} source.</p>
 *
 * @param <InputType>  the runtime input type
 * @param <OutputType> the runtime output type
 */
public class Runtime<InputType, OutputType>
		implements IRuntime<InputType, OutputType>, IObservable {
    private static final Logger log = Logger.getLogger(Runtime.class);

	private final String name;
	private final IInjectionContext injectionContext;
	private final Class<InputType> inputType;
	private final Class<OutputType> outputType;
	private final Map<String, IRuntimeStep<?, InputType, OutputType>> steps;
	private final Map<String, ISupplier<?>> presetVariables;
	private final ObservableRegistry observers = new ObservableRegistry();

	/**
	 * Creates a runtime.
	 *
	 * @param name             the runtime name (used as observability source prefix)
	 * @param steps            ordered steps to execute, keyed by step name
	 * @param injectionContext the parent injection context used to spawn child contexts
	 * @param inputType        the declared input type
	 * @param outputType       the declared output type
	 * @param variables        preset variables supplied to each execution context
	 */
	public Runtime(
			String name,
			Map<String, IRuntimeStep<?, InputType, OutputType>> steps,
			IInjectionContext injectionContext,
			Class<InputType> inputType,
			Class<OutputType> outputType,
			Map<String, ISupplier<?>> variables) {

		log.trace(
				"[Runtime.<init>] Initializing Runtime with name={}, inputType={}, outputType={}, steps={}, presetVariables={}",
				name, inputType, outputType, steps, variables);

		this.steps = Collections.synchronizedMap(
				new java.util.LinkedHashMap<>(Objects.requireNonNull(steps, "Steps map cannot be null")));

		this.inputType = Objects.requireNonNull(inputType, "Input type cannot be null");
		this.outputType = Objects.requireNonNull(outputType, "Output Type cannot be null");
		this.name = Objects.requireNonNull(name, "Name cannot be null");
		this.injectionContext = Objects.requireNonNull(injectionContext, "Context cannot be null");
		this.presetVariables = Collections.synchronizedMap(
				Map.copyOf(Objects.requireNonNull(variables, "Preset variables map cannot be null")));

		log.debug("[Runtime.<init>] Runtime initialized successfully with name={}", this.name);
	}

	@Override
	public void addObserver(IObserver<ObservableEvent> observer) {
		this.observers.addObserver(observer);
	}

	@Override
	public void removeObserver(IObserver<ObservableEvent> observer) {
		this.observers.removeObserver(observer);
	}

	@Override
	public Optional<IRuntimeResult<InputType, OutputType>> execute(InputType input) throws RuntimeException {
		return this.execute(UuidCreator.getTimeOrderedEpoch(), input);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Optional<IRuntimeResult<InputType, OutputType>> execute(UUID uuid, InputType input)
			throws RuntimeException {

		// Note: SLF4J MDC removed during diagnostic facade migration. The uuid
		// flows through observability events (StartEvent.executionId etc.)
		// instead. If log-prefix correlation is later needed, a per-thread
		// scope value on IDiagnosticContext can be added.
		log.debug("Starting runtime execution uuid={}", uuid);
		log.trace("Runtime input received");

		IRuntimeContext<InputType, OutputType> runtimeContext = null;
		IRuntimeResult<InputType, OutputType> result = null;
		Integer endCode = null;
		String runtimeSource = "runtime:" + this.name;

		try (ObservabilityEmitter.Scope scope = ObservabilityEmitter.open(this.observers, uuid)) {
			scope.fireStart(runtimeSource);

			try {
				log.debug("Creating runtime context");

				runtimeContext = this.injectionContext
						.newChildContext(IClass.getClass(IRuntimeContext.class), input, this.outputType,
								this.presetVariables, uuid);

				runtimeContext.onInit().onStart();

				log.debug("Building executor chain");

				IExecutorChain<IRuntimeContext<InputType, OutputType>> chain = new ExecutorChain<>(false);

				this.steps.values().forEach(step -> {
					log.trace("Registering step");
					step.defineExecutionStep(chain);
				});

				log.debug("Executing runtime chain");

				chain.execute(runtimeContext);

			} catch (Exception e) {

				log.error("Fatal error during runtime execution", e);

				scope.fireError(runtimeSource, e);
				throw new RuntimeException(e, Optional.ofNullable(runtimeContext));

			} finally {

				if (runtimeContext != null) {
					log.debug("Stopping runtime context");

					endCode = runtimeContext.getCode().orElse(null);

					runtimeContext.onStop();

					result = runtimeContext.getResult();

					log.trace("Runtime result collected");

					runtimeContext.onFlush();
				}

				log.debug("Runtime execution finished");
			}

			scope.fireEnd(runtimeSource, endCode);
		}

		return Optional.ofNullable(result);
	}
}
