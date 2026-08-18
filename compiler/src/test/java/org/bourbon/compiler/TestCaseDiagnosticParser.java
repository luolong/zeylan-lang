package org.bourbon.compiler;

import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Supplier;

import org.bourbon.compiler.Diagnostic.Severity;
import org.bourbon.compiler.DiagnosticFormatter.Symbol.NerdFont;
import org.bourbon.compiler.DiagnosticFormatter.Symbol.Unicode;
import org.bourbon.compiler.SourceSpan.SourceName;
import org.jspecify.annotations.NonNull;

public class TestCaseDiagnosticParser {

    private final Source source;
    private final int lineNumber;
    private final int lineOffset;

    public TestCaseDiagnosticParser(Source source, int lineNumber, int lineOffset) {
        this.source = source;
        this.lineNumber = lineNumber;
        this.lineOffset = lineOffset;
    }

    public static Diagnostic parse(Source source, int lineNumber, int lineOffset) {
        return new TestCaseDiagnosticParser(source, lineNumber, lineOffset).parseDiagnostic();
    }

    public static boolean isDiagnosticStart(Source source) {
        // In a general case, this would be considered algorithmically slow,
        // but we have just a handful of symbols to check, so here it's okay.
        // If it ever becomes a bottleneck, we can consider using a proper state machine.
        for (var symbol : NerdFont.values()) {
            if (source.peek(symbol.symbol())) return true;
        }

        for (var symbol : Unicode.values()) {
            if (source.peek(symbol.symbol())) return true;
        }

        return false;
    }

    private Diagnostic parseDiagnostic() {
        consumeDiagnosticStartSymbol();
        var severity = consumeSeverity();
        var code = consumeCode();
        var message = consumeMessage();

        String sourceFileName = null;
        int primaryColumn = -1;
        int primaryLine = lineNumber;

        var labels = new ArrayList<Label>();
        while (!source.isAtEnd()) {
            if (!consumeArrow()) break;
            sourceFileName = consumeSourceFileName();
            requireCharacter(':', () -> "Expecting ':' after source file name");
            primaryLine = consumeLineNumber(primaryLine);
            requireCharacter(':', () -> "Expecting ':' after line number");
            primaryColumn = consumeInteger();
            skipWhitespace();
            requireNewline();

            sourceLines:
            while (!source.isAtEnd()) {
                switch (consumeSourceLabel()) {
                    case LabelResult.Some(int labelLine, int labelColumn, int spanLength, String labelMessage) -> {
                        var startOffset = lineOffset + labelColumn;
                        var sourceSpan = new SourceSpan(SourceName.of(sourceFileName), labelLine, labelColumn, startOffset, spanLength);
                        var isPrimary = labelLine == primaryLine && labelColumn == primaryColumn;
                        labels.add(new Label(sourceSpan, labelMessage, isPrimary));
                    }

                    case LabelResult.Empty.EMPTY_SOURCE_LINE -> {
                        continue;
                    }

                    case LabelResult.Empty.BLANK_LINE, LabelResult.Empty.NOT_LABEL -> {
                        break sourceLines;
                    }
                };

            }

            skipBlankLines();
        }

        if (labels.isEmpty()) {
            throw Exceptions.expectLabels(source.currentSpan());
        }

        return new Diagnostic(code, severity, message, labels);
    }

    private int consumeLineNumber(int primaryLine) {
        int lineNumber = consumeInteger();
        if (primaryLine != lineNumber) {
            // TODO: Issue a warning
        }
        return lineNumber;
    }

    private int consumeInteger() {
        while (!isAtEndOfLine() && Character.isDigit(source.peek()))
            source.advance();
        try {
            int integer = Integer.parseInt(source.lexeme());
            source.tokenStart();
            return integer;
        } catch (NumberFormatException e) {
            throw Exceptions.expectInteger(source.currentSpan());
        }
    }

    private String consumeSourceFileName() {
        advanceUntilMatch(':');
        var sourceFileName = source.lexeme();
        if (sourceFileName.isBlank())
            throw Exceptions.expectSourceFileName(source.currentSpan());

        return sourceFileName.trim();
    }

    private boolean consumeArrow() {
        advanceWhitespace();
        if (source.match("--> ")) {
            source.tokenStart();
            return true;
        }

        source.tokenReset();
        return false;
    }

    sealed interface LabelResult {
        enum Empty implements LabelResult { EMPTY_SOURCE_LINE, BLANK_LINE, NOT_LABEL }
        record Some(int lineNumber, int columnNumber, int spanLength, String message) implements LabelResult {

        }
    }
    private LabelResult consumeSourceLabel() {
        skipWhitespace();
        if (isAtEndOfLine()) {
            requireNewline();
            return LabelResult.Empty.BLANK_LINE;
        }

        if (source.match('|')) {
            skipWhitespace();
            requireNewline();
            return LabelResult.Empty.EMPTY_SOURCE_LINE;
        }

        if (!Character.isDigit(source.peek())) {
            source.tokenReset();
            return LabelResult.Empty.NOT_LABEL;
        }

        var lineNumber = consumeInteger();
        skipWhitespace();
        requireCharacter('|', () -> "Expecting '|' after source line number");
        requireCharacter(' ', () -> "Expecting at least one space indent after line number gutter separator");
        while (!isAtEndOfLine()) source.advance();
        requireNewline();

        skipWhitespace();
        requireCharacter('|', () -> "Expecting '|' before diagnostic label message");
        int columnNumber = consumeUnderlineIndent();
        var spanLength = consumeLabelUnderline();
        skipWhitespace();
        var message = consumeLabelMessage();

        return new LabelResult.Some(lineNumber, columnNumber, spanLength, message);
    }

