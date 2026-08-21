package org.bourbon.compiler.junit.diff;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.bourbon.compiler.Diagnostic;
import org.bourbon.compiler.Label;
import org.bourbon.compiler.SourceSpan;

public final class DiagnosticDiffEngine {

    private final StructuredDiffEngine<Diagnostic> engine;
    private final StructuredDiffEngine<Label> labelDiffEngine;
    private final StructuredDiffEngine<String> suggestionDiffEngine;

    public DiagnosticDiffEngine() {
        this(0.30);
    }

    public DiagnosticDiffEngine(double cutoffThreshold) {
        this.labelDiffEngine = new StructuredDiffEngine<>(
                DiagnosticDiffEngine::labelSimilarity,
                DiagnosticDiffEngine::extractLabelChanges,
                cutoffThreshold
        );

        this.suggestionDiffEngine = new StructuredDiffEngine<>(
                DiagnosticDiffEngine::tokenJaccard,
                (expectedSuggestion, actualSuggestion) ->
                        List.of(new FieldChange.ModifiedField("suggestion", expectedSuggestion, actualSuggestion)),
                cutoffThreshold
        );

        this.engine = new StructuredDiffEngine<>(
                this::calculateSimilarity,
                this::extractFieldChanges,
                cutoffThreshold
        );
    }

    public List<DiffEntry<Diagnostic>> diff(List<Diagnostic> expectedList, List<Diagnostic> actualList) {
        return engine.diff(expectedList, actualList);
    }

    public static List<DiffEntry<Diagnostic>> compare(List<Diagnostic> expectedList, List<Diagnostic> actualList) {
        return new DiagnosticDiffEngine().diff(expectedList, actualList);
    }

    private double calculateSimilarity(Diagnostic expected, Diagnostic actual) {
        var score = 0.0;

        // 1. Primary Label Location Match (50% Weight - Primary Identity in Source Code)
        var expectedPrimaryLabel = Label.of(expected.labels());
        var actualPrimaryLabel = Label.of(actual.labels());
        if (expectedPrimaryLabel.isPresent() && actualPrimaryLabel.isPresent()) {
            score += 0.50 * labelSimilarity(expectedPrimaryLabel.get(), actualPrimaryLabel.get());
        } else if (expectedPrimaryLabel.isEmpty() && actualPrimaryLabel.isEmpty()) {
            score += 0.50;
        }

        // 2. Message Similarity via Token Jaccard (30% Weight)
        score += 0.30 * tokenJaccard(expected.message(), actual.message());

        // 3. Diagnostic Code Match (20% Weight)
        if (expected.code() == actual.code()) {
            score += 0.20;
        }

        return score;
    }

    private List<FieldChange> extractFieldChanges(Diagnostic expected, Diagnostic actual) {
        var changes = new ArrayList<FieldChange>();

        if (expected.code() != actual.code()) {
            changes.add(new FieldChange.ModifiedField("code", expected.code(), actual.code()));
        }

        if (expected.severity() != actual.severity()) {
            changes.add(new FieldChange.ModifiedField("severity", expected.severity(), actual.severity()));
        }

        if (!expected.message().equals(actual.message())) {
            changes.add(new FieldChange.ModifiedField("message", expected.message(), actual.message()));
        }

        // Structural Diffing of Labels
        var labelDiffEntries = labelDiffEngine.diff(expected.labels(), actual.labels());
        for (var labelDiffEntry : labelDiffEntries) {
            switch (labelDiffEntry) {
                case DiffEntry.Added<Label> added ->
                        changes.add(new FieldChange.AddedField("labels[" + added.actualIndex() + "]", added.item()));
                case DiffEntry.Deleted<Label> deleted ->
                        changes.add(new FieldChange.RemovedField("labels[" + deleted.expectedIndex() + "]", deleted.item()));
                case DiffEntry.Modified<Label> modified -> {
                    for (var fieldChange : modified.fieldChanges()) {
                        var fieldNamePrefix = "labels[" + modified.expectedIndex() + "].";
                        switch (fieldChange) {
                            case FieldChange.ModifiedField(var fieldName, var expectedValue, var actualValue) ->
                                    changes.add(new FieldChange.ModifiedField(fieldNamePrefix + fieldName, expectedValue, actualValue));
                            case FieldChange.AddedField(var fieldName, var actualValue) ->
                                    changes.add(new FieldChange.AddedField(fieldNamePrefix + fieldName, actualValue));
                            case FieldChange.RemovedField(var fieldName, var expectedValue) ->
                                    changes.add(new FieldChange.RemovedField(fieldNamePrefix + fieldName, expectedValue));
                            default -> throw new IllegalStateException("Unexpected value: " + fieldChange);
                        }
                    }
                }
                case DiffEntry.Unchanged<Label> _ -> {}
                case null -> {}
            }
        }

        // Structural Diffing of Suggestions
        var suggestionDiffEntries = suggestionDiffEngine.diff(expected.suggestions(), actual.suggestions());
        for (var suggestionDiffEntry : suggestionDiffEntries) {
            switch (suggestionDiffEntry) {
                case DiffEntry.Added<String> added ->
                        changes.add(new FieldChange.AddedField("suggestions[" + added.actualIndex() + "]", added.item()));
                case DiffEntry.Deleted<String> deleted ->
                        changes.add(new FieldChange.RemovedField("suggestions[" + deleted.expectedIndex() + "]", deleted.item()));
                case DiffEntry.Modified<String> modified ->
                        changes.add(new FieldChange.ModifiedField(
                                "suggestions[" + modified.expectedIndex() + "]",
                                modified.expected(),
                                modified.actual()));
                case DiffEntry.Unchanged<String> _ -> {}
                case null -> {}
            }
        }

        return changes;
    }

