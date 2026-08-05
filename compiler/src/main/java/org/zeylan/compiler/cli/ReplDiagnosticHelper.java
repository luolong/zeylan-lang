package org.zeylan.compiler.cli;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.zeylan.compiler.Diagnostic;
import org.zeylan.compiler.DiagnosticFormatter;
import org.zeylan.compiler.DiagnosticReporter;

public class ReplDiagnosticHelper implements DiagnosticReporter {
    private final List<Diagnostic> diagnostics = new ArrayList<>();

    @Override
    public void report(Diagnostic diagnostic) {
        diagnostics.add(diagnostic);
    }

    public void printDiagnostics(CharSequence source, PrintWriter out) {
        for (var diagnostic : diagnostics) {
            DiagnosticFormatter.format(diagnostic, source, s -> out.print(s.toAnsi()));
            out.println();
        }
        diagnostics.clear();
    }
}
