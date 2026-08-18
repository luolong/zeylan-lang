package org.bourbon.compiler;

import static java.util.Map.entry;
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
import static org.bourbon.compiler.TokenType.GREATER;
import static org.bourbon.compiler.TokenType.GREATER_EQUAL;
import static org.bourbon.compiler.TokenType.LEFT_BRACE;
import static org.bourbon.compiler.TokenType.LEFT_BRACKET;
import static org.bourbon.compiler.TokenType.LEFT_PAREN;
import static org.bourbon.compiler.TokenType.LESS;
import static org.bourbon.compiler.TokenType.LESS_EQUAL;
import static org.bourbon.compiler.TokenType.LESS_EQUAL_GREATER;
import static org.bourbon.compiler.TokenType.MINUS;
import static org.bourbon.compiler.TokenType.MINUS_EQUAL;
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
import static org.bourbon.compiler.TokenType.SEMICOLON;
import static org.bourbon.compiler.TokenType.SLASH;
import static org.bourbon.compiler.TokenType.SLASH_EQUAL;
import static org.bourbon.compiler.TokenType.STAR;
import static org.bourbon.compiler.TokenType.STAR_DOT;
import static org.bourbon.compiler.TokenType.STAR_EQUAL;
import static org.bourbon.compiler.TokenType.STAR_STAR;
import static org.bourbon.compiler.TokenType.THIN_ARROW;
import static org.bourbon.compiler.TokenType.TILDE;
import static org.bourbon.compiler.TokenType.TILDE_EQUAL;
import static org.bourbon.compiler.TokenType.TRIPLE_DOT;
import static org.bourbon.compiler.TokenType.TRIPLE_EQUAL;

import java.util.Map;

import org.jspecify.annotations.Nullable;

public class TestCaseTokenParser {
    private static final Map<TokenType, String> DEFAULT_LEXEMES = Map.<TokenType, String>ofEntries(
            entry(LEFT_BRACE, "{"),
            entry(RIGHT_BRACE, "}"),
            entry(LEFT_PAREN, "("),
            entry(RIGHT_PAREN, ")"),
            entry(LEFT_BRACKET, "["),
            entry(RIGHT_BRACKET, "]"),
            entry(COMMA, ","),
            entry(COLON, ":"),
            entry(SEMICOLON, ";"),
            entry(BACKTICK, "`"),
            entry(CARET, "^"),
            entry(DOT, "."),
            entry(DOT_DOT, ".."),
            entry(TRIPLE_DOT, "..."),
            entry(QUESTION, "?"),
            entry(QUESTION_DOT, "?."),
            entry(EQUAL, "="),
            entry(EQUAL_EQUAL, "=="),
            entry(TRIPLE_EQUAL, "==="),
            entry(FAT_ARROW, "=>"),
            entry(PLUS, "+"),
            entry(PLUS_PLUS, "++"),
            entry(PLUS_EQUAL, "+="),
            entry(MINUS, "-"),
            entry(MINUS_MINUS, "--"),
            entry(MINUS_EQUAL, "-="),
            entry(THIN_ARROW, "->"),
            entry(STAR, "*"),
            entry(STAR_STAR, "**"),
            entry(STAR_DOT, "*."),
            entry(STAR_EQUAL, "*="),
            entry(BANG, "!"),
            entry(BANG_EQUAL, "!="),
            entry(TILDE, "~"),
            entry(TILDE_EQUAL, "~="),
            entry(PIPE, "|"),
            entry(PIPE_PIPE, "||"),
            entry(PIPE_EQUAL, "|="),
            entry(PIPE_PIPE_EQUAL, "||="),
            entry(LESS, "<"),
            entry(LESS_EQUAL, "<="),
            entry(LESS_EQUAL_GREATER, "<=>"),
            entry(GREATER, ">"),
            entry(GREATER_EQUAL, ">="),
            entry(SLASH, "/"),
            entry(SLASH_EQUAL, "/="),
            entry(PERCENT, "%"),
            entry(PERCENT_EQUAL, "%="),
            entry(AMPERSAND, "&"),
            entry(AMPERSAND_AMPERSAND, "&&"),
            entry(AMPERSAND_EQUAL, "&="),
            entry(AMPERSAND_AMPERSAND_EQUAL, "&&="),

            // Keywords

            // EOF
            entry(EOF, "")
    );

