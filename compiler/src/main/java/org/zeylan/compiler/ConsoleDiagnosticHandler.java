package org.zeylan.compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.zeylan.compiler.util.Lists;

public class ConsoleDiagnosticHandler implements DiagnosticReporter {

    @Override
    public void report(Diagnostic diagnostic) {
        var severity = diagnostic.severity();
        var code = diagnostic.code();
        var message = diagnostic.message();
        var labels = diagnostic.labels();
        var suggestions = diagnostic.suggestions();

        print(DiagnosticFormatter.formatHeader(severity, code, message));

        int lineNumWidth = 3;
        var primaryLabel = Label.primary(labels).orElseGet(() -> Lists.head(labels));
        if (primaryLabel != null) {
            var span = primaryLabel.span();
            var filepath = span.filepath();
            if (filepath != null) {
                try {
                    var sourceLines = Files.readAllLines(filepath);
                    var fileContent = Files.readString(filepath);

                    // Calculate max line number width to ensure perfect vertical bar alignment
                    int maxLineNum = 1;
                    for (var label : labels) {
                        var loc = DiagnosticFormatter.lineCol(fileContent, label.span().startOffset());
                        if (loc.line() + 1 > maxLineNum) {
                            maxLineNum = loc.line() + 1;
                        }
                    }
                    lineNumWidth = Math.max(3, String.valueOf(maxLineNum).length());

                    var startLoc = DiagnosticFormatter.lineCol(fileContent, span.startOffset());
                    print(DiagnosticFormatter.formatLocation(filepath.toString(), startLoc.line() + 1, startLoc.col() + 1, lineNumWidth));
                    print(DiagnosticFormatter.formatDivider(lineNumWidth));

                    var sortedLabels = new ArrayList<>(labels);
                    sortedLabels.sort((l1, l2) -> Integer.compare(l1.span().startOffset(), l2.span().startOffset()));

                    int lastLineIndex = -1;
                    for (var label : sortedLabels) {
                        var labelSpan = label.span();
                        var loc = DiagnosticFormatter.lineCol(fileContent, labelSpan.startOffset());
                        int lineIndex = loc.line();
                        int colIndex = loc.col();

                        if (lineIndex >= 0 && lineIndex < sourceLines.size()) {
                            var lineStr = sourceLines.get(lineIndex);

                            if (lineIndex != lastLineIndex) {
                                print(DiagnosticFormatter.formatSourceLine(lineIndex + 1, lineStr, lineNumWidth));
                                lastLineIndex = lineIndex;
                            }

                            print(DiagnosticFormatter.formatAnnotation(lineNumWidth, colIndex, labelSpan.length(), label.message(), severity, label.isPrimary(), lineStr));
                        }
                    }
                    print(DiagnosticFormatter.formatDivider(lineNumWidth));
                } catch (IOException e) {
                    print(DiagnosticFormatter.formatLocation(filepath.toString(), span.line(), span.column(), lineNumWidth));
                }
            } else {
                print(DiagnosticFormatter.formatLocation("<anonymous>", span.line(), span.column(), lineNumWidth));
            }
        }

        if (suggestions != null && !suggestions.isEmpty()) {
            for (var suggestion : suggestions) {
                print(DiagnosticFormatter.formatSuggestion(suggestion, lineNumWidth));
            }
        }
        System.err.println();
    }

    private void print(org.jline.utils.AttributedString s) {
        if (System.console() != null) {
            System.err.print(s.toAnsi());
        } else {
            System.err.print(s.toString());
        }
    }
}
