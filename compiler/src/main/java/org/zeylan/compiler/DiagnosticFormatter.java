package org.zeylan.compiler;

import java.util.ArrayList;
import java.util.function.Consumer;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jspecify.annotations.Nullable;
import org.zeylan.compiler.util.Lists;

public final class DiagnosticFormatter {
    @FunctionalInterface
    public interface OutputHandler {
        void print(AttributedString string);

        default void println() {
            print(AttributedString.NEWLINE);
        }

        static OutputHandler of(Consumer<AttributedString> consumer) {
            return consumer::accept;
        }
    }

    public record LineCol(int line, int col) {}

    public static void format(Source source, Diagnostic diagnostic, OutputHandler out) {
        var severity = diagnostic.severity();
        var code = diagnostic.code();
        var message = diagnostic.message();
        var labels = diagnostic.labels();
        var suggestions = diagnostic.suggestions();

        out.print(formatHeader(severity, code, message));

        int lineNumWidth = 3;
        var primaryLabel = Label.of(labels).orElseGet(() -> Lists.head(labels));
        if (primaryLabel != null) {
            var span = primaryLabel.span();
            var filepathStr = source.name();
            var fileContent = source.content().toString();

            var sourceLines = fileContent.lines().toArray(String[]::new);

            // Calculate max line number width to ensure perfect vertical bar alignment
            int maxLineNum = labels.stream()
                    .mapToInt(label -> lineCol(fileContent, label.span().startOffset()).line() + 1)
                    .max()
                    .orElse(1);
            lineNumWidth = Math.max(3, String.valueOf(maxLineNum).length());

            var startLoc = lineCol(fileContent, span.startOffset());
            out.print(formatLocation(filepathStr, startLoc.line() + 1, startLoc.col() + 1, lineNumWidth));
            out.print(formatDivider(lineNumWidth));

            var sortedLabels = new ArrayList<>(labels);
            sortedLabels.sort((l1, l2) -> Integer.compare(l1.span().startOffset(), l2.span().startOffset()));

            int lastLineIndex = -1;
            for (var label : sortedLabels) {
                var labelSpan = label.span();
                var loc = lineCol(fileContent, labelSpan.startOffset());
                int lineIndex = loc.line();
                int colIndex = loc.col();

                if (lineIndex >= 0 && lineIndex < sourceLines.length) {
                    var lineStr = sourceLines[lineIndex];

                    if (lineIndex != lastLineIndex) {
                        out.print(formatSourceLine(lineIndex + 1, lineStr, lineNumWidth));
                        lastLineIndex = lineIndex;
                    }

                    out.print(formatAnnotation(lineNumWidth, colIndex, labelSpan.length(), label.message(), severity, label.isPrimary(), lineStr));
                }
            }
            out.print(formatDivider(lineNumWidth));
        }

        final int finalLineNumWidth = lineNumWidth;
        suggestions.forEach(suggestion -> out.print(formatSuggestion(suggestion, finalLineNumWidth)));
    }

    public static LineCol lineCol(String source, int startOffset) {
        var prefix = source.substring(0, Math.min(startOffset, source.length()));
        var normalized = prefix.replace("\r\n", "\n").replace('\r', '\n');
        int line = (int) normalized.chars().filter(c -> c == '\n').count();
        int lastNewline = normalized.lastIndexOf('\n');
        int col = lastNewline == -1 ? normalized.length() : normalized.length() - 1 - lastNewline;
        return new LineCol(line, col);
    }

