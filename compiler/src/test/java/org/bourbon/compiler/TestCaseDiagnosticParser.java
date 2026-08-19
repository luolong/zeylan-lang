package org.bourbon.compiler;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import org.bourbon.compiler.Diagnostic.Code;
import org.bourbon.compiler.Diagnostic.Severity;
import org.bourbon.compiler.DiagnosticFormatter.Symbol.NerdFont;
import org.bourbon.compiler.DiagnosticFormatter.Symbol.Unicode;
import org.bourbon.compiler.SourceSpan.SourceName;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.TestInstantiationException;

public class TestCaseDiagnosticParser {

    private final Source source;
    private final int lineNumber;
    private final int lineOffset;

    private final DiagnosticReportWrapper Report = new DiagnosticReportWrapper();

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

        String sourceFileName;
        int primaryLine = lineNumber;
        int primaryColumn;

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
                        var startOffset = lineOffset + labelColumn - 1;
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

        if (labels.isEmpty()) throw Report.expectLabels();
        return new Diagnostic(code, severity, message, labels);
    }

    private int consumeLineNumber(int primaryLine) {
        int lineNumber = consumeInteger();
        if (primaryLine != lineNumber) {
            Report.primaryLineNumberMismatch(primaryLine, lineNumber);
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
            throw Report.expecInteger();
        }
    }

    private String consumeSourceFileName() {
        advanceUntilMatch(':');
        var sourceFileName = source.lexeme();
        if (sourceFileName.isBlank())
            throw Report.expectSourceFileName();

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
            throw Report.expectingSourceLineIndent();
        int columnNumber = source.lexeme().length();
        source.tokenStart();
        return columnNumber;
    }

    private String consumeMessage() {
        requireCharacter(':', () -> "Expecting ':' before message");
        skipWhitespace();

        if (isAtEndOfLine())
            throw Report.expectingDiagnosticMessage();

        while (!isAtEndOfLine()) source.advance();
        var message = source.lexeme();
        source.tokenStart();

        requireNewline();
        return message;
    }

    private Code consumeCode() {
        requireCharacter('[', () -> "Expecting '['");
        while (!source.isAtEnd() && Character.isLetterOrDigit(source.peek())) source.advance();
        var code = parseCode(source.lexeme());
        requireCharacter(']', () -> "Expecting ']' after diagnostic code");
        return code;
    }

    private Code parseCode(String value) {
        try {
            Code code = Diagnostic.Code.fromCode(value);
            source.tokenStart();
            return code;
        } catch (IllegalArgumentException e) {
            throw Report.expectingDiagnosticCode();
        }
    }

    private void requireCharacter(char c, Supplier<String> message) {
        if (!source.match(c)) {
            throw Report.expectingCharacter(message.get());
        }
        source.tokenStart();
    }

    private void requireNewline() {
        if (!source.isAtEnd() && !source.match('\n')) {
            throw Report.expectingCharacter("Expecting newline");
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

        throw Report.severityExpected();
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


    @SuppressWarnings("SameParameterValue")
    class DiagnosticReportWrapper {

        private @NonNull TestInstantiationException exception(Diagnostic diagnostic, Label label) {
            return exception(diagnostic, label, null);
        }

        private @NonNull TestInstantiationException exception(Diagnostic diagnostic, Label label, @Nullable String suggestion) {
            String message = "%s: %s on line %d, column %d".formatted(
                    diagnostic.message(), label.message(), label.span().line(), label.span().column());
            if (suggestion != null)
                message += "\nSuggestion: " + suggestion;

            var exception = new TestInstantiationException(message);

            var stackTrace = exception.getStackTrace();
            int i = 0;
            while (i < stackTrace.length) {
                var element = stackTrace[i];
                if (!element.getClassName().equals(DiagnosticReportWrapper.class.getName())) {
                    break;
                }
                i++;
            }
            exception.setStackTrace(Arrays.copyOf(stackTrace, i));
            return exception;
        }

        TestInstantiationException error(String message, Label label) {
            var error = DiagnosticReporter.error(Code.ScannerTestCaseParserError, message, List.of(label));
            return exception(error, label);
        }

        TestInstantiationException error(String message, Label label, String suggestion) {
            var error = DiagnosticReporter.error(Code.ScannerTestCaseParserError, message, List.of(label), List.of(suggestion));
            return exception(error, label, suggestion);
        }

        Diagnostic warning(String message, Label label) {
            return DiagnosticReporter.warning(Code.ScannerTestCaseParserError, message, List.of(label));
        }

        TestInstantiationException expectingCharacter(String message) {
            return error("Failed to parse diagnostic message!", Label.primaryOf(source.currentSpan(), message));
        }

        TestInstantiationException severityExpected() {
            var severityNames = Arrays.stream(Severity.values())
                    .map(Severity::name)
                    .map(String::toLowerCase)
                    .collect(joining("|"));

            return error("Failed to parse diagnostic message header!",
                    Label.primaryOf(source.currentSpan(), "Expected diagnostic message severity"),
                    "Must be one of " + severityNames);
        }

        TestInstantiationException expectingDiagnosticCode() {
            return error("Failed to parse diagnostic message header!",
                    Label.primaryOf(source.currentSpan(), "Expected diagnostic message code"),
                    "Must be a valid diagnostic code! See org.bourbon.compiler.Diagnostic.Code for valid values!");
        }

        TestInstantiationException expectingDiagnosticMessage() {
            return error("Failed to parse diagnostic message header!",
                    Label.primaryOf(source.currentSpan(), "Expected diagnostic message"));
        }

        TestInstantiationException expectLabels() {
            return error("Failed to parse diagnostic message!",
                    Label.primaryOf(source.currentSpan(), "Expected one or more diagnostic labels"));
        }

        TestInstantiationException expectSourceFileName() {
            return error("Failed to parse diagnostic message!",
                    Label.primaryOf(source.currentSpan(), "Expected source file name"));
        }

        TestInstantiationException expecInteger() {
            return error("Failed to parse diagnostic message!",
                    Label.primaryOf(source.currentSpan(), "Expected an integer"));
        }

        void primaryLineNumberMismatch(int primaryLine, int lineNumber) {
            warning("Primary line number mismatch! Expected %d but found %d".formatted(primaryLine, lineNumber),
                    Label.primaryOf(source.currentSpan(), "Expected primary line number"));
        }

        TestInstantiationException expectingSourceLineIndent() {
            return error("Failed to parse diagnostic message!",
                    Label.primaryOf(source.currentSpan(), "Expected at least one character source line indent"));
        }

    }

}
