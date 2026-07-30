package org.zeylan.compiler;

import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

public record SourceSpan(
        @Nullable Path filepath,
        int line,
        int column,
        int startOffset,
        int length) {

    public interface Provider {
        SourceSpan at(int line, int column, int startOffset, int length);
    }
}