    public static AttributedStyle severityStyle(Diagnostic.Severity severity) {
        return switch (severity) {
            case ERROR -> AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED);
            case WARNING -> AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.YELLOW);
            case NOTE -> AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN);
            case INFO -> AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN);
        };
    }

    private static boolean useNerdFonts = false;

    public static void setUseNerdFonts(boolean use) {
        useNerdFonts = use;
    }

    public static boolean isUseNerdFonts() {
        return useNerdFonts;
    }

    public static String severityIcon(Diagnostic.Severity severity) {
        if (useNerdFonts) {
            return switch (severity) {
                case ERROR -> "\uF057 ";
                case WARNING -> "\uF071 ";
                case NOTE, INFO -> "\uF05A ";
            };
        } else {
            return switch (severity) {
                case ERROR -> "\u2717 ";
                case WARNING -> "\u26A0 ";
                case NOTE, INFO -> "\u2139 ";
            };
        }
    }

    public static AttributedString formatHeader(Diagnostic.Severity severity, Diagnostic.Code code, String message) {
        var sb = new AttributedStringBuilder();
        var severityStr = severity.name().toLowerCase();
        sb.style(severityStyle(severity)).append(severityIcon(severity)).append(severityStr);
        sb.style(AttributedStyle.DEFAULT.bold()).append("[").append(code.code()).append("]");
        sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.WHITE)).append(": ").append(message).append("\n");
        return sb.toAttributedString();
    }

    public static AttributedString formatLocation(String filepathStr, int line, int column, int lineNumWidth) {
        var sb = new AttributedStringBuilder();
        var boldBlue = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.BLUE);
        var arrowSpacing = " ".repeat(Math.max(0, lineNumWidth - 1));
        sb.style(boldBlue).append(arrowSpacing).append("--> ").style(AttributedStyle.DEFAULT)
          .append(filepathStr).append(":").append(String.valueOf(line)).append(":").append(String.valueOf(column)).append("\n");
        return sb.toAttributedString();
    }

    public static AttributedString formatDivider(int lineNumWidth) {
        var sb = new AttributedStringBuilder();
        var boldBlue = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.BLUE);
        var dividerLine = String.format("%" + lineNumWidth + "s |", "");
        sb.style(boldBlue).append(dividerLine).append("\n");
        return sb.toAttributedString();
    }

    public static AttributedString formatSourceLine(int lineNum, String lineStr, int lineNumWidth) {
        var sb = new AttributedStringBuilder();
        var boldBlue = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.BLUE);
        var lineNumStr = String.format("%" + lineNumWidth + "d", lineNum);
        sb.style(boldBlue).append(lineNumStr).append(" | ").style(AttributedStyle.DEFAULT).append(lineStr).append("\n");
        return sb.toAttributedString();
    }

    public static AttributedString formatAnnotation(int lineNumWidth, int colIndex, int length, @Nullable String labelMessage, Diagnostic.Severity severity, boolean isPrimary, String lineStr) {
        var sb = new AttributedStringBuilder();
        var boldBlue = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.BLUE);
        var emptyLineNum = String.format("%" + lineNumWidth + "s", "");
        sb.style(boldBlue).append(emptyLineNum).append(" | ").style(AttributedStyle.DEFAULT);

        int limit = Math.min(colIndex, lineStr.length());
        var align = lineStr.substring(0, limit).replaceAll("[^\t]", " ");
        if (colIndex > limit) {
            align += " ".repeat(colIndex - limit);
        }
        sb.append(align);

        var underlineStyle = isPrimary ? severityStyle(severity) : AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN);
        int underlineLen = colIndex < lineStr.length() ? Math.clamp(length, 1, lineStr.length() - colIndex) : 1;
        sb.style(underlineStyle).append("^".repeat(underlineLen));

        if (labelMessage != null && !labelMessage.isEmpty()) {
            sb.append(" ").append(labelMessage);
        }
        sb.append("\n");
        return sb.toAttributedString();
    }

    public static AttributedString formatSuggestion(String suggestion, int lineNumWidth) {
        var sb = new AttributedStringBuilder();
        var prefix = String.format("%" + lineNumWidth + "s = ", "");
        sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.BLUE)).append(prefix)
          .style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN)).append("\uF0EB help: ")
          .style(AttributedStyle.DEFAULT).append(suggestion).append("\n");
        return sb.toAttributedString();
    }
}