    private @NonNull String consumeLabelMessage() {
        while (!isAtEndOfLine()) source.advance();
        var message = source.lexeme().trim();
        requireNewline();
        return message;
    }

    private int consumeLabelUnderline() {
        while (!isAtEndOfLine() && source.peek('^')) source.advance();
        var spanLength = source.lexeme().length();
        source.tokenStart();
        return spanLength;
    }

    private int consumeUnderlineIndent() {
        var indent = advanceWhitespace();
        if (indent.isEmpty())
            throw Exceptions.expectingSourceLineIndent(source.currentSpan());
        int columnNumber = source.lexeme().length() - 1;
        source.tokenStart();
        return columnNumber;
    }

    private String consumeMessage() {
        requireCharacter(':', () -> "Expecting ':' before message");
        skipWhitespace();

        if (isAtEndOfLine())
            throw Exceptions.expectingDiagnosticMessage(source.currentSpan());

        while (!isAtEndOfLine()) source.advance();
        var message = source.lexeme();
        source.tokenStart();

        requireNewline();
        return message;
    }

    private Diagnostic.Code consumeCode() {
        requireCharacter('[', () -> "Expecting '['");
        while (!source.isAtEnd() && Character.isLetterOrDigit(source.peek())) source.advance();
        var code = parseCode(source.lexeme());
        requireCharacter(']', () -> "Expecting ']' after diagnostic code");
        return code;
    }

    private Diagnostic.Code parseCode(String value) {
        try {
            Diagnostic.Code code = Diagnostic.Code.fromCode(value);
            source.tokenStart();
            return code;
        } catch (IllegalArgumentException e) {
            throw Exceptions.expectingDiagnosticCode(source.currentSpan());
        }
    }

    private void requireCharacter(char c, Supplier<String> message) {
        if (!source.match(c)) {
            throw Exceptions.expectingCharacter(source.currentSpan(), message.get());
        }
        source.tokenStart();
    }

    private void requireNewline() {
        if (!source.isAtEnd() && !source.match('\n')) {
            throw Exceptions.expectingCharacter(source.currentSpan(), "Expecting newline");
        }
        source.tokenStart();
    }

    private Severity consumeSeverity() {
        skipWhitespace();
        for (var severity : Severity.values()) {
            if (source.match(severity.name().toLowerCase(Locale.ROOT))) {
                source.tokenStart();
                return severity;
            }
        }

        throw Exceptions.severityExpected(source.currentSpan());
    }

    private void skipWhitespace() {
        advanceWhitespace();
        source.tokenStart();
    }

    private String advanceWhitespace() {
        while (!isAtEndOfLine() && Character.isWhitespace(source.peek())) source.advance();
        return source.lexeme();
    }

    private void skipBlankLines() {
        while (!source.isAtEnd()) {
            advanceWhitespace();
            if (!isAtEndOfLine()) {
                source.tokenReset();
                return;
            }
            requireNewline();
        }
    }

    private void consumeDiagnosticStartSymbol() {
        for (var symbol : NerdFont.values()) {
            if (source.match(symbol.symbol())) {
                source.tokenStart();
                return;
            };
        }

        for (var symbol : Unicode.values()) {
            if (source.match(symbol.symbol().charAt(0))) {
                source.tokenStart();
                return;
            };
        }
    }

    private void advanceUntilMatch(char end) {
        while (!isAtEndOfLine() && source.peek() != end) {
            source.advance();
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isAtEndOfLine() {
        return source.isAtEnd() || source.peek('\n');
    }

    static class Exceptions {

        public static TestCaseParserException severityExpected(SourceSpan sourceSpan) {
            return new TestCaseParserException(Diagnostic.Code.ScannerTestCaseParserError,
                    sourceSpan, "Expected message severity (%s)".formatted(String.join("|", Severity.names())));
        }

        public static TestCaseParserException expectingCharacter(SourceSpan sourceSpan, String message) {
            return new TestCaseParserException(Diagnostic.Code.ScannerTestCaseParserError, sourceSpan, message);
        }

        public static TestCaseParserException expectingDiagnosticCode(SourceSpan sourceSpan) {
            return new TestCaseParserException(Diagnostic.Code.ScannerTestCaseParserError,
                    sourceSpan, "Expected message diagnostic code!");
            // Suggestion: "Did you mean".
        }

        public static TestCaseParserException expectingDiagnosticMessage(SourceSpan sourceSpan) {
            return new TestCaseParserException(Diagnostic.Code.ScannerTestCaseParserError,
                    sourceSpan, "Expected diagnostic message!");
            // Suggestion: "Did you mean".
        }

        public static TestCaseParserException expectLabels(SourceSpan sourceSpan) {
            return new TestCaseParserException(Diagnostic.Code.ScannerTestCaseParserError, sourceSpan, "Expected one or more diagnostic labels!");
        }

        public static TestCaseParserException expectSourceFileName(SourceSpan sourceSpan) {
            return new TestCaseParserException(Diagnostic.Code.ScannerTestCaseParserError, sourceSpan, "Expected source file name!");
        }

        public static TestCaseParserException expectInteger(SourceSpan sourceSpan) {
            return new TestCaseParserException(Diagnostic.Code.ScannerTestCaseParserError, sourceSpan, "Expected an integer!");
        }

        public static TestCaseParserException expectingSourceLineIndent(SourceSpan sourceSpan) {
            return new TestCaseParserException(Diagnostic.Code.ScannerTestCaseParserError, sourceSpan, "Expected at least one character source line indent!");
        }

        public static TestCaseParserException notImplementedYet(SourceSpan sourceSpan, String message) {
            return new TestCaseParserException(Diagnostic.Code.NotImplemented, sourceSpan, message);
        }
        // TODO: exception thrown
    }
}
