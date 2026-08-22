package org.bourbon.compiler.junit;

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bourbon.compiler.Diagnostic;
import org.bourbon.compiler.Token;
import org.bourbon.compiler.junit.diff.DiagnosticDiffEngine;
import org.bourbon.compiler.junit.diff.DiffEntry;
import org.bourbon.compiler.junit.diff.FieldChange;
import org.bourbon.compiler.junit.diff.InlineTextDiff;
import org.bourbon.compiler.junit.diff.TokenDiffEngine;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.MultipleFailuresError;

public final class CompilerAssertions {

    private CompilerAssertions() {
        // Utility class for compiler test assertions
    }

    // =========================================================================
    // Executable Producers for use inside Assertions.assertAll(...)
    // =========================================================================

    public static Executable diagnosticSequenceMatch(
            List<Diagnostic> expectedDiagnostics,
            List<Diagnostic> actualDiagnostics) {
        return () -> assertDiagnosticsMatch(expectedDiagnostics, actualDiagnostics);
    }

    public static Executable tokenSequenceMatch(
            List<Token> expectedTokens,
            List<Token> actualTokens) {
        return () -> assertTokensMatch(expectedTokens, actualTokens);
    }

    // =========================================================================
    // Direct Assertion Methods (Hierarchical MultipleFailuresError)
    // =========================================================================

    public static void assertDiagnosticsMatch(List<Diagnostic> expectedDiagnostics, List<Diagnostic> actualDiagnostics) {

        var diffEntries = DiagnosticDiffEngine.compare(expectedDiagnostics, actualDiagnostics);
        var itemFailures = new ArrayList<Throwable>();

        for (var diffEntry : diffEntries) {
            switch (diffEntry) {
                case DiffEntry.Unchanged<Diagnostic> _ -> {}

                case DiffEntry.Added<Diagnostic> added ->
                    itemFailures.add(new AssertionError(String.format(
                            "Unexpected diagnostic at index %d: %s",
                            added.actualIndex(),
                            added.item())));

                case DiffEntry.Deleted<Diagnostic> deleted ->
                    itemFailures.add(new AssertionError(String.format(
                            "Missing expected diagnostic at index %d: %s",
                            deleted.expectedIndex(),
                            deleted.item())));

                case DiffEntry.Modified<Diagnostic>(int expectedIndex, int actualIndex, Diagnostic expected, Diagnostic actual, double similarity, List<FieldChange> fieldChanges) -> {
                    var fieldFailures = new ArrayList<Throwable>();

                    for (var fieldChange : fieldChanges) {
                        switch (fieldChange) {
                            case FieldChange.ModifiedField(var fieldName, var expectedValue, var actualValue) ->
                                    fieldFailures.add(assertionFailure()
                                            .message(String.format("field '%s': ", fieldName))
                                            .expected(expectedValue)
                                            .actual(actualValue)
                                            .build());
                            case FieldChange.AddedField(var fieldName, var actualValue) ->
                                fieldFailures.add(assertionFailure()
                                        .message("Unexpected field '%s'".formatted(fieldName))
                                        .actual(actualValue)
                                        .build());

                            case FieldChange.RemovedField(var fieldName, var expectedValue) ->
                                fieldFailures.add(assertionFailure()
                                        .message("Missing field '%s'".formatted(fieldName))
                                        .expected(expectedValue)
                                        .build());
                            case null -> {}
                        }
                    }

                    if (!fieldFailures.isEmpty()) {
                        var heading = String.format("Diagnostic <%s[%s] %s>", expected.severity().name().toLowerCase(Locale.ROOT), expected.code().code(), expected.message()) +
                                ((expectedIndex == actualIndex)
                                        ? String.format(" at index %d", expectedIndex)
                                        : String.format(" at index %d (expected index %d)", actualIndex, expectedIndex));
                        itemFailures.add(new MultipleFailuresError(heading, fieldFailures));
                    }
                }
                case null -> {}
            }
        }

        if (!itemFailures.isEmpty()) {
            throw new MultipleFailuresError("Diagnostic Sequence Assertion Failure", itemFailures);
        }
    }

    public static void assertTokensMatch(List<Token> expectedTokens, List<Token> actualTokens) {

        var diffEntries = TokenDiffEngine.compare(expectedTokens, actualTokens);
        var itemFailures = new ArrayList<Throwable>();

        for (var diffEntry : diffEntries) {
            switch (diffEntry) {
                case DiffEntry.Unchanged<Token> _ -> {}

                case DiffEntry.Added<Token>(int actualIndex, Token item) -> itemFailures.add(assertionFailure()
                        .message(String.format("Unexpected actual token at index %d", actualIndex))
                        .actual(item)
                        .build());

                case DiffEntry.Deleted<Token>(int expectedIndex, Token item) -> itemFailures.add(assertionFailure()
                        .message(String.format("Missing expected token at index %d", expectedIndex))
                        .expected(item)
                        .build());

                case DiffEntry.Modified<Token>(int expectedIndex, int actualIndex, Token expected, Token actual, double similarity, List<FieldChange> fieldChanges) -> {
                    var fieldFailures = new ArrayList<Throwable>();

                    for (var fieldChange : fieldChanges) {
                        switch (fieldChange) {
                            case FieldChange.ModifiedField(var fieldName, var expectedValue, var actualValue) -> fieldFailures.add(assertionFailure()
                                    .message(String.format("field '%s'", fieldName))
                                    .expected(expectedValue)
                                    .actual(actualValue)
                                    .build());

                            case FieldChange.AddedField(var fieldName, var actualValue) -> fieldFailures.add(assertionFailure()
                                    .message(String.format("field '%s'", fieldName))
                                    .actual(actualValue)
                                    .build());

                            case FieldChange.RemovedField(var fieldName, var expectedValue) ->
                                fieldFailures.add(assertionFailure()
                                        .message(String.format("field '%s'", fieldName))
                                        .expected(expectedValue)
                                        .build());

                            case null -> {}
                        }
                    }

                    if (!fieldFailures.isEmpty()) {
                        var heading = String.format("Token <%s @ %d:%d [%d..%d]>",
                                expected.type(), expected.line(), expected.column(), expected.startOffset(), expected.end()) +
                                ((expectedIndex == actualIndex)
                                        ? String.format(" at index %d", expectedIndex)
                                        : String.format(" at index %d (expected index %d)", actualIndex, expectedIndex));
                        itemFailures.add(new MultipleFailuresError(heading, fieldFailures));
                    }
                }
                case null -> {}
            }
        }

        if (!itemFailures.isEmpty()) {
            throw new MultipleFailuresError("Token Sequence Assertion Failure", itemFailures);
        }
    }
}