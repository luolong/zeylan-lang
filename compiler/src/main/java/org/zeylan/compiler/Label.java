package org.zeylan.compiler;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.zeylan.compiler.util.Lists;

/**
 * A label associates a message with a specific source code span.
 * It is used to build rich diagnostics with inline code annotations.
 */
public record Label(
    SourceSpan span,
    @Nullable String message,
    boolean isPrimary
) {

    public static Optional<Label> primary(List<Label> labels) {
        return Lists.findFirst(labels, Label::isPrimary);
    }
}
