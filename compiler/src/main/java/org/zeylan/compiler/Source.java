package org.zeylan.compiler;

import java.nio.file.Path;

import org.jspecify.annotations.NullMarked;
import org.zeylan.compiler.SourceSpan.Provider;

@NullMarked
public sealed interface Source extends CharSequence {
    CharSequence content();

    non-sealed interface FileSource extends Source {
        Path filePath();

        default Provider sourceSpan() {
            return (int line, int column, int startOffset, int length) -> new SourceSpan(filePath(), line, column, startOffset, length);
        }
    }
    non-sealed interface AnonymousSource extends Source {
        default Provider sourceSpan() {
            return (int line, int column, int startOffset, int length) -> new SourceSpan(null, line, column, startOffset, length);
        }
    }


    //<editor-fold desc="Folding: SourceSpan Creation methods">

    Provider sourceSpan();

    default SourceSpan spanAt(int line, int column, int startOffset, int length) {
        return sourceSpan().at(line, column, startOffset, length);
    }

    //</editor-fold>

    //<editor-fold desc="Folding: CharSequence default implementation">

    @Override
    default int length() {
        return content().length();
    }

    @Override
    default char charAt(int index) {
        return content().charAt(index);
    }

    @Override
    default CharSequence subSequence(int start, int end) {
        return content().subSequence(start, end);
    }

    String toString();

    //</editor-fold>
}
