package org.bourbon.compiler;

import java.io.Console;
import java.io.PrintStream;
import java.util.List;

import org.junit.jupiter.api.extension.TestInstantiationException;

public class TestCaseParserException extends TestInstantiationException {

    private final Diagnostic.Code exceptionCode;
    private final SourceSpan sourceSpan;

    public TestCaseParserException(Diagnostic.Code exceptionCode, SourceSpan sourceSpan, String message) {
        super(message);
        this.sourceSpan = sourceSpan;
        this.exceptionCode = exceptionCode;
    }

    public Diagnostic toDiagnostic() {
        return new Diagnostic(exceptionCode,
                Diagnostic.Severity.ERROR,
                "Failed to parse expected token!",
                List.of(new Label(sourceSpan, getMessage(), true)),
                List.of());
    }

    public static void printStackTrace(TestCaseParserException exception, Source source) {
        printStackTrace(System.err, exception, source);
    }

    public static void printStackTrace(PrintStream out, TestCaseParserException exception, Source source) {
        boolean isStandardStream = System.err == out || System.out == out;
        if (isStandardStream && isTerminal()) {
            DiagnosticFormatter.format(source, exception.toDiagnostic(), s -> out.println(s.toAnsi()));
        } else {
            DiagnosticFormatter.format(source, exception.toDiagnostic(), out::print);
        }

        exception.printStackTrace(out);
    }

    private static boolean isTerminal() {
        var console = System.console();
        return console != null && console.isTerminal();
    }

}
