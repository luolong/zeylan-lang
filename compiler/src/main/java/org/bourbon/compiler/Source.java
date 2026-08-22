package org.bourbon.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Stateful source for scanner.
 */
@NullMarked
public class Source implements CharSequence {

    private final CharSequence content;
    private final String name;

    private int start = 0;
    private int current = 0;
    private char currentChar = '\0';

    private int startLine = 1;
    private int currentLine = 1;
    private final List<Integer> lineOffsets = new ArrayList<>(List.of(start));


    private Source(CharSequence content, String name) {
        this.content = content;
        this.name = name;
    }

    public CharSequence content() {
        return content;
    }

    public String name() {
        return name;
    }

    //<editor-fold desc="Current source positions">

    int column() {
        return column(start);
    }

    int column(int offset) {
        if (offset == start) return start - lineOffset(startLine) + 1;
        if (offset == current) return current - lineOffset(currentLine) + 1;

        int lineOffset = lineOffsetAt(offset);
        return offset - lineOffset + 1;
    }

    int start() {
        return start;
    }

    int current() {
        return current;
    }

    int currentLine() {
        return currentLine;
    }

    private int lineOffset(int line) {
        return lineOffsets.get(line - 1);
    }

    private int lineOffsetAt(int offset) {
        return lineOffset(lineNumberAt(offset));
    }

    public List<Integer> lineOffsets() {
        return Collections.unmodifiableList(lineOffsets);
    }

    private int lineNumberAt(int offset) {
        if (offset == start) return startLine;
        if (offset == current) return currentLine;

        int index = Collections.binarySearch(lineOffsets, offset);
        return index >= 0 ? index : -index - 1;
    }

    //</editor-fold>

    //<editor-fold desc="Scanner helpers">

    public boolean isAtEnd() {
        return current >= length();
    }

    public void tokenStart() {
        startLine = currentLine;
        start = current;
    }

    public void tokenReset() {
        current = start;
    }

    Source resetToLine(int line) {
        if (line < 1) throw new IllegalArgumentException("line must be greater than zero");
        if (line <= lineOffsets.size()) {
            current = lineOffset(line);
            currentLine = line;
            tokenStart();
        } else {
            while (!isAtEnd() && currentLine < line) advance();
            tokenStart();
        }
        return this;
    }

    public char advance() {
        currentChar = charAt(current++);
        if (currentChar == '\n') {
            lineOffsets.add(currentLine++, current);
        }
        return currentChar;
    }

    public char peek() {
        if (isAtEnd())
            return '\0';
        return charAt(current);
    }

    public boolean peek(char expected) {
        if (isAtEnd())
            return false;

        return charAt(current) == expected;
    }

    public boolean peek(CharSequence expected) {
        if (isAtEnd())
            return false;

        for (int i = 0; i < expected.length(); i++) {
            if (charAt(current + i) != expected.charAt(i))
                return false;
        }
        return true;
    }

    public boolean match(char expected) {
        if (peek(expected)) {
            current++;
            return true;
        }

        return false;
    }

    public boolean match(CharSequence expected) {
        if (peek(expected)) {
            current += expected.length();
            return true;
        }

        return false;
    }

    public String lexeme() {
        return subSequence(start, current).toString();
    }

    public Token token(TokenType type) {
        return token(type, null);
    }

    public Token token(TokenType type, @Nullable Object literal) {
        return new Token(type, lexeme(), literal, startLine, column(), start, current - start);
    }

    //</editor-fold>

    //<editor-fold desc="SourceSpan creation">

    protected SourceSpan.SourceName sourceName() {
        return SourceSpan.SourceName.of(name());
    }

    protected final SourceSpan.Provider sourceSpan() {
        return (int line, int column, int startOffset, int length) ->
                new SourceSpan(sourceName(), line, column, startOffset, length);
    }

    public final SourceSpan spanAt(int startOffset, int length) {
        assert current <= startOffset : "Cannot create spans past current cursor position!";

        if (startOffset == start) return sourceSpan().at(startLine, column(), start, length);
        if (startOffset == current) return sourceSpan().at(currentLine, column(current), current, length);

        return sourceSpan().at(lineNumberAt(startOffset), column(startOffset), startOffset, length);
    }

    public final SourceSpan currentSpan() {
        return sourceSpan().at(startLine, column(), start, current - start);
    }

    //</editor-fold>

    //<editor-fold desc="CharSequence default implementation">

    @Override
    public final int length() {
        return content().length();
    }

    @Override
    public final char charAt(int index) {
        return content().charAt(index);
    }

    @Override
    public final CharSequence subSequence(int start, int end) {
        return content().subSequence(start, end);
    }

    public final String toString() {
        return content().toString();
    }

    //</editor-fold>

    //<editor-fold desc="Creating a source">

    public static Source of(CharSequence content) {
        if (content instanceof Source source) {
            return named(source.name()).of(source.content());
        }
        return named("<string>").of(content);
    }

    public static Source of(InputStream stream) throws IOException {
        if (stream == System.in) return stdIn();
        return named("<stream>").of(Content.read(stream));
    }

    public static Source of(Path filePath) throws IOException {
        String sourceName = filePath.getFileName().toString();
        return named(sourceName).of(Content.read(filePath));
    }

    public static Source stdIn() throws IOException {
        return named("<stdin>").of(Content.read(System.in));
    }

    static SourceName named(String name) {
        return new SourceName(name);
    }

    public static class SourceName {
        private final String name;

        private SourceName(String name) {
            this.name = name;
        }

        public Source of(CharSequence content) {
            return new Source(content, name);
        }
    }

    //</editor-fold>

    static abstract class Content {
        public static CharSequence read(InputStream input) throws IOException {
            return read(input, StandardCharsets.UTF_8);
        }

        public static CharSequence read(InputStream input, Charset charset) throws IOException {;
            return new String(input.readAllBytes(), charset);
        }

        public static CharSequence read(Path filePath) throws IOException {
            return Files.readString(filePath);
        }
    }
}
