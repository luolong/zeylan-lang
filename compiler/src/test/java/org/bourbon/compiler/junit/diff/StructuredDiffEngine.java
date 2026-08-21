package org.bourbon.compiler.junit.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class StructuredDiffEngine<T> {

    @FunctionalInterface
    public interface SimilarityCalculator<T> {
        double calculate(T expected, T actual);
    }

    @FunctionalInterface
    public interface FieldDiffExtractor<T> {
        List<FieldChange> extract(T expected, T actual);
    }

    private final SimilarityCalculator<T> similarityCalculator;
    private final FieldDiffExtractor<T> fieldDiffExtractor;
    private final double cutoffThreshold;

    public StructuredDiffEngine(
            SimilarityCalculator<T> similarityCalculator,
            FieldDiffExtractor<T> fieldDiffExtractor,
            double cutoffThreshold) {
        this.similarityCalculator = similarityCalculator;
        this.fieldDiffExtractor = fieldDiffExtractor;
        this.cutoffThreshold = cutoffThreshold;
    }

    private double computeSimilarity(T expected, T actual) {
        if (Objects.deepEquals(expected, actual)) {
            return 1.0;
        }
        return similarityCalculator.calculate(expected, actual);
    }

    public List<DiffEntry<T>> diff(List<T> expectedList, List<T> actualList) {
        var expectedSize = expectedList.size();
        var actualSize = actualList.size();

        // 1. Build Dynamic Programming Edit Cost Matrix
        var costMatrix = new double[expectedSize + 1][actualSize + 1];

        for (var expectedIndex = 1; expectedIndex <= expectedSize; expectedIndex++) {
            costMatrix[expectedIndex][0] = expectedIndex * 1.0;
        }
        for (var actualIndex = 1; actualIndex <= actualSize; actualIndex++) {
            costMatrix[0][actualIndex] = actualIndex * 1.0;
        }

        for (var expectedIndex = 1; expectedIndex <= expectedSize; expectedIndex++) {
            for (var actualIndex = 1; actualIndex <= actualSize; actualIndex++) {
                var costDeletion = costMatrix[expectedIndex - 1][actualIndex] + 1.0;
                var costInsertion = costMatrix[expectedIndex][actualIndex - 1] + 1.0;

                var expectedItem = expectedList.get(expectedIndex - 1);
                var actualItem = actualList.get(actualIndex - 1);
                var similarity = computeSimilarity(expectedItem, actualItem);

                var isSinglePairFallback = (expectedSize == 1 && actualSize == 1 && similarity > 0.0);
                var costModification = (similarity >= cutoffThreshold || isSinglePairFallback)
                        ? costMatrix[expectedIndex - 1][actualIndex - 1] + (2.0 * (1.0 - similarity))
                        : Double.POSITIVE_INFINITY;

                costMatrix[expectedIndex][actualIndex] = Math.min(
                        costDeletion, Math.min(costInsertion, costModification));
            }
        }

        // 2. Backtrack Matrix to Construct Diff Entries
        var entries = new ArrayList<DiffEntry<T>>();
        var expectedIndex = expectedSize;
        var actualIndex = actualSize;

        while (expectedIndex > 0 || actualIndex > 0) {
            if (expectedIndex > 0 && actualIndex > 0) {
                var expected = expectedList.get(expectedIndex - 1);
                var actual = actualList.get(actualIndex - 1);
                var similarity = computeSimilarity(expected, actual);

                var isSinglePairFallback = (expectedSize == 1 && actualSize == 1 && similarity > 0.0);
                var costModification = (similarity >= cutoffThreshold || isSinglePairFallback)
                        ? costMatrix[expectedIndex - 1][actualIndex - 1] + (2.0 * (1.0 - similarity))
                        : Double.POSITIVE_INFINITY;

                if (costMatrix[expectedIndex][actualIndex] == costModification) {
                    var fieldChanges = fieldDiffExtractor.extract(expected, actual);
                    if (fieldChanges.isEmpty()) {
                        entries.add(new DiffEntry.Unchanged<>(expectedIndex - 1, actualIndex - 1, actual));
                    } else {
                        entries.add(new DiffEntry.Modified<>(
                                expectedIndex - 1, actualIndex - 1, expected, actual, similarity, fieldChanges));
                    }
                    expectedIndex--;
                    actualIndex--;
                    continue;
                }
            }

            if (actualIndex > 0 && costMatrix[expectedIndex][actualIndex] == costMatrix[expectedIndex][actualIndex - 1] + 1.0) {
                entries.add(new DiffEntry.Added<>(actualIndex - 1, actualList.get(actualIndex - 1)));
                actualIndex--;
            } else if (expectedIndex > 0 && costMatrix[expectedIndex][actualIndex] == costMatrix[expectedIndex - 1][actualIndex] + 1.0) {
                entries.add(new DiffEntry.Deleted<>(expectedIndex - 1, expectedList.get(expectedIndex - 1)));
                expectedIndex--;
            }
        }

        Collections.reverse(entries);
        return entries;
    }
}