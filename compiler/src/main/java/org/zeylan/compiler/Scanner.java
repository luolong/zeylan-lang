package org.zeylan.compiler;

import static org.zeylan.compiler.TokenType.COMMA;
import static org.zeylan.compiler.TokenType.DOT;
import static org.zeylan.compiler.TokenType.EOF;
import static org.zeylan.compiler.TokenType.LEFT_BRACE;
import static org.zeylan.compiler.TokenType.LEFT_PAREN;
import static org.zeylan.compiler.TokenType.MINUS;
import static org.zeylan.compiler.TokenType.PLUS;
import static org.zeylan.compiler.TokenType.RIGHT_BRACE;
import static org.zeylan.compiler.TokenType.RIGHT_PAREN;
import static org.zeylan.compiler.TokenType.SEMICOLON;
import static org.zeylan.compiler.TokenType.STAR;

import java.util.ArrayList;
import java.util.List;

/**
 * The Scanner class is responsible for tokenizing the source code.
 * It reads the input stream and breaks it down into tokens.
 */
public class Scanner {

    private final Source source;
    private final DiagnosticReporter diagnosticReporter;
    private final List<Token> tokens = new ArrayList<>();

    public Scanner(Source source, DiagnosticReporter diagnosticReporter) {
        this.source = source;
        this.diagnosticReporter = diagnosticReporter;
    }

    public List<Token> scanTokens() {
        if (!tokens.isEmpty() && !source.isAtEnd()) {
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
            source.startAtCurrent();
            scanToken();
        }

        tokens.add(source.token(EOF));
        return tokens;
    }

    private void scanToken() {
        char c = source.advance();
        switch (c) {
            case '(' -> addToken(LEFT_PAREN);
            case ')' -> addToken(RIGHT_PAREN);
            case '{' -> addToken(LEFT_BRACE);
            case '}' -> addToken(RIGHT_BRACE);
            case ',' -> addToken(COMMA);
            case '.' -> addToken(DOT);
            case '-' -> addToken(MINUS);
            case '+' -> addToken(PLUS);
            case ';' -> addToken(SEMICOLON);
            case '*' -> addToken(STAR);

            default -> diagnosticReporter.report(Diagnostic.unexpectedCharacter(
                    source.spanAtCurrent(), "Unrecognized symbol: '" + c + "'"
            ));
        }
    }

    private void addToken(TokenType tokenType) {
        tokens.add(source.token(tokenType));
    }
}