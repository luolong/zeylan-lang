package org.bourbon.compiler;

import static java.util.stream.Collectors.joining;

import org.jspecify.annotations.Nullable;

public record Token(
        TokenType type,
        String lexeme,
        @Nullable Object literal,
        int line,
        int column,
        int startOffset,
        int length) {

    @Override
    public String toString() {
        var literalString = switch (literal) {
            case null -> "null";
            case String s -> "\"" + escape(s) + "\"";
            default -> literal.toString();
        };
        return "Token[type=" + type + ", lexeme=" + escape(lexeme) + ", literal=" + literalString + ", line=" + line + ", column=" + column + ", startOffset=" + startOffset + ", length=" + length + "]";
    }

    private static String escape(String s) {
        return s.chars().mapToObj(c -> (char) c).map(c -> switch (c) {
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            case '\b' -> "\\b";
            case '\f' -> "\\f";
            case '\\' -> "\\\\";
            case '"' -> "\\\"";
            default -> String.valueOf(c);
        }).collect(joining());
    }

}
