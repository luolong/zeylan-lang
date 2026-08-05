package org.zeylan.compiler.cli;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.zeylan.compiler.Diagnostic;
import org.zeylan.compiler.DiagnosticFormatter;
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
        var sourceStr = source.toString();
        var sourceLines = sourceStr.split("\\r?\\n", -1);

        for (var diagnostic : diagnostics) {
            if (diagnostic instanceof Diagnostic(var code, var severity, var message, var labels, var suggestions)) {
                out.print(DiagnosticFormatter.formatHeader(severity, code, message).toAnsi());

                int lineNumWidth = 3;
                var primaryLabel = Label.primary(labels).orElseGet(() -> Lists.head(labels));
                if (primaryLabel != null) {
                    var span = primaryLabel.span();
                    var filepathStr = span.filepath() != null ? span.filepath().toString() : "<repl>";

                    // Calculate max line number width to ensure perfect vertical bar alignment
                    int maxLineNum = 1;
                    for (var label : labels) {
                        var loc = DiagnosticFormatter.lineCol(sourceStr, label.span().startOffset());
                        if (loc.line() + 1 > maxLineNum) {
                            maxLineNum = loc.line() + 1;
                        }
                    }
                    lineNumWidth = Math.max(3, String.valueOf(maxLineNum).length());

                    var startLoc = DiagnosticFormatter.lineCol(sourceStr, span.startOffset());
                    out.print(DiagnosticFormatter.formatLocation(filepathStr, startLoc.line() + 1, startLoc.col() + 1, lineNumWidth).toAnsi());
                    out.print(DiagnosticFormatter.formatDivider(lineNumWidth).toAnsi());

                    var sortedLabels = new ArrayList<>(labels);
                    sortedLabels.sort((l1, l2) -> Integer.compare(l1.span().startOffset(), l2.span().startOffset()));

                    int lastLineIndex = -1;
                    for (var label : sortedLabels) {
                        var labelSpan = label.span();
                        var loc = DiagnosticFormatter.lineCol(sourceStr, labelSpan.startOffset());
                        int lineIndex = loc.line();
                        int colIndex = loc.col();

                        if (lineIndex >= 0 && lineIndex < sourceLines.length) {
                            var lineStr = sourceLines[lineIndex];

                            if (lineIndex != lastLineIndex) {
                                out.print(DiagnosticFormatter.formatSourceLine(lineIndex + 1, lineStr, lineNumWidth).toAnsi());
                                lastLineIndex = lineIndex;
                            }

                            out.print(DiagnosticFormatter.formatAnnotation(lineNumWidth, colIndex, labelSpan.length(), label.message(), severity, label.isPrimary(), lineStr).toAnsi());
                        }
                    }
                    out.print(DiagnosticFormatter.formatDivider(lineNumWidth).toAnsi());
                }

                if (suggestions != null && !suggestions.isEmpty()) {
                    for (var suggestion : suggestions) {
                        out.print(DiagnosticFormatter.formatSuggestion(suggestion, lineNumWidth).toAnsi());
                    }
                }
                out.println();
            }
        }
        diagnostics.clear();
    }
}
