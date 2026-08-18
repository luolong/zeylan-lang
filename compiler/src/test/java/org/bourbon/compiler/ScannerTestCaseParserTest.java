package org.bourbon.compiler;

import static org.bourbon.compiler.TokenType.COMMA;
import static org.bourbon.compiler.TokenType.EOF;
import static org.bourbon.compiler.TokenType.LEFT_BRACE;
import static org.bourbon.compiler.TokenType.NUMBER;
import static org.bourbon.compiler.TokenType.RIGHT_BRACE;
import static org.bourbon.compiler.TokenType.SEMICOLON;
import static org.bourbon.compiler.TokenType.STRING;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ScannerTestCaseParserTest {

    static final String VALID_TEST_CASE = """
            ==========================================
            Test Case Token Parser Test -- happy cases
            ==========================================
            // Simple line comment skipped
            {}; "Hello"
            42 @,
            ---
            ---
            LEFT_BRACE @ 1
            RIGHT_BRACE } @ 2
            SEMICOLON ; @ 3 [34..35]
            STRING '"Hello"' "Hello" @ 5 [36..43]
            ---
            NUMBER 42 42.0 @ 13 [44..46]
            ✗ error[BCE000100]: Unexpected character
              --> <VALID_TEST_CASE>:3:4
                |
              3 | 42 @
                |    ^ Unexpected symbol
                |
            COMMA @ 5
            ---
            EOF @ 1
            """;

    static final int HEADER_SEPARATOR_LENGTH = 42;
    static final int SOURCE_BLOCK_START = 3 * (HEADER_SEPARATOR_LENGTH + 1);
    static final int SOURCE_BLOCK_LENGTH = 49;
    static final int SOURCE_BLOCK_END = SOURCE_BLOCK_START + SOURCE_BLOCK_LENGTH;

    @Nested
    @DisplayName("Token Parser Test")
    class TokenParserTest {

        @DisplayName("Simple Token Parsing")
        @ParameterizedTest(name = "[Line {0}] should be parsed as {1} @ {4}:{5}")
        @CsvSource({
                "9, LEFT_BRACE, '{', null, 2, 1, 32, 1",
                "10, RIGHT_BRACE, '}', null, 2, 2, 33, 1",
                "11, SEMICOLON, ';', null, 2, 3, 34, 1",
                //"12, STRING, '\"Hello\"', 'Hello', 2, 3, 36, 7",
                //"14, NUMBER, '42', 42.0, 3, 1, 42, 2",
        })
        void parseSimpleToken(int testCaseLine, String expectedTypeName, String expectedLexeme, String expectedLiteralString, int expectedLine, int expectedColumn, int expectedStartOffset, int expectedLength) {
            var source = Source.of(VALID_TEST_CASE).resetToLine(testCaseLine);

            var token = TestCaseTokenParser.parse(source, expectedLine, 32);

            var expectedType = TokenType.valueOf(expectedTypeName);
            var expectedLiteral = switch (expectedType) {
                case STRING -> expectedLiteralString;
                case NUMBER -> Double.parseDouble(expectedLiteralString);
                default -> null;
            };

            assertAll("Token on line " + testCaseLine,
                    () -> assertEquals(expectedType, token.type()),
                    () -> assertEquals(expectedLexeme, token.lexeme()),
                    () -> assertEquals(expectedLiteral, token.literal()),
                    () -> assertEquals(expectedLine, token.line()),
                    () -> assertEquals(expectedColumn, token.column()),
                    () -> assertEquals(expectedStartOffset, token.startOffset()),
                    () -> assertEquals(expectedLength, token.length()));
        }
    }


    @Nested
    @DisplayName("Test Case Parser")
    class TestCaseParserTest {

        private ScannerTestCaseParser parser;
        private ScannerTestCase testCase;

        @BeforeEach
        void parseTestCase() {
            assumeStartsWithHeader(VALID_TEST_CASE, HEADER_SEPARATOR_LENGTH);
            assumeSourceBlockBetweenSeparators(VALID_TEST_CASE, HEADER_SEPARATOR_LENGTH, SOURCE_BLOCK_START, SOURCE_BLOCK_END);

            this.parser = new ScannerTestCaseParser(getSource());
            this.testCase = parser.parseTestCase();
        }

        @Test
        void getDisplayName() {
            var expectedDisplayName = VALID_TEST_CASE.substring(HEADER_SEPARATOR_LENGTH + 1, 2 * HEADER_SEPARATOR_LENGTH + 1);
            assertEquals(expectedDisplayName, parser.getDisplayName(), "Display name");
        }

        @Test
        void getInput() {
            var expectedTestCaseInput = VALID_TEST_CASE.substring(SOURCE_BLOCK_START, SOURCE_BLOCK_END);
            assertEquals(expectedTestCaseInput, testCase.input(), "Input");
        }

        @Test
        void getTokens() {
            var parsedTokens = testCase.expectedTokens();
            assertEquals(7, parsedTokens.size(), "Parsed token count");

            var actualTokenTypes = parsedTokens.stream().map(Token::type).toArray(TokenType[]::new);
            assertArrayEquals(new TokenType[]{LEFT_BRACE, RIGHT_BRACE, SEMICOLON, STRING, NUMBER, COMMA, EOF}, actualTokenTypes, "Token types");
        }

        @Test
        void getDiagnostic() {
            var parserDiagnostics = testCase.expectedDiagnostics();
            assertEquals(1, parserDiagnostics.size(), "Diagnostic count");

            var diagnostic = parserDiagnostics.getFirst();
            assertAll("Parsed diagnbostic",
                    () -> assertEquals(Diagnostic.Severity.ERROR, diagnostic.severity(), "severity"),
                    () -> assertEquals(Diagnostic.Code.ScannerUnexpectedCharacter, diagnostic.code(), "code"),
                    () -> assertEquals("Unexpected character", diagnostic.message(), "message"),
                    () -> {
                        var labels = diagnostic.labels();
                        var label = labels.getFirst();
                        assertAll("Labels",
                                () -> assertEquals(1, labels.size(), "count"),
                                () -> assertEquals("<VALID_TEST_CASE>", label.span().name().name(), "label source"),
                                () -> assertEquals(3, label.span().line(), "label line"),
                                () -> assertEquals(3, label.span().column(), "label column"),
                                () -> assertEquals(1, label.span().length(), "label length"),
                                () -> assertEquals("Unexpected symbol", label.message(), "label message")
                        );
                    }
            );
        }
    }

    private static @NonNull Source getSource() {
        return Source.named("<VALID_TEST_CASE>").of(VALID_TEST_CASE);
    }

    private static void assumeStartsWithHeader(String testCase, int separatorLength) {
        Assumptions.assumeTrue(testCase.startsWith("=".repeat(separatorLength) + '\n'),
                "Test case starts with header separator of length " + separatorLength);
    }

    private static void assumeSourceBlockBetweenSeparators(String testCase, int headerSeparatorLength, int sourceBlockStart, int sourceBlockEnd) {
        var expectedHeaderSeparator = "=".repeat(headerSeparatorLength) + '\n';

        boolean hasHeaderBlockEndSeparatorBeforeStart = testCase.startsWith(expectedHeaderSeparator, sourceBlockStart - expectedHeaderSeparator.length());
        Assumptions.assumeTrue(hasHeaderBlockEndSeparatorBeforeStart, "Test case source block starts after header separator:\n"
                + "Expected: " + escape(expectedHeaderSeparator) + "\n"
                + "Actual:   " + escape(testCase.substring(sourceBlockStart - headerSeparatorLength, sourceBlockEnd)));

        boolean hasAssertionBlockSeparatorAfterEnd = testCase.startsWith("---\n", sourceBlockEnd);
        Assumptions.assumeTrue(hasAssertionBlockSeparatorAfterEnd, "Test case source block is followed by assertion block separator");
    }

    private static String escape(String s) {
        return s.replace("\n", "\\n");
    }

}
