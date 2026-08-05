package org.zeylan.compiler.cli;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
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

    private record LineCol(int line, int col) {}

    private static LineCol getLineCol(String source, int startOffset) {
        int line = 0;
        int col = 0;
        int offset = 0;
        int len = source.length();
        while (offset < startOffset && offset < len) {
            char c = source.charAt(offset);
            if (c == '\n') {
                line++;
                col = 0;
            } else if (c == '\r') {
                if (offset + 1 < len && source.charAt(offset + 1) == '\n') {
                    offset++;
                }
                line++;
                col = 0;
            } else {
                col++;
            }
            offset++;
        }
        return new LineCol(line, col);
    }

    @SuppressWarnings("ConstantValue")
    public void printDiagnostics(CharSequence source, PrintWriter out) {
        String sourceStr = source.toString();
        String[] sourceLines = sourceStr.split("\\r?\\n", -1);

        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic instanceof Diagnostic(var code, var severity, var message, var labels, var suggestions)) {
                AttributedStringBuilder sb = new AttributedStringBuilder();

                String severityStr = severity.name().toLowerCase();
                AttributedStyle severityStyle = switch (severity) {
                    case ERROR -> AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED);
                    case WARNING -> AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.YELLOW);
                    case NOTE -> AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN);
                    case INFO -> AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN);
                };
                String icon = switch (severity) {
                    case ERROR -> "\uF057 ";
                    case WARNING -> "\uF071 ";
                    case NOTE, INFO -> "\uF05A ";
                };

                sb.style(severityStyle).append(icon).append(severityStr);
                if (code != null) {
                    sb.style(AttributedStyle.DEFAULT.bold()).append("[").append(code.code()).append("]");
                }
                sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.WHITE)).append(": ").append(message).append("\n");

                Label primaryLabel = Label.primary(labels).orElseGet(() -> Lists.head(labels));
                if (primaryLabel != null) {
                    SourceSpan span = primaryLabel.span();
                    String filepathStr = span.filepath() != null ? span.filepath().toString() : "<repl>";

                    // Calculate max line number width to ensure perfect vertical bar alignment
                    int maxLineNum = 1;
                    for (Label label : labels) {
                        LineCol loc = getLineCol(sourceStr, label.span().startOffset());
                        if (loc.line + 1 > maxLineNum) {
                            maxLineNum = loc.line + 1;
                        }
                    }
                    int lineNumWidth = Math.max(3, String.valueOf(maxLineNum).length());

                    LineCol startLoc = getLineCol(sourceStr, span.startOffset());
                    AttributedStyle boldBlue = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.BLUE);

                    String arrowSpacing = " ".repeat(Math.max(0, lineNumWidth - 1));
                    sb.style(boldBlue).append(arrowSpacing).append("--> ").style(AttributedStyle.DEFAULT).append(filepathStr).append(":").append(String.valueOf(startLoc.line + 1)).append(":").append(String.valueOf(startLoc.col + 1)).append("\n");

                    String dividerLine = String.format("%" + lineNumWidth + "s |", "");
                    sb.style(boldBlue).append(dividerLine).append("\n");

                    List<Label> sortedLabels = new ArrayList<>(labels);
                    sortedLabels.sort((l1, l2) -> Integer.compare(l1.span().startOffset(), l2.span().startOffset()));

                    int lastLineIndex = -1;
                    for (Label label : sortedLabels) {
                        SourceSpan labelSpan = label.span();
                        LineCol loc = getLineCol(sourceStr, labelSpan.startOffset());
                        int lineIndex = loc.line;
                        int colIndex = loc.col;

                        if (lineIndex >= 0 && lineIndex < sourceLines.length) {
                            String lineStr = sourceLines[lineIndex];

                            if (lineIndex != lastLineIndex) {
                                String lineNumStr = String.format("%" + lineNumWidth + "d", lineIndex + 1);
                                sb.style(boldBlue).append(lineNumStr).append(" | ").style(AttributedStyle.DEFAULT).append(lineStr).append("\n");
                                lastLineIndex = lineIndex;
                            }

                            String emptyLineNum = String.format("%" + lineNumWidth + "s", "");
                            sb.style(boldBlue).append(emptyLineNum).append(" | ").style(AttributedStyle.DEFAULT);

                            StringBuilder align = new StringBuilder();
                            for (int i = 0; i < colIndex && i < lineStr.length(); i++) {
                                char c = lineStr.charAt(i);
                                if (c == '\t') {
                                    align.append('\t');
                                } else {
                                    align.append(' ');
                                }
                            }
                            sb.append(align.toString());

                            AttributedStyle underlineStyle = label.isPrimary() ? severityStyle : AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN);
                            char underlineChar = '^';

                            int underlineLen = Math.max(1, Math.min(labelSpan.length(), lineStr.length() - colIndex));
                            sb.style(underlineStyle);
                            for (int i = 0; i < underlineLen; i++) {
                                sb.append(underlineChar);
                            }

                            if (label.message() != null && !label.message().isEmpty()) {
                                sb.append(" ").append(label.message());
                            }
                            sb.append("\n");
                        }
                    }
                    sb.style(boldBlue).append(dividerLine).append("\n");
                }

                if (suggestions != null && !suggestions.isEmpty()) {
                    for (String suggestion : suggestions) {
                        sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.BLUE)).append("   = ")
                          .style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN)).append("\uF0EB help: ")
                          .style(AttributedStyle.DEFAULT).append(suggestion).append("\n");
                    }
                }
                sb.append("\n");
                out.print(sb.toAnsi());
            }
        }
        diagnostics.clear();
    }
}
