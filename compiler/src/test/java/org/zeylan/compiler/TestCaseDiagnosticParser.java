package org.zeylan.compiler;

import java.io.StringWriter;
import java.util.List;

import org.junit.jupiter.api.extension.TestInstantiationException;
import org.junit.platform.commons.logging.LoggerFactory;
import org.zeylan.compiler.DiagnosticFormatter.OutputHandler;

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
        try {
            return new TestCaseDiagnosticParser(source, lineNumber, lineOffset).parseDiagnostic();
        } catch (TestCaseDiagnosticParserException e) {
            var out = new StringWriter();
            DiagnosticFormatter.format(source, e.toDiagnostic(), OutputHandler.of(s -> out.write(s.toString())));
            LoggerFactory.getLogger(TestCaseDiagnosticParser.class).error(out::toString);
            throw new TestInstantiationException("Failed to parse diagnostic message!", e);
        }
    }

    private Diagnostic parseDiagnostic() {
        throw new TestCaseDiagnosticParserException(source.currentSpan(), "Diagnostic message parser not implemented!");
    }


    public static class TestCaseDiagnosticParserException extends TestCaseParserException {
        public TestCaseDiagnosticParserException(SourceSpan sourceSpan, String message) {
            super(Diagnostic.Code.ScannerTestCaseParserError, sourceSpan, message);
        }
    }
}