    private final Source source;
    private final int lineNumber;
    private final int lineOffset;

    private TestCaseTokenParser(Source source, int lineNumber, int lineOffset) {
        this.source = source;
        this.lineNumber = lineNumber;
        this.lineOffset = lineOffset;
    }

    public static Token parse(Source source, int lineNumber, int lineOffset) {
        return new TestCaseTokenParser(source, lineNumber, lineOffset).parseToken();
    }

    private Token parseToken() {
        var type = conumeTokenType();

        var lexeme = consumeLexeme();
        if (lexeme == null) {
            lexeme = DEFAULT_LEXEMES.get(type);
            if (lexeme == null) {
                throw new TestCaseTokenParserException(source.currentSpan(), "Expected lexeme for token type " + type);
            }
        }

        var literal = consumeLiteral();
        if (literal == null) {
            literal = parseLiteral(type, lexeme);
        }

        consumeAtCharacter();

        int columnNumber = consumeNumber("column number");

        int startOffset = lineOffset + columnNumber - 1;
        int length = lexeme.length();

        char c = skipWhitespace();
        if (c == '[') {
            source.tokenStart();
            startOffset = consumeNumber("start offset");
            consumeRangeSeparator();
            int endOffset = consumeNumber("end offset");
            length = endOffset - startOffset;
            consumeClosingBracket();
            c = skipWhitespace();
        }

        if (c != '\n' && c != '\0') {
            throw new TestCaseTokenParserException(source.currentSpan(), "Expected source span range starting with '[' or end of line");
        }

        return new Token(type, lexeme, literal, lineNumber, columnNumber, startOffset, length);
    }

    private void consumeClosingBracket() {
        if (!source.match(']')) {
            throw new TestCaseTokenParserException(source.currentSpan(), "Expected closing bracket ']' to complete the source range");
        }
        source.tokenStart();
    }

    private void consumeRangeSeparator() {
        if (!source.match("..")) {
            throw new TestCaseTokenParserException(source.currentSpan(), "Expected '..' range separator");
        }
        source.tokenStart();
    }

    private void consumeAtCharacter() {
        char c = requireNotEnd(skipWhitespace());
        if (c != '@') throw new TestCaseTokenParserException(source.currentSpan(), "Expected '@' before column number");
        source.tokenStart();
    }

    private int consumeNumber(String numberRole) {
        char c = requireNotEnd(skipWhitespace());
        if  (!isDigit(c)) throw new TestCaseTokenParserException(source.currentSpan(), "Expected token " + numberRole);
        while (!isAtEnd() && isDigit(source.peek())) source.advance();
        String lexeme = source.lexeme();
        source.tokenStart();
        return Integer.parseInt(lexeme);
    }

    private static boolean isDigit(char c) {
        return '0' <= c && c <= '9';
    }

    private @Nullable Object consumeLiteral() {
        String lexeme = consumeLexeme();
        if (lexeme == null) return null;
        if (lexeme.isBlank()) return null;

        if (lexeme.startsWith("\"") && lexeme.endsWith("\"")) {
            return lexeme.substring(1, lexeme.length() - 1);
        }

        if (lexeme.startsWith("'") && lexeme.endsWith("'")) {
            return lexeme.substring(1, lexeme.length() - 1);
        }

        if (lexeme.startsWith("0x") && lexeme.length() > 2) {
            return Integer.parseInt(lexeme.substring(2), 16);
        }

        if (lexeme.startsWith("0b") && lexeme.length() > 2) {
            return Integer.parseInt(lexeme.substring(2), 2);
        }

        if (lexeme.startsWith("0") && lexeme.length() > 1) {
            return Integer.parseInt(lexeme.substring(1), 8);
        }

        if (lexeme.matches("-?\\d+(\\.\\d+)?")) {
            return Double.parseDouble(lexeme);
        }

        return lexeme;
    }

