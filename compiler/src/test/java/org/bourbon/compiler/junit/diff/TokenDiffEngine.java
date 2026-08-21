package org.bourbon.compiler.junit.diff;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bourbon.compiler.Token;

public final class TokenDiffEngine {

    private final StructuredDiffEngine<Token> engine;

    public TokenDiffEngine() {
        this(0.60);
    }

    public TokenDiffEngine(double cutoffThreshold) {
        this.engine = new StructuredDiffEngine<>(
                TokenDiffEngine::calculateSimilarity,
                TokenDiffEngine::extractFieldChanges,
                cutoffThreshold
        );
    }

    public List<DiffEntry<Token>> diff(List<Token> expectedList, List<Token> actualList) {
        return engine.diff(expectedList, actualList);
    }

    public static List<DiffEntry<Token>> compare(List<Token> expectedList, List<Token> actualList) {
        return new TokenDiffEngine().diff(expectedList, actualList);
    }

    private static double calculateSimilarity(Token expected, Token actual) {
        var score = 0.0;

        if (expected.type() == actual.type()) {
            score += 0.50;
        }

        score += 0.30 * jaroWinkler(expected.lexeme(), actual.lexeme());

        var lineDifference = Math.abs(expected.line() - actual.line());
        if (lineDifference == 0) {
            var columnDifference = Math.abs(expected.column() - actual.column());
            score += 0.20 * Math.max(0.0, 1.0 - (columnDifference / 20.0));
        } else if (lineDifference <= 2) {
            score += 0.10;
        }

        return score;
    }

    private static List<FieldChange> extractFieldChanges(Token expected, Token actual) {
        var changes = new ArrayList<FieldChange>();

        if (expected.type() != actual.type()) {
            changes.add(new FieldChange.ModifiedField("type", expected.type(), actual.type()));
        }

        if (!expected.lexeme().equals(actual.lexeme())) {
            changes.add(new FieldChange.ModifiedField("lexeme", expected.lexeme(), actual.lexeme()));
        }

        if (!Objects.equals(expected.literal(), actual.literal())) {
            if (expected.literal() == null) {
                changes.add(new FieldChange.AddedField("literal", actual.literal()));
            } else if (actual.literal() == null) {
                changes.add(new FieldChange.RemovedField("literal", expected.literal()));
            } else {
                changes.add(new FieldChange.ModifiedField("literal", expected.literal(), actual.literal()));
            }
        }

        if (expected.line() != actual.line() || expected.column() != actual.column()) {
            changes.add(new FieldChange.ModifiedField(
                    "position",
                    expected.line() + ":" + expected.column(),
                    actual.line() + ":" + actual.column()));
        }

        if (expected.startOffset() != actual.startOffset()) {
            changes.add(new FieldChange.ModifiedField("startOffset", expected.startOffset(), actual.startOffset()));
        }

        if (expected.length() != actual.length()) {
            changes.add(new FieldChange.ModifiedField("length", expected.length(), actual.length()));
        }

        return changes;
    }

    private static double jaroWinkler(String firstString, String secondString) {
        if (firstString.equals(secondString)) {
            return 1.0;
        }
        var jaro = jaroDistance(firstString, secondString);
        if (jaro < 0.7) {
            return jaro;
        }

        var prefixLength = 0;
        var maxPrefix = Math.min(4, Math.min(firstString.length(), secondString.length()));
        for (var characterIndex = 0; characterIndex < maxPrefix; characterIndex++) {
            if (firstString.charAt(characterIndex) == secondString.charAt(characterIndex)) {
                prefixLength++;
            } else {
                break;
            }
        }

        return jaro + (prefixLength * 0.1 * (1.0 - jaro));
    }

    private static double jaroDistance(String firstString, String secondString) {
        var firstLength = firstString.length();
        var secondLength = secondString.length();
        if (firstLength == 0 || secondLength == 0) {
            return 0.0;
        }

        var matchDistance = Math.max(firstLength, secondLength) / 2 - 1;
        var firstMatches = new boolean[firstLength];
        var secondMatches = new boolean[secondLength];

        var matches = 0;
        for (var firstIndex = 0; firstIndex < firstLength; firstIndex++) {
            var searchStart = Math.max(0, firstIndex - matchDistance);
            var searchEnd = Math.min(firstIndex + matchDistance + 1, secondLength);

            for (var secondIndex = searchStart; secondIndex < searchEnd; secondIndex++) {
                if (secondMatches[secondIndex] || firstString.charAt(firstIndex) != secondString.charAt(secondIndex)) {
                    continue;
                }
                firstMatches[firstIndex] = true;
                secondMatches[secondIndex] = true;
                matches++;
                break;
            }
        }
        if (matches == 0) {
            return 0.0;
        }

        var transpositions = 0;
        var secondMatchIndex = 0;
        for (var firstIndex = 0; firstIndex < firstLength; firstIndex++) {
            if (!firstMatches[firstIndex]) {
                continue;
            }
            while (!secondMatches[secondMatchIndex]) {
                secondMatchIndex++;
            }
            if (firstString.charAt(firstIndex) != secondString.charAt(secondMatchIndex)) {
                transpositions++;
            }
            secondMatchIndex++;
        }

        return ((double) matches / firstLength
                + (double) matches / secondLength
                + (matches - transpositions / 2.0) / matches) / 3.0;
    }
}