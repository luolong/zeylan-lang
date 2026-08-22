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
        var position = line + ":" + column;
        var range = "[" + startOffset+ ".." + end() + "]";
        return String.join(" ", type.name(), escape(lexeme), literalString, "@", position, range);
    }

    public int end() {
        return startOffset + length;
    }

    private static String escape(String s) {
        if (s.isEmpty()) return "";

        if (s.length() > 1) {
            if (s.contains("\""))
                return s.chars().mapToObj(c -> (char) c)
                        .map(c -> c == '"' ? String.valueOf(c) : escape(c))
                        .collect(joining("", "'", "'"));

            if (s.contains("'") || s.contains(" "))
                return s.chars().mapToObj(c -> (char) c)
                        .map(c -> c == '\'' ? String.valueOf(c) : escape(c))
                        .collect(joining("", "\"", "\""));

            return s.chars().mapToObj(c -> (char) c).map(Token::escape).collect(joining());
        }
        return escape(s.charAt(0));
    }

    private static String escape(char c) {
        return switch (c) {
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            case '\b' -> "\\b";
            case '\f' -> "\\f";
            case '\\' -> "\\\\";
            case '"' -> "\\\"";
            default -> String.valueOf(c);
        };
    }

}
