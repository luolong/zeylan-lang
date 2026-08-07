package org.bourbon.compiler.junit;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.function.Executable;
import org.bourbon.compiler.Label;

@NullMarked
public final class ListAssert<T> {

    private final List<T> expected;
    private final List<T> actual;
    private BiPredicate<T, T> semanticEquality;

    private ListAssert(List<T> expected, List<T> actual) {
        this.expected = expected;
        this.actual = actual;
        this.semanticEquality = Objects::equals;
    }

    public static <T> ListAssert<T> assertLists(List<T> expected, List<T> actual) {
        return new ListAssert<>(expected, actual);
    }

    @SafeVarargs
    public final ListAssert<T> withSemanticEquality(Function<T, ?>... keyExtractors) {
        this.semanticEquality = (t1, t2) -> {
            for (var extractor : keyExtractors) {
                if (!Objects.equals(extractor.apply(t1), extractor.apply(t2))) {
                    return false;
                }
            }
            return true;
        };
        return this;
    }

    public ListAssert<T> withSemanticEquality(BiPredicate<T, T> semanticEquality) {
        this.semanticEquality = semanticEquality;
        return this;
    }

    // Difference Model
    public sealed interface Difference<T> {
        record Addition<T>(int actualIndex, T actual) implements Difference<T> {}
        record Deletion<T>(int expectedIndex, T expected) implements Difference<T> {}
        record Mismatch<T>(int expectedIndex, int actualIndex, T expected, T actual, List<FieldDifference> fields) implements Difference<T> {}
    }

    public record FieldDifference(String fieldName, @Nullable Object expected, @Nullable Object actual, @Nullable String details) {}

    // Main Assertion Logic
    public Stream<Executable> stream() {
        return Stream.of(() -> {
            List<Difference<T>> differences = calculateDifferences();
            if (!differences.isEmpty()) {
                fail(formatDifferences(differences));
            }
        });
    }

    // Difference Calculation
    private List<Difference<T>> calculateDifferences() {
        List<AlignmentStep<T>> steps = alignSequences(expected, actual, semanticEquality);
        var differences = new ArrayList<Difference<T>>();

        for (AlignmentStep<T> step : steps) {
            switch (step.op) {
                case MATCH -> {
                    T exp = Objects.requireNonNull(step.expected);
                    T act = Objects.requireNonNull(step.actual);
                    if (!exp.equals(act)) {
                        List<FieldDifference> fieldDiffs = structuralDiff(exp, act);
                        differences.add(new Difference.Mismatch<>(step.expectedIndex, step.actualIndex, exp, act, fieldDiffs));
                    }
                }
                case DELETE -> differences.add(new Difference.Deletion<>(step.expectedIndex, Objects.requireNonNull(step.expected)));
                case INSERT -> differences.add(new Difference.Addition<>(step.actualIndex, Objects.requireNonNull(step.actual)));
            }
        }
        return Collections.unmodifiableList(differences);
    }

    private List<FieldDifference> structuralDiff(Object expected, Object actual) {
        if (!expected.getClass().isRecord() || !actual.getClass().isRecord()) {
            return Collections.emptyList();
        }
        var fieldDifferences = new ArrayList<FieldDifference>();
        try {
            RecordComponent[] components = expected.getClass().getRecordComponents();
            for (RecordComponent component : components) {
                Object expectedValue = component.getAccessor().invoke(expected);
                Object actualValue = component.getAccessor().invoke(actual);
                if (!Objects.equals(expectedValue, actualValue)) {
                    // 1. Special Case: List<Label>
                    if (expectedValue instanceof List<?> expList && actualValue instanceof List<?> actList) {
                        if ((!expList.isEmpty() && expList.getFirst() instanceof Label) || (!actList.isEmpty() && actList.getFirst() instanceof Label)) {
                            @SuppressWarnings("unchecked")
                            String nestedDiff = diffLabels((List<Label>) expList, (List<Label>) actList);
                            fieldDifferences.add(new FieldDifference(component.getName(), null, null, nestedDiff));
                            continue;
                        }
                    }

                    // 2. Special Case: Nested Record (like SourceSpan)
                    if (expectedValue != null && expectedValue.getClass().isRecord() && actualValue != null && actualValue.getClass().isRecord()) {
                        List<FieldDifference> nestedDiffs = structuralDiff(expectedValue, actualValue);
                        StringJoiner sj = new StringJoiner("\n", "\n", "");
                        for (var nd : nestedDiffs) {
                            if (nd.details() != null) {
                                sj.add("- " + nd.fieldName() + ":" + indent(nd.details(), 2));
                            } else {
                                sj.add(String.format("- %s: expected <%s>, but got <%s>", nd.fieldName(), nd.expected(), nd.actual()));
                            }
                        }
                        fieldDifferences.add(new FieldDifference(component.getName(), null, null, indent(sj.toString(), 2)));
                        continue;
                    }

                    fieldDifferences.add(new FieldDifference(component.getName(), expectedValue, actualValue, null));
                }
            }
        } catch (Exception e) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(fieldDifferences);
    }

