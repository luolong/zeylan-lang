package org.bourbon.compiler.effects;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/// Lightweight effect system emulation layer for the compiler pipeline implementation.
///
/// Just enough implementation to make future porting to an effect-native self-hosted language easier.
///
/// Two major primitives: handlers and effect contexts.
///
/// == Handlers
///
public final class Effects {
    private Effects() {
        throw new UnsupportedOperationException("Do not instantiate!");
    }

    /// Wrap a void block in an effect context
    public static HandlerRunner<Void, RuntimeException> handle(Runnable body) {
        return new HandlerRunner<>(() -> {
            body.run();
            return null;
        });
    }

    /// Wrap a callable block in an effect context
    public static <T> HandlerRunner<T, Exception> handle(Callable<T> body) {
        return new HandlerRunner<>(body);
    }

    public static <H> H get(Class<H> handlerType) {
        return EffectContext.get(handlerType);
    }
}
