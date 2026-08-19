package org.bourbon.compiler.effects;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;

import org.bourbon.compiler.util.Exceptions;

public final class HandlerRunner<T, E extends Throwable> implements HandlerRegistrar<T, E> {
    private final Callable<T> computation;
    private final Map<Class<?>, Object> handlers = new HashMap<>();
    private final Map<Class<? extends Throwable>, Function<Throwable, T>> exceptionHandlers = new HashMap<>();

    HandlerRunner(Callable<T> computation) {
        this.computation = computation;
    }

    public <T2> HandlerRunner<T2, E> andThen(Function<T, T2> next) {
        return new HandlerRunner<>(() -> next.apply(computation.call()));
    }

    @Override
    public <H> HandlerRegistrar<T, E> with(Class<H> handlerType, H handler) {
        this.handlers.put(handlerType, handler);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <EX extends Throwable> HandlerRunner<T, E> onException(Class<EX> exceptionType, Function<EX, E> fallback) {
        this.exceptionHandlers.put(exceptionType, (Function<Throwable, T>) fallback);
        return this;
    }

    public T call() throws E {
        try {
            // Execute inside ScopedValue frame (thread/fiber safe, zero thread-local leak)
            return EffectContext.with(handlers).call(this::callWithEffects);
        } catch (Exception e) {
            throw Exceptions.sneakyThrow(e);
        }
    }

    private T callWithEffects() throws Exception {
        try {
            return computation.call();
        } catch (Exception e) {
            for (var entry : exceptionHandlers.entrySet()) {
                if (entry.getKey().isInstance(e)) {
                    return entry.getValue().apply(e);
                }
            }

            throw e;
        }
    }
}
