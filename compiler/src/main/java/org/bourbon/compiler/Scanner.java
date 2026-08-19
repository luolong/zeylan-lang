package org.bourbon.compiler;

import static org.bourbon.compiler.TokenType.AMPERSAND;
import static org.bourbon.compiler.TokenType.AMPERSAND_AMPERSAND;
import static org.bourbon.compiler.TokenType.AMPERSAND_AMPERSAND_EQUAL;
import static org.bourbon.compiler.TokenType.AMPERSAND_EQUAL;
import static org.bourbon.compiler.TokenType.BACKTICK;
import static org.bourbon.compiler.TokenType.BANG;
import static org.bourbon.compiler.TokenType.BANG_EQUAL;
import static org.bourbon.compiler.TokenType.CARET;
import static org.bourbon.compiler.TokenType.COLON;
import static org.bourbon.compiler.TokenType.COMMA;
import static org.bourbon.compiler.TokenType.DOT;
import static org.bourbon.compiler.TokenType.DOT_DOT;
import static org.bourbon.compiler.TokenType.EOF;
import static org.bourbon.compiler.TokenType.EQUAL;
import static org.bourbon.compiler.TokenType.EQUAL_EQUAL;
import static org.bourbon.compiler.TokenType.FAT_ARROW;
import static org.bourbon.compiler.TokenType.LEFT_BRACE;
import static org.bourbon.compiler.TokenType.LEFT_BRACKET;
import static org.bourbon.compiler.TokenType.LEFT_PAREN;
import static org.bourbon.compiler.TokenType.LESS;
import static org.bourbon.compiler.TokenType.LESS_EQUAL;
import static org.bourbon.compiler.TokenType.LESS_EQUAL_GREATER;
import static org.bourbon.compiler.TokenType.MINUS;
import static org.bourbon.compiler.TokenType.MINUS_MINUS;
import static org.bourbon.compiler.TokenType.PERCENT;
import static org.bourbon.compiler.TokenType.PERCENT_EQUAL;
import static org.bourbon.compiler.TokenType.PIPE;
import static org.bourbon.compiler.TokenType.PIPE_EQUAL;
import static org.bourbon.compiler.TokenType.PIPE_PIPE;
import static org.bourbon.compiler.TokenType.PIPE_PIPE_EQUAL;
import static org.bourbon.compiler.TokenType.PLUS;
import static org.bourbon.compiler.TokenType.PLUS_EQUAL;
import static org.bourbon.compiler.TokenType.PLUS_PLUS;
import static org.bourbon.compiler.TokenType.QUESTION;
import static org.bourbon.compiler.TokenType.QUESTION_DOT;
import static org.bourbon.compiler.TokenType.RIGHT_BRACE;
import static org.bourbon.compiler.TokenType.RIGHT_BRACKET;
import static org.bourbon.compiler.TokenType.RIGHT_PAREN;
import static org.bourbon.compiler.TokenType.STAR;
import static org.bourbon.compiler.TokenType.STAR_DOT;
import static org.bourbon.compiler.TokenType.STAR_EQUAL;
import static org.bourbon.compiler.TokenType.STAR_STAR;
import static org.bourbon.compiler.TokenType.TRIPLE_EQUAL;
import static org.bourbon.compiler.TokenType.SEMICOLON;
import static org.bourbon.compiler.TokenType.SLASH;
import static org.bourbon.compiler.TokenType.SLASH_EQUAL;
import static org.bourbon.compiler.TokenType.TILDE;
import static org.bourbon.compiler.TokenType.TILDE_EQUAL;
import static org.bourbon.compiler.TokenType.TRIPLE_DOT;

import java.util.ArrayList;
import java.util.List;

/**
 * The Scanner class is responsible for tokenizing the source code.
 * It reads the input stream and breaks it down into tokens.
 */
public class Scanner {

    private final Source source;
    private final List<Token> tokens = new ArrayList<>();

    public static List<Token> scanTokens(Source source) {
        var scanner = new Scanner(source);
        return scanner.scanTokens();
    }

