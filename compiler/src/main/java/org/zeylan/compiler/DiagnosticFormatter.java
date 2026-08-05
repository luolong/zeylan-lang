package org.zeylan.compiler;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jspecify.annotations.Nullable;

public final class DiagnosticFormatter {

    public record LineCol(int line, int col) {}

    public static LineCol lineCol(String source, int startOffset) {
        int line = 0;
        int col = 0;
        int offset = 0;
        int len = source.length();
        while (offset < startOffset && offset < len) {
            var c = source.charAt(offset);
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

    public static AttributedStyle severityStyle(Diagnostic.Severity severity) {
        return switch (severity) {
            case ERROR -> AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED);
            case WARNING -> AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.YELLOW);
            case NOTE -> AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN);
            case INFO -> AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN);
        };
    }

    public static String severityIcon(Diagnostic.Severity severity) {
        return switch (severity) {
            case ERROR -> "\uF057 ";
            case WARNING -> "\uF071 ";
            case NOTE, INFO -> "\uF05A ";
        };
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

        var align = new StringBuilder();
        for (int i = 0; i < colIndex && i < lineStr.length(); i++) {
            var c = lineStr.charAt(i);
            if (c == '\t') {
                align.append('\t');
            } else {
                align.append(' ');
            }
        }
        sb.append(align.toString());

        var underlineStyle = isPrimary ? severityStyle(severity) : AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN);
        var underlineChar = '^';

        int underlineLen = Math.clamp(length, 1, lineStr.length() - colIndex);
        sb.style(underlineStyle);
        for (int i = 0; i < underlineLen; i++) {
            sb.append(underlineChar);
        }

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