    private String diffLabels(List<Label> expectedLabels, List<Label> actualLabels) {
        BiPredicate<Label, Label> labelSemanticEquality = (l1, l2) -> {
            if (!Objects.equals(l1.span().name(), l2.span().name())) {
                return false;
            }
            return Objects.equals(l1.message(), l2.message());
        };

        List<AlignmentStep<Label>> steps = alignSequences(expectedLabels, actualLabels, labelSemanticEquality);
        var joiner = new StringJoiner("\n", "\n", "");

        for (var step : steps) {
            switch (step.op) {
                case MATCH -> {
                    Label exp = Objects.requireNonNull(step.expected);
                    Label act = Objects.requireNonNull(step.actual);
                    if (!exp.equals(act)) {
                        List<FieldDifference> fieldDiffs = structuralDiff(exp, act);
                        var labelJoiner = new StringJoiner("\n", "\n", "");
                        for (var fd : fieldDiffs) {
                            if (fd.details() != null) {
                                labelJoiner.add("- " + fd.fieldName() + ":" + indent(fd.details(), 2));
                            } else {
                                labelJoiner.add(String.format("- %s: expected <%s>, but got <%s>", fd.fieldName(), fd.expected(), fd.actual()));
                            }
                        }
                        joiner.add(String.format("[?] Label mismatch (%s):%s", formatLabelIdentifier(exp), indent(labelJoiner.toString(), 2)));
                    } else {
                        joiner.add(String.format("[ ] Label (%s) (OK)", formatLabelIdentifier(act)));
                    }
                }
                case DELETE -> joiner.add(String.format("[-] Missing expected label: <%s>", step.expected));
                case INSERT -> joiner.add(String.format("[+] Unexpected extra label: <%s>", step.actual));
            }
        }
        return joiner.toString();
    }

    private String formatLabelIdentifier(Label label) {
        String filename = label.span().name().toString();
        String message = label.message() != null ? "\"" + label.message() + "\"" : "<no message>";
        return String.format("File: %s, Msg: %s", filename, message);
    }

    private static String indent(String text, int spaces) {
        String indent = " ".repeat(spaces);
        return text.replace("\n", "\n" + indent);
    }


    // Sequence Alignment (LCS)
    private enum Op { MATCH, INSERT, DELETE }

    private record AlignmentStep<E>(Op op, @Nullable E expected, @Nullable E actual, int expectedIndex, int actualIndex) {}

    private <E> List<AlignmentStep<E>> alignSequences(List<E> expList, List<E> actList, BiPredicate<E, E> eq) {
        int n = expList.size();
        int m = actList.size();
        int[][] dp = new int[n + 1][m + 1];

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (eq.test(expList.get(i - 1), actList.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        var steps = new ArrayList<AlignmentStep<E>>();
        int i = n, j = m;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && eq.test(expList.get(i - 1), actList.get(j - 1))) {
                steps.add(new AlignmentStep<>(Op.MATCH, expList.get(i - 1), actList.get(j - 1), i - 1, j - 1));
                i--; j--;
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                steps.add(new AlignmentStep<>(Op.INSERT, null, actList.get(j - 1), -1, j - 1));
                j--;
            } else if (i > 0 && (j == 0 || dp[i - 1][j] > dp[i][j - 1])) {
                steps.add(new AlignmentStep<>(Op.DELETE, expList.get(i - 1), null, i - 1, -1));
                i--;
            } else {
                break;
            }
        }
        Collections.reverse(steps);
        return steps;
    }


    // Formatting
    private String formatDifferences(List<Difference<T>> differences) {
        var joiner = new StringJoiner("\n", "List differences found:\n", "");
        for (var diff : differences) {
            switch (diff) {
                case Difference.Addition<T> a ->
                    joiner.add(String.format("[+] (Index A:%d) Unexpected item: <%s>", a.actualIndex(), a.actual()));
                case Difference.Deletion<T> d ->
                    joiner.add(String.format("[-] (Index E:%d) Missing item: <%s>", d.expectedIndex(), d.expected()));
                case Difference.Mismatch<T> m -> {
                    var fieldJoiner = new StringJoiner("\n", "\n", "");
                    if (m.fields().isEmpty()) {
                        fieldJoiner.add(String.format("Expected: <%s>", m.expected()));
                        fieldJoiner.add(String.format("Actual:   <%s>", m.actual()));
                    } else {
                        for (var field : m.fields()) {
                            if (field.details() != null) {
                                fieldJoiner.add("- " + field.fieldName() + ":" + indent(field.details(), 2));
                            } else {
                                fieldJoiner.add(String.format("- %s: expected <%s>, but got <%s>", field.fieldName(), field.expected(), field.actual()));
                            }
                        }
                    }
                    joiner.add(String.format("[?] (Index E:%d, A:%d) Item mismatch:%s", m.expectedIndex(), m.actualIndex(), indent(fieldJoiner.toString(), 4)));
                }
            }
        }
        return joiner.toString();
    }
}
