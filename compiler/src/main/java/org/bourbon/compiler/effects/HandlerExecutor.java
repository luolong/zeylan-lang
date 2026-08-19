package org.bourbon.compiler.effects;

import org.bourbon.compiler.util.Exceptions;

public sealed interface HandlerExecutor<T, E extends Throwable> permits HandlerRegistrar {

    T call() throws E;

    /** Alias for {@link #call()} matching Supplier convention. */
    default T get() {
        try {
            return call();
        } catch (Throwable e) {
            throw Exceptions.sneakyThrow(e);
        }
    }

    /** Convenience method to execute void computation blocks. */
    default void run() {
        try {
            call();
        } catch (Throwable e) {
            throw Exceptions.sneakyThrow(e);
        }
    }

}
