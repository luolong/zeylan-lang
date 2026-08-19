package org.bourbon.compiler.effects;

import java.util.function.Function;

public sealed interface HandlerRegistrar<T, E extends Throwable> extends HandlerExecutor<T, E> permits HandlerRunner {
    /// Register an effect handler.
    <H> HandlerRegistrar<T, E> with(Class<H> handlerType, H handler);
    <EX extends Throwable> HandlerRunner<T, E> onException(Class<EX> exceptionType, Function<EX, E> fallback);
}
