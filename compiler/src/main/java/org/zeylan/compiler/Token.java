package org.zeylan.compiler;

public record Token(
        TokenType type,
        String lexeme,
        Object literal,
        int line,
        int column,
        int startOffset,
        int length) {
}
