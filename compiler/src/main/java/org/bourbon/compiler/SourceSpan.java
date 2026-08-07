package org.bourbon.compiler;

import java.nio.file.Path;

public record SourceSpan(
        SourceName name,
        int line,
        int column,
        int startOffset,
        int length) {

    public interface Provider {
        SourceSpan at(int line, int column, int startOffset, int length);
    }

    public sealed interface SourceName {
        String name();

        static SourceName of(String name) {
            return new Name(name);
        }

        static SourceName of(Path path) {
            return new FilePath(path);
        }

        record Name(String name) implements SourceName {

            @Override public String toString() {
                return name();
            }

        }
        record FilePath(Path path) implements SourceName {
            public String name() {
                return path.toString();
            }

            @Override public String toString() {
                return name();
            }

        }
    }
}
