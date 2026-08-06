package org.zeylan.compiler;

import org.jspecify.annotations.Nullable;

public record Token(
        TokenType type,
        String lexeme,
        @Nullable Object literal,
        int line,
        int column,
        int startOffset,
        int length) {
}
