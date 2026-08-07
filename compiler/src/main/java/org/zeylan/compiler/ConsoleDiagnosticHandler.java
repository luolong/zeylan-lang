package org.zeylan.compiler;

import java.io.Console;

import org.zeylan.compiler.DiagnosticFormatter.OutputHandler;

public class ConsoleDiagnosticHandler implements DiagnosticReporter {

    private final Source source;
    private final OutputHandler out;

    private boolean hadError = false;

    public ConsoleDiagnosticHandler(Source source) {
        this.source = source;
        if (System.console() instanceof Console console && console.isTerminal()) {
            this.out = OutputHandler.of(s -> System.err.print(s.toAnsi()));
        } else {
            this.out = OutputHandler.of(System.err::print);
        }
    }

    @Override
    public void report(Diagnostic diagnostic) {
        hadError = hadError || (diagnostic.severity() == Diagnostic.Severity.ERROR);
        DiagnosticFormatter.format(source, diagnostic, out);
        System.err.println();
    }

}
