package org.zeylan.compiler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;import java.util.List;
import java.util.Objects;

/**
 * A rich diagnostic message representing compiler errors, warnings, notes, or
 * info.
 * Designed to support multi-span annotations and suggestions similar to Rust's
 * compiler errors.
 */
public record Diagnostic(
        Code code,
        Severity severity,
        String message,
        List<Label> labels,
        List<String> suggestions) {



    public enum Code {
        /// Marker exception for pieces of code that are not implemented yet.
        NotImplemented("ZSC0000"),

        /// File not found.
        IoFileNotFound("ZSC0010");

        Code(String code) {
            this.code = code;
        }

        private final String code;
        public String code() {
            return code;
        }
    }

    public enum Severity {
        ERROR,
        WARNING,
        NOTE,
        INFO;
    }

    public static Diagnostic notImplemented(SourceSpan sourceSpan, String message) {
        return new Diagnostic(
                Code.NotImplemented,
                Severity.ERROR,
                "Not implemented!",
                List.of(new Label(sourceSpan, message, true)),
                List.of());
    }

    public static Diagnostic ioError(Path path, IOException e) {
        return switch (e) {
            case FileNotFoundException fileNotFound -> fileNotFound(path, fileNotFound);
            default -> new Diagnostic(
                        Code.NotImplemented,
                        Severity.ERROR,
                        "IO error",
                        List.of(new Label(new SourceSpan(path, 0, 0, 0, 0), "IO error", true)),
                        List.of());
        };
    }

    private static Diagnostic fileNotFound(Path path, FileNotFoundException exception) {
        return new Diagnostic(
                Code.IoFileNotFound,
                Severity.ERROR,
                Objects.requireNonNull(exception.getMessage()),
                List.of(new Label(new SourceSpan(path, 0, 0, 0, 0), "File not found", true)),
                List.of());
    }

}
