package org.bourbon.compiler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bourbon.compiler.SourceSpan.SourceName;

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

    public Diagnostic(Code code, Severity severity, String message, List<Label> labels) {
        this(code, severity, message, labels, List.of());
    }

    public boolean isError() {
        return severity == Severity.ERROR;
    }

    /// Code for compiler pipeline diagnostic messages.
    ///
    /// The codes are roughly divided into several subcategories:
    /// - BRB000000–BRB000099 — Technical internal compiler pipeline errors. Quite likely these signify a bug or compiler implementation issues.
    /// - BRB000100–BRB000999 — Bourbon language scanner issues. Most likely these are caused by invalid or malformed source code.
    /// - BRB001000–BRB009999 — Bourbon language parser issues. Most likely these are caused by invalid or malformed source code.
    /// - BRB010000–BRB999999 — Bourbon compiler diagnostic messages. These are related to semantic issues with code.
    ///                         These include typechecker diagnostics, linting notices, correctness issues, improvement suggestions, etc.
    public enum Code {
        /// Marker exception for pieces of code that are not implemented yet.
        NotImplemented("BCE000000"),

        /// Internal diagnostic message to be printed if Scanner test cases fail to be parsed.
        ///
        /// This is an internal exception signaling that compiler scanner tests failed due to invalid or malformed test cases.
        ScannerTestCaseParserError("BCE000001"),

        /// File not found.
        ///
        /// Returned by whenever the compiler tries to access a file and fails to open it at an expected location.
        /// This is a compiler internal error, signifying inability to complete the compilation process due to a missing or unreachable file.
        IoFileNotFound("BCE000010"),

        /// Generic IO error occurred.
        ///
        /// An unexpected IO exception occurred while trying to access IO-bound resources.
        /// This is a compiler internal error, signifying inability to complete the compilation process due to unexpected IO issues.
        IoError("BCE000019"),

        /// Unexpected character while scanning the source
        ScannerUnexpectedCharacter("BCE000100"),

        /// Unbalanced multiline comment
        ScannerUnbalancedMultilineComment("BCE000101");

        Code(String code) {
            this.code = code;
        }

        private final String code;

        public String code() {
            return code;
        }

        private static final Map<String, Code> CODES = Arrays.stream(values())
                .collect(Collectors.toMap(Code::code, Function.identity()));

        public static Code fromCode(String value) {
            var code = CODES.get(value);
            if (code == null) {
                throw new IllegalArgumentException("Unknown diagnostic code: " + value);
            }

            return code;
        }
    }

    public enum Severity {
        ERROR, WARNING, NOTE, INFO;
        public static String[] names() {
            var values = values();
            var names = new String[values.length];
            for (var i = 0; i < names.length; i++) {
                names[i] = values[i].name();
            }
            return names;
        }
    }

    public static Diagnostic error(Code code, String message, List<Label> labels) {
        return new Diagnostic(code, Severity.ERROR, message, labels);
    }
    public static Diagnostic error(Code code, String message, List<Label> labels, List<String> suggestions) {
        return new Diagnostic(code, Severity.ERROR, message, labels, suggestions);
    }

    public static Diagnostic warning(Code code, String message, List<Label> labels) {
        return new Diagnostic(code, Severity.WARNING, message, labels);
    }
    public static Diagnostic warning(Code code, String message, List<Label> labels, List<String> suggestions) {
        return new Diagnostic(code, Severity.WARNING, message, labels, suggestions);
    }


    @SuppressWarnings("unused")
    public static final class Internal {
        private Internal() { /* sealed */}

        public static Diagnostic notImplemented(SourceSpan sourceSpan, String message) {
            return error(Code.NotImplemented, "Not implemented!",
                    List.of(new Label(sourceSpan, message, true)));
        }

        public static Diagnostic ioError(Path path, IOException exception) {
            var sourceSpan = new SourceSpan(SourceName.of(path), 0, 0, 0, 0);
            return ioError(path, sourceSpan, exception);
        }

        public static Diagnostic ioError(Path path, SourceSpan sourceSpan, IOException exception) {
            return switch (exception) {
                case FileNotFoundException fileNotFound -> fileNotFound(path, sourceSpan, fileNotFound);
                default -> unknownIoError(path, sourceSpan, exception);
            };
        }

        public static Diagnostic fileNotFound(Path path, SourceSpan sourceSpan, FileNotFoundException exception) {
            return error(Code.IoFileNotFound, Objects.requireNonNull(exception.getMessage()),
                    List.of(new Label(sourceSpan, "File not found", true)));
        }

        public static Diagnostic unknownIoError(Path path, SourceSpan sourceSpan, IOException exception) {
            return error(Code.IoError, "IO error", List.of(new Label(sourceSpan, exception.getMessage(), true)));
        }

    }

    public static final class Scanner {
        private Scanner() {/* sealed */}

        public static Diagnostic unexpectedCharacter(SourceSpan span) {
            return error(Code.ScannerUnexpectedCharacter, "Unexpected character",
                    List.of(Label.primaryOf(span, "Unexpected symbol")));
        }

        public static Diagnostic unbalancedMultilineComment(SourceSpan startSpan, SourceSpan endSpan) {
            return error(Code.ScannerUnbalancedMultilineComment, "Unbalanced Multiline comment",
                    List.of(Label.primaryOf(startSpan, "Multi-line comment starts here"),
                            Label.of(endSpan, "Still no end of comment")));
        }

    }

}
