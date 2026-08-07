package org.zeylan.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.jspecify.annotations.Nullable;
import org.zeylan.compiler.ScannerTestContextProvider.ScannerTestCase;

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

    public ScannerTestCase parseTestCase() {
        displayName = consumeHeader();

        var source = consumeSource();
        var testCaseInput = source.source();
        var lineOffsets = source.lineOffsets();

        consumeExpectations(lineOffsets);

        return new ScannerTestCase(testCaseInput, expectedTokens, expectedDiagnostics);
    }

    private void consumeExpectations(List<Integer> lineOffsets) {

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

        return new TestCaseSource(lines.toString(), List.copyOf(lineOffsets));
    }

    private String skipEmptyLines() {
        var line = nextLine();
        while (line.isBlank()) {
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