    public Scanner(Source source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        if (!tokens.isEmpty() && !isAtEnd()) {
            // Calling scanner scanTokens() repeatedly on same Source input
            // (Like in REPL session multi-line input evaluation), the
            // Source content may have grown compared to the previous call.
            // If so, remove the trailing EOF from the token list and resume
            // scanning as if the input was always the longer version.

            // Source cannot be modified other than by appending content,
            // so this is safe by construction.
            var lastToken = tokens.getLast();
            if (lastToken.type() == EOF) {
                tokens.removeLast();
            }
        }

        while (!source.isAtEnd()) {
            source.tokenStart();
            scanToken();
        }

        source.tokenStart();
        addToken(EOF);
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            // <editor-fold desc="2.2 Comments">
            case '/' -> {
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd())
                        advance();
                    // ignore comment tokens for now
                } else if (match('*')) {
                    // Multi line comments
                    multiLineComment();
                } else if (match('=')) {
                    addToken(SLASH_EQUAL);
                } else {
                    addToken(SLASH);
                }
            }

            case '#' -> {
                if (match('!')) {
                    while (peek() != '\n' && !isAtEnd())
                        advance();
                    // ignore comment tokens for now
                }
            }

            // </editor-fold>

            // <editor-fold desc="2.5 Operators and delimiters (from Ceylon spec)">

            // <editor-fold desc="Single char tokens: , : ; { } ( ) [ ] ` ^ ">
            case ',' -> addToken(COMMA);
            case ':' -> addToken(COLON);
            case ';' -> addToken(SEMICOLON);
            case '{' -> addToken(LEFT_BRACE);
            case '}' -> addToken(RIGHT_BRACE);
            case '(' -> addToken(LEFT_PAREN);
            case ')' -> addToken(RIGHT_PAREN);
            case '[' -> addToken(LEFT_BRACKET);
            case ']' -> addToken(RIGHT_BRACKET);
            case '`' -> addToken(BACKTICK);
            case '^' -> addToken(CARET);
            // </editor-fold>

            // <editor-fold desc="Multi-char tokens">

            // <editor-fold desc="Operators: ? ?. % %= ! != ~ ~= ">
            case '?' -> addDoubleToken(QUESTION, '.', QUESTION_DOT);
            case '%' -> addDoubleToken(PERCENT, '=', PERCENT_EQUAL);
            case '!' -> addDoubleToken(BANG, '=', BANG_EQUAL);
            case '~' -> addDoubleToken(TILDE, '=', TILDE_EQUAL);
            // </editor-fold>

            // <editor-fold desc="Operators: > < <= <=> ">
            case '<' -> {
                if (match('=')) {
                    if (match('>')) {
                        addToken(LESS_EQUAL_GREATER);
                    } else {
                        addToken(LESS_EQUAL);
                    }
                } else {
                    addToken(LESS);
                }
            }
            // </editor-fold>

            // <editor-fold desc="Operators: . .. ... ">
            case '.' -> {
                if (match('.')) {
                    if (match('.')) {
                        addToken(TRIPLE_DOT);
                    } else {
                        addToken(DOT_DOT);
                    }
                } else {
                    addToken(DOT);
                }
            }
            // </editor-fold>

            // <editor-fold desc="Operators: + ++ += ">
            case '+' -> {
                if (match('+')) {
                    addToken(PLUS_PLUS);
                } else if (match('=')) {
                    addToken(PLUS_EQUAL);
                } else {
                    addToken(PLUS);
                }
            }
            // </editor-fold>

            // <editor-fold desc="Operators: - -- -= ">
            case '-' -> {
                if (match('-')) {
                    addToken(MINUS_MINUS);
                } else if (match('=')) {
                    addToken(PLUS_EQUAL);
                } else {
                    addToken(MINUS);
                }
            }
            // </editor-fold>

            // <editor-fold desc="Operators: & && &&= &= ">
            case '&' -> {
                if (match('&')) {
                    if (match('=')) {
                        addToken(AMPERSAND_AMPERSAND_EQUAL);
                    } else {
                        addToken(AMPERSAND_AMPERSAND);
                    }
                } else if (match('=')) {
                    addToken(AMPERSAND_EQUAL);
                } else {
                    addToken(AMPERSAND);
                }
            }
            // </editor-fold>

            // <editor-fold desc="Operators: | || ||= |= ">
            case '|' -> {
                if (match('|')) {
                    if (match('=')) {
                        addToken(PIPE_PIPE_EQUAL);
                    } else {
                        addToken(PIPE_PIPE);
                    }
                } else if (match('=')) {
                    addToken(PIPE_EQUAL);
                } else {
                    addToken(PIPE);
                }
            }
            // </editor-fold>

            // <editor-fold desc="Operators: = == === => ">
            case '=' -> {
                if (match('=')) {
                    if (match('=')) {
                        addToken(TRIPLE_EQUAL);
                    } else {
                        addToken(EQUAL_EQUAL);
                    }
                } else if (match('>')) {
                    addToken(FAT_ARROW);
                } else {
                    addToken(EQUAL);
                }
            }
            // </editor-fold>

            // <editor-fold desc="Operators: * ** *. *= ">
            case '*' -> {
                switch (peek()) {
                    case '*' -> {
                        addToken(STAR_STAR);
                        advance();
                    }
                    case '.' -> {
                        addToken(STAR_DOT);
                        advance();
                    }
                    case '=' -> {
                        addToken(STAR_EQUAL);
                        advance();
                    }
                    default -> addToken(STAR);
                }
            }
            // </editor-fold>

            // </editor-fold>

            // </editor-fold>

            // <editor-fold desc="2.1 Whitespace">
            case ' ', '\r', '\t', '\n' -> {
                // Ignore whitespace for now
            }
            // </editor-fold>

            // Rest
            default ->
                DiagnosticReporter.report(Diagnostic.Scanner.unexpectedCharacter(source.currentSpan()));
        }
    }

    private void multiLineComment() {
        var start = source.currentSpan();
        while (!isAtEnd()) {
            var c = advance();
            switch (c) {
                case '*' -> {
                    if (match('/')) {
                        return;
                    }
                }
                case '/' -> {
                    if (match('*')) {
                        multiLineComment();
                    }
                }
            }
        }

        DiagnosticReporter.report(Diagnostic.Scanner.unbalancedMultilineComment(
                start, source.spanAt(source.current(), 1)));
    }

    private void addToken(TokenType tokenType) {
        tokens.add(source.token(tokenType));
    }

    private void addDoubleToken(TokenType singleToken, char secondChar, TokenType doubleToken) {
        addToken(match(secondChar) ? doubleToken : singleToken);
    }

    private boolean isAtEnd() {
        return source.isAtEnd();
    }

    private char advance() {
        return source.advance();
    }

    private boolean match(char c) {
        return source.match(c);
    }

    private char peek() {
        return source.peek();
    }
}