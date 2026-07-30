package org.zeylan.compiler.cli;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.zeylan.compiler.Diagnostic;
import org.zeylan.compiler.DiagnosticReporter;
import org.zeylan.compiler.Label;
import org.zeylan.compiler.SourceSpan;
import org.zeylan.compiler.util.Lists;

public class ReplDiagnosticHelper implements DiagnosticReporter {
    private final List<Diagnostic> diagnostics = new ArrayList<>();

    @Override
    public void report(Diagnostic diagnostic) {
        diagnostics.add(diagnostic);
    }

    @SuppressWarnings("ConstantValue")
    public void printDiagnostics(CharSequence source, PrintWriter out) {
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic instanceof Diagnostic(var code, var severity, var message, var labels, var suggestions)) {
                out.println("| " + severity + " [" + code.code() + "]: " + message);
                var primaryLabel = Label.primary(labels).orElseGet(() -> Lists.head(labels));
                if (primaryLabel instanceof Label(SourceSpan(var filepath, var line, var column, var startOffset, var length), String label, boolean isPrimary)) {
                    if (filepath != null) {
                        out.println("| " + filepath + ":" + line + "," + column);
                    }
                }
            }
            out.println();
        }
    }

}
