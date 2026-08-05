package org.zeylan.compiler;

public class ConsoleDiagnosticHandler implements DiagnosticReporter {

    @Override
    public void report(Diagnostic diagnostic) {
        DiagnosticFormatter.format(diagnostic, null, s -> {
            if (System.console() != null) {
                System.err.print(s.toAnsi());
            } else {
                System.err.print(s);
            }
        });
        System.err.println();
    }
}