    private Object parseLiteral(TokenType type, String lexeme) {
        if (type == TokenType.STRING) {
            return lexeme.substring(1, lexeme.length() - 1);
        }

        return null;
    }

    private @Nullable String consumeLexeme() {
        char c = requireNotEnd(skipWhitespace());

        if (c == '\'' || c == '"') return consumeString(c);
        if (c == '\\') return consumeEscapeChar();
        if (c == '@') {
            source.tokenReset();
            return null;
        }

        while (!isAtEnd() && !isWhitespace()) {
            if (source.peek('@')) {
                break;
            }

            source.advance();
        }

        String lexeme = source.lexeme();
        source.tokenStart();
        return lexeme;
    }

    private TokenType conumeTokenType() {
        char c = requireNotEnd(skipWhitespace());

        if (!Character.isJavaIdentifierStart(c))
            throw new TestCaseTokenParserException(source.currentSpan(), "Token type identifier expected!");

        while (!isAtEnd() && Character.isJavaIdentifierPart(source.peek())) source.advance();
        try {
            TokenType tokenType = TokenType.valueOf(source.lexeme());
            source.tokenStart();
            return tokenType;
        } catch (IllegalArgumentException e) {
            throw new TestCaseTokenParserException(source.currentSpan(), "Unrecognized token: " + source.lexeme());
        }
    }

    private char requireNotEnd(char c) {
        if (c == '\n')
            throw TestCaseTokenParserException.unexpectedEndOfLine(source.currentSpan());

        if (c == '\0')
            throw TestCaseTokenParserException.unexpectedEndOfInput(source.currentSpan());

        return c;
    }

    private char skipWhitespace() {
        while (!isAtEnd() && isWhitespace()) source.advance();
        source.tokenStart();
        return source.advance();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isAtEnd() {
        return source.isAtEnd() || source.peek('\n');
    }

    private boolean isWhitespace() {
        return Character.isWhitespace(source.peek());
    }

    private String consumeString(char quote) {
        int start = source.start();
        StringBuilder s = new StringBuilder();
        while (!source.isAtEnd()) {
            char c = source.peek();
            if (c == quote) {
                s.append(source.subSequence(start, source.current()));
                source.advance();
                source.tokenStart();
                return s.toString();
            }

            if (c == '\\') {
                s.append(source.subSequence(start, source.current()));
                source.advance();
                s.append(readEscapeChar());
                start = source.current();
            }

            if (c == '\n') {
                throw TestCaseTokenParserException.unexpectedEndOfLine(source.currentSpan());
            }
            source.advance();
        }

        throw TestCaseTokenParserException.unexpectedEndOfInput(source.currentSpan());
    }

    private String consumeEscapeChar() {
        var c = readEscapeChar();
        source.tokenStart();
        return String.valueOf(c);
    }

    private char readEscapeChar() {
        if (source.isAtEnd()) throw new IllegalStateException("Unexpected end of input");
        if (source.peek() == '\n') throw new IllegalStateException("Unexpected newline");
        char c = source.advance();
        return switch (c) {
            case '0' -> '\0'; // Null character
            case 't' -> '\t'; // Tab character
            case 'b' -> '\b'; // Backspace character
            case 'n' -> '\n'; // Newline character
            case 'r' -> '\r'; // Carriage return character
            case 'f' -> '\f'; // Form feed character
            default -> c;
        };
    }

    public static class TestCaseTokenParserException extends TestCaseParserException {
    
        public TestCaseTokenParserException(SourceSpan sourceSpan, String message) {
            super(Diagnostic.Code.ScannerTestCaseParserError, sourceSpan, message);
        }
    
        static TestCaseTokenParserException unexpectedEndOfLine(SourceSpan sourceSpan) {
            return new TestCaseTokenParserException(sourceSpan, "Unexpected end of line");
        }

        static TestCaseTokenParserException unexpectedEndOfInput(SourceSpan sourceSpan) {
            return new TestCaseTokenParserException(sourceSpan, "Unexpected end of input");
        }
    }

}
