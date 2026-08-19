package org.bourbon.compiler.effects;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

final class EffectContext {
    private EffectContext() {
        throw new UnsupportedOperationException("Cannot instantiate");
    }

    static ScopedValue<Map<Class<?>, Object>> ACTIVE_HANDLERS = ScopedValue.newInstance();

    static <H> H get(Class<H> handlerType) {
        if (!ACTIVE_HANDLERS.isBound()) {
            throw new IllegalStateException("No effect context found on thread stack. Missing 'Effects.handle(...)' wrapper?");
        }

        Map<Class<?>, Object> handlers = ACTIVE_HANDLERS.get();
        Object handler = handlers.get(handlerType);

        if (handler == null) {
            throw new NoSuchElementException("No handler bound for effect: " + handlerType.getName());
        }

        return handlerType.cast(handler);
    }

    static ScopedValue.Carrier with(Map<Class<?>, Object> handlers) {
        Map<Class<?>, Object> mergedHandlers = new LinkedHashMap<>();
        if (ACTIVE_HANDLERS.isBound()) {
            mergedHandlers.putAll(ACTIVE_HANDLERS.get());
        }
        mergedHandlers.putAll(handlers);
        Map<Class<?>, Object> deterministicMap = Collections.unmodifiableMap(mergedHandlers);

        return ScopedValue.where(ACTIVE_HANDLERS, deterministicMap);
    }
}
