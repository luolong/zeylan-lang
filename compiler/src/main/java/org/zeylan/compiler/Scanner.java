package org.zeylan.compiler;

import java.util.List;

/**
 * The Scanner class is responsible for tokenizing the source code.
 * It reads the input stream and breaks it down into tokens.
 */
public class Scanner {

    private final Source source;
    private final DiagnosticReporter diagnosticReporter;

    public Scanner(Source source, DiagnosticReporter diagnosticReporter) {
        this.source = source;
        this.diagnosticReporter = diagnosticReporter;
    }

    public List<Token> scanTokens() {
        diagnosticReporter.report(Diagnostic.notImplemented(
                source.spanAt(0, 0, 0, source.length()),
                "Token scanner is not implemented!"));
        return List.of();
    }

}