    private static double labelSimilarity(Label expected, Label actual) {
        var score = 0.0;

        if (expected.isPrimary() == actual.isPrimary()) {
            score += 0.30;
        }

        var expectedSpan = expected.span();
        var actualSpan = actual.span();
        if (Objects.equals(expectedSpan, actualSpan)) {
            score += 0.40;
        } else if (expectedSpan.line() == actualSpan.line()) {
            score += 0.20;
        }

        var expectedMessage = expected.message();
        var actualMessage = actual.message();
        if (Objects.equals(expectedMessage, actualMessage)) {
            score += 0.30;
        } else if (expectedMessage != null && actualMessage != null) {
            score += 0.30 * tokenJaccard(expectedMessage, actualMessage);
        }

        return score;
    }

    private static List<FieldChange> extractLabelChanges(Label expected, Label actual) {
        var changes = new ArrayList<FieldChange>();

        if (expected.isPrimary() != actual.isPrimary()) {
            changes.add(new FieldChange.ModifiedField("isPrimary", expected.isPrimary(), actual.isPrimary()));
        }

        if (!Objects.equals(expected.span(), actual.span())) {
            var spanChanges = extractSourceSpanChanges(expected.span(), actual.span());
            for (var spanChange : spanChanges) {
                switch (spanChange) {
                    case FieldChange.ModifiedField(var fieldName, var expectedValue, var actualValue) ->
                        changes.add(new FieldChange.ModifiedField("span." + fieldName, expectedValue, actualValue));
                    case FieldChange.AddedField(var fieldName, var actualValue) ->
                        changes.add(new FieldChange.AddedField("span." + fieldName, actualValue));
                    case FieldChange.RemovedField(var fieldName, var expectedValue) ->
                        changes.add(new FieldChange.RemovedField("span." + fieldName, expectedValue));
                    case null -> {}
                }
            }
        }

        if (!Objects.equals(expected.message(), actual.message())) {
            if (expected.message() == null) {
                changes.add(new FieldChange.AddedField("message", actual.message()));
            } else if (actual.message() == null) {
                changes.add(new FieldChange.RemovedField("message", expected.message()));
            } else {
                changes.add(new FieldChange.ModifiedField("message", expected.message(), actual.message()));
            }
        }

        return changes;
    }

    private static List<FieldChange> extractSourceSpanChanges(SourceSpan expectedSpan, SourceSpan actualSpan) {
        var spanChanges = new ArrayList<FieldChange>();

        if (!expectedSpan.name().equals(actualSpan.name())) {
            spanChanges.add(new FieldChange.ModifiedField("name", expectedSpan.name().name(), actualSpan.name().name()));
        }

        if (expectedSpan.line() != actualSpan.line()) {
            spanChanges.add(new FieldChange.ModifiedField("line", expectedSpan.line(), actualSpan.line()));
        }

        if (expectedSpan.column() != actualSpan.column()) {
            spanChanges.add(new FieldChange.ModifiedField("column", expectedSpan.column(), actualSpan.column()));
        }

        if (expectedSpan.startOffset() != actualSpan.startOffset()) {
            spanChanges.add(new FieldChange.ModifiedField("startOffset", expectedSpan.startOffset(), actualSpan.startOffset()));
        }

        if (expectedSpan.length() != actualSpan.length()) {
            spanChanges.add(new FieldChange.ModifiedField("length", expectedSpan.length(), actualSpan.length()));
        }

        return spanChanges;
    }

    private static double tokenJaccard(String firstString, String secondString) {
        if (firstString.equalsIgnoreCase(secondString)) {
            return 1.0;
        }

        var firstWords = Set.of(firstString.toLowerCase().split("\\s+"));
        var secondWords = Set.of(secondString.toLowerCase().split("\\s+"));

        var intersection = new HashSet<>(firstWords);
        intersection.retainAll(secondWords);

        var union = new HashSet<>(firstWords);
        union.addAll(secondWords);

        if (union.isEmpty()) {
            return 1.0;
        }
        return (double) intersection.size() / union.size();
    }
}