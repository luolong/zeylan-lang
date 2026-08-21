package org.bourbon.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.TestInstantiationException;

class ScannerTestCaseParser {

    public static final String HEADER_SEPARATOR_REGEX = "^={3,}$";
    public static final String TEST_CASE_SEPARATOR_REGEX = "^-{3,}$";

    private final Source source;

    private final List<Token> expectedTokens = new ArrayList<>();
    private final List<Diagnostic> expectedDiagnostics = new ArrayList<>();

    @Nullable
    private String displayName = null;

    ScannerTestCaseParser(Source source) {
        this.source = source;
    }

    public String getDisplayName() {
        return displayName != null && !displayName.isBlank() ? displayName : source.name();
    }

    @FunctionalInterface
    public interface LineOffsets {
        int get(String sourceName, int lineNumber);
    }

    public ScannerTestCase parseTestCase() {
        displayName = consumeHeader();

        var source = consumeSource();
        var testCaseInput = source.source();
        var lineOffsets = source.lineOffsets();

        consumeExpectations(lineOffsets);

        var namedSource = Source.named(this.source.name()).of(testCaseInput);
        return new ScannerTestCase(namedSource, expectedTokens, expectedDiagnostics);
    }

    private void consumeExpectations(List<Integer> lineOffsets) {
        int lineNumber = 0;
        int lineOffset = 0;
        while (!source.isAtEnd()) {
            var line = skipEmptyLines();
            if (line.equals("---")) {
                lineOffset = lineOffsets.get(lineNumber++);
                source.tokenStart();
                continue;
            }

            if (lineNumber == 0)
                throw Exceptions.expectLineSeparator(source.currentSpan());

            if (line.stripLeading().startsWith("#")) {
                // This is a line comment. Skip it.
                source.tokenStart();
                continue;
            }

            // Reset scanner position to start of the line
            source.tokenReset();
            if (TestCaseDiagnosticParser.isDiagnosticStart(source)) {
                expectedDiagnostics.add(consumeDiagnostic(lineNumber, (_, number) -> lineOffsets.get(number - 1)));
            } else {
                expectedTokens.add(consumeToken(lineNumber, lineOffset));
            }
        }
    }

    private Token consumeToken(int lineNumber, int lineOffset) {
        try {
            Token token = TestCaseTokenParser.parse(source, lineNumber, lineOffset);
            return token;
        } catch (TestCaseParserException e) {
            TestCaseParserException.printStackTrace(e, source);
            throw new TestInstantiationException("Failed to parse token for " + source.name(), e);
        }
    }

    private Diagnostic consumeDiagnostic(int lineNumber, LineOffsets lineOffsets) {
        try {
            return TestCaseDiagnosticParser.parse(source, lineNumber, lineOffsets);
        } catch (TestCaseParserException e) {
            TestCaseParserException.printStackTrace(e, source);
            throw new TestInstantiationException("Failed to parse diagnostic for " + source.name(), e);
        }
    }


    private String consumeHeader() {
        requireHeaderSeparator();
        var displayName = requireDisplayName().trim();
        requireHeaderSeparator();

        return displayName;
    }

    private String requireDisplayName() {
        String line;
        line = skipEmptyLines();
        if (line.matches(HEADER_SEPARATOR_REGEX)) {
            throw Exceptions.expectDisplayName(source.currentSpan());
        }
        return line;
    }

    private void requireHeaderSeparator() {
        var line = skipEmptyLines();
        if (!line.matches(HEADER_SEPARATOR_REGEX)) {
            throw Exceptions.expectHeaderSeparator(source.currentSpan());
        }
    }

    record TestCaseSource(String source, List<Integer> lineOffsets) {}

    private TestCaseSource consumeSource() {
        source.tokenStart();

        var sourceLength = 0;
        var lineOffsets = new ArrayList<Integer>();

        var lines = stringJoiner();
        while (!source.isAtEnd()) {
            sourceLength += source.currentSpan().length();
            lineOffsets.add(sourceLength);

            var line = nextLine();
            if (line.matches(TEST_CASE_SEPARATOR_REGEX)) {
                break;
            }
            lines.add(line);
        }

        source.tokenReset();
        return new TestCaseSource(lines.toString(), List.copyOf(lineOffsets));
    }

    private String skipEmptyLines() {
        var line = nextLine();
        while (!source.isAtEnd() && line.isBlank()) {
            line = nextLine();
        }
        return line;
    }

    private String nextLine() {
        source.tokenStart();
        return advanceLine();
    }

    private String advanceLine() {
        while (!source.isAtEnd() && !source.peek('\n'))
            source.advance();
        // Return the line without trailing newline
        var line = source.lexeme();
        if (!source.isAtEnd())
            source.advance();
        return line;
    }


    private static StringJoiner stringJoiner() {
        return new StringJoiner("\n", "", "\n");
    }


    static class Exceptions {

        public static TestCaseParserException expectLineSeparator(SourceSpan sourceSpan) {
            return new TestCaseParserException(Diagnostic.Code.ScannerTestCaseParserError, sourceSpan,
                    "Expected at least one line separator: \"---\"");
        }

        static TestCaseParserException expectHeaderSeparator(SourceSpan sourceSpan) {
            return new TestCaseParserException(Diagnostic.Code.ScannerTestCaseParserError, sourceSpan,
                    "Expected test header separator (a line of three or more '=' characters)");
        }

        static TestCaseParserException expectDisplayName(SourceSpan sourceSpan) {
            return new TestCaseParserException(Diagnostic.Code.ScannerTestCaseParserError, sourceSpan,
                    "Expected test case display name");
        }
    }
}
