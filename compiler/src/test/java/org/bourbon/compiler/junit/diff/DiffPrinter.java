package org.bourbon.compiler.junit.diff;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.bourbon.compiler.Diagnostic;
import org.bourbon.compiler.Token;

public final class DiffPrinter {

    private final String headerTitle;

    private final List<DiffEntry<Token>> tokenDiffEntries = new ArrayList<>();
    private final List<DiffEntry<Diagnostic>> diagnosticDiffEntries = new ArrayList<>();

    private boolean verbose = false;

    public DiffPrinter(String name) {
        this.headerTitle = name;
    }

    public DiffPrinter withTokenDiff(List<Token> expectedTokens, List<Token> actualTokens) {
        tokenDiffEntries.addAll(TokenDiffEngine.compare(expectedTokens, actualTokens));
        return this;
    }

    public DiffPrinter withDiagnosticDiff(List<Diagnostic> expectedDiagnostics, List<Diagnostic> actualDiagnostics) {
        diagnosticDiffEntries.addAll(DiagnosticDiffEngine.compare(expectedDiagnostics, actualDiagnostics));
        return this;
    }

    public DiffPrinter verbose() {
        this.verbose = true;
        return this;
    }

    public void printDiff() {
        printDiff(System.out);
    }

    public void printDiff(PrintStream destinationStream) {
        printHeaderSummary(destinationStream);
        printDiffEntries(verbose, tokenDiffEntries, destinationStream);
        printDiffEntries(verbose, diagnosticDiffEntries, destinationStream);
    }

    private void printHeaderSummary(PrintStream destinationStream) {
        destinationStream.println("================================================================================");
        destinationStream.printf(" %s%n", headerTitle);

        printStatLine("Tokens", tokenDiffEntries, destinationStream);
        printStatLine("Diagnostic messages", diagnosticDiffEntries, destinationStream);

        destinationStream.println("================================================================================");
    }

    private static <T> void printStatLine(String statHeader, List<DiffEntry<T>> diffEntries, PrintStream destinationStream) {
        int unchangedCount = 0;
        int modifiedCount = 0;
        int addedCount = 0;
        int deletedCount = 0;

        for (var diffEntry : diffEntries) {
            switch (diffEntry) {
                case DiffEntry.Unchanged<T> ignored -> unchangedCount++;
                case DiffEntry.Modified<T> ignored -> modifiedCount++;
                case DiffEntry.Added<T> ignored -> addedCount++;
                case DiffEntry.Deleted<T> ignored -> deletedCount++;
            }
        }

        destinationStream.printf(" %s: Total=%d | Unchanged=%d | Modified=%d | Added=%d | Deleted=%d%n",
                statHeader, diffEntries.size(), unchangedCount, modifiedCount, addedCount, deletedCount);
    }


    public static <T> void printDiffEntries(boolean verbose, List<DiffEntry<T>> diffEntries, PrintStream destinationStream) {
        for (var diffEntry : diffEntries) {
            switch (diffEntry) {
                case DiffEntry.Unchanged<T>(int expectedIndex, int actualIndex, T item) -> {
                    if (verbose) {
                        if (expectedIndex == actualIndex) {
                            destinationStream.printf("  [OK] [index:%d] %s%n", expectedIndex, item);
                        } else {
                            destinationStream.printf("  [OK] [expected:%d, actual:%d] %s%n", expectedIndex, actualIndex, item);
                        }
                    }
                }

                case DiffEntry.Added<T>(int index, T item) -> destinationStream.printf("  + [ADDED at actual:%d] %s%n", index, item);

                case DiffEntry.Deleted<T>(int index, T item) -> destinationStream.printf("  - [DELETED at expected:%d] %s%n", index, item);

                case DiffEntry.Modified<T>(int expectedIndex, int actualIndex, T expected, T _, double _, List<FieldChange> fieldChanges) -> {
                    if (expectedIndex == actualIndex) {
                        destinationStream.printf("  ~ [MODIFIED at index:%d] %s%n", expectedIndex, expected);
                    } else {
                        destinationStream.printf("  ~ [MODIFIED at expected:%d, actual:%d] %s%n", expectedIndex, actualIndex, expected);
                    }

                    for (var fieldChange : fieldChanges) {
                        switch (fieldChange) {
                            case FieldChange.ModifiedField(var fieldName, var expectedValue, var actualValue) -> {
                                if (expectedValue instanceof String expectedString
                                        && actualValue instanceof String actualString
                                        && !expectedString.contains("\n")
                                        && !actualString.contains("\n")) {
                                    var inlineDiff = InlineTextDiff.formatInlineDiff(expectedString, actualString);
                                    destinationStream.printf("      * %s: %s%n", fieldName, inlineDiff);
                                } else {
                                    destinationStream.printf("      * %s: expected '%s', got '%s'%n",
                                            fieldName,
                                            expectedValue,
                                            actualValue);
                                }
                            }

                            case FieldChange.AddedField(var fieldName, var actualValue) ->
                                    destinationStream.printf("      + %s: unexpected value '%s'%n",
                                            fieldName,
                                            actualValue);

                            case FieldChange.RemovedField(var fieldName, var expectedValue) ->
                                    destinationStream.printf("      - %s: missing expected value '%s'%n",
                                            fieldName,
                                            expectedValue);
                            case null -> {}
                        }
                    }
                }
                case null -> {}
            }
        }

        destinationStream.println("================================================================================");
    }
}