package org.zeylan.compiler;

import java.nio.file.Path;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.zeylan.compiler.SourceSpan.Provider;

/**
 * Stateful source for scanner.
 */
@NullMarked
public abstract class Source implements CharSequence {

    private int start = 0;
    private int current = 0;

    private int line = 1;
    private int column = 1;

    protected abstract CharSequence content();

    abstract static class FileSource extends Source {
        abstract Path filePath();

        final Provider sourceSpan() {
            return (int line, int column, int startOffset, int length) -> new SourceSpan(filePath(), line, column, startOffset, length);
        }
    }

    public abstract static class AnonymousSource extends Source {
        final Provider sourceSpan() {
            return (int line, int column, int startOffset, int length) -> new SourceSpan(null, line, column, startOffset, length);
        }
    }

    //<editor-fold desc="Folding: Scanner helpers">

    public boolean isAtEnd() {
        return current >= length();
    }

    public void startAtCurrent() {
        start = current;
    }

    public char advance() {
        return charAt(current++);
    }

    public Token token(TokenType type) {
        return token(type, null);
    }

    public Token token(TokenType type, @Nullable Object literal) {
        var lexeme = subSequence(start, current).toString();
        return new Token(type, lexeme, literal, line, column, start, current - start);
    }

    //</editor-fold>


    //<editor-fold desc="Folding: SourceSpan Creation methods">

    abstract Provider sourceSpan();

    public final SourceSpan spanAt(int line, int column, int startOffset, int length) {
        return sourceSpan().at(line, column, startOffset, length);
    }

    public final SourceSpan spanAtCurrent() {
        return sourceSpan().at(line, column, start, current - start);
    }

    //</editor-fold>

    //<editor-fold desc="Folding: CharSequence default implementation">

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
}